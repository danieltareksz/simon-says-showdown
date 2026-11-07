import java.util.ArrayList;

/** What the game tells its observers (window, console, voice) each time something changes. */
public class GameUpdate {

    public static final String PHASE = "PHASE";             // lobby, countdown, paused, game over
    public static final String INSTRUCTION = "INSTRUCTION"; // Simon's new instruction
    public static final String GO = "GO";                   // players may act now
    public static final String RESULT = "RESULT";           // round judged
    public static final String SCORES = "SCORES";           // scores or lives changed
    public static final String INFO = "INFO";               // anything else worth showing

    private String type;
    private String text;
    private Player[] players;                                // read-only for observers
    private ArrayList<String> clips = new ArrayList<String>();  // voice clips to play, in order

    public GameUpdate(String type, String text, Player[] players) {
        this.type = type;
        this.text = text;
        this.players = players;
    }

    public void addClip(String name) {
        clips.add(name);
    }

    public void addClips(ArrayList<String> names) {
        for (int i = 0; i < names.size(); i++) {
            clips.add(names.get(i));
        }
    }

    public String getType()              { return type; }
    public String getText()              { return text; }
    public Player[] getPlayers()         { return players; }
    public ArrayList<String> getClips()  { return clips; }
}
