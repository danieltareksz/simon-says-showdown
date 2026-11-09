import java.io.IOException;
import java.util.Random;

/**
 * Entry point: builds every part of the game and wires the observers together.
 *   --sim          play from the keyboard, no pods needed
 *   --port COM5    use the laptop XBee on this serial port
 *   --nogui        console only, no window
 * Run it from the java folder, so sounds/ and leaderboard.txt are found.
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

        ClipLibrary clips;
        try {
            clips = new ClipLibrary("sounds");
        } catch (IOException e) {
            System.out.println("Cannot read sounds/clips.csv (run from the java folder): " + e.getMessage());
            System.exit(1);
            return;
        }

        // The pods: both choices are a pod Subject and a PodSender
        ConcretePodSubject pods;
        PodSender sender;
        SimulatedPods simPods = null;
        if (sim) {
            simPods = new SimulatedPods(true);
            pods = simPods;
            sender = simPods;
        } else if (port != null) {
            try {
                XBeeLink link = new XBeeLink(port);
                pods = link;
                sender = link;
            } catch (IOException e) {
                System.out.println(e.getMessage());
                printPorts();
                System.exit(1);
                return;
            }
        } else {
            System.out.println("Use --sim, or --port NAME for the XBee.");
            printPorts();
            System.exit(1);
            return;
        }

        GameController game = new GameController(sender, clips, new Leaderboard("leaderboard.txt"),
                new InstructionFactory(new Random()));

        // Game observers, in this order: text first, then the voice
        game.registerObserver(new ConsoleDisplay());
        if (gui) {
            try {
                game.registerObserver(new GameWindow(game));
            } catch (Exception e) {
                System.out.println("No window (" + e + "); playing in the console.");
            }
        }
        try {
            game.registerObserver(new VoicePlayer(clips));
        } catch (Exception e) {
            System.out.println("Voice off: " + e.getMessage());
            game.setVoiceOn(false);
        }

        // Pod observers
        HeartbeatMonitor monitor = new HeartbeatMonitor(game);
        pods.registerObserver(game);
        pods.registerObserver(monitor);

        new ConsoleCommands(game, simPods);
        System.out.println("Type help for the commands.");
        game.start();
        try {
            game.getThread().join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }

    private static void printPorts() {
        String[] names = XBeeLink.listPorts();
        System.out.println("Serial ports found: " + names.length);
        for (int i = 0; i < names.length; i++) {
            System.out.println("  " + names[i]);
        }
    }
}
