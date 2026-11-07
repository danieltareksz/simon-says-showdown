/** Subject that produces game updates (the GameController). */
public interface GameSubject {
    public void registerObserver(GameObserver o);
    public void removeObserver(GameObserver o);
    public void notifyObservers(GameUpdate u);
}
