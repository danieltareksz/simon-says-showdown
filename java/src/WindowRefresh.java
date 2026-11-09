/**
 * One pending change to the window. GameWindow.update() runs on the game thread, but Swing
 * labels may only be changed on the Swing event thread, so update() hands this Runnable to
 * SwingUtilities.invokeLater, and the event thread later calls run().
 */
public class WindowRefresh implements Runnable {

    private GameWindow window;
    private GameUpdate update;

    public WindowRefresh(GameWindow window, GameUpdate update) {
        this.window = window;
        this.update = update;
    }

    public void run() {
        window.apply(update);
    }
}
