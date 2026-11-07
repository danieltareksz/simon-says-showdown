/** Checks that a pod subject notifies every registered observer, and stops after removal. */
public class TestObserver {

    public static void run() {
        ConcretePodSubject subject = new ConcretePodSubject();
        CountingObserver a = new CountingObserver();
        CountingObserver b = new CountingObserver();
        subject.registerObserver(a);
        subject.registerObserver(b);
        subject.notifyObservers(PodMessage.parse("P1,HB"));
        Check.equal("observer a notified", 1, a.getCount());
        Check.equal("observer b notified", 1, b.getCount());
        subject.removeObserver(b);
        subject.notifyObservers(PodMessage.parse("P1,HB"));
        Check.equal("observer a notified again", 2, a.getCount());
        Check.equal("removed observer not notified", 1, b.getCount());
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
