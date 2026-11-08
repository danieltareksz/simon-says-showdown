/**
 * Watches the heartbeats (one HB every 500 ms) of the joined pods. A pod silent for more
 * than 2 s is reported to the GameController as lost, which pauses a running game; its
 * next heartbeat reports it back.
 * Observer in the Observer pattern (of XBeeLink or SimulatedPods), with its own thread
 * that does the checking.
 */
public class HeartbeatMonitor implements PodObserver, Runnable {

    public static final long CHECK_MS = 500;
    public static final long SILENT_MS = 2000;

    private GameController game;
    private long[] lastBeat = new long[2];     // shared with the radio thread: synchronized access only
    private boolean[] lost = new boolean[2];   // used only by this monitor's thread
    private long[] beatWhenLost = new long[2];
    private Thread t;

    public HeartbeatMonitor(GameController game) {
        this.game = game;
        long now = System.currentTimeMillis();
        lastBeat[0] = now;                     // give both pods a full 2 s from start-up
        lastBeat[1] = now;
        t = new Thread(this);
        t.setName("heartbeat");
        t.start();
    }

    public Thread getThread() {
        return t;
    }

    /** Stores the time of each heartbeat; everything else is ignored. */
    public synchronized void update(PodMessage msg) {
        if (msg.getKind().equals("HB")) {
            lastBeat[msg.getPod().equals("P2") ? 1 : 0] = System.currentTimeMillis();
        }
    }

    private synchronized long getLastBeat(int pod) {
        return lastBeat[pod];
    }

    public void run() {
        try {
            for (;;) {
                Thread.sleep(CHECK_MS);
                for (int i = 0; i < 2; i++) {
                    check(i);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Heartbeat monitor stopped");
        }
    }

    // Not synchronized: the game's methods are called without holding this monitor's lock,
    // so this thread never holds two locks at once
    private void check(int pod) {
        if (!game.isJoined(pod)) {
            return;
        }
        long beat = getLastBeat(pod);
        if (!lost[pod] && System.currentTimeMillis() - beat > SILENT_MS) {
            lost[pod] = true;
            beatWhenLost[pod] = beat;
            game.podLost(pod);
        } else if (lost[pod] && beat > beatWhenLost[pod]) {
            lost[pod] = false;                 // a heartbeat arrived since it was lost
            game.podBack(pod);
        }
    }
}
