import java.util.ArrayList;

/** Checks every rule of ClassicScoring and ShowdownScoring, including the floor at 0 and the win at 21. */
public class TestScoring {

    private static Instruction make(boolean simonSays) {
        ArrayList<Step> steps = new ArrayList<Step>();
        steps.add(new Step("TILT", "L"));
        return new Instruction(steps, simonSays, 4000, false);
    }

    // An answer from the given pod lines, for example "P1,ACT,TILT,L,500"
    private static Answer answer(String line1, String line2) {
        Answer a = new Answer();
        if (line1 != null) {
            a.add(PodMessage.parse(line1));
        }
        if (line2 != null) {
            a.add(PodMessage.parse(line2));
        }
        return a;
    }

    // Judges every player with the real Judge, then scores the round
    private static String play(ScoringStrategy s, Instruction ins, Player[] players, Answer[] answers) {
        boolean[] correct = new boolean[players.length];
        for (int i = 0; i < players.length; i++) {
            correct[i] = Judge.isCorrect(ins, answers[i].getActs(), players[i]);
        }
        return s.scoreRound(ins, players, answers, correct);
    }

    private static void classic() {
        ClassicScoring s = new ClassicScoring();
        Player p = new Player("Daniel", "P1", "0415AB3C9D");
        Player[] ps = { p };
        Instruction real = make(true);
        Instruction trick = make(false);
        Answer right = answer("P1,ACT,TILT,L,500", null);
        Answer wrong = answer("P1,ACT,TILT,R,500", null);
        Answer none = answer(null, null);

        Check.equal("classic needs 1 player", 1, s.playersNeeded());
        p.setScore(7);
        s.startGame(ps);
        Check.equal("classic start score", 0, p.getScore());
        Check.equal("classic start lives", 3, p.getLives());

        Check.equal("classic correct clip", "correct", play(s, real, ps, new Answer[] { right }));
        Check.equal("classic correct +1", 1, p.getScore());
        Check.equal("classic still on trick clip", "correct", play(s, trick, ps, new Answer[] { none }));
        Check.equal("classic still on trick +1", 2, p.getScore());
        Check.equal("classic lives unchanged", 3, p.getLives());

        Check.equal("classic wrong clip", "wrong", play(s, real, ps, new Answer[] { wrong }));
        Check.equal("classic wrong -1 life", 2, p.getLives());
        Check.equal("classic wrong keeps score", 2, p.getScore());
        Check.isTrue("classic not over at 2 lives", !s.isOver(ps));

        Check.equal("classic acted on trick clip", "gotcha", play(s, trick, ps, new Answer[] { right }));
        Check.equal("classic trick -1 life", 1, p.getLives());

        Check.equal("classic timeout clip", "too_slow", play(s, real, ps, new Answer[] { none }));
        Check.equal("classic timeout -1 life", 0, p.getLives());
        Check.isTrue("classic over at 0 lives", s.isOver(ps));
        Check.equal("classic end clip", "game_over", s.endClip(ps));
        Check.equal("classic result text", "Game over: 2 points", s.resultText(ps));
    }

    private static void showdown() {
        ShowdownScoring s = new ShowdownScoring();
        Player p1 = new Player("Ann", "P1", "0415AB3C9D");
        Player p2 = new Player("Bob", "P2", "0415AB3C9E");
        Player[] ps = { p1, p2 };
        Instruction real = make(true);
        Instruction trick = make(false);

        Check.equal("showdown needs 2 players", 2, s.playersNeeded());
        p1.setScore(9);
        s.startGame(ps);
        Check.equal("showdown start score", 0, p1.getScore());

        // Both correct: the faster gets +3, the other +1
        String clip = play(s, real, ps, new Answer[] {
            answer("P1,ACT,TILT,L,400", null), answer("P2,ACT,TILT,L,700", null) });
        Check.equal("p1 faster clip", "p1_fastest", clip);
        Check.equal("fastest +3", 3, p1.getScore());
        Check.equal("slower correct +1", 1, p2.getScore());

        clip = play(s, real, ps, new Answer[] {
            answer("P1,ACT,TILT,L,900", null), answer("P2,ACT,TILT,L,300", null) });
        Check.equal("p2 faster clip", "p2_fastest", clip);
        Check.equal("p1 slower +1", 4, p1.getScore());
        Check.equal("p2 fastest +3", 4, p2.getScore());

        // Equal times are both fastest
        clip = play(s, real, ps, new Answer[] {
            answer("P1,ACT,TILT,L,500", null), answer("P2,ACT,TILT,L,500", null) });
        Check.equal("dead heat clip", "correct", clip);
        Check.equal("dead heat p1 +3", 7, p1.getScore());
        Check.equal("dead heat p2 +3", 7, p2.getScore());

        // Wrong action -2; the only correct player is fastest
        clip = play(s, real, ps, new Answer[] {
            answer("P1,ACT,TILT,R,200", null), answer("P2,ACT,TILT,L,800", null) });
        Check.equal("only p2 correct clip", "p2_fastest", clip);
        Check.equal("wrong action -2", 5, p1.getScore());
        Check.equal("only correct gets +3", 10, p2.getScore());

        // Timeout on a real instruction: 0
        clip = play(s, real, ps, new Answer[] { answer(null, null), answer(null, null) });
        Check.equal("nobody correct clip", "nobody", clip);
        Check.equal("timeout p1 0", 5, p1.getScore());
        Check.equal("timeout p2 0", 10, p2.getScore());

        // Trick: still +1, acted -2
        clip = play(s, trick, ps, new Answer[] { answer(null, null), answer(null, null) });
        Check.equal("trick both still clip", "correct", clip);
        Check.equal("trick still p1 +1", 6, p1.getScore());
        Check.equal("trick still p2 +1", 11, p2.getScore());
        clip = play(s, trick, ps, new Answer[] { answer("P1,ACT,TILT,L,300", null), answer(null, null) });
        Check.equal("trick acted clip", "gotcha", clip);
        Check.equal("acted on trick -2", 4, p1.getScore());
        Check.equal("trick still +1", 12, p2.getScore());

        // Multi-step: fastest is decided by the last action
        ArrayList<Step> two = new ArrayList<Step>();
        two.add(new Step("TILT", "L"));
        two.add(new Step("JOY", "UP"));
        Instruction seq = new Instruction(two, true, 8000, false);
        clip = play(s, seq, ps, new Answer[] {
            answer("P1,ACT,TILT,L,100", "P1,ACT,JOY,UP,900"),
            answer("P2,ACT,TILT,L,300", "P2,ACT,JOY,UP,600") });
        Check.equal("two-step: last action decides", "p2_fastest", clip);

        // Floor at 0
        p1.setScore(1);
        play(s, real, ps, new Answer[] { answer("P1,ACT,TILT,R,200", null), answer(null, null) });
        Check.equal("score floors at 0", 0, p1.getScore());

        // Winning
        p1.setScore(20);
        p2.setScore(3);
        Check.isTrue("20 is not a win", !s.isOver(ps));
        p1.setScore(21);
        p2.setScore(15);
        Check.isTrue("21 wins", s.isOver(ps));
        Check.equal("p1 win clip", "p1_wins", s.endClip(ps));
        Check.equal("result text", "Ann wins 21 to 15", s.resultText(ps));
        p1.setScore(21);
        p2.setScore(21);
        Check.isTrue("tie at 21 plays on", !s.isOver(ps));
        p2.setScore(22);
        Check.isTrue("22 to 21 wins", s.isOver(ps));
        Check.equal("p2 win clip", "p2_wins", s.endClip(ps));
        Check.equal("p2 result text", "Bob wins 22 to 21", s.resultText(ps));
    }

    public static void run() {
        classic();
        showdown();
    }

    public static void main(String[] args) {
        run();
        Check.summary();
    }
}
