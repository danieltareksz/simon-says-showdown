package controller;

import model.GameEvent;
import java.util.concurrent.*;

/** Control class. Its thread is the ONLY one that changes game data. */
public class GameEngine implements Runnable {
    private final BlockingQueue<GameEvent> events = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService timers = Executors.newSingleThreadScheduledExecutor();
    private GameState state;

    public GameEngine(GameState first) { this.state = first; }

    public BlockingQueue<GameEvent> events() { return events; }

    public void setState(GameState next) { state = next; state.enter(this); }

    /** Posts TimerEvent(name) after ms; the state that set it decides what it means. */
    public ScheduledFuture<?> schedule(String name, long ms) {
        return timers.schedule(() -> events.offer(new GameEvent.TimerEvent(name)), ms, TimeUnit.MILLISECONDS);
    }

    @Override public void run() {
        state.enter(this);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                GameEvent e = events.take();          // waits for the next event
                state.handle(this, e);                // the current state decides what it means
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
