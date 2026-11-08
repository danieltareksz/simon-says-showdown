import java.util.ArrayList;

/**
 * Two pods played from the keyboard, so the whole game runs and can be tested without hardware.
 * It follows the same rules as the firmware: actions count only after GO, at most n actions
 * after SEQ,n, a TIMEOUT once the limit passes, a scan outside a round is a lobby TAG, and a
 * heartbeat every 500 ms.
 * Concrete Subject in the Observer pattern (it replaces XBeeLink) and a PodSender.
 * inject() runs on the console thread, send() on the game thread and the heartbeats on this
 * object's own thread, so every method that touches the pod state is synchronized.
 */
public class SimulatedPods extends ConcretePodSubject implements PodSender, Runnable {

    public static final long HEARTBEAT_MS = 500;
    public static final long TICK_MS = 20;            // how often the thread checks for a timeout
    public static final String[] TAGS = { "SIM0000001", "SIM0000002" };

    private static final String[] JOY_DIRS = { "up", "down", "left", "right" };
    private static final String[] COLOURS = { "red", "green", "blue", "yellow" };
    private static final String[] ZONES = { "low", "mid", "high" };
    private static final String[] TILTS = { "l", "r", "f", "b" };

    private boolean echo;                              // print LED, beep and calibration messages
    private boolean[] listening = new boolean[2];      // index 0 = P1, 1 = P2
    private int[] actionsLeft = new int[2];
    private long[] goTime = new long[2];
    private long[] limitMs = new long[2];
    private boolean[] unplugged = new boolean[2];      // "p1 off": the pod sends and hears nothing
    private int goCount;                               // GO and SEQ messages received
    private Thread t;

    public SimulatedPods(boolean echo) {
        this.echo = echo;
        t = new Thread(this);
        t.setName("sim-heartbeat");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    /** Sends timeouts as soon as they are due, and both heartbeats every 500 ms. */
    public void run() {
        long lastBeat = 0;
        try {
            for (;;) {
                Thread.sleep(TICK_MS);
                long now = System.currentTimeMillis();
                checkTimeouts(now);
                if (now - lastBeat >= HEARTBEAT_MS) {
                    lastBeat = now;
                    heartbeat();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Simulator stopped");
        }
    }

    private synchronized void checkTimeouts(long now) {
        for (int i = 0; i < 2; i++) {
            if (listening[i] && now - goTime[i] > limitMs[i]) {
                listening[i] = false;
                notifyObservers(new PodMessage(podId(i), "TIMEOUT", null, null, 0));
            }
        }
    }

    private synchronized void heartbeat() {
        for (int i = 0; i < 2; i++) {
            if (!unplugged[i]) {
                notifyObservers(new PodMessage(podId(i), "HB", null, null, 0));
            }
        }
    }

    // ===== PodSender: messages from the game =====

    /** Acts on a laptop message the way the firmware's handle() does. */
    public synchronized void send(String target, String body) {
        String[] f = body.split(",");
        String cmd = f[0];
        if (cmd.equals("GO") || cmd.equals("SEQ")) {
            goCount++;
        }
        for (int i = 0; i < 2; i++) {
            if (unplugged[i] || !(target.equals("ALL") || target.equals(podId(i)))) {
                continue;                              // not for this pod
            }
            try {
                if (cmd.equals("GO") && f.length == 2) {
                    startListening(i, 1, Long.parseLong(f[1]));
                } else if (cmd.equals("SEQ") && f.length == 3) {
                    startListening(i, Integer.parseInt(f[1]), Long.parseLong(f[2]));
                } else if (cmd.equals("STOP")) {
                    listening[i] = false;
                } else if (cmd.equals("CAL")) {
                    say(i, "calibrated");
                } else if (cmd.equals("FB") && f.length == 2) {
                    if (f[1].equals("G")) {
                        say(i, "LED green, high beep");
                    } else if (f[1].equals("R")) {
                        say(i, "LED red, low beep");
                    } else if (f[1].equals("B")) {
                        say(i, "LED blue (joined)");
                    }
                } else if (cmd.equals("BEEP")) {
                    say(i, "beep");
                }
            } catch (NumberFormatException e) {
                say(i, "bad message ignored: " + body);   // a real pod would ignore it too
            }
        }
    }

    // Caller holds the lock
    private void startListening(int i, int steps, long limit) {
        goTime[i] = System.currentTimeMillis();
        limitMs[i] = limit;
        actionsLeft[i] = steps;
        listening[i] = true;
    }

    private void say(int i, String text) {
        if (echo) {
            System.out.println("[" + podId(i) + "] " + text);
        }
    }

    // ===== Keyboard input =====

    /**
     * Turns a console line such as "p1 joy up", "p2 btn red 2", "p1 tag", "p1 dist low",
     * "p2 tilt l", "p1 off" or "p1 on" into what that pod would send.
     * Returns false if the line is not a pod command. A valid action outside a round is
     * dropped, as on a real pod, except a scan, which becomes a lobby TAG.
     */
    public synchronized boolean inject(String line) {
        ArrayList<String> w = words(line.toLowerCase());
        if (w.size() < 2) {
            return false;
        }
        int i;
        if (w.get(0).equals("p1")) {
            i = 0;
        } else if (w.get(0).equals("p2")) {
            i = 1;
        } else {
            return false;
        }
        String cmd = w.get(1);
        String arg = w.size() > 2 ? w.get(2) : "";

        if (cmd.equals("off")) {
            unplugged[i] = true;
            listening[i] = false;
            say(i, "unplugged");
            return true;
        }
        if (cmd.equals("on")) {
            unplugged[i] = false;
            say(i, "plugged in");
            return true;
        }

        String action;
        String value;
        if (cmd.equals("joy") && isOneOf(arg, JOY_DIRS)) {
            action = "JOY";
            value = arg.toUpperCase();
        } else if (cmd.equals("btn") && isOneOf(arg, COLOURS)) {
            String count = w.size() > 3 ? w.get(3) : "1";
            if (!count.equals("1") && !count.equals("2")) {
                return false;
            }
            action = "BTN";
            value = arg.toUpperCase() + "_" + count;
        } else if (cmd.equals("tag")) {
            action = "RFID";
            value = TAGS[i];
        } else if (cmd.equals("dist") && isOneOf(arg, ZONES)) {
            action = "DIST";
            value = arg.toUpperCase();
        } else if (cmd.equals("tilt") && isOneOf(arg, TILTS)) {
            action = "TILT";
            value = arg.toUpperCase();
        } else {
            return false;
        }

        if (unplugged[i]) {
            return true;                               // an unplugged pod sends nothing
        }
        if (listening[i]) {
            long ms = System.currentTimeMillis() - goTime[i];
            actionsLeft[i]--;
            if (actionsLeft[i] == 0) {
                listening[i] = false;
            }
            notifyObservers(new PodMessage(podId(i), "ACT", action, value, ms));
        } else if (action.equals("RFID")) {
            notifyObservers(new PodMessage(podId(i), "TAG", "RFID", value, 0));
        }
        return true;
    }

    /** Number of GO and SEQ messages received so far. */
    public synchronized int getGoCount() {
        return goCount;
    }

    /** True while this pod (0 = P1, 1 = P2) is waiting for actions. */
    public synchronized boolean isListening(int i) {
        return listening[i];
    }

    // Splits a line at spaces, skipping repeated spaces
    private static ArrayList<String> words(String line) {
        ArrayList<String> out = new ArrayList<String>();
        int start = 0;
        while (start < line.length()) {
            int space = line.indexOf(' ', start);
            if (space < 0) {
                space = line.length();
            }
            if (space > start) {
                out.add(line.substring(start, space));
            }
            start = space + 1;
        }
        return out;
    }

    private static boolean isOneOf(String s, String[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static String podId(int i) {
        return "P" + (i + 1);
    }
}
