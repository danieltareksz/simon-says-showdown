package model;

/** One line from a pod, e.g. "P1,ACT,TILT,L,1240" (without the < >). */
public record PodMessage(String pod, String kind, String action, String value, long ms) {

    /** Returns null for anything malformed, so bad radio lines are simply dropped. */
    public static PodMessage parse(String line) {
        String[] p = line.split(",");
        if (p.length < 2 || !p[0].matches("P[12]")) return null;
        try {
            return switch (p[1]) {
                case "ACT"     -> p.length == 5 ? new PodMessage(p[0], "ACT", p[2], p[3], Long.parseLong(p[4])) : null;
                case "TAG"     -> p.length == 3 ? new PodMessage(p[0], "TAG", "RFID", p[2], 0) : null;
                case "TIMEOUT", "HB" -> new PodMessage(p[0], p[1], null, null, 0);
                case "DBG"     -> new PodMessage(p[0], "DBG", null, line, 0);
                default        -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
