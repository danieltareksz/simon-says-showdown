import java.util.ArrayList;

/** What one player did during one round: the actions their pod reported, and whether it timed out. */
public class Answer {

    private ArrayList<PodMessage> acts = new ArrayList<PodMessage>();
    private boolean timedOut;

    public void add(PodMessage act)             { acts.add(act); }
    public void setTimedOut()                   { timedOut = true; }
    public boolean isTimedOut()                 { return timedOut; }
    public ArrayList<PodMessage> getActs()      { return acts; }

    /** Done once the pod sent the expected number of actions, or a TIMEOUT. */
    public boolean isDone(int expectedSteps) {
        return timedOut || acts.size() >= expectedSteps;
    }

    /** Reaction time of the last action, used to find the fastest player; -1 if none. */
    public long lastMs() {
        if (acts.size() == 0) {
            return -1;
        }
        return acts.get(acts.size() - 1).getMs();
    }
}
