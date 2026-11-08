import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/** Checks the leaderboard keeps the top 10 sorted and survives a save and reload. */
public class TestLeaderboard {

    // Counts the lines in a file, to check what save() wrote
    private static int countLines(File f) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(f));
        int n = 0;
        try {
            while (input.readLine() != null) {
                n++;
            }
        } finally {
            input.close();
        }
        return n;
    }

    public static void run() {
        File f = null;
        try {
            f = File.createTempFile("leaderboard", ".txt");
            f.delete();                                    // start from a missing file
            String path = f.getPath();

            Leaderboard board = new Leaderboard(path);
            Check.equal("missing file is empty", 0, board.size());
            Check.isTrue("0 is never a high score", !board.isHighScore(0));
            Check.isTrue("any score fits an empty board", board.isHighScore(1));

            int[] scores = { 5, 12, 3, 8, 20, 1, 15, 7, 9, 11, 2, 6 };
            for (int i = 0; i < scores.length; i++) {
                board.add("P" + i, scores[i]);
            }
            Check.equal("keeps only 10", 10, board.size());
            Check.equal("highest first", 20, board.get(0).getScore());
            Check.equal("10th place", 3, board.get(9).getScore());
            boolean sorted = true;
            for (int i = 1; i < board.size(); i++) {
                if (board.get(i).getScore() > board.get(i - 1).getScore()) {
                    sorted = false;
                }
            }
            Check.isTrue("sorted high to low", sorted);
            Check.isTrue("3 no longer fits", !board.isHighScore(3));
            Check.equal("too low is not added", -1, board.add("Low", 2));
            Check.equal("tie goes below the older score", 3, board.add("Tie", 12));
            Check.equal("older score keeps its place", "P1", board.get(2).getName());
            Check.equal("new top score", 0, board.add("Smith, J", 30));

            board.save();
            Check.equal("file has 10 lines", 10, countLines(f));

            Leaderboard again = new Leaderboard(path);
            Check.equal("reload size", 10, again.size());
            boolean same = true;
            for (int i = 0; i < board.size(); i++) {
                if (!board.get(i).getName().equals(again.get(i).getName())
                        || board.get(i).getScore() != again.get(i).getScore()) {
                    same = false;
                }
            }
            Check.isTrue("reload keeps names, scores and order", same);
            Check.equal("name with a comma survives", "Smith, J", again.get(0).getName());

            // A hand-edited file: unsorted, with a junk line
            FileOutputStream out = new FileOutputStream(f);
            out.write("Amy,4\nnot a score\nBen,9\n,5\nCat,x\n".getBytes());
            out.close();
            Leaderboard edited = new Leaderboard(path);
            Check.equal("junk lines skipped", 2, edited.size());
            Check.equal("loaded file is re-sorted", "Ben", edited.get(0).getName());
        } catch (IOException e) {
            Check.isTrue("leaderboard file I/O: " + e.getMessage(), false);
        } finally {
            if (f != null) {
                f.delete();
            }
        }
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
