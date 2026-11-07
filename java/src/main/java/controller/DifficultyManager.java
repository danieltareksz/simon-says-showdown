package controller;

/** Level = round / 5. Time drops 4.0 s -> 1.5 s; trick chance rises 15% -> 45%. */
public final class DifficultyManager {
    private DifficultyManager() {}
    public static int    level(int round)       { return round / 5; }
    public static long   timeLimitMs(int round) { return Math.max(1500, 4000 - 500L * level(round)); }
    public static double trickChance(int round) { return Math.min(0.45, 0.15 + 0.07 * level(round)); }
}
