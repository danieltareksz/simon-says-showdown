package view;

import com.fazecast.jSerialComm.SerialPort;
import model.GameEvent;
import model.PodMessage;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

/** Boundary class: the only class that touches the laptop's XBee. */
public class XBeeManager implements PodLink {
    private final SerialPort port;
    private final BlockingQueue<GameEvent> events;

    public XBeeManager(String portName, BlockingQueue<GameEvent> events) {
        this.events = events;
        port = SerialPort.getCommPort(portName);          // "COM5" or "/dev/tty.usbserial-A1"
        port.setBaudRate(9600);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
        if (!port.openPort()) throw new IllegalStateException("Cannot open " + portName);
        Thread reader = new Thread(this::readLoop, "xbee-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        StringBuilder line = new StringBuilder();
        boolean inMsg = false;
        byte[] buf = new byte[64];
        while (port.isOpen()) {
            int n = port.readBytes(buf, buf.length);
            for (int i = 0; i < n; i++) {
                char c = (char) buf[i];
                if (c == '<') { inMsg = true; line.setLength(0); }
                else if (c == '>' && inMsg) {
                    inMsg = false;
                    PodMessage m = PodMessage.parse(line.toString());
                    if (m != null) events.offer(new GameEvent.PodEvent(m));
                } else if (inMsg) line.append(c);
            }
        }
    }

    /** send("ALL", "GO,4000") or send("P2", "FB,G") */
    @Override public synchronized void send(String target, String body) {
        byte[] out = ("<" + target + "," + body + ">\n").getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(out, out.length);
    }
}
