import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Observer that speaks each update's clips one after another. update() runs on the
 * game thread and returns only when the last clip has finished playing, so the game
 * sends GO right after Simon stops talking.
 * javax.sound.sampled is used only in this class.
 */
public class VoicePlayer implements GameObserver {

    private HashMap<String, byte[]> sounds = new HashMap<String, byte[]>();
    private SourceDataLine line;
    private byte[] gap;                    // 60 ms of silence between clips

    public VoicePlayer(ClipLibrary library) throws IOException, LineUnavailableException {
        AudioFormat format = null;
        ArrayList<String> names = library.getNames();
        for (int i = 0; i < names.size(); i++) {
            File wav = new File(library.getFolder() + "/" + names.get(i) + ".wav");
            if (!wav.exists()) {
                System.out.println("Missing clip: " + wav);
                continue;
            }
            try {
                AudioInputStream in = AudioSystem.getAudioInputStream(wav);
                if (format == null) {
                    format = in.getFormat();       // every clip must share this format
                }
                if (format.matches(in.getFormat())) {
                    sounds.put(names.get(i), readAll(in));
                } else {
                    System.out.println("Wrong format, skipped: " + wav);
                }
                in.close();
            } catch (UnsupportedAudioFileException e) {
                System.out.println("Not a WAV file: " + wav);
            }
        }
        if (format == null) {
            throw new IOException("No voice clips found in " + library.getFolder());
        }
        gap = new byte[format.getFrameSize() * Math.round(format.getFrameRate() * 0.06f)];
        line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
    }

    public void update(GameUpdate u) {
        ArrayList<String> clips = u.getClips();
        for (int i = 0; i < clips.size(); i++) {
            byte[] data = sounds.get(clips.get(i));
            if (data == null) {
                continue;                          // missing clip: skip it, don't stall the game
            }
            line.write(data, 0, data.length);
            line.write(gap, 0, gap.length);
        }
        if (clips.size() > 0) {
            line.drain();                          // wait until the last sample has played
        }
    }

    // Reads a whole clip into memory with read(byte[], off, len), as in the Java I/O slides
    private byte[] readAll(AudioInputStream in) throws IOException {
        int size = (int) (in.getFrameLength() * in.getFormat().getFrameSize());
        byte[] data = new byte[size];
        int done = 0;
        while (done < size) {
            int n = in.read(data, done, size - done);
            if (n == -1) {
                break;
            }
            done += n;
        }
        return data;
    }
}
