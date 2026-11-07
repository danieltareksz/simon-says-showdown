/** Anything that can send a message to the pods: the real XBee link or the simulator. */
public interface PodSender {
    /** send("ALL", "GO,4000") writes "<ALL,GO,4000>" followed by a newline. */
    void send(String target, String body);
}
