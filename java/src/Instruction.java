import java.util.ArrayList;

/** One round's command from Simon: one or more steps, real or trick, with a time limit. */
public class Instruction {

    private ArrayList<Step> steps;
    private boolean simonSays;     // false = trick round: the right answer is to do nothing
    private long timeLimitMs;
    private boolean memory;        // memory round: the sequence is hidden once spoken

    public Instruction(ArrayList<Step> steps, boolean simonSays, long timeLimitMs, boolean memory) {
        this.steps = steps;
        this.simonSays = simonSays;
        this.timeLimitMs = timeLimitMs;
        this.memory = memory;
    }

    public boolean isSimonSays()  { return simonSays; }
    public long getTimeLimitMs()  { return timeLimitMs; }
    public boolean isMemory()     { return memory; }
    public int stepCount()        { return steps.size(); }
    public Step getStep(int i)    { return steps.get(i); }

    /** Clip names in the order they are spoken. */
    public ArrayList<String> clips() {
        ArrayList<String> out = new ArrayList<String>();
        if (memory) {
            out.add("memory_round");
        }
        if (simonSays) {
            out.add("simon_says");
        }
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0 && !memory) {
                out.add("then");
            }
            out.add(steps.get(i).clipName());
        }
        return out;
    }

    /** Screen text, built from the same phrases as the voice clips. */
    public String text(ClipLibrary library) {
        String text = memory ? "Memory round: " : (simonSays ? "Simon says: " : "");
        for (int i = 0; i < steps.size(); i++) {
            String phrase = library.getPhrase(steps.get(i).clipName());
            if (i > 0 || text.length() > 0) {
                phrase = phrase.substring(0, 1).toLowerCase() + phrase.substring(1);
            }
            if (i > 0) {
                text = text + (memory ? ", " : ", then ");
            }
            text = text + phrase;
        }
        return text;
    }

    /** True if this player's reported actions match every step, in order. */
    public boolean matches(ArrayList<PodMessage> acts, Player p) {
        if (acts.size() != steps.size()) {
            return false;
        }
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            PodMessage a = acts.get(i);
            String want = s.getAction().equals("RFID") ? p.getTag() : s.getTarget();
            if (!s.getAction().equals(a.getAction()) || !want.equals(a.getValue())) {
                return false;
            }
        }
        return true;
    }
}
