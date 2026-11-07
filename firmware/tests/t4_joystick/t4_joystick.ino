// Module test 4: joystick (VRx = A0, VRy = A1)
// Pass: both axes rest at about 480-540; extremes go below 100 and above 920.
// Note which value means "up" as the stick sits on the pod.

const uint8_t JOY_X = A0, JOY_Y = A1;

void setup() {
  Serial.begin(9600);
}

void loop() {
  Serial.print(analogRead(JOY_X));
  Serial.print('\t');
  Serial.println(analogRead(JOY_Y));
  delay(100);
}
