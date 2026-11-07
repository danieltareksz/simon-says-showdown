// Module test 6: LilyPad accelerometer (X = A2, Y = A3, powered from 3.3 V)
// Hold the controller flat at power-up; that reading becomes zero.
// Pass: zero reads about 320-355; tilting each way passes +-30 degrees.
// To measure countsPerG, stand the controller on its left edge: X raw minus xZero = 1 g.

const uint8_t TILT_X = A2, TILT_Y = A3;
float countsPerG = 68.0;                 // replace with the measured value
int xZero, yZero;

int readAvg(uint8_t pin) {
  long s = 0;
  for (byte i = 0; i < 8; i++) s += analogRead(pin);
  return s / 8;
}

float toDeg(int counts) {
  return asin(constrain(counts / countsPerG, -1.0, 1.0)) * 57.2958;
}

void setup() {
  Serial.begin(9600);
  delay(500);
  xZero = readAvg(TILT_X);
  yZero = readAvg(TILT_Y);
  Serial.print("zero "); Serial.print(xZero); Serial.print(' '); Serial.println(yZero);
}

void loop() {
  int x = readAvg(TILT_X), y = readAvg(TILT_Y);
  Serial.print("raw ");     Serial.print(x); Serial.print(' '); Serial.print(y);
  Serial.print("  roll ");  Serial.print(toDeg(x - xZero), 0);
  Serial.print("  pitch "); Serial.println(toDeg(y - yZero), 0);
  delay(150);
}
