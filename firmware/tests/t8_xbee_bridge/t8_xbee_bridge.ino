// Module test 8: XBee link (DOUT -> D2, DIN <- D3 through the 10k/20k divider)
// Keep RFID SOUT unplugged. Serial Monitor at 9600; laptop radio open in XCTU.
// Pass: text typed in XCTU appears here, and text typed here appears in XCTU.

#include <SoftwareSerial.h>

SoftwareSerial xbee(2, 3);   // RX = D2 (from DOUT), TX = D3 (to DIN)

void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
}

void loop() {
  if (xbee.available())   Serial.write(xbee.read());   // radio -> Serial Monitor
  if (Serial.available()) xbee.write(Serial.read());   // Serial Monitor -> radio
}
