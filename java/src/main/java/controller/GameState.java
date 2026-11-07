package controller;

import model.GameEvent;

/** Lobby, Countdown, InstructionRound, MemoryRound, Paused, GameOver. */
public interface GameState {
    default void enter(GameEngine engine) {}
    void handle(GameEngine engine, GameEvent event);
}
