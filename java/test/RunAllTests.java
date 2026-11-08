/** Runs every test driver. Exit code 1 if anything failed. */
public class RunAllTests {

    public static void main(String[] args) {
        TestPodMessage.run();
        TestInstruction.run();
        TestInstructionFactory.run();
        TestObserver.run();
        TestScoring.run();
        TestLeaderboard.run();
        TestSimulatedPods.run();
        TestGameFlow.run();
        // Add new test drivers here
        // System.exit in both cases: the game and simulator threads started by the tests never end
        if (!Check.summary()) {
            System.exit(1);
        }
        System.exit(0);
    }
}
