/** One action Simon can ask for, for example TILT + L (tilt left) or BTN + RED_2. */
public class Step {

    private String action;   // JOY, BTN, RFID, DIST or TILT
    private String target;   // UP, RED_2, TAG, LOW, L, ...

    public Step(String action, String target) {
        this.action = action;
        this.target = target;
    }

    public String getAction() { return action; }
    public String getTarget() { return target; }

    /** Name of the voice clip for this step, for example "tilt_l" or "btn_red_2". */
    public String clipName() {
        return (action + "_" + target).toLowerCase();
    }

    public boolean sameAs(Step other) {
        return other != null && action.equals(other.action) && target.equals(other.target);
    }

    /** Button colour ("RED" from "RED_2"); null for other sensors. */
    public String colour() {
        if (!action.equals("BTN")) {
            return null;
        }
        return target.substring(0, target.indexOf('_'));
    }

    public String toString() {
        return action + "," + target;
    }
}
