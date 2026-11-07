import java.util.ArrayList;

/** Checks Instruction.matches, clips() and Judge. */
public class TestInstruction {

    private static Instruction make(boolean simonSays, String action, String target) {
        ArrayList<Step> steps = new ArrayList<Step>();
        steps.add(new Step(action, target));
        return new Instruction(steps, simonSays, 4000, false);
    }

    private static ArrayList<PodMessage> acts(String line) {
        ArrayList<PodMessage> list = new ArrayList<PodMessage>();
        if (line != null) {
            list.add(PodMessage.parse(line));
        }
        return list;
    }

    public static void run() {
        Player p1 = new Player("Player 1", "P1", "0415AB3C9D");

        Instruction red2 = make(true, "BTN", "RED_2");
        Check.isTrue("red twice matches RED_2", red2.matches(acts("P1,ACT,BTN,RED_2,300"), p1));
        Check.isTrue("red once does not match RED_2", !red2.matches(acts("P1,ACT,BTN,RED_1,300"), p1));

        Instruction scan = make(true, "RFID", "TAG");
        Check.isTrue("own tag matches", scan.matches(acts("P1,ACT,RFID,0415AB3C9D,800"), p1));
        Check.isTrue("other tag does not match", !scan.matches(acts("P1,ACT,RFID,0415AB3C9E,800"), p1));

        ArrayList<String> clips = red2.clips();
        Check.equal("clip 1", "simon_says", clips.get(0));
        Check.equal("clip 2", "btn_red_2", clips.get(1));

        Instruction trick = make(false, "TILT", "L");
        Check.isTrue("trick: doing nothing is correct", Judge.isCorrect(trick, acts(null), p1));
        Check.isTrue("trick: acting is wrong", !Judge.isCorrect(trick, acts("P1,ACT,TILT,L,500"), p1));
        Check.isTrue("trick has no 'simon says' clip", !trick.clips().get(0).equals("simon_says"));
        Check.isTrue("real: timeout is wrong", !Judge.isCorrect(red2, acts(null), p1));
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
