/** Observer of the pods: is told about every message a pod sends. */
public interface PodObserver {
    void update(PodMessage msg);
}
