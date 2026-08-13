import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class MainGUI extends JFrame {

    // -----------------------------------------------------------------------
    // Visual style constants (cosmetic only — no effect on simulator behavior)
    // -----------------------------------------------------------------------
    private static final Color  BG_PANEL     = new Color(245, 247, 250);
    private static final Color  BORDER_COLOR = new Color(210, 214, 220);
    private static final Color  ACCENT       = new Color(47, 111, 237);
    private static final Color  TEXT_MUTED   = new Color(108, 117, 125);
    private static final Font   FONT_UI      = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font   FONT_UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private static final String OUTPUT_PLACEHOLDER =
            "No output yet. Click Step or Auto to run the simulation.";
    private static final String HISTORY_PLACEHOLDER =
            "State history will appear here as the simulation progresses.";
    private static final String MEMORY_PLACEHOLDER =
            "Memory contents will be shown once the simulation starts.";
    private static final String PROCESS_STATE_PLACEHOLDER =
            "Process state (running process, queues, disk, mutexes) will appear here.";

    // Tracks whether real content has replaced the placeholder text in the
    // append-only areas (purely cosmetic bookkeeping).
    private boolean outputStarted  = false;
    private boolean historyStarted = false;

    // -----------------------------------------------------------------------
    // Widgets
    // -----------------------------------------------------------------------
    private final JTextArea    outputArea   = new JTextArea();
    private final JTextField   inputField   = new JTextField();
    private final JButton      inputSubmit  = new JButton("Submit");
    private final JButton      stepButton   = new JButton("Step");
    private final JButton      autoButton   = new JButton("Auto");
    private final JButton      pauseButton  = new JButton("Pause");
    private final JLabel       clockLabel   = new JLabel("Clock: 0");
    private final JLabel       statusLabel  = new JLabel("Status: Idle");
    private final JTextArea    memoryArea       = new JTextArea();
    private final JTextArea    processStateArea = new JTextArea();
    private final JTextArea    historyArea      = new JTextArea(); // append-only history
    private final JComboBox<String> schedulerBox =
            new JComboBox<>(new String[]{"Round Robin", "HRRN", "MLFQ"});

    // -----------------------------------------------------------------------
    // Kernel (set after construction to break the circular dependency)
    // -----------------------------------------------------------------------
    private Kernel kernel;
    private Runnable kernelFactory;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public MainGUI() {
        super("OS Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLayout(new BorderLayout(4, 4));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> AboutDialog.show(this));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // --- Output panel (left tab 1) ---
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setText(OUTPUT_PLACEHOLDER);
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // --- History panel (left tab 2) — append-only, never overwritten ---
        historyArea.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyArea.setText(HISTORY_PLACEHOLDER);
        JScrollPane historyScroll = new JScrollPane(historyArea);

        // Wrap both in a tabbed pane so they share the left column without resizing
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.setFont(FONT_UI);
        leftTabs.addTab("Output / Log",    outputScroll);
        leftTabs.addTab("State History",   historyScroll);

        // --- Process state panel (top-right: running, queues, disk, mutexes) ---
        processStateArea.setEditable(false);
        processStateArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        processStateArea.setText(PROCESS_STATE_PLACEHOLDER);
        JScrollPane stateScroll = new JScrollPane(processStateArea);
        javax.swing.border.TitledBorder stateBorder = BorderFactory.createTitledBorder("Process State / Queues");
        stateBorder.setTitleFont(FONT_UI_BOLD);
        stateBorder.setTitleColor(ACCENT);
        stateScroll.setBorder(stateBorder);

        // --- Memory panel (bottom-right) ---
        memoryArea.setEditable(false);
        memoryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        memoryArea.setText(MEMORY_PLACEHOLDER);
        JScrollPane memScroll = new JScrollPane(memoryArea);
        javax.swing.border.TitledBorder memBorder = BorderFactory.createTitledBorder("Memory (40 words)");
        memBorder.setTitleFont(FONT_UI_BOLD);
        memBorder.setTitleColor(ACCENT);
        memScroll.setBorder(memBorder);

        JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stateScroll, memScroll);
        rightPane.setResizeWeight(0.45);
        rightPane.setPreferredSize(new Dimension(300, 0));

        JSplitPane centre = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, rightPane);
        centre.setResizeWeight(0.68);
        add(centre, BorderLayout.CENTER);

        // --- Top bar ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        JLabel schedulerCaption = new JLabel("Scheduler:");
        schedulerCaption.setFont(FONT_UI_BOLD);
        topBar.add(schedulerCaption);

        schedulerBox.setFont(FONT_UI);
        schedulerBox.setToolTipText("Choose the CPU scheduling algorithm (applies before the simulation starts)");
        topBar.add(schedulerBox);

        stepButton.setFont(FONT_UI);
        stepButton.setFocusPainted(false);
        stepButton.setToolTipText("Advance the simulation by one clock tick");
        topBar.add(stepButton);

        autoButton.setFont(FONT_UI);
        autoButton.setFocusPainted(false);
        autoButton.setToolTipText("Run the simulation automatically");
        topBar.add(autoButton);

        pauseButton.setFont(FONT_UI);
        pauseButton.setFocusPainted(false);
        pauseButton.setToolTipText("Pause automatic execution");
        topBar.add(pauseButton);

        clockLabel.setFont(FONT_UI_BOLD);
        topBar.add(clockLabel);

        statusLabel.setFont(FONT_UI_BOLD);
        statusLabel.setForeground(ACCENT);
        topBar.add(statusLabel);
        add(topBar, BorderLayout.NORTH);

        // --- Bottom input bar ---
        JPanel inputPanel = new JPanel(new BorderLayout(4, 0));
        javax.swing.border.TitledBorder inputBorder = BorderFactory.createTitledBorder("User Input");
        inputBorder.setTitleFont(FONT_UI_BOLD);
        inputBorder.setTitleColor(ACCENT);
        inputPanel.setBorder(inputBorder);
        inputField.setFont(FONT_UI);
        inputField.setToolTipText("Enter the value requested by the running process");
        inputSubmit.setFont(FONT_UI);
        inputSubmit.setToolTipText("Submit the entered value to the simulator");
        inputPanel.add(inputField,  BorderLayout.CENTER);
        inputPanel.add(inputSubmit, BorderLayout.EAST);
        enableInput(false);          // hidden/disabled until a process asks

        // --- Footer status bar ---
        JLabel footerLabel = new JLabel("OS Simulator v1.0 — Process Scheduling & Memory Simulation");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(TEXT_MUTED);
        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setBackground(BG_PANEL);
        footerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        footerBar.add(footerLabel, BorderLayout.WEST);

        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.add(inputPanel, BorderLayout.NORTH);
        southContainer.add(footerBar,  BorderLayout.SOUTH);
        add(southContainer, BorderLayout.SOUTH);

        // -----------------------------------------------------------------------
        // Button actions — kernel may be null at construction time; all lambdas
        // check for null defensively.
        // -----------------------------------------------------------------------
        stepButton.addActionListener(e -> {
            ensureKernel();
            if (kernel != null) kernel.requestStep();
        });

        autoButton.addActionListener(e -> {
            ensureKernel();
            if (kernel != null) kernel.setAutoMode(true, 800);
            statusLabel.setText("Status: Auto");
        });

        pauseButton.addActionListener(e -> {
            if (kernel != null) kernel.setPaused(true);
            statusLabel.setText("Status: Paused");
        });

        inputSubmit.addActionListener(e -> submitInput());
        inputField.addActionListener(e -> submitInput());

        schedulerBox.addActionListener(e -> {
            // Scheduler selection is effective only before the simulation starts.
            // A running kernel ignores this (changing mid-run is not supported).
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // -----------------------------------------------------------------------
    // Kernel wiring (called from Main after both objects are created)
    // -----------------------------------------------------------------------
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public void setKernelFactory(Runnable factory) {
        this.kernelFactory = factory;
    }

    private void ensureKernel() {
        if (kernel == null && kernelFactory != null) {
            kernelFactory.run();
        }
    }

    // -----------------------------------------------------------------------
    // Methods called by Kernel (always on the EDT via SwingUtilities.invokeLater)
    // -----------------------------------------------------------------------

    /** Append a line to the output/log area and auto-scroll to the bottom. */
    public void appendOutput(String text) {
        if (!outputStarted) {
            outputArea.setText("");
            outputStarted = true;
        }
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    /**
     * Called by Kernel.printState() (via invokeLater) with a pre-computed snapshot.
     * Bypasses refresh() so the panel always reflects state at the printState() call
     * site, not a later re-read that races with selectNextProcess().
     */
    public void setProcessStatePanel(String text) {
        processStateArea.setText(text);
        processStateArea.setCaretPosition(0);
    }

    /** Show or hide/disable the user-input controls. */
    public void enableInput(boolean enabled) {
        inputField.setEnabled(enabled);
        inputSubmit.setEnabled(enabled);
        inputField.setVisible(enabled);
        inputSubmit.setVisible(enabled);
        if (enabled) {
            inputField.requestFocusInWindow();
            inputField.setText("");
        }
    }

    /**
     * Full GUI refresh — reads current state from the kernel and repaints
     * the memory view, process-state panel, clock label, and status label.
     */
    public void refresh() {
        if (kernel == null) return;

        // Clock
        clockLabel.setText("Clock: " + kernel.getClock());

        // Status label
        PCB cur = kernel.getCurrentProcess();
        if (kernel.isSimulationDone2()) {
            statusLabel.setText("Status: Done");
        } else if (cur != null) {
            statusLabel.setText("Status: Running P" + cur.pid);
        } else {
            statusLabel.setText("Status: Idle");
        }

        // Process state panel is updated directly by Kernel.printState() via
        // setProcessStatePanel(), which captures state at the correct moment on the
        // sim thread before selectNextProcess() can change currentProcess.

        // ── Memory dump ──────────────────────────────────────────────────
        String[] words = kernel.getMemory().getWords();
        StringBuilder mb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            mb.append(String.format("[%2d] %s%n", i, words[i] == null ? "(free)" : words[i]));
        }
        memoryArea.setText(mb.toString());

        // ── State History (append-only snapshot, same filtering rules) ───
        Scheduler sched = kernel.getScheduler();
        StringBuilder h = new StringBuilder();
        h.append("Clock ").append(kernel.getClock()).append(":\n");
        h.append("Running : ").append(cur != null ? "P" + cur.pid : "None").append("\n");

        if (sched instanceof MLFQScheduler) {
            MLFQScheduler mlfq = (MLFQScheduler) sched;
            for (int lvl = 0; lvl < 4; lvl++) {
                List<PCB> q = mlfq.getQueueAtLevel(lvl);
                h.append(String.format("Ready Q%d: [", lvl));
                boolean first = true;
                for (PCB p : q) {
                    if (p.isSwapped) continue;
                    if (!first) h.append(", ");
                    h.append("P").append(p.pid);
                    first = false;
                }
                h.append("]\n");
            }
        } else {
            List<PCB> rq = sched.getReadyQueue();
            h.append("Ready   : [");
            boolean first = true;
            for (PCB p : rq) {
                if (p.isSwapped) continue;
                if (!first) h.append(", ");
                h.append("P").append(p.pid);
                first = false;
            }
            h.append("]\n");
        }

        h.append("Blocked : [");
        boolean hbFirst = true;
        for (PCB p : kernel.getMutexManager().getAllBlocked()) {
            if (p.isSwapped) continue;
            if (!hbFirst) h.append(", ");
            h.append("P").append(p.pid);
            hbFirst = false;
        }
        h.append("]\n");

        h.append("Disk    : [");
        boolean hfirst = true;
        for (PCB pcb : kernel.getProcessTable().values()) {
            if (pcb.isSwapped && pcb != cur) {
                if (!hfirst) h.append(", ");
                h.append("P").append(pcb.pid);
                hfirst = false;
            }
        }
        h.append("]\n");

        for (Map.Entry<String, Mutex> e : kernel.getMutexManager().getMutexes().entrySet()) {
            h.append("Mutex ").append(e.getKey()).append(": ")
             .append(e.getValue().getStatus(cur)).append("\n");
        }
        h.append("----------------\n");

        if (!historyStarted) {
            historyArea.setText("");
            historyStarted = true;
        }
        historyArea.append(h.toString());
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    // -----------------------------------------------------------------------
    // Helper: read input field and forward to kernel
    // -----------------------------------------------------------------------
    private void submitInput() {
        if (kernel == null) return;
        String val = inputField.getText().trim();
        inputField.setText("");
        kernel.submitInput(val);
    }

    // -----------------------------------------------------------------------
    // Utility: chosen scheduler name (used by Main to pick the right Scheduler)
    // -----------------------------------------------------------------------
    public String getSelectedScheduler() {
        return (String) schedulerBox.getSelectedItem();
    }
}
