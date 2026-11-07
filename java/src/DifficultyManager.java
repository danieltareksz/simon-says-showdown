/** Level = round / 5. The time limit drops from 4.0 s to 1.5 s; the trick chance rises from 15% to 45%. */
public class DifficultyManager {

    public static int level(int round) {
        return round / 5;
    }

    public static long timeLimitMs(int round) {
        return Math.max(1500, 4000 - 500L * level(round));
    }

    public static double trickChance(int round) {
        return Math.min(0.45, 0.15 + 0.07 * level(round));
    }
}
