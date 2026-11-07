package controller;

import model.Instruction;
import model.Player;
import model.PodMessage;
import java.util.List;

final class Judge {
    private Judge() {}
    /** Simon says: correct if the actions match. Trick round: correct only on doing nothing. */
    static boolean isCorrect(Instruction ins, List<PodMessage> acts, Player p) {
        if (!ins.simonSays()) return acts.isEmpty();
        return !acts.isEmpty() && ins.matches(acts, p);
    }
}
