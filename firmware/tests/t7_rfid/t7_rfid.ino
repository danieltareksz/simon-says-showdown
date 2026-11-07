// Module test 7: Parallax 125 kHz RFID reader (SOUT -> RX, ENABLE = D11)
// Upload with SOUT unplugged. Then run from the power bank with SOUT plugged in,
// and connect only FTDI GND -> LilyPad GND and FTDI RXI -> LilyPad TX.
// Serial Monitor at 2400.
// Pass: each tag prints its 10-character ID while held near the reader.

const uint8_t RFID_EN = 11;
char id[11];
byte len = 0;
bool inTag = false;

void setup() {
  Serial.begin(2400);                 // the reader's fixed baud rate
  pinMode(RFID_EN, OUTPUT);
  digitalWrite(RFID_EN, LOW);         // LOW = reader on
  Serial.println("t7_rfid ready");
}

void loop() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c == 0x0A) { inTag = true; len = 0; }            // start byte
    else if (c == 0x0D && inTag) {                       // stop byte
      if (len == 10) { id[10] = 0; Serial.println(id); }
      inTag = false;
    }
    else if (inTag && len < 10) id[len++] = c;
  }
}
