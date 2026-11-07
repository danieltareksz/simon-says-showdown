/**
 * One player and their pod. The game thread changes the score and lives while the
 * window thread reads them, so every method is synchronized.
 */
public class Player {

    private String name;
    private String podId;      // "P1" or "P2"
    private String tag;        // RFID tag ID registered in the lobby
    private int score;
    private int lives;
    private boolean connected = true;

    public Player(String name, String podId, String tag) {
        this.name = name;
        this.podId = podId;
        this.tag = tag;
    }

    public synchronized String getName()  { return name; }
    public synchronized String getPodId() { return podId; }
    public synchronized String getTag()   { return tag; }
    public synchronized int getScore()    { return score; }
    public synchronized int getLives()    { return lives; }
    public synchronized boolean isConnected() { return connected; }

    public synchronized void setName(String name)          { this.name = name; }
    public synchronized void setScore(int score)           { this.score = score; }
    public synchronized void setLives(int lives)           { this.lives = lives; }
    public synchronized void setConnected(boolean c)       { this.connected = c; }

    /** Adds points (negative to subtract); the score never goes below 0. */
    public synchronized void addScore(int points) {
        score = Math.max(0, score + points);
    }

    public synchronized void loseLife() {
        if (lives > 0) {
            lives--;
        }
    }
}
