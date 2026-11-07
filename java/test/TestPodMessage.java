/** Checks PodMessage.parse against every message the pods can send. */
public class TestPodMessage {

    public static void run() {
        PodMessage m = PodMessage.parse("P1,ACT,TILT,L,1240");
        Check.equal("ACT pod", "P1", m.getPod());
        Check.equal("ACT action", "TILT", m.getAction());
        Check.equal("ACT value", "L", m.getValue());
        Check.equal("ACT ms", 1240, m.getMs());

        m = PodMessage.parse("P2,ACT,BTN,RED_2,610");
        Check.equal("button value", "RED_2", m.getValue());

        m = PodMessage.parse("P1,TAG,0415AB3C9D");
        Check.equal("TAG kind", "TAG", m.getKind());
        Check.equal("TAG id", "0415AB3C9D", m.getValue());

        Check.equal("TIMEOUT", "TIMEOUT", PodMessage.parse("P1,TIMEOUT").getKind());
        Check.equal("HB", "HB", PodMessage.parse("P2,HB").getKind());
        Check.equal("DBG", "DBG", PodMessage.parse("P1,DBG,boot").getKind());

        Check.isTrue("unknown pod is ignored", PodMessage.parse("P3,HB") == null);
        Check.isTrue("missing field is ignored", PodMessage.parse("P1,ACT,TILT") == null);
        Check.isTrue("bad number is ignored", PodMessage.parse("P1,ACT,BTN,RED_1,x") == null);
        Check.isTrue("garbage is ignored", PodMessage.parse("garbage") == null);
        Check.isTrue("null is ignored", PodMessage.parse(null) == null);
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
