// Module test 1: the four coloured buttons (D4, D12, A4, A5)
// Pass: each press prints its colour once; two buttons together print both.
// Serial Monitor at 9600. Unplug the RFID SOUT wire first.

const uint8_t BTN_PINS[4]  = {4, 12, A4, A5};
const char*   BTN_NAMES[4] = {"RED", "GREEN", "BLUE", "YELLOW"};

void setup() {
  Serial.begin(9600);
  for (byte i = 0; i < 4; i++) pinMode(BTN_PINS[i], INPUT_PULLUP);
  Serial.println("t1_buttons ready");
}

void loop() {
  static bool last[4] = {HIGH, HIGH, HIGH, HIGH};
  static unsigned long changedAt[4];
  for (byte i = 0; i < 4; i++) {
    bool now = digitalRead(BTN_PINS[i]);
    if (now != last[i] && millis() - changedAt[i] > 25) {   // 25 ms debounce
      changedAt[i] = millis();
      last[i] = now;
      if (now == LOW) Serial.println(BTN_NAMES[i]);
    }
  }
}
