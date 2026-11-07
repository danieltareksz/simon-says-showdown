import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/** Reads sounds/clips.csv: each clip's name and the words it speaks (also used for screen text). */
public class ClipLibrary {

    private String folder;
    private ArrayList<String> names = new ArrayList<String>();
    private HashMap<String, String> phrases = new HashMap<String, String>();

    public ClipLibrary(String folder) throws IOException {
        this.folder = folder;
        BufferedReader input = new BufferedReader(new FileReader(folder + "/clips.csv"));
        try {
            for (;;) {
                String line = input.readLine();
                if (line == null) {
                    break;
                }
                int comma = line.indexOf(',');
                if (comma <= 0 || line.startsWith("name,")) {
                    continue;
                }
                String name = line.substring(0, comma).trim();
                names.add(name);
                phrases.put(name, line.substring(comma + 1).trim());
            }
        } finally {
            input.close();
        }
    }

    public String getFolder()              { return folder; }
    public ArrayList<String> getNames()    { return names; }

    /** The words for a clip; the clip name itself if it isn't in the file. */
    public String getPhrase(String name) {
        if (phrases.containsKey(name)) {
            return phrases.get(name);
        }
        return name;
    }
}
