/**
 * One message received from a pod, for example "P1,ACT,TILT,L,1240"
 * (the text between the < and > of a radio frame).
 */
public class PodMessage {

    private String pod;     // "P1" or "P2"
    private String kind;    // ACT, TAG, TIMEOUT, HB or DBG
    private String action;  // JOY, BTN, RFID, DIST or TILT (ACT and TAG only)
    private String value;   // UP, RED_2, a tag ID, ... (ACT and TAG only)
    private long ms;        // milliseconds after GO (ACT only)

    public PodMessage(String pod, String kind, String action, String value, long ms) {
        this.pod = pod;
        this.kind = kind;
        this.action = action;
        this.value = value;
        this.ms = ms;
    }

    public String getPod()    { return pod; }
    public String getKind()   { return kind; }
    public String getAction() { return action; }
    public String getValue()  { return value; }
    public long getMs()       { return ms; }

    /** Returns null for anything malformed, so a corrupted radio line is simply ignored. */
    public static PodMessage parse(String line) {
        if (line == null) {
            return null;
        }
        String[] p = line.split(",");
        if (p.length < 2 || !(p[0].equals("P1") || p[0].equals("P2"))) {
            return null;
        }
        try {
            if (p[1].equals("ACT") && p.length == 5) {
                return new PodMessage(p[0], "ACT", p[2], p[3], Long.parseLong(p[4]));
            }
            if (p[1].equals("TAG") && p.length == 3) {
                return new PodMessage(p[0], "TAG", "RFID", p[2], 0);
            }
            if ((p[1].equals("TIMEOUT") || p[1].equals("HB")) && p.length == 2) {
                return new PodMessage(p[0], p[1], null, null, 0);
            }
            if (p[1].equals("DBG")) {
                return new PodMessage(p[0], "DBG", null, line, 0);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    public String toString() {
        return pod + "," + kind + "," + action + "," + value + "," + ms;
    }
}
