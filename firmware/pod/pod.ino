// Simon Says Showdown - pod firmware (LilyPad Arduino 328)
// One sketch for both pods: only POD changes.
#include <SoftwareSerial.h>

const char POD[] = "P1";               // "P2" on pod 2

// ===== Pod pin map (identical on P1 and P2) =====
// Hardware Serial RX (D0) <- Parallax RFID SOUT, 2400 baud
const uint8_t XBEE_RX    = 2;   // from LilyPad XBee DOUT
const uint8_t XBEE_TX    = 3;   // to XBee DIN, through the divider
const uint8_t BTN_RED    = 4;
const uint8_t LED_R      = 5;
const uint8_t LED_G      = 6;
const uint8_t US_TRIG    = 7;
const uint8_t US_ECHO    = 8;
const uint8_t LED_B      = 9;
const uint8_t BUZ        = 10;
const uint8_t RFID_EN    = 11;  // LOW = reader on
const uint8_t BTN_GREEN  = 12;
const uint8_t JOY_X      = A0;
const uint8_t JOY_Y      = A1;
const uint8_t TILT_X     = A2;
const uint8_t TILT_Y     = A3;
const uint8_t BTN_BLUE   = A4;  // A4, A5 used as digital inputs
const uint8_t BTN_YELLOW = A5;
// D13 free (on-board LED)

const uint8_t BTN_PINS[4]  = {BTN_RED, BTN_GREEN, BTN_BLUE, BTN_YELLOW};
const char*   BTN_NAMES[4] = {"RED", "GREEN", "BLUE", "YELLOW"};

// ===== Tuning: set per pod from the firmware/tests readings =====
const int   JOY_DEAD   = 350;          // counts from centre before a direction counts
const float TILT_DEG   = 30.0;         // degrees before a tilt counts
float       countsPerG = 68.0;         // measured with t6_tilt
const int   ZONE_LOW[2]  = {5, 15};    // cm
const int   ZONE_MID[2]  = {18, 28};
const int   ZONE_HIGH[2] = {32, 45};
const unsigned long ZONE_HOLD  = 300;  // ms a hand must stay in a zone
const unsigned long DOUBLE_GAP = 400;  // ms window for a double press, per button
const unsigned long TAG_REPEAT = 1500; // ms to ignore the same tag

SoftwareSerial xbee(XBEE_RX, XBEE_TX);

// ===== Round state =====
bool listening = false;
unsigned long goTime = 0, limitMs = 0;
byte actionsLeft = 0;                      // 1 for GO, n for SEQ
unsigned long ledOffAt = 0;
int joyX0 = 512, joyY0 = 512, tiltX0 = 338, tiltY0 = 338;

// ---------- sending ----------
void sendLine(const char* body) {          // <P1,body>
  xbee.print('<'); xbee.print(POD); xbee.print(',');
  xbee.print(body); xbee.print(">\n");
}

void report(const char* type, const char* value, unsigned long when) {
  if (!listening) return;                  // not in a round
  if (when < goTime) return;               // started before GO
  xbee.print('<'); xbee.print(POD); xbee.print(",ACT,");
  xbee.print(type); xbee.print(','); xbee.print(value); xbee.print(',');
  xbee.print(when - goTime); xbee.print(">\n");
  if (--actionsLeft == 0) listening = false;   // memory rounds report several
}

// ---------- feedback ----------
void setLed(bool r, bool g, bool b) {
  digitalWrite(LED_R, r); digitalWrite(LED_G, g); digitalWrite(LED_B, b);
}

void feedback(char c, unsigned long ms) {
  if (c == 'G')      { setLed(0, 1, 0); tone(BUZ, 1800, 150); }   // correct
  else if (c == 'R') { setLed(1, 0, 0); tone(BUZ, 300, 300); }    // wrong
  else if (c == 'B') { setLed(0, 0, 1); tone(BUZ, 1200, 80); }  // joined the game
  else return;
  ledOffAt = millis() + ms;
}

// ---------- sensor helpers ----------
int readAvg(uint8_t pin) {                 // 1 throwaway read after switching channel
  analogRead(pin);
  int s = 0;
  for (byte i = 0; i < 4; i++) s += analogRead(pin);
  return s / 4;
}

float toDeg(int counts) {
  return asin(constrain(counts / countsPerG, -1.0, 1.0)) * 57.2958;
}

void calibrate() {                         // hold controllers flat, hands off
  long jx = 0, jy = 0, tx = 0, ty = 0;
  for (byte i = 0; i < 16; i++) {
    jx += readAvg(JOY_X);  jy += readAvg(JOY_Y);
    tx += readAvg(TILT_X); ty += readAvg(TILT_Y);
    delay(5);                              // the only delay(): 80 ms, at CAL
  }
  joyX0 = jx / 16; joyY0 = jy / 16; tiltX0 = tx / 16; tiltY0 = ty / 16;
}

int readCm() {
  digitalWrite(US_TRIG, LOW);  delayMicroseconds(2);
  digitalWrite(US_TRIG, HIGH); delayMicroseconds(10);
  digitalWrite(US_TRIG, LOW);
  unsigned long us = pulseIn(US_ECHO, HIGH, 5000);   // never blocks > 5 ms
  return us ? (int)(us / 58) : -1;
}

const char* zoneOf(int cm) {
  if (cm >= ZONE_LOW[0]  && cm <= ZONE_LOW[1])  return "LOW";
  if (cm >= ZONE_MID[0]  && cm <= ZONE_MID[1])  return "MID";
  if (cm >= ZONE_HIGH[0] && cm <= ZONE_HIGH[1]) return "HIGH";
  return 0;
}

// ---------- sensors: each tracks its own state ----------
void checkJoystick() {
  static char last = 0;
  int dx = readAvg(JOY_X) - joyX0, dy = readAvg(JOY_Y) - joyY0;
  char d = 0;
  if (abs(dx) >= JOY_DEAD || abs(dy) >= JOY_DEAD)
    d = abs(dx) > abs(dy) ? (dx > 0 ? 'R' : 'L') : (dy > 0 ? 'D' : 'U');   // swap if t4_joystick shows an axis reversed
  else if (last && (abs(dx) > JOY_DEAD - 80 || abs(dy) > JOY_DEAD - 80))
    d = last;                                                              // hysteresis
  if (d && d != last)
    report("JOY", d == 'U' ? "UP" : d == 'D' ? "DOWN" : d == 'L' ? "LEFT" : "RIGHT", millis());
  last = d;
}

void checkTilt() {
  static char last = 0;
  float r = toDeg(readAvg(TILT_X) - tiltX0);
  float p = toDeg(readAvg(TILT_Y) - tiltY0);
  char d = 0;
  if (fabs(r) > TILT_DEG || fabs(p) > TILT_DEG)
    d = fabs(r) > fabs(p) ? (r > 0 ? 'R' : 'L') : (p > 0 ? 'F' : 'B');    // swap if t6_tilt shows an axis reversed
  else if (last && (fabs(r) > TILT_DEG - 8 || fabs(p) > TILT_DEG - 8))
    d = last;                                                              // hysteresis
  if (d && d != last) { char v[2] = {d, 0}; report("TILT", v, millis()); }
  last = d;
}

void checkButtons() {                      // 4 coloured buttons, each with its own timing
  static bool last[4] = {HIGH, HIGH, HIGH, HIGH};
  static unsigned long changedAt[4], firstPress[4];
  static byte presses[4];
  for (byte i = 0; i < 4; i++) {
    bool now = digitalRead(BTN_PINS[i]);
    if (now != last[i] && millis() - changedAt[i] > 25) {   // debounced edge
      changedAt[i] = millis(); last[i] = now;
      if (now == LOW) { if (presses[i] == 0) firstPress[i] = millis(); presses[i]++; }
    }
    if (presses[i] && millis() - firstPress[i] > DOUBLE_GAP) {   // window closed
      char v[10];
      snprintf(v, sizeof v, "%s_%d", BTN_NAMES[i], presses[i] >= 2 ? 2 : 1);   // "RED_1", "YELLOW_2"
      report("BTN", v, firstPress[i]);
      presses[i] = 0;
    }
  }
}

void checkDistance() {
  static const char* zone = 0;
  static unsigned long zoneSince = 0;
  static bool sent = false;
  const char* z = zoneOf(readCm());
  if (z != zone) { zone = z; zoneSince = millis(); sent = false; }
  else if (zone && !sent && millis() - zoneSince >= ZONE_HOLD) {
    report("DIST", zone, zoneSince);                   // timed from entering the zone
    sent = true;
  }
}

void checkRFID() {                         // Parallax frame: 0x0A + 10 chars + 0x0D
  static char id[11];
  static byte len = 0;
  static bool inTag = false;
  static char lastId[11] = "";
  static unsigned long lastAt = 0;
  while (Serial.available()) {
    char c = Serial.read();
    if (c == 0x0A) { inTag = true; len = 0; }
    else if (c == 0x0D && inTag) {
      inTag = false;
      if (len != 10) continue;
      id[10] = 0;
      unsigned long now = millis();
      bool repeat = strcmp(id, lastId) == 0 && now - lastAt < TAG_REPEAT;
      lastAt = now;                        // a held tag keeps extending the window
      if (repeat) continue;
      strcpy(lastId, id);
      if (listening) report("RFID", id, now);
      else { char body[16]; snprintf(body, sizeof body, "TAG,%s", id); sendLine(body); }
    }
    else if (inTag && len < 10) id[len++] = c;
  }
}

// ---------- messages from the laptop ----------
void startListening(byte n, unsigned long limit) {
  goTime = millis(); limitMs = limit; actionsLeft = n; listening = true;
}

void handle(char* msg) {                   // msg = text between < and >
  char* target = strtok(msg, ",");
  char* cmd    = strtok(NULL, ",");
  char* a1     = strtok(NULL, ",");
  char* a2     = strtok(NULL, ",");
  if (!target || !cmd) return;
  if (strcmp(target, "ALL") != 0 && strcmp(target, POD) != 0) return;   // not for us
  if      (!strcmp(cmd, "GO")   && a1)       startListening(1, atol(a1));
  else if (!strcmp(cmd, "SEQ")  && a1 && a2) startListening(atoi(a1), atol(a2));
  else if (!strcmp(cmd, "STOP"))             listening = false;
  else if (!strcmp(cmd, "CAL"))              calibrate();
  else if (!strcmp(cmd, "FB")   && a1)       feedback(a1[0], 600);
  else if (!strcmp(cmd, "BEEP"))             tone(BUZ, 1000, 80);
}

void readXBee() {
  static char buf[32];
  static byte len = 0;
  static bool inMsg = false;
  while (xbee.available()) {
    char c = xbee.read();
    if (c == '<') { inMsg = true; len = 0; }
    else if (c == '>' && inMsg) { buf[len] = 0; inMsg = false; handle(buf); }
    else if (inMsg && len < sizeof(buf) - 1) buf[len++] = c;
    else inMsg = false;                    // too long or stray byte: drop it
  }
}

// ---------- main ----------
void setup() {
  Serial.begin(2400);                      // Parallax RFID
  xbee.begin(9600);
  for (byte i = 0; i < 4; i++) pinMode(BTN_PINS[i], INPUT_PULLUP);
  pinMode(LED_R, OUTPUT); pinMode(LED_G, OUTPUT); pinMode(LED_B, OUTPUT);
  pinMode(BUZ, OUTPUT);
  pinMode(US_TRIG, OUTPUT); pinMode(US_ECHO, INPUT);
  pinMode(RFID_EN, OUTPUT); digitalWrite(RFID_EN, LOW);   // reader on
  calibrate();                             // controllers flat at power-up
  feedback('G', 500);                      // "I'm alive"
  sendLine("DBG,boot");
}

void loop() {
  static unsigned long lastFast = 0, lastDist = 0, lastHB = 0;
  readXBee();                              // first, so a GO is timed at once
  checkRFID();
  checkButtons();
  unsigned long now = millis();
  if (now - lastFast >= 20) { lastFast = now; checkJoystick(); checkTilt(); }
  if (now - lastDist >= 60) { lastDist = now; checkDistance(); }
  now = millis();
  if (listening && now - goTime > limitMs) { listening = false; sendLine("TIMEOUT"); }
  if (ledOffAt && (long)(now - ledOffAt) >= 0) { setLed(0, 0, 0); ledOffAt = 0; }
  if (now - lastHB >= 500) { lastHB = now; sendLine("HB"); }
}
