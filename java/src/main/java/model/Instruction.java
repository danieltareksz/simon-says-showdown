package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** One round's command from Simon: one or more steps, real or trick, with a time limit. */
public record Instruction(List<Step> steps, boolean simonSays, long timeLimitMs, boolean memory) {

    /** ("TILT","L") = tilt left. ("RFID","TAG") = scan your tag. */
    public record Step(String action, String target) {
        public String clip() { return (action + "_" + target).toLowerCase(); }   // "tilt_l", "btn_red_2"
    }

    /** Clip names in the order they are spoken. */
    public List<String> clips() {
        List<String> out = new ArrayList<>();
        if (memory) out.add("memory_round");
        if (simonSays) out.add("simon_says");
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0 && !memory) out.add("then");
            out.add(steps.get(i).clip());
        }
        return out;
    }

    /** Screen text, built from the same phrases as the clips (clips.csv). */
    public String text(Map<String, String> phrase) {
        String prefix = memory ? "Memory round: " : simonSays ? "Simon says: " : "";
        StringJoiner j = new StringJoiner(memory ? "  →  " : ", then ");
        for (int i = 0; i < steps.size(); i++) {
            String w = phrase.getOrDefault(steps.get(i).clip(), steps.get(i).clip());
            if (i > 0 || !prefix.isEmpty()) w = Character.toLowerCase(w.charAt(0)) + w.substring(1);
            j.add(w);
        }
        return prefix + j;
    }

    /** True if this player's reported actions match every step, in order. */
    public boolean matches(List<PodMessage> acts, Player p) {
        if (acts.size() != steps.size()) return false;
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            PodMessage a = acts.get(i);
            String want = s.action().equals("RFID") ? p.tag() : s.target();
            if (!s.action().equals(a.action()) || !want.equals(a.value())) return false;
        }
        return true;
    }
}
