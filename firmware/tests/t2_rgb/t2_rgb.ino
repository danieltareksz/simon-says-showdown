// Module test 2: RGB LED (R = D5, G = D6, B = D9, common cathode to GND)
// Pass: red, green, blue, off, repeating. Inverted colours = common-anode LED.

const uint8_t LED_R = 5, LED_G = 6, LED_B = 9;

void show(bool r, bool g, bool b) {
  digitalWrite(LED_R, r); digitalWrite(LED_G, g); digitalWrite(LED_B, b);
  delay(700);
}

void setup() {
  pinMode(LED_R, OUTPUT); pinMode(LED_G, OUTPUT); pinMode(LED_B, OUTPUT);
}

void loop() {
  show(1, 0, 0);
  show(0, 1, 0);
  show(0, 0, 1);
  show(0, 0, 0);
}
