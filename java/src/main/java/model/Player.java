package model;

public class Player {
    private final String name, podId, tag;     // tag = this player's RFID tag ID
    public int score, lives = 3;
    public boolean connected = true;
    public Player(String name, String podId, String tag) { this.name = name; this.podId = podId; this.tag = tag; }
    public String name()  { return name; }
    public String podId() { return podId; }
    public String tag()   { return tag; }
}
