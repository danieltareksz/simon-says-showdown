/** Runs every test driver. Exit code 1 if anything failed. */
public class RunAllTests {

    public static void main(String[] args) {
        TestPodMessage.run();
        TestInstruction.run();
        TestInstructionFactory.run();
        TestObserver.run();
        TestScoring.run();
        TestLeaderboard.run();
        // Add new test drivers here
        if (!Check.summary()) {
            System.exit(1);
        }
    }
}
