import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The Classic top 10, kept in a text file with one "name,score" line per entry,
 * highest first. The list is kept sorted by inserting each entry at its place,
 * so no sorting library is needed. The game thread adds scores while the window
 * may read them, so every public method is synchronized to protect the list.
 */
public class Leaderboard {

    public static final int SIZE = 10;

    private String fileName;
    private ArrayList<ScoreEntry> entries = new ArrayList<ScoreEntry>();

    /** Loads the file; a missing or unreadable file means an empty leaderboard. */
    public Leaderboard(String fileName) {
        this.fileName = fileName;
        try {
            load();
        } catch (IOException e) {
            System.out.println("Could not read " + fileName + ": " + e.getMessage());
        }
    }

    // Reads every line; malformed lines are skipped and the order is rebuilt by add()
    private synchronized void load() throws IOException {
        if (!new File(fileName).exists()) {
            return;
        }
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        try {
            for (;;) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                ScoreEntry e = ScoreEntry.parse(line);
                if (e != null) {
                    insert(e);
                }
            }
        } finally {
            input.close();
        }
    }

    /** True if this score would earn a place: above 0, and the board has room or beats the 10th. */
    public synchronized boolean isHighScore(int score) {
        if (score <= 0) {
            return false;
        }
        return entries.size() < SIZE || score > entries.get(entries.size() - 1).getScore();
    }

    /** Adds a score if it makes the top 10. Returns its place (0 = top), or -1 if it did not. */
    public synchronized int add(String name, int score) {
        if (!isHighScore(score)) {
            return -1;
        }
        return insert(new ScoreEntry(name, score));
    }

    // Puts the entry below every equal or higher score, so an older score keeps its place in a tie,
    // then drops anything past the 10th. Returns the index, or -1 if it fell off the end.
    // Only called from load() and add(), which already hold the lock.
    private int insert(ScoreEntry e) {
        int i = 0;
        while (i < entries.size() && entries.get(i).getScore() >= e.getScore()) {
            i++;
        }
        entries.add(i, e);
        while (entries.size() > SIZE) {
            entries.remove(entries.size() - 1);
        }
        return i < SIZE ? i : -1;
    }

    /** Writes the whole list back to the file, highest first. */
    public synchronized void save() throws IOException {
        FileOutputStream out = new FileOutputStream(fileName);
        try {
            for (int i = 0; i < entries.size(); i++) {
                out.write((entries.get(i).toLine() + "\n").getBytes());
            }
        } finally {
            out.close();
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized ScoreEntry get(int i) {
        return entries.get(i);
    }

    /** A copy of the list, so the caller can read it without holding the lock. */
    public synchronized ArrayList<ScoreEntry> getEntries() {
        return new ArrayList<ScoreEntry>(entries);
    }
}
