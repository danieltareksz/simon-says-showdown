package controller;

import model.Instruction;
import model.Instruction.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Control class: builds each round's instruction at random from the fixed command set. */
public class InstructionFactory {

    /** Every command the pods can detect, grouped by sensor: 20 in total. */
    public static final List<List<Step>> ACTIONS = List.of(
        List.of(new Step("JOY", "UP"), new Step("JOY", "DOWN"), new Step("JOY", "LEFT"), new Step("JOY", "RIGHT")),
        List.of(new Step("BTN", "RED_1"), new Step("BTN", "GREEN_1"), new Step("BTN", "BLUE_1"), new Step("BTN", "YELLOW_1"),
                new Step("BTN", "RED_2"), new Step("BTN", "GREEN_2"), new Step("BTN", "BLUE_2"), new Step("BTN", "YELLOW_2")),
        List.of(new Step("RFID", "TAG")),
        List.of(new Step("DIST", "LOW"), new Step("DIST", "MID"), new Step("DIST", "HIGH")),
        List.of(new Step("TILT", "L"), new Step("TILT", "R"), new Step("TILT", "F"), new Step("TILT", "B")));

    private final Random rng;
    private Step lastStep;                       // so two rounds in a row never open the same way

    public InstructionFactory(Random rng) { this.rng = rng; }

    /** Rounds count from 1. Every 5th round is a memory round. */
    public Instruction next(int round) {
        int level = DifficultyManager.level(round);
        if (round % 5 == 0) {
            int n = Math.min(5, 1 + level);      // 2, 3, 4, then 5 steps
            return new Instruction(randomSteps(n), true, 2000L * n, true);   // memory rounds are always real
        }
        int n = (level >= 3 && rng.nextBoolean()) ? 2 : 1;                   // two-step from level 3
        boolean real = rng.nextDouble() >= DifficultyManager.trickChance(round);
        return new Instruction(randomSteps(n), real, DifficultyManager.timeLimitMs(round) * n, false);
    }

    private List<Step> randomSteps(int n) {
        List<Step> steps = new ArrayList<>();
        boolean hasScan = false;
        while (steps.size() < n) {
            List<Step> sensor = ACTIONS.get(rng.nextInt(ACTIONS.size()));   // each sensor equally likely,
            Step s = sensor.get(rng.nextInt(sensor.size()));                // then one of its commands
            Step prev = steps.isEmpty() ? lastStep : steps.get(steps.size() - 1);
            if (clashes(s, prev)) continue;
            if (s.action().equals("RFID")) {
                if (hasScan) continue;           // the pod ignores the same tag for 1.5 s
                hasScan = true;
            }
            steps.add(s);
        }
        lastStep = steps.get(0);
        return steps;
    }

    /** Steps the pod can't report back to back. */
    private static boolean clashes(Step s, Step prev) {
        if (prev == null) return false;
        if (s.equals(prev)) return true;         // the pod reports changes, not repeats
        return s.action().equals("BTN") && prev.action().equals("BTN")
            && colour(s).equals(colour(prev));   // same colour twice would merge into a double press
    }

    private static String colour(Step s) { return s.target().substring(0, s.target().indexOf('_')); }
}
