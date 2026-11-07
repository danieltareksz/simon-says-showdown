/** Runs every test driver. Exit code 1 if anything failed. */
public class RunAllTests {

    public static void main(String[] args) {
        TestPodMessage.run();
        TestInstruction.run();
        TestInstructionFactory.run();
        TestObserver.run();
        // Add new test drivers here (TestScoring, TestLeaderboard, ...)
        if (!Check.summary()) {
            System.exit(1);
        }
    }
}
