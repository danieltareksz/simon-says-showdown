import java.util.Random;

/** Generates many rounds and checks the factory never breaks its rules. */
public class TestInstructionFactory {

    public static void run() {
        InstructionFactory factory = new InstructionFactory(new Random(1));
        int broken = 0;
        int twoStepTooEarly = 0;
        for (int k = 0; k < 20000; k++) {
            int round = 1 + k % 25;
            Instruction ins = factory.next(round);
            int scans = 0;
            for (int i = 0; i < ins.stepCount(); i++) {
                Step s = ins.getStep(i);
                if (s.getAction().equals("RFID")) {
                    scans++;
                }
                if (i > 0) {
                    Step prev = ins.getStep(i - 1);
                    if (s.sameAs(prev)) {
                        broken++;
                    }
                    if (s.colour() != null && s.colour().equals(prev.colour())) {
                        broken++;
                    }
                }
            }
            if (scans > 1) {
                broken++;
            }
            if (!ins.isMemory() && ins.stepCount() == 2 && round < 15) {
                twoStepTooEarly++;
            }
        }
        Check.equal("rule breaks in 20,000 rounds", 0, broken);
        Check.equal("two-step rounds before round 15", 0, twoStepTooEarly);

        InstructionFactory f = new InstructionFactory(new Random(2));
        int[] memoryRounds = {5, 10, 15, 20, 25};
        int[] lengths = {2, 3, 4, 5, 5};
        for (int i = 0; i < memoryRounds.length; i++) {
            Instruction ins = f.next(memoryRounds[i]);
            Check.isTrue("round " + memoryRounds[i] + " is a memory round", ins.isMemory() && ins.isSimonSays());
            Check.equal("round " + memoryRounds[i] + " length", lengths[i], ins.stepCount());
        }
        Check.equal("time limit level 0", 4000, DifficultyManager.timeLimitMs(1));
        Check.equal("time limit level 6", 1500, DifficultyManager.timeLimitMs(30));
        Check.isTrue("trick chance capped at 0.45", DifficultyManager.trickChance(100) <= 0.45);
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
