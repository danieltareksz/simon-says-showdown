/**
 * Classic mode rules: one player, 3 lives, +1 point per correct round.
 * Concrete Strategy in the Strategy pattern; GameController is the context.
 * The players array holds just the one player in the game.
 */
public class ClassicScoring implements ScoringStrategy {

    public static final int START_LIVES = 3;

    public int playersNeeded() {
        return 1;
    }

    public void startGame(Player[] players) {
        players[0].setScore(0);
        players[0].setLives(START_LIVES);
    }

    /**
     * Correct (including staying still on a trick) scores a point; anything else costs a life.
     * The returned clip says why: wrong action, fell for a trick, or no action at all.
     */
    public String scoreRound(Instruction ins, Player[] players, Answer[] answers, boolean[] correct) {
        Player p = players[0];
        if (correct[0]) {
            p.addScore(1);
            return "correct";
        }
        p.loseLife();
        if (!ins.isSimonSays()) {
            return "gotcha";
        }
        if (answers[0].getActs().size() == 0) {
            return "too_slow";                 // did nothing before the limit
        }
        return "wrong";
    }

    public boolean isOver(Player[] players) {
        return players[0].getLives() == 0;
    }

    public String resultText(Player[] players) {
        return "Game over: " + players[0].getScore() + " points";
    }

    public String endClip(Player[] players) {
        return "game_over";
    }

    public String toString() {
        return "Classic";
    }
}
