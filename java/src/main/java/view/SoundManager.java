package view;

import model.GameEvent;
import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Boundary class: plays WAV clips back to back on one audio line. */
public class SoundManager {
    private final Map<String, byte[]> sounds = new HashMap<>();
    private final Map<String, String> phrases = new LinkedHashMap<>();
    private final BlockingQueue<GameEvent> events;
    private final ExecutorService player = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sound"); t.setDaemon(true); return t;
    });
    private final AudioFormat format;            // every clip must share it
    private final SourceDataLine line;
    private final byte[] gap;                    // 60 ms of silence between clips

    public SoundManager(Path dir, BlockingQueue<GameEvent> events) throws IOException, LineUnavailableException {
        this.events = events;
        for (String row : Files.readAllLines(dir.resolve("clips.csv"))) {          // name,text
            int c = row.indexOf(',');
            if (c > 0 && !row.startsWith("name,")) phrases.put(row.substring(0, c).trim(), row.substring(c + 1).trim());
        }
        AudioFormat f = null;
        for (String name : phrases.keySet()) {
            Path wav = dir.resolve(name + ".wav");
            if (!Files.exists(wav)) { System.err.println("Missing clip: " + wav); continue; }
            try (AudioInputStream in = AudioSystem.getAudioInputStream(wav.toFile())) {
                if (f == null) f = in.getFormat();
                else if (!f.matches(in.getFormat())) { System.err.println("Wrong format, skipped: " + wav); continue; }
                sounds.put(name, in.readAllBytes());
            } catch (UnsupportedAudioFileException e) {
                System.err.println("Not a WAV file: " + wav);
            }
        }
        if (f == null) throw new IOException("No clips found in " + dir);
        format = f;
        gap = new byte[format.getFrameSize() * Math.round(format.getFrameRate() * 0.06f)];
        line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
    }

    /** clip name -> spoken text, so the screen shows the same words the voice says. */
    public Map<String, String> phrases() { return Collections.unmodifiableMap(phrases); }

    /** An instruction: plays the clips in order, then posts VoiceDone so the round can send GO. */
    public void say(List<String> names) { play(names, true); }

    /** Feedback ("correct", "too_slow", ...): nothing waits for it. */
    public void announce(String... names) { play(List.of(names), false); }

    private void play(List<String> names, boolean notify) {
        player.submit(() -> {
            for (String n : names) {
                byte[] data = sounds.get(n);
                if (data == null) { System.err.println("No clip: " + n); continue; }
                line.write(data, 0, data.length);
                line.write(gap, 0, gap.length);
            }
            line.drain();                        // returns once the last sample has played
            if (notify) events.offer(new GameEvent.VoiceDone());
        });
    }
}
