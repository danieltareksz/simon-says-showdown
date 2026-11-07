import java.util.ArrayList;

/**
 * Keeps the list of game observers and notifies them in the order they registered.
 * Register the window and console before the voice, so the text appears before it is spoken.
 */
public class ConcreteGameSubject implements GameSubject {

    private ArrayList<GameObserver> observers = new ArrayList<GameObserver>();

    public synchronized void registerObserver(GameObserver o) {
        observers.add(o);
    }

    public synchronized void removeObserver(GameObserver o) {
        observers.remove(o);
    }

    // Not synchronized: the voice observer blocks while it speaks, and the radio
    // thread must never wait for that. Only the game thread calls this method.
    public void notifyObservers(GameUpdate u) {
        ArrayList<GameObserver> copy;
        synchronized (this) {
            copy = new ArrayList<GameObserver>(observers);
        }
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).update(u);
        }
    }
}
