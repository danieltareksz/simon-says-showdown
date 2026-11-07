package app;

/**
 * Entry point.
 *   --sim          play with the keyboard simulator, no pods needed
 *   --port COM5    use the XBee on this serial port and skip the port picker
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        boolean sim = false;
        String port = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--sim")) sim = true;
            else if (args[i].equals("--port") && i + 1 < args.length) port = args[++i];
        }
        System.out.println("Simon Says Showdown"
                + (sim ? " (simulator)" : port != null ? " (port " + port + ")" : ""));
    }
}
