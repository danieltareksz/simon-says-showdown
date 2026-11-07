/** Test observer that counts the pod messages it receives. */
public class CountingObserver implements PodObserver {

    private int count = 0;

    public synchronized void update(PodMessage msg) {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
