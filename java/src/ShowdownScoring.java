/**
 * Showdown mode rules: two players, same instruction, first to 21 wins.
 * Concrete Strategy in the Strategy pattern; GameController is the context.
 * The players array is always {P1, P2}, so index 0 is player one.
 */
public class ShowdownScoring implements ScoringStrategy {

    public static final int WIN_SCORE = 21;
    public static final int FASTEST_POINTS = 3;
    public static final int CORRECT_POINTS = 1;
    public static final int WRONG_POINTS = -2;

    public int playersNeeded() {
        return 2;
    }

    public void startGame(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            players[i].setScore(0);
            players[i].setLives(0);            // lives are not used in Showdown
        }
    }

    /**
     * Real instruction: fastest correct +3, other correct +1, wrong action -2, no action 0.
     * Trick: stayed still +1, acted -2. Player.addScore keeps scores at 0 or more.
     */
    public String scoreRound(Instruction ins, Player[] players, Answer[] answers, boolean[] correct) {
        if (!ins.isSimonSays()) {
            boolean anyoneActed = false;
            for (int i = 0; i < players.length; i++) {
                if (correct[i]) {
                    players[i].addScore(CORRECT_POINTS);
                } else {
                    players[i].addScore(WRONG_POINTS);
                    anyoneActed = true;
                }
            }
            return anyoneActed ? "gotcha" : "correct";
        }

        // Fastest = smallest reaction time of the last action among the correct players
        long best = -1;
        for (int i = 0; i < players.length; i++) {
            if (correct[i] && (best < 0 || answers[i].lastMs() < best)) {
                best = answers[i].lastMs();
            }
        }
        int fastestCount = 0;
        int fastest = -1;
        for (int i = 0; i < players.length; i++) {
            if (correct[i] && answers[i].lastMs() == best) {
                players[i].addScore(FASTEST_POINTS);   // equal times are both fastest
                fastestCount++;
                fastest = i;
            } else if (correct[i]) {
                players[i].addScore(CORRECT_POINTS);
            } else if (answers[i].getActs().size() > 0) {
                players[i].addScore(WRONG_POINTS);
            }
            // no action at all (timeout): 0 points
        }
        if (fastestCount == 0) {
            return "nobody";
        }
        if (fastestCount > 1) {
            return "correct";                  // a dead heat: neither was faster
        }
        return fastest == 0 ? "p1_fastest" : "p2_fastest";
    }

    /** Over once someone reaches 21 and the scores differ; a tie at 21 or more plays on. */
    public boolean isOver(Player[] players) {
        int a = players[0].getScore();
        int b = players[1].getScore();
        return Math.max(a, b) >= WIN_SCORE && a != b;
    }

    public String resultText(Player[] players) {
        Player win = winner(players);
        Player lose = win == players[0] ? players[1] : players[0];
        return win.getName() + " wins " + win.getScore() + " to " + lose.getScore();
    }

    public String endClip(Player[] players) {
        return winner(players) == players[0] ? "p1_wins" : "p2_wins";
    }

    private Player winner(Player[] players) {
        return players[0].getScore() > players[1].getScore() ? players[0] : players[1];
    }

    public String toString() {
        return "Showdown";
    }
}
