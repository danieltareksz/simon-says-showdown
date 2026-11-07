/** Tiny test helper: prints PASS or FAIL for each check and keeps a count. */
public class Check {

    private static int passed = 0;
    private static int failed = 0;

    public static void isTrue(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }

    public static void equal(String label, String expected, String actual) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        isTrue(label + " (expected " + expected + ", got " + actual + ")", same);
    }

    public static void equal(String label, long expected, long actual) {
        isTrue(label + " (expected " + expected + ", got " + actual + ")", expected == actual);
    }

    /** Prints the totals; returns true if nothing failed. */
    public static boolean summary() {
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        return failed == 0;
    }
}
