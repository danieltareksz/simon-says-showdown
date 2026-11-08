import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Plays real Classic rounds with GameController and SimulatedPods, voice off. Checks that GO
 * is sent only after the instruction, that a correct answer scores, and that an answer given
 * after the time limit is ignored. Runs at the game's normal pace, so it takes about 15 s.
 */
public class TestGameFlow {

    // Waits until the recorder has seen n updates of this type; false after maxMs
    private static boolean waitFor(GameRecorder rec, String type, int n, long maxMs) throws InterruptedException {
        long end = System.currentTimeMillis() + maxMs;
        while (rec.count(type) < n) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            Thread.sleep(20);
        }
        return true;
    }

    /** The simulator command that performs one step, for example "p1 tilt l" or "p1 btn red 2". */
    public static String command(String pod, Step s) {
        String action = s.getAction();
        String target = s.getTarget().toLowerCase();
        if (action.equals("BTN")) {
            int u = target.indexOf('_');
            return pod + " btn " + target.substring(0, u) + " " + target.substring(u + 1);
        }
        if (action.equals("RFID")) {
            return pod + " tag";
        }
        return pod + " " + action.toLowerCase() + " " + target;
    }

    // Does what Simon asked; on a trick, the right answer is to do nothing
    private static void answer(SimulatedPods sim, Instruction ins, String pod) {
        if (!ins.isSimonSays()) {
            return;
        }
        for (int i = 0; i < ins.stepCount(); i++) {
            sim.inject(command(pod, ins.getStep(i)));
        }
    }

    public static void run() {
        File board = null;
        try {
            board = File.createTempFile("leaderboard", ".txt");
            board.delete();
            SimulatedPods sim = new SimulatedPods(false);
            GameController game = new GameController(sim, new ClipLibrary("sounds"),
                    new Leaderboard(board.getPath()), new InstructionFactory(new Random(7)));
            game.setVoiceOn(false);
            GameRecorder rec = new GameRecorder(sim);
            game.registerObserver(rec);
            sim.registerObserver(game);
            game.start();

            sim.inject("p1 tag");
            long end = System.currentTimeMillis() + 2000;
            while (!game.isJoined(0) && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.isTrue("a scan in the lobby joins player 1", game.isJoined(0));
            Check.isTrue("player 2 not joined", !game.isJoined(1));
            Player p = game.getSlots()[0];
            Check.equal("registered tag", "SIM0000001", p.getTag());
            Check.isTrue("start accepted in the lobby", game.requestStart());

            Check.isTrue("first GO within 8 s", waitFor(rec, GameUpdate.GO, 1, 8000));
            int instruction = rec.indexOf(GameUpdate.INSTRUCTION);
            Check.isTrue("instruction shown before GO", instruction >= 0 && instruction < rec.indexOf(GameUpdate.GO));
            Check.equal("no GO sent before the instruction", 0, rec.getGoCountAtFirstInstruction());
            Check.equal("GO sent before the GO update", 1, rec.getGoCountAtFirstGo());
            Check.isTrue("start refused during a game", !game.requestStart());
            Check.isTrue("mode change refused during a game", !game.setStrategy(new ShowdownScoring()));

            answer(sim, game.getInstruction(), "p1");
            Check.isTrue("round 1 judged", waitFor(rec, GameUpdate.RESULT, 1, 6000));
            Check.equal("correct answer scores 1", 1, p.getScore());
            Check.equal("correct answer keeps 3 lives", 3, p.getLives());
            Check.isTrue("result says correct", rec.lastText(GameUpdate.RESULT).startsWith("Correct!"));

            // Answer a real instruction only after the pod's time limit has passed
            boolean tested = false;
            for (int round = 2; round <= 8 && !tested; round++) {
                if (!waitFor(rec, GameUpdate.GO, round, 8000)) {
                    break;
                }
                Instruction ins = game.getInstruction();
                if (ins.isSimonSays()) {
                    long stop = System.currentTimeMillis() + ins.getTimeLimitMs() + 2000;
                    while (sim.isListening(0) && System.currentTimeMillis() < stop) {
                        Thread.sleep(20);
                    }
                    answer(sim, ins, "p1");
                    tested = true;
                }
                if (!waitFor(rec, GameUpdate.RESULT, round, 12000)) {
                    break;
                }
                if (tested) {
                    Check.equal("late answer costs a life", 2, p.getLives());
                    Check.isTrue("late answer is too slow", rec.lastText(GameUpdate.RESULT).startsWith("Too slow!"));
                }
            }
            Check.isTrue("a real instruction came up for the late-answer check", tested);
        } catch (IOException e) {
            Check.isTrue("game flow I/O: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Check.isTrue("game flow interrupted", false);
        } finally {
            if (board != null) {
                board.delete();
            }
        }
    }

    public static void main(String[] args) {
        run();
        Check.summary();
        System.exit(0);                        // the game and simulator threads never end on their own
    }
}
