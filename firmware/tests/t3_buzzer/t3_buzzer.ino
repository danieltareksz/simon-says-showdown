// Module test 3: passive buzzer on D10
// Pass: a high beep, then a low beep. One flat buzz for both = active buzzer.

const uint8_t BUZ = 10;

void setup() {
  pinMode(BUZ, OUTPUT);
}

void loop() {
  tone(BUZ, 1800, 150);
  delay(400);
  tone(BUZ, 300, 300);
  delay(1200);
}
