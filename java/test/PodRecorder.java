import java.util.ArrayList;

/** Test observer that keeps every pod message it receives, to check what a pod subject sent. */
public class PodRecorder implements PodObserver {

    private ArrayList<PodMessage> messages = new ArrayList<PodMessage>();

    public synchronized void update(PodMessage msg) {
        messages.add(msg);
    }

    public synchronized void clear() {
        messages.clear();
    }

    /** Messages of this kind (ACT, TAG, TIMEOUT, HB), from any pod if pod is null. */
    public synchronized int count(String kind, String pod) {
        int n = 0;
        for (int i = 0; i < messages.size(); i++) {
            PodMessage m = messages.get(i);
            if (m.getKind().equals(kind) && (pod == null || m.getPod().equals(pod))) {
                n++;
            }
        }
        return n;
    }

    public synchronized PodMessage first(String kind) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getKind().equals(kind)) {
                return messages.get(i);
            }
        }
        return null;
    }

    public synchronized PodMessage last(String kind) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getKind().equals(kind)) {
                return messages.get(i);
            }
        }
        return null;
    }
}
