// Simon Says Showdown - pod firmware (LilyPad Arduino 328)
// One sketch for both pods: only POD_NUMBER changes. Each pod has its own tuning block below,
// because the joystick and the tilt controller sit differently in each box.
#include <SoftwareSerial.h>

#define POD_NUMBER 2                   // 1 or 2: set before uploading to each pod

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

// ===== Tuning per pod, from the firmware/tests readings =====
// Joystick: after the swap and flips, "h" > 0 must mean RIGHT and "v" > 0 must mean DOWN.
// Tilt: TILT_FP holds a "fingerprint" per direction (forward, back, left, right): the change
// from the flat reading on A2 and A3 at the point where that tilt should count. Measured with
// t6_tilt, so it also covers a sensor mounted at an angle or with unequal axes.
#if POD_NUMBER == 1
const char  POD[]       = "P1";
const int   JOY_DEAD    = 300;         // full push reads about 500 from centre on this stick
const bool  JOY_SWAP    = true;        // t4: A0 moves with up/down, A1 with left/right
const bool  JOY_FLIP_H  = true;        // t4: pushing right lowers A1
const bool  JOY_FLIP_V  = false;       // t4: pushing down raises A0
// t6: A2 rises tilting forward, A3 rises tilting left; 34 counts is about 30 degrees
const int   TILT_FP[4][2] = {{34, 0}, {-34, 0}, {0, 34}, {0, -34}};
const int   ZONE_LOW[2]  = {2, 8};     // cm; t5: hand low reads 3-6
const int   ZONE_MID[2]  = {10, 17};   // t5: hand in the middle reads 11-14
const int   ZONE_HIGH[2] = {20, 30};   // t5: hand high reads 22-24
#else
const char  POD[]       = "P2";
const int   JOY_DEAD    = 300;         // full push reads about 500 from centre on this stick
const bool  JOY_SWAP    = true;        // t4: A0 moves with up/down, A1 with left/right
const bool  JOY_FLIP_H  = false;       // t4: pushing right raises A1
const bool  JOY_FLIP_V  = true;        // t4: pushing down lowers A0
// t6 deliberate tilts read F +60,+46  B -68,-51  L -56,+2  R +60,-3; a tilt counts at 60% of those
const int   TILT_FP[4][2] = {{36, 28}, {-41, -31}, {-34, 1}, {36, -2}};
const int   ZONE_LOW[2]  = {2, 9};     // cm; t5: hand low reads 4-7
const int   ZONE_MID[2]  = {11, 20};   // t5: hand in the middle reads 12-14
const int   ZONE_HIGH[2] = {23, 35};   // t5: hand high reads 26-27
#endif

// Same on both pods
const char  TILT_DIRS[4] = {'F', 'B', 'L', 'R'};   // order of the TILT_FP rows
const float TILT_KEEP  = 0.75;         // share of its fingerprint a reported tilt must keep (hysteresis)
const unsigned long ZONE_HOLD  = 500;  // ms a hand must stay in a zone; passing through doesn't count
const byte ZONE_MISSES = 2;            // stray readings in a row ignored before a hand counts as moved
const unsigned long TILT_HOLD  = 150;  // ms a tilt must last, so shakes and bumps don't count
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
  int h = JOY_SWAP ? dy : dx, v = JOY_SWAP ? dx : dy;     // turn raw axes into right/down for this pod
  if (JOY_FLIP_H) h = -h;
  if (JOY_FLIP_V) v = -v;
  char d = 0;
  if (abs(h) >= JOY_DEAD || abs(v) >= JOY_DEAD)
    d = abs(h) > abs(v) ? (h > 0 ? 'R' : 'L') : (v > 0 ? 'D' : 'U');
  else if (last && (abs(h) > JOY_DEAD - 80 || abs(v) > JOY_DEAD - 80))
    d = last;                                                              // hysteresis
  if (d && d != last)
    report("JOY", d == 'U' ? "UP" : d == 'D' ? "DOWN" : d == 'L' ? "LEFT" : "RIGHT", millis());
  last = d;
}

void checkTilt() {
  static char last = 0;                    // direction last reported, or 0 back at level
  static char held = 0;                    // direction seen now, waiting out TILT_HOLD
  static unsigned long heldSince = 0;
  float dx = readAvg(TILT_X) - tiltX0, dy = readAvg(TILT_Y) - tiltY0;
  float len = sqrt(dx * dx + dy * dy);
  char d = 0;
  if (len > 0) {
    // the fingerprint pointing the same way as this reading (largest cosine) is the direction
    byte best = 0;
    float bestCos = -2, bestShare = 0;
    for (byte i = 0; i < 4; i++) {
      float fx = TILT_FP[i][0], fy = TILT_FP[i][1];
      float dot = dx * fx + dy * fy, fl2 = fx * fx + fy * fy;
      float c = dot / (len * sqrt(fl2));
      if (c > bestCos) { bestCos = c; best = i; bestShare = dot / fl2; }   // share of the fingerprint reached
    }
    if (bestShare >= 1.0) d = TILT_DIRS[best];
    else if (last == TILT_DIRS[best] && bestShare >= TILT_KEEP) d = last;  // hysteresis
  }
  if (d != held) { held = d; heldSince = millis(); }
  if (!d) last = 0;
  else if (d != last && millis() - heldSince >= TILT_HOLD) {
    char v[2] = {d, 0};
    report("TILT", v, heldSince);          // timed from the start of the tilt, like the hand zones
    last = d;
  }
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
  static const char* zone = 0;             // zone the hand is held in, or 0
  static unsigned long zoneSince = 0;
  static byte misses = 0;                  // readings in a row that disagree with zone
  static unsigned long missSince = 0;      // when the first of them came
  static bool sent = false;
  const char* z = zoneOf(readCm());
  unsigned long now = millis();
  if (z == zone) misses = 0;
  else {
    if (misses == 0) missSince = now;
    if (!zone || ++misses > ZONE_MISSES) {   // a stray echo or two doesn't restart the hold
      zone = z; zoneSince = missSince; sent = false; misses = 0;   // timed from its first reading
    }
  }
  if (zone && !sent && now - zoneSince >= ZONE_HOLD) {
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
