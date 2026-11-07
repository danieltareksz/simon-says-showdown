// Module test 5: HC-SR04 (TRIG = D7, ECHO = D8)
// Pass: a flat hand at 10, 20 and 40 cm reads within +-2 cm; nothing in range reads -1.

const uint8_t US_TRIG = 7, US_ECHO = 8;

void setup() {
  Serial.begin(9600);
  pinMode(US_TRIG, OUTPUT);
  pinMode(US_ECHO, INPUT);
}

void loop() {
  digitalWrite(US_TRIG, LOW);  delayMicroseconds(2);
  digitalWrite(US_TRIG, HIGH); delayMicroseconds(10);
  digitalWrite(US_TRIG, LOW);
  unsigned long us = pulseIn(US_ECHO, HIGH, 5000);   // 5 ms cap, about 85 cm
  Serial.println(us == 0 ? -1 : (int)(us / 58));     // cm
  delay(60);
}
