import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The big-screen display: Simon's instruction, a status line, one line per player, and the
 * lobby controls (mode, names, start). In simulator mode the keyboard plays both pods: each
 * key is turned into the same command a player could type in the console, for example
 * W gives "p1 joy up".
 * Observer in the Observer pattern (of GameController), exactly like ConsoleDisplay; the game
 * never depends on it and plays the same with --nogui.
 * Threads: update() is called on the game thread and only queues a WindowRefresh; all label
 * changes happen in apply() on the Swing event thread. The buttons only call the controller's
 * synchronized setters. A key press calls SimulatedPods.inject() on the Swing event thread,
 * which takes the simulator's lock and then the controller's, the same order as the console
 * thread. Swing and AWT are used only here and in WindowRefresh.
 */
public class GameWindow implements GameObserver, ActionListener, KeyListener {

    private static final Color BACKGROUND = new Color(20, 22, 34);
    private static final Color TEXT = Color.WHITE;
    private static final Color GOOD = new Color(80, 220, 110);
    private static final Color BAD = new Color(240, 80, 80);
    private static final Color ACCENT = new Color(250, 210, 80);

    // Simulator keys: the character at position i on P1_KEYS or P2_KEYS sends COMMANDS[i] for
    // that pod. Shift + a colour key (! @ # $ and & * ( ) on a US keyboard) is a double press.
    private static final String[] COMMANDS = {
        "joy up", "joy left", "joy down", "joy right",
        "btn red", "btn green", "btn blue", "btn yellow",
        "btn red 2", "btn green 2", "btn blue 2", "btn yellow 2",
        "dist low", "dist mid", "dist high",
        "tilt l", "tilt r", "tilt f", "tilt b",
        "tag"
    };
    private static final String P1_KEYS = "wasd1234!@#$zxcqerft";
    private static final String P2_KEYS = "ijkl7890&*()nm,uop;y";
    private static final String KEY_HELP =
        "P1:  WASD joystick   1-4 buttons (Shift = twice)   Z X C hand low/mid/high   Q E R F tilt L/R/F/B   T card"
        + "        P2:  IJKL   7-0 (Shift = twice)   N M ,   U O P ;   Y";

    private GameController game;
    private SimulatedPods sim;                                    // null with real pods: no key control
    private JFrame frame;
    private JLabel title = new JLabel("", JLabel.CENTER);         // mode
    private JLabel instruction = new JLabel("", JLabel.CENTER);   // Simon's words, 56 pt
    private JLabel info = new JLabel(" ", JLabel.CENTER);         // GO, results, joins, level up
    private JLabel[] playerLabels = { new JLabel("", JLabel.CENTER), new JLabel("", JLabel.CENTER) };
    private JButton classicButton = new JButton("Classic");
    private JButton showdownButton = new JButton("Showdown");
    private JButton startButton = new JButton("Start");
    // Empty until someone types, so a name set from the console is never overwritten by stale text
    private JTextField[] nameFields = { new JTextField(""), new JTextField("") };

    /**
     * Builds and shows the window. Called from main before the game thread starts.
     * sim is the keyboard simulator, or null when playing with real pods.
     */
    public GameWindow(GameController game, SimulatedPods sim) {
        this.game = game;
        this.sim = sim;
        frame = new JFrame("Simon Says Showdown");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel top = gridPanel(2, 1);
        style(title, 28, ACCENT);
        style(info, 30, TEXT);
        top.add(title);
        top.add(info);
        frame.add(top, BorderLayout.NORTH);

        JPanel middle = borderPanel();
        style(instruction, 56, TEXT);
        middle.add(instruction, BorderLayout.CENTER);
        frame.add(middle, BorderLayout.CENTER);

        JPanel bottom = gridPanel(sim != null ? 3 : 2, 1);
        JPanel players = gridPanel(1, 2);
        for (int i = 0; i < 2; i++) {
            style(playerLabels[i], 30, TEXT);
            players.add(playerLabels[i]);
        }
        bottom.add(players);
        JPanel controls = gridPanel(1, 7);
        controls.add(caption("Player 1"));
        controls.add(nameFields[0]);
        controls.add(caption("Player 2"));
        controls.add(nameFields[1]);
        controls.add(classicButton);
        controls.add(showdownButton);
        controls.add(startButton);
        bottom.add(controls);
        if (sim != null) {
            JLabel keys = new JLabel(KEY_HELP, JLabel.CENTER);
            style(keys, 13, Color.LIGHT_GRAY);
            bottom.add(keys);
        }
        frame.add(bottom, BorderLayout.SOUTH);

        classicButton.addActionListener(this);
        showdownButton.addActionListener(this);
        startButton.addActionListener(this);
        nameFields[0].addActionListener(this);     // Enter in a name field applies the name
        nameFields[1].addActionListener(this);
        // Keys reach whichever button has the focus; the name fields keep their keys for typing
        classicButton.addKeyListener(this);
        showdownButton.addKeyListener(this);
        startButton.addKeyListener(this);

        title.setText(titleText());
        instruction.setText(wrap("Scan your card to join"));
        refreshPlayers();
        frame.setSize(1280, 720);
        frame.setVisible(true);
        startButton.requestFocusInWindow();            // so the keys work straight away
    }

    // ===== Observer: game thread =====

    /** Only queues the change for the Swing event thread; never touches a label here. */
    public void update(GameUpdate u) {
        SwingUtilities.invokeLater(new WindowRefresh(this, u));
    }

    // ===== Swing event thread =====

    /** Shows one update. Called by WindowRefresh.run() on the Swing event thread. */
    public void apply(GameUpdate u) {
        String type = u.getType();
        String text = u.getText();
        String clip = u.getClips().size() > 0 ? u.getClips().get(0) : "";
        if (type.equals(GameUpdate.PHASE)) {
            show(text, TEXT);
            info.setText(" ");
        } else if (type.equals(GameUpdate.INSTRUCTION)) {
            show(text, TEXT);
            info.setText(" ");
        } else if (type.equals(GameUpdate.GO)) {
            instruction.setForeground(ACCENT);     // the instruction turns yellow: act now
            info.setText("GO!  " + (Long.parseLong(text) / 1000.0) + " s");
        } else if (type.equals(GameUpdate.RESULT)) {
            boolean good = clip.equals("correct") || clip.equals("p1_fastest") || clip.equals("p2_fastest");
            instruction.setForeground(good ? GOOD : BAD);
            info.setText(text);
        } else if (type.equals(GameUpdate.INFO)) {
            if (clip.startsWith("count_")) {
                show(text, ACCENT);                // countdown numbers fill the screen
            } else if (text.equals(GameController.YOUR_TURN)) {
                show(text, TEXT);                  // memory round: hide the sequence
            } else {
                info.setText(text);
            }
        }
        title.setText(titleText());
        refreshPlayers();
    }

    /** Buttons and name fields: only the controller's synchronized setters. */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == classicButton) {
            changeMode(new ClassicScoring());
        } else if (source == showdownButton) {
            changeMode(new ShowdownScoring());
        } else if (source == startButton) {
            applyName(0);
            applyName(1);
            if (!game.requestStart()) {
                info.setText("A game is already running");
            }
        } else if (source == nameFields[0]) {
            applyName(0);
            startButton.requestFocusInWindow();        // back to playing keys after Enter
        } else if (source == nameFields[1]) {
            applyName(1);
            startButton.requestFocusInWindow();
        }
        refreshPlayers();
    }

    /** Simulator keys: one typed character becomes one pod command. */
    public void keyTyped(KeyEvent e) {
        if (sim == null) {
            return;
        }
        String key = String.valueOf(e.getKeyChar()).toLowerCase();
        int i = P1_KEYS.indexOf(key);
        if (i >= 0) {
            sim.inject("p1 " + COMMANDS[i]);
            return;
        }
        i = P2_KEYS.indexOf(key);
        if (i >= 0) {
            sim.inject("p2 " + COMMANDS[i]);
        }
    }

    // Required by KeyListener; typed characters are enough
    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    private void changeMode(ScoringStrategy s) {
        if (game.setStrategy(s)) {
            title.setText(titleText());
            info.setText("Mode: " + s);
        } else {
            info.setText("The mode can only change between games");
        }
    }

    // Sends a typed name to the controller; an empty field changes nothing
    private void applyName(int i) {
        String name = nameFields[i].getText().trim();
        if (name.length() > 0) {
            game.setPlayerName(i, name);
        }
    }

    // One line per pod: who joined, score, lives in Classic, and whether the pod is connected
    private void refreshPlayers() {
        Player[] slots = game.getSlots();
        boolean classic = game.getModeName().equals("Classic");
        for (int i = 0; i < 2; i++) {
            Player p = slots[i];
            if (p == null) {
                playerLabels[i].setText("Player " + (i + 1) + ": scan your card");
                playerLabels[i].setForeground(Color.GRAY);
                continue;
            }
            String text = p.getName() + "   " + p.getScore() + " pts";
            if (classic) {
                text = text + "   lives " + p.getLives();
            }
            text = text + (p.isConnected() ? "   connected" : "   DISCONNECTED");
            playerLabels[i].setText(text);
            playerLabels[i].setForeground(p.isConnected() ? TEXT : BAD);
        }
    }

    private String titleText() {
        return "Simon Says Showdown  -  " + game.getModeName();
    }

    private void show(String text, Color colour) {
        instruction.setText(wrap(text));
        instruction.setForeground(colour);
    }

    // A JLabel shows HTML text on several centred lines, so long instructions fit the screen
    private static String wrap(String text) {
        return "<html><center>" + text + "</center></html>";
    }

    private static JPanel gridPanel(int rows, int cols) {
        JPanel p = new JPanel(new GridLayout(rows, cols));
        p.setBackground(BACKGROUND);
        return p;
    }

    private static JPanel borderPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BACKGROUND);
        return p;
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        style(label, 18, TEXT);
        return label;
    }

    private static void style(JLabel label, int size, Color colour) {
        label.setFont(new Font("SansSerif", Font.BOLD, size));
        label.setForeground(colour);
    }
}
