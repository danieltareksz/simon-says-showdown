import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Reads commands typed in the console on its own thread: choosing the mode, naming
 * players, starting, and (in simulator mode) the keyboard pods' actions.
 * It only calls the controller's synchronized setters, never game logic.
 */
public class ConsoleCommands implements Runnable {

    private GameController game;
    private SimulatedPods sim;                 // null when playing with real pods
    private Thread t;

    public ConsoleCommands(GameController game, SimulatedPods sim) {
        this.game = game;
        this.sim = sim;
        t = new Thread(this);
        t.setName("console");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    public void run() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        try {
            for (;;) {
                String line = input.readLine();
                if (line == null) {
                    break;                     // console closed; the game keeps running
                }
                handle(line.trim());
            }
        } catch (IOException e) {
            System.out.println("Console closed: " + e.getMessage());
        }
    }

    private void handle(String line) {
        String lower = line.toLowerCase();
        if (lower.length() == 0) {
            return;
        }
        if (lower.equals("classic")) {
            changeMode(new ClassicScoring());
        } else if (lower.equals("showdown")) {
            changeMode(new ShowdownScoring());
        } else if (lower.startsWith("name ")) {
            rename(line.substring(5).trim());
        } else if (lower.equals("start")) {
            if (!game.requestStart()) {
                System.out.println("A game is already running");
            }
        } else if (lower.equals("help")) {
            printHelp();
        } else if (lower.equals("quit")) {
            System.exit(0);
        } else if (lower.startsWith("p1 ") || lower.startsWith("p2 ")) {
            if (sim == null) {
                System.out.println("Pod commands only work in simulator mode (--sim)");
            } else if (!sim.inject(lower)) {
                System.out.println("Unknown pod command; type help");
            }
        } else {
            System.out.println("Unknown command; type help");
        }
    }

    private void changeMode(ScoringStrategy s) {
        if (game.setStrategy(s)) {
            System.out.println("Mode: " + s);
        } else {
            System.out.println("The mode can only change between games");
        }
    }

    // "1 Daniel" names player 1 Daniel
    private void rename(String rest) {
        int space = rest.indexOf(' ');
        String number = space > 0 ? rest.substring(0, space) : "";
        String name = space > 0 ? rest.substring(space + 1).trim() : "";
        if (!(number.equals("1") || number.equals("2")) || name.length() == 0) {
            System.out.println("Usage: name 1 Daniel");
            return;
        }
        game.setPlayerName(Integer.parseInt(number) - 1, name);
        System.out.println("Player " + number + " is now " + name);
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  classic | showdown     choose the mode (between games)");
        System.out.println("  name 1 Daniel          name player 1 or 2");
        System.out.println("  start                  start the game, or play again");
        System.out.println("  quit                   exit");
        if (sim != null) {
            System.out.println("Simulated pods (p1 or p2):");
            System.out.println("  p1 tag                 scan the card (joins in the lobby)");
            System.out.println("  p1 joy up|down|left|right");
            System.out.println("  p1 btn red|green|blue|yellow [2]");
            System.out.println("  p1 dist low|mid|high");
            System.out.println("  p1 tilt l|r|f|b");
            System.out.println("  p1 off | p1 on         unplug or plug in the pod");
        }
    }
}
