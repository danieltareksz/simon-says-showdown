package model;

/** Everything the engine reacts to arrives as one of these, on one queue. */
public sealed interface GameEvent {
    record PodEvent(PodMessage msg)  implements GameEvent {}
    record TimerEvent(String name)   implements GameEvent {}
    record PodLost(String pod)       implements GameEvent {}
    record PodBack(String pod)       implements GameEvent {}
    record VoiceDone()               implements GameEvent {}
    record UiEvent(String button)    implements GameEvent {}   // Start, Pause, ...
}
