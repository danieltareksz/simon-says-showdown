/**
 * Checks that the keyboard pods behave like the firmware: nothing before GO except lobby
 * scans, ACT with the time since GO, only n actions after SEQ,n, STOP, TIMEOUT and heartbeats.
 */
public class TestSimulatedPods {

    public static void run() {
        try {
            SimulatedPods sim = new SimulatedPods(false);
            PodRecorder rec = new PodRecorder();
            sim.registerObserver(rec);

            Check.isTrue("valid command accepted", sim.inject("p1 joy up"));
            Check.equal("action before GO is ignored", 0, rec.count("ACT", null));
            sim.inject("p1 tag");
            sim.inject("p2 tag");
            Check.equal("scans before GO are lobby tags", 2, rec.count("TAG", null));
            Check.equal("P1 tag id", "SIM0000001", rec.first("TAG").getValue());
            Check.equal("P2 tag id", "SIM0000002", rec.last("TAG").getValue());
            Check.isTrue("unknown command rejected", !sim.inject("p1 jump"));
            Check.isTrue("bad button count rejected", !sim.inject("p1 btn red 3"));
            Check.isTrue("unknown pod rejected", !sim.inject("p3 joy up"));

            sim.send("ALL", "GO,4000");
            Thread.sleep(100);
            sim.inject("p1 tilt l");
            PodMessage a = rec.last("ACT");
            Check.isTrue("ACT after GO", a != null && a.getPod().equals("P1")
                    && a.getAction().equals("TILT") && a.getValue().equals("L"));
            Check.isTrue("ms counted from GO (" + (a == null ? -1 : a.getMs()) + ")",
                    a != null && a.getMs() >= 80 && a.getMs() < 1000);
            sim.inject("p1 joy up");
            Check.equal("only 1 action after GO", 1, rec.count("ACT", "P1"));
            sim.inject("p2 btn red 2");
            Check.equal("other pod still listening", "RED_2", rec.last("ACT").getValue());
            sim.send("ALL", "STOP");

            sim.send("ALL", "SEQ,2,4000");
            sim.inject("p2 btn green");
            Check.equal("single press", "GREEN_1", rec.last("ACT").getValue());
            sim.inject("p2 dist mid");
            sim.inject("p2 joy left");
            Check.equal("only n actions after SEQ,n", 3, rec.count("ACT", "P2"));
            Check.equal("last of the sequence", "MID", rec.last("ACT").getValue());
            sim.inject("p1 tag");
            Check.equal("scan while listening is an ACT with the tag", "SIM0000001", rec.last("ACT").getValue());
            sim.send("ALL", "STOP");
            sim.inject("p1 joy down");
            Check.equal("STOP ends listening", 5, rec.count("ACT", null));

            sim.send("P2", "GO,100");
            sim.inject("p1 joy right");
            Check.equal("GO for P2 does not wake P1", 5, rec.count("ACT", null));
            Thread.sleep(300);
            Check.equal("TIMEOUT after the limit", 1, rec.count("TIMEOUT", null));
            Check.equal("TIMEOUT from P2", "P2", rec.last("TIMEOUT").getPod());
            Check.equal("GO and SEQ counted", 3, sim.getGoCount());

            rec.clear();
            Thread.sleep(600);
            Check.isTrue("heartbeats from both pods",
                    rec.count("HB", "P1") >= 1 && rec.count("HB", "P2") >= 1);
            sim.inject("p2 off");
            rec.clear();
            Thread.sleep(600);
            Check.equal("unplugged pod is silent", 0, rec.count("HB", "P2"));
            Check.isTrue("other pod still beats", rec.count("HB", "P1") >= 1);
            sim.inject("p2 on");
            Thread.sleep(600);
            Check.isTrue("plugged back in, it beats again", rec.count("HB", "P2") >= 1);
        } catch (InterruptedException e) {
            Check.isTrue("interrupted", false);
        }
    }

    public static void main(String[] args) {
        run();
        Check.summary();
        System.exit(0);                        // the simulator thread never ends on its own
    }
}
