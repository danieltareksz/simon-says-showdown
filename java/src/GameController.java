import java.io.IOException;
import java.util.ArrayList;

/**
 * Runs the game on its own thread: lobby, countdown, rounds, judging, pause and game over.
 * Three roles:
 *  - Observer of the pods (Observer pattern #1): update() is called on the radio thread
 *    for every pod message.
 *  - Subject for the outputs (Observer pattern #2): the console, window and voice get a
 *    GameUpdate for every change.
 *  - Context of the Strategy pattern: a ScoringStrategy holds the rules of the chosen mode.
 * Thread rules: only the game thread sends game messages to the pods and notifies observers,
 * and it never does either while holding this object's lock (so a radio thread holding a
 * subject's lock can always get into update()). Every field shared with another thread is
 * read and written only in synchronized methods.
 */
public class GameController extends ConcreteGameSubject implements PodObserver, Runnable {

    // Phases
    public static final int LOBBY = 0;
    public static final int COUNTDOWN = 1;
    public static final int ROUND = 2;
    public static final int PAUSED = 3;
    public static final int GAME_OVER = 4;

    // Timing
    public static final long POLL_MS = 20;            // how often a waiting loop checks again
    public static final long LOBBY_POLL_MS = 50;
    public static final long PAUSE_POLL_MS = 100;
    public static final long GRACE_MS = 300;          // extra wait after the limit, for radio delay
    public static final long RESULT_PAUSE_MS = 1200;  // time to read the result
    public static final long NO_VOICE_MS = 1500;      // time to read the instruction with the voice off
    public static final long TICK_MS = 1000;          // countdown tick

    // Results of takeStartRequest()
    private static final int NO_REQUEST = 0;
    private static final int READY = 1;
    private static final int NOT_ENOUGH = 2;

    private PodSender pods;
    private ClipLibrary clips;
    private Leaderboard leaderboard;
    private InstructionFactory factory;
    private Thread t;

    // Shared with the radio, heartbeat, console and window threads: synchronized access only
    private int phase = LOBBY;
    private Player[] slots = new Player[2];            // index 0 = pod P1, 1 = pod P2; null until joined
    private String[] names = { "Player 1", "Player 2" };
    private String[] pendingTag = new String[2];       // tag scanned in the lobby, not yet announced
    private boolean[] lost = new boolean[2];           // pod gone silent, from HeartbeatMonitor
    private String news;                               // connection change to announce in the lobby
    private Player[] players = new Player[0];          // the players in the current game
    private Answer[] answers = new Answer[0];          // one per player in the current round
    private int expectedSteps;
    private boolean listening;
    private boolean startRequested;
    private boolean voiceOn = true;
    private ScoringStrategy strategy = new ClassicScoring();
    private Instruction current;

    // Used only by the game thread
    private int announcedLevel;

    public GameController(PodSender pods, ClipLibrary clips, Leaderboard leaderboard, InstructionFactory factory) {
        this.pods = pods;
        this.clips = clips;
        this.leaderboard = leaderboard;
        this.factory = factory;
    }

    /** Starts the game thread. Not done in the constructor, so every observer can register first. */
    public void start() {
        t = new Thread(this);
        t.setName("game");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    public void run() {
        try {
            for (;;) {
                lobby();
                playGame();
            }
        } catch (InterruptedException e) {
            System.out.println("Game stopped");
        }
    }

    // ===== Game thread =====

    /** Announces joins until a start is requested with enough players. Players stay joined between games. */
    private void lobby() throws InterruptedException {
        setPhase(LOBBY);
        tell(GameUpdate.PHASE, "Scan your card to join (" + getModeName() + ")", null, getSlots());
        for (;;) {
            int joined = takePendingJoin();
            if (joined >= 0) {
                pods.send(podId(joined), "FB,B");
                Player[] slotsNow = getSlots();
                tell(GameUpdate.INFO, slotsNow[joined].getName() + " joined", "p" + (joined + 1) + "_joined", slotsNow);
            }
            String n = takeNews();
            if (n != null) {
                tell(GameUpdate.INFO, n, null, getSlots());
            }
            int start = takeStartRequest();
            if (start == READY) {
                return;
            }
            if (start == NOT_ENOUGH) {
                tell(GameUpdate.INFO, getModeName() + " needs " + getStrategy().playersNeeded()
                        + " player(s): scan a card to join", null, getSlots());
            }
            Thread.sleep(LOBBY_POLL_MS);
        }
    }

    /** One whole game with the players and rules fixed when it started. */
    private void playGame() throws InterruptedException {
        Player[] ps = getGamePlayers();
        ScoringStrategy rules = getStrategy();
        rules.startGame(ps);
        announcedLevel = 0;
        pods.send("ALL", "CAL");
        tell(GameUpdate.SCORES, scoreText(ps, rules), null, ps);
        countdown(ps);
        int round = 1;
        while (!rules.isOver(ps)) {
            if (isPaused()) {
                waitForReconnect(ps);
                countdown(ps);
            }
            if (playRound(round, ps, rules)) {
                round++;                       // a round cut short by a pause is played again
            }
        }
        gameOver(ps, rules);
    }

    /** "Get ready", then 3, 2, 1 with a beep on the pods, about one second apart. */
    private void countdown(Player[] ps) throws InterruptedException {
        setPhase(COUNTDOWN);
        tell(GameUpdate.PHASE, "Get ready", "get_ready", ps);
        for (int k = 3; k >= 1; k--) {
            long tick = System.currentTimeMillis();
            pods.send("ALL", "BEEP," + k);
            tell(GameUpdate.INFO, String.valueOf(k), "count_" + k, ps);
            // the voice may already have used part of the second
            Thread.sleep(Math.max(0, TICK_MS - (System.currentTimeMillis() - tick)));
        }
    }

    /**
     * Plays one round: instruction, GO, collect answers, judge, score.
     * Returns false if a disconnect interrupted it, so the caller plays the round again.
     */
    private boolean playRound(int round, Player[] ps, ScoringStrategy rules) throws InterruptedException {
        if (isPaused()) {
            return false;
        }
        Instruction ins = factory.next(round);
        setCurrent(ins);
        int level = DifficultyManager.level(round);
        if (level > announcedLevel) {
            announcedLevel = level;
            tell(GameUpdate.INFO, "Level up! Level " + level, "level_up", ps);
        }
        tell(GameUpdate.INFO, "Round " + round, null, ps);

        // The voice observer blocks until Simon has finished, so GO follows the last word
        GameUpdate u = new GameUpdate(GameUpdate.INSTRUCTION, ins.text(clips), ps);
        u.addClips(ins.clips());
        notifyObservers(u);
        if (!isVoiceOn()) {
            Thread.sleep(NO_VOICE_MS);
        }
        if (ins.isMemory()) {
            tell(GameUpdate.INFO, "Your turn!", null, ps);      // the window hides the sequence now
        }

        int n = ins.stepCount();
        long limit = ins.getTimeLimitMs();
        startListening(n);
        long start = System.currentTimeMillis();
        if (n == 1) {
            pods.send("ALL", "GO," + limit);
        } else {
            pods.send("ALL", "SEQ," + n + "," + limit);
        }
        tell(GameUpdate.GO, String.valueOf(limit), null, ps);

        while (System.currentTimeMillis() <= start + limit + GRACE_MS && !allDone() && !isPaused()) {
            Thread.sleep(POLL_MS);
        }
        Answer[] got = stopListening();
        pods.send("ALL", "STOP");
        if (isPaused()) {
            return false;
        }

        boolean[] correct = new boolean[ps.length];
        String details = "";
        for (int i = 0; i < ps.length; i++) {
            correct[i] = Judge.isCorrect(ins, got[i].getActs(), ps[i]);
            pods.send(ps[i].getPodId(), correct[i] ? "FB,G" : "FB,R");
            details = details + "  " + ps[i].getName() + ": " + (correct[i] ? "right" : "wrong");
            if (got[i].getActs().size() > 0) {
                details = details + " (" + got[i].lastMs() + " ms)";
            }
        }
        String clip = rules.scoreRound(ins, ps, got, correct);
        tell(GameUpdate.RESULT, clips.getPhrase(clip) + details, clip, ps);
        tell(GameUpdate.SCORES, scoreText(ps, rules), null, ps);
        Thread.sleep(RESULT_PAUSE_MS);
        return true;
    }

    /** Holds the game until every pod in it is back. */
    private void waitForReconnect(Player[] ps) throws InterruptedException {
        setPhase(PAUSED);
        tell(GameUpdate.PHASE, "Pod disconnected - game paused", "pod_lost", ps);
        pods.send("ALL", "STOP");
        while (isPaused()) {
            Thread.sleep(PAUSE_POLL_MS);
        }
        takeNews();                            // the countdown that follows says it all
        tell(GameUpdate.INFO, "Pod back - resuming", null, ps);
    }

    /** Shows the result, updates the Classic leaderboard, then waits for "start" (play again). */
    private void gameOver(Player[] ps, ScoringStrategy rules) throws InterruptedException {
        setPhase(GAME_OVER);
        tell(GameUpdate.PHASE, rules.resultText(ps), rules.endClip(ps), ps);
        if (rules instanceof ClassicScoring) {
            int place = leaderboard.add(ps[0].getName(), ps[0].getScore());
            if (place >= 0) {
                try {
                    leaderboard.save();
                } catch (IOException e) {
                    System.out.println("Could not save the leaderboard: " + e.getMessage());
                }
                tell(GameUpdate.INFO, "New high score! Place " + (place + 1), "new_high_score", ps);
            }
            tell(GameUpdate.INFO, leaderboardText(), null, ps);
        }
        tell(GameUpdate.INFO, "Start to play again", null, ps);
        // The request is left set, so the lobby starts the next game straight away
        while (!isStartRequested()) {
            Thread.sleep(LOBBY_POLL_MS);
        }
    }

    // Builds an update, adds the clip if there is one, and notifies every observer
    private void tell(String type, String text, String clip, Player[] who) {
        GameUpdate u = new GameUpdate(type, text, who);
        if (clip != null) {
            u.addClip(clip);
        }
        notifyObservers(u);
    }

    private String scoreText(Player[] ps, ScoringStrategy rules) {
        String text = "";
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) {
                text = text + "   ";
            }
            text = text + ps[i].getName() + ": " + ps[i].getScore();
            if (rules instanceof ClassicScoring) {
                text = text + " points, " + ps[i].getLives() + " lives";
            }
            if (!ps[i].isConnected()) {
                text = text + " (DISCONNECTED)";
            }
        }
        return text;
    }

    private String leaderboardText() {
        ArrayList<ScoreEntry> top = leaderboard.getEntries();
        String text = "Leaderboard:";
        for (int i = 0; i < top.size(); i++) {
            text = text + "  " + (i + 1) + ". " + top.get(i).getName() + " " + top.get(i).getScore();
        }
        return text;
    }

    // ===== Observer of the pods (radio thread) =====

    /** Stores the message where the game thread will find it, then returns at once. */
    public synchronized void update(PodMessage m) {
        String kind = m.getKind();
        if (kind.equals("TAG") && (phase == LOBBY || phase == GAME_OVER)) {
            // also on the game-over screen, so a new player can join before "play again"
            noteJoin(podIndex(m.getPod()), m.getValue());
        } else if (kind.equals("ACT") && listening) {
            int i = playerOf(m.getPod());
            if (i >= 0 && !answers[i].isTimedOut() && answers[i].getActs().size() < expectedSteps) {
                answers[i].add(m);
            }
        } else if (kind.equals("TIMEOUT") && listening) {
            int i = playerOf(m.getPod());
            if (i >= 0) {
                answers[i].setTimedOut();
            }
        } else if (kind.equals("DBG")) {
            System.out.println("[debug] " + m.getValue());
        }
    }

    // A tag already used by the other pod is ignored: one card per player. Caller holds the lock.
    private void noteJoin(int pod, String tag) {
        int other = 1 - pod;
        if (slots[other] != null && tag.equals(slots[other].getTag())) {
            return;
        }
        if (tag.equals(pendingTag[other])) {
            return;
        }
        pendingTag[pod] = tag;
    }

    // Index in the current game of the player on this pod; -1 if that pod isn't playing. Caller holds the lock.
    private int playerOf(String podId) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].getPodId().equals(podId)) {
                return i;
            }
        }
        return -1;
    }

    // ===== Synchronized state used by the game thread =====

    private synchronized void setPhase(int p) {
        phase = p;
    }

    // Also moves to the ROUND phase; the window and tests read the instruction with getInstruction()
    private synchronized void setCurrent(Instruction ins) {
        current = ins;
        phase = ROUND;
    }

    /** Turns a pending lobby scan into a joined player. Returns the pod index, or -1 if none. */
    private synchronized int takePendingJoin() {
        for (int i = 0; i < slots.length; i++) {
            if (pendingTag[i] != null) {
                slots[i] = new Player(names[i], podId(i), pendingTag[i]);
                slots[i].setConnected(!lost[i]);
                pendingTag[i] = null;
                return i;
            }
        }
        return -1;
    }

    private synchronized String takeNews() {
        String n = news;
        news = null;
        return n;
    }

    /**
     * Answers a start request. With enough players it fixes who plays (Classic: the first joined
     * pod; Showdown: P1 and P2) and leaves the lobby, so later tags and mode changes are refused.
     */
    private synchronized int takeStartRequest() {
        if (!startRequested) {
            return NO_REQUEST;
        }
        startRequested = false;
        int need = strategy.playersNeeded();
        Player[] ps = new Player[need];
        int n = 0;
        for (int i = 0; i < slots.length && n < need; i++) {
            if (slots[i] != null) {
                ps[n] = slots[i];
                n++;
            }
        }
        if (n < need) {
            return NOT_ENOUGH;
        }
        players = ps;
        phase = COUNTDOWN;
        return READY;
    }

    private synchronized Player[] getGamePlayers() {
        return players;
    }

    private synchronized ScoringStrategy getStrategy() {
        return strategy;
    }

    // A fresh Answer per player; from now on update() files ACT and TIMEOUT messages
    private synchronized void startListening(int steps) {
        answers = new Answer[players.length];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = new Answer();
        }
        expectedSteps = steps;
        listening = true;
    }

    // After this the radio thread no longer touches these answers, so the game thread can read them freely
    private synchronized Answer[] stopListening() {
        listening = false;
        return answers;
    }

    private synchronized boolean allDone() {
        for (int i = 0; i < answers.length; i++) {
            if (!answers[i].isDone(expectedSteps)) {
                return false;
            }
        }
        return true;
    }

    /** True during a game while any pod in it is silent. */
    private synchronized boolean isPaused() {
        if (phase == LOBBY || phase == GAME_OVER) {
            return false;
        }
        for (int i = 0; i < players.length; i++) {
            if (lost[podIndex(players[i].getPodId())]) {
                return true;
            }
        }
        return false;
    }

    private synchronized boolean isStartRequested() {
        return startRequested;
    }

    // ===== Setters for the console and window threads =====

    /** Chooses the mode; only between games. Returns false if refused. */
    public synchronized boolean setStrategy(ScoringStrategy s) {
        if (phase != LOBBY && phase != GAME_OVER) {
            return false;
        }
        strategy = s;
        return true;
    }

    /** index 0 = player 1 (pod P1), 1 = player 2. */
    public synchronized void setPlayerName(int index, String name) {
        names[index] = name;
        if (slots[index] != null) {
            slots[index].setName(name);
        }
    }

    /** Start from the lobby, or play again after a game. Returns false during a game. */
    public synchronized boolean requestStart() {
        if (phase != LOBBY && phase != GAME_OVER) {
            return false;
        }
        startRequested = true;
        return true;
    }

    public synchronized void setVoiceOn(boolean on) {
        voiceOn = on;
    }

    public synchronized boolean isVoiceOn() {
        return voiceOn;
    }

    // ===== For the heartbeat monitor =====

    public synchronized boolean isJoined(int pod) {
        return slots[pod] != null;
    }

    public synchronized void podLost(int pod) {
        lost[pod] = true;
        if (slots[pod] != null) {
            slots[pod].setConnected(false);
            news = slots[pod].getName() + "'s pod disconnected";
        }
    }

    public synchronized void podBack(int pod) {
        lost[pod] = false;
        if (slots[pod] != null) {
            slots[pod].setConnected(true);
            news = slots[pod].getName() + "'s pod is back";
        }
    }

    // ===== Getters for the window and tests =====

    public synchronized int getPhase() {
        return phase;
    }

    public synchronized String getModeName() {
        return strategy.toString();
    }

    /** A copy of the two player slots (P1, P2); an entry is null until that pod joins. */
    public synchronized Player[] getSlots() {
        Player[] copy = { slots[0], slots[1] };
        return copy;
    }

    public synchronized Instruction getInstruction() {
        return current;
    }

    // "P1" is index 0, "P2" is index 1
    private static int podIndex(String podId) {
        return podId.equals("P2") ? 1 : 0;
    }

    private static String podId(int index) {
        return "P" + (index + 1);
    }
}
