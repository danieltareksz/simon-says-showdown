/** One line of the leaderboard: a player's name and their Classic score. */
public class ScoreEntry {

    private String name;
    private int score;

    public ScoreEntry(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() { return name; }
    public int getScore()   { return score; }

    /** The line written to leaderboard.txt, for example "Daniel,14". */
    public String toLine() {
        return name + "," + score;
    }

    /**
     * Reads a line written by toLine(); null if it is malformed. The score is after the
     * last comma, so a name that contains a comma still loads.
     */
    public static ScoreEntry parse(String line) {
        if (line == null) {
            return null;
        }
        int comma = line.lastIndexOf(',');
        if (comma <= 0) {
            return null;
        }
        try {
            int score = Integer.parseInt(line.substring(comma + 1).trim());
            return new ScoreEntry(line.substring(0, comma).trim(), score);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String toString() {
        return name + " " + score;
    }
}
