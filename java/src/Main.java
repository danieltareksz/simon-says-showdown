/**
 * Entry point.
 *   --sim          play from the keyboard, no pods needed
 *   --port COM5    use the laptop XBee on this serial port
 *   --nogui        console only, no window
 */
public class Main {

    public static void main(String[] args) {
        boolean sim = false;
        boolean gui = true;
        String port = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--sim")) {
                sim = true;
            } else if (args[i].equals("--nogui")) {
                gui = false;
            } else if (args[i].equals("--port") && i + 1 < args.length) {
                port = args[++i];
            }
        }
        System.out.println("Simon Says Showdown" + (sim ? " (simulator)" : "")
                + (port != null ? " (port " + port + ")" : "") + (gui ? "" : " (no window)"));
    }
}
