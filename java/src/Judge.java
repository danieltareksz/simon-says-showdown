import java.util.ArrayList;

/** Decides whether one player's answer to an instruction is correct. */
public class Judge {

    /** Simon says: the actions must match. Trick round: correct only if the player did nothing. */
    public static boolean isCorrect(Instruction ins, ArrayList<PodMessage> acts, Player p) {
        if (!ins.isSimonSays()) {
            return acts.size() == 0;
        }
        return acts.size() > 0 && ins.matches(acts, p);
    }
}
