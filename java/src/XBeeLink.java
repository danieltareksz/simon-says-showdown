import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * The real radio link to the pods, through the laptop's XBee.
 * As a Subject, its thread reads one line per radio frame and notifies the observers.
 * As a PodSender, it writes frames to the pods.
 * jSerialComm is used only to open the port; reading and writing use the ordinary
 * InputStream / OutputStream classes from the Java I/O slides.
 */
public class XBeeLink extends ConcretePodSubject implements PodSender, Runnable {

    private SerialPort port;
    private OutputStream out;
    private Thread t;

    public XBeeLink(String portName) throws IOException {
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (!port.openPort()) {
            throw new IOException("Cannot open serial port " + portName);
        }
        out = port.getOutputStream();
        t = new Thread(this);
        t.setName("xbee-reader");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(port.getInputStream()));
            for (;;) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                int start = line.lastIndexOf('<');
                int end = line.indexOf('>', start + 1);
                if (start < 0 || end < 0) {
                    continue;                      // radio noise or half a frame
                }
                PodMessage msg = PodMessage.parse(line.substring(start + 1, end));
                if (msg != null) {
                    notifyObservers(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("XBee link closed: " + e.getMessage());
        }
    }

    public void send(String target, String body) {
        String frame = "<" + target + "," + body + ">\n";
        synchronized (out) {                       // one frame at a time from any thread
            try {
                out.write(frame.getBytes());
                out.flush();
            } catch (IOException e) {
                System.out.println("Could not send to the pods: " + e.getMessage());
            }
        }
    }

    /** Names of the serial ports on this computer, to pick one for --port. */
    public static String[] listPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        return names;
    }
}
