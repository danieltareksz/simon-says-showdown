/**
 * Prints every game update to the console, so the game can be followed with no window.
 * Observer in the Observer pattern (of GameController). update() runs on the game thread
 * and only prints.
 */
public class ConsoleDisplay implements GameObserver {

    public void update(GameUpdate u) {
        String type = u.getType();
        String text = u.getText();
        if (type.equals(GameUpdate.PHASE)) {
            System.out.println();
            System.out.println("=== " + text + " ===");
        } else if (type.equals(GameUpdate.INSTRUCTION)) {
            System.out.println("SIMON: " + text);
        } else if (type.equals(GameUpdate.GO)) {
            System.out.println("GO! (" + text + " ms)");
        } else if (type.equals(GameUpdate.RESULT)) {
            System.out.println(">> " + text);
        } else if (type.equals(GameUpdate.SCORES)) {
            System.out.println("   " + text);
        } else {
            System.out.println(text);
        }
    }
}
