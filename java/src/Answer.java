import java.util.ArrayList;

/** What one player did during one round: the actions their pod reported, and whether it timed out. */
public class Answer {

    private ArrayList<PodMessage> acts = new ArrayList<PodMessage>();
    private boolean timedOut;

    /**
     * Keeps the actions in the order they happened (their ms after GO), not the order they arrived:
     * a pod reports a button 400 ms late (it waits for a double press) and a tilt or hand height
     * after its hold, so a quick next action can arrive first.
     */
    public void add(PodMessage act) {
        int i = acts.size();
        while (i > 0 && acts.get(i - 1).getMs() > act.getMs()) {
            i--;
        }
        acts.add(i, act);
    }

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
