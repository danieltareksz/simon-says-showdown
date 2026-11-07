/** Subject that produces pod messages (the real XBee link, or the keyboard simulator). */
public interface PodSubject {
    public void registerObserver(PodObserver o);
    public void removeObserver(PodObserver o);
    public void notifyObservers(PodMessage msg);
}
