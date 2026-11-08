import java.util.ArrayList;

/**
 * Test observer that keeps the type and text of every game update. It also notes how many
 * GO messages the simulator had received when the first instruction and the first GO update
 * arrived, to check the order of events on the game thread.
 */
public class GameRecorder implements GameObserver {

    private SimulatedPods sim;
    private ArrayList<String> types = new ArrayList<String>();
    private ArrayList<String> texts = new ArrayList<String>();
    private int goCountAtFirstInstruction = -1;
    private int goCountAtFirstGo = -1;

    public GameRecorder(SimulatedPods sim) {
        this.sim = sim;
    }

    public synchronized void update(GameUpdate u) {
        types.add(u.getType());
        texts.add(u.getText());
        if (u.getType().equals(GameUpdate.INSTRUCTION) && goCountAtFirstInstruction < 0) {
            goCountAtFirstInstruction = sim.getGoCount();
        }
        if (u.getType().equals(GameUpdate.GO) && goCountAtFirstGo < 0) {
            goCountAtFirstGo = sim.getGoCount();
        }
    }

    public synchronized int count(String type) {
        int n = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).equals(type)) {
                n++;
            }
        }
        return n;
    }

    /** Position of the first update of this type; -1 if none. */
    public synchronized int indexOf(String type) {
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).equals(type)) {
                return i;
            }
        }
        return -1;
    }

    public synchronized String lastText(String type) {
        for (int i = types.size() - 1; i >= 0; i--) {
            if (types.get(i).equals(type)) {
                return texts.get(i);
            }
        }
        return null;
    }

    public synchronized int getGoCountAtFirstInstruction() { return goCountAtFirstInstruction; }
    public synchronized int getGoCountAtFirstGo()          { return goCountAtFirstGo; }
}
