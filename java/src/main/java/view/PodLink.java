package view;

/** Anything that can talk to the pods: the real XBee, or the keyboard simulator. */
public interface PodLink {
    /** send("ALL", "GO,4000") or send("P2", "FB,G") -> writes "<ALL,GO,4000>\n" */
    void send(String target, String body);
}
