import java.util.ArrayList;
import java.util.Random;

/** Builds each round's instruction at random from the fixed set of 20 commands. */
public class InstructionFactory {

    // Every command the pods can detect, one row per sensor
    private Step[][] actions = {
        { new Step("JOY", "UP"), new Step("JOY", "DOWN"), new Step("JOY", "LEFT"), new Step("JOY", "RIGHT") },
        { new Step("BTN", "RED_1"), new Step("BTN", "GREEN_1"), new Step("BTN", "BLUE_1"), new Step("BTN", "YELLOW_1"),
          new Step("BTN", "RED_2"), new Step("BTN", "GREEN_2"), new Step("BTN", "BLUE_2"), new Step("BTN", "YELLOW_2") },
        { new Step("RFID", "TAG") },
        { new Step("DIST", "LOW"), new Step("DIST", "MID"), new Step("DIST", "HIGH") },
        { new Step("TILT", "L"), new Step("TILT", "R"), new Step("TILT", "F"), new Step("TILT", "B") }
    };

    // How often each row above is picked (out of their total): the hand-height sensor misreads
    // in play, so it comes up about 1 round in 17 and never in a memory sequence
    private int[] weights = { 4, 4, 4, 1, 4 };
    private static final int HAND_ROW = 3;

    public static final int MEMORY_MAX_STEPS = 3;
    public static final long MEMORY_MS_PER_STEP = 2500;

    private Random rng;
    private Step lastStep;   // two rounds in a row never open with the same step

    public InstructionFactory(Random rng) {
        this.rng = rng;
    }

    /** Rounds count from 1. Every 5th round is a memory round. */
    public Instruction next(int round) {
        int level = DifficultyManager.level(round);
        if (round % 5 == 0) {
            int n = Math.min(MEMORY_MAX_STEPS, 1 + level);       // 2 steps, then 3
            return new Instruction(randomSteps(n, false), true, MEMORY_MS_PER_STEP * n, true);
        }
        int n = 1;
        if (level >= 3 && rng.nextBoolean()) {                   // two-step from level 3
            n = 2;
        }
        boolean real = rng.nextDouble() >= DifficultyManager.trickChance(round);
        return new Instruction(randomSteps(n, true), real, DifficultyManager.timeLimitMs(round) * n, false);
    }

    /** Picks a sensor row by weight; the hand-height row only if allowed. */
    private int randomSensor(boolean handAllowed) {
        int total = 0;
        for (int i = 0; i < weights.length; i++) {
            if (handAllowed || i != HAND_ROW) {
                total += weights[i];
            }
        }
        int r = rng.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            if (handAllowed || i != HAND_ROW) {
                if (r < weights[i]) {
                    return i;
                }
                r -= weights[i];
            }
        }
        return 0;                              // not reached: r is always below the total
    }

    private ArrayList<Step> randomSteps(int n, boolean handAllowed) {
        ArrayList<Step> steps = new ArrayList<Step>();
        boolean hasScan = false;
        while (steps.size() < n) {
            Step[] sensor = actions[randomSensor(handAllowed)];     // a sensor by weight,
            Step s = sensor[rng.nextInt(sensor.length)];            // then one of its commands
            Step prev = steps.size() == 0 ? lastStep : steps.get(steps.size() - 1);
            if (clashes(s, prev)) {
                continue;
            }
            if (s.getAction().equals("RFID")) {
                if (hasScan) {
                    continue;              // the pod ignores the same tag for 1.5 s
                }
                hasScan = true;
            }
            steps.add(s);
        }
        lastStep = steps.get(0);
        return steps;
    }

    /** Steps the pod can't report back to back. */
    private boolean clashes(Step s, Step prev) {
        if (prev == null) {
            return false;
        }
        if (s.sameAs(prev)) {
            return true;                   // the pod reports changes, not repeats
        }
        // the same colour twice would merge into one double press
        return s.colour() != null && s.colour().equals(prev.colour());
    }
}
