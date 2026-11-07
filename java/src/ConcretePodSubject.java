import java.util.ArrayList;

/**
 * Keeps the list of pod observers and notifies them. The radio thread notifies while
 * other threads may register, so the methods are synchronized.
 */
public class ConcretePodSubject implements PodSubject {

    private ArrayList<PodObserver> observers = new ArrayList<PodObserver>();

    public synchronized void registerObserver(PodObserver o) {
        observers.add(o);
    }

    public synchronized void removeObserver(PodObserver o) {
        observers.remove(o);
    }

    public synchronized void notifyObservers(PodMessage msg) {
        for (int i = 0; i < observers.size(); i++) {
            observers.get(i).update(msg);
        }
    }
}
