/**
 * Strategy for the game's rules. GameController is the context and holds one of
 * ClassicScoring or ShowdownScoring, chosen in the lobby with setStrategy().
 */
public interface ScoringStrategy {

    /** Players needed to start (Classic 1, Showdown 2). */
    public int playersNeeded();

    /** Sets each player's starting score and lives. */
    public void startGame(Player[] players);

    /**
     * Scores one round. correct[i] is the Judge's verdict for players[i].
     * Returns the name of the clip to announce, for example "correct" or "p1_fastest".
     */
    public String scoreRound(Instruction ins, Player[] players, Answer[] answers, boolean[] correct);

    public boolean isOver(Player[] players);

    /** Text for the game-over screen, for example "Player 1 wins 21 to 15". */
    public String resultText(Player[] players);

    /** Clip for the end of the game, for example "p1_wins" or "game_over". */
    public String endClip(Player[] players);
}
