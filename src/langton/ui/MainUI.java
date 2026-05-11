package langton.ui;

import langton.metrics.BenchmarkRunner;
import langton.metrics.PerformanceMetrics;
import langton.metrics.PerformanceMetrics.BenchmarkResult;
import langton.simulation.SimulationEngine;
import langton.simulation.SimulationEngine.Mode;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

// Main window. Holds the engine and all UI panels.
@SuppressWarnings("this-escape")
public class MainUI {

    private final JFrame frame;
    private final SimulationEngine engine = new SimulationEngine();
    private final PerformanceMetrics metrics = new PerformanceMetrics();

    private final ControlPanel controlPanel = new ControlPanel();
    private final PerformancePanel performancePanel = new PerformancePanel();
    private final GridCanvas canvas = new GridCanvas(engine);

    // Animation timer for the live view.
    private Timer animationTimer;
    // Prevents overlapping ticks.
    private final AtomicBoolean stepInFlight = new AtomicBoolean(false);

    private long lastFpsTimestampNs = 0L;
    private int framesSinceLastFps = 0;

    private Thread benchmarkThread;
    // Worker thread for max-speed runs.
    private Thread maxSpeedThread;

    // Builds the window and wires everything up.
    public MainUI() {
        this.frame = new JFrame("Langton's Ant - Parallel Simulation (CMP6011)");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                frame.dispose();
                System.exit(0);
            }
        });
        buildScene();
        wireEvents();
        applyInitialConfiguration();
        frame.pack();
        frame.setSize(new Dimension(1280, 800));
        frame.setLocationRelativeTo(null);
    }

    // Shows the window.
    public void show() {
        frame.setVisible(true);
    }

    // Lays out the panels.
    private void buildScene() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildMenuBarPanel(), BorderLayout.NORTH);
        root.add(controlPanel, BorderLayout.WEST);

        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.setBorder(null);

        JSplitPane verticalSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, canvasScroll, performancePanel);
        verticalSplit.setOneTouchExpandable(true);
        verticalSplit.setResizeWeight(0.85);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setBorder(null);

        root.add(verticalSplit, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setJMenuBar(buildMenuBar());
    }

    // Builds the menu bar.
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newConfig = new JMenuItem("New Config...");
        JMenuItem exitItem = new JMenuItem("Exit");
        newConfig.addActionListener(e -> openNewConfigDialog());
        exitItem.addActionListener(e -> {
            shutdown();
            frame.dispose();
            System.exit(0);
        });
        fileMenu.add(newConfig);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu simMenu = new JMenu("Simulation");
        JMenuItem runBenchmark = new JMenuItem("Run Benchmark");
        runBenchmark.addActionListener(e -> startBenchmark());
        simMenu.add(runBenchmark);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> showAbout());
        helpMenu.add(about);

        bar.add(fileMenu);
        bar.add(simMenu);
        bar.add(helpMenu);
        return bar;
    }

    // Empty placeholder panel.
    private JPanel buildMenuBarPanel() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(0, 0));
        return p;
    }

    // Hooks up all the buttons and sliders.
    private void wireEvents() {
        controlPanel.getApplyGridSizeButton().addActionListener(e -> applyGridSize());
        controlPanel.getApplyConfigButton().addActionListener(e -> applyConfiguration());

        controlPanel.getStartButton().addActionListener(e -> startSimulation());
        controlPanel.getPauseButton().addActionListener(e -> pauseSimulation());
        controlPanel.getStopButton().addActionListener(e -> stopSimulation());
        controlPanel.getStepOnceButton().addActionListener(e -> stepOnce());
        controlPanel.getResetButton().addActionListener(e -> resetGrid());
        controlPanel.getMaxSpeedButton().addActionListener(e -> runMaxSpeed());

        controlPanel.getModeToggle().addActionListener(e -> {
            boolean parallel = controlPanel.getModeToggle().isSelected();
            engine.setMode(parallel ? Mode.PARALLEL : Mode.SEQUENTIAL);
            controlPanel.getModeToggle().setText(parallel ? "Parallel" : "Sequential");
            performancePanel.setModeLabel(parallel
                    ? "Parallel (" + engine.getParallelism() + " threads)"
                    : "Sequential");
        });

        controlPanel.getSpeedSlider().addChangeListener(e -> {
            if (animationTimer != null) {
                int delay = Math.max(1, controlPanel.getSpeedSlider().getValue());
                animationTimer.setDelay(delay);
            }
        });

        controlPanel.getRunBenchmarkButton().addActionListener(e -> startBenchmark());
    }

    // Loads the initial settings.
    private void applyInitialConfiguration() {
        int w = parseFieldOrDefault(controlPanel.getGridWidthField().getText(), 1000);
        int h = parseFieldOrDefault(controlPanel.getGridHeightField().getText(), 1000);
        int r = ((Number) controlPanel.getNumRegionsSpinner().getValue()).intValue();
        int a = ((Number) controlPanel.getAntsPerRegionSpinner().getValue()).intValue();
        engine.initialize(w, h, r, a);
        canvas.setEngine(engine);
        SwingUtilities.invokeLater(canvas::centreView);
        printConfigSummary();
        updateStatusLabels();
        canvas.repaint();
    }

    // Applies a new grid size.
    private void applyGridSize() {
        int w = parseFieldOrDefault(controlPanel.getGridWidthField().getText(), engine.getGridWidth());
        int h = parseFieldOrDefault(controlPanel.getGridHeightField().getText(), engine.getGridHeight());
        w = Math.max(10, Math.min(10_000, w));
        h = Math.max(10, Math.min(10_000, h));
        stopSimulation();
        engine.initialize(w, h,
                ((Number) controlPanel.getNumRegionsSpinner().getValue()).intValue(),
                ((Number) controlPanel.getAntsPerRegionSpinner().getValue()).intValue());
        canvas.setEngine(engine);
        canvas.centreView();
        canvas.repaint();
        updateStatusLabels();
    }

    // Applies new region/ant counts.
    private void applyConfiguration() {
        int r = ((Number) controlPanel.getNumRegionsSpinner().getValue()).intValue();
        int a = ((Number) controlPanel.getAntsPerRegionSpinner().getValue()).intValue();
        stopSimulation();
        engine.initialize(engine.getGridWidth(), engine.getGridHeight(), r, a);
        canvas.setEngine(engine);
        canvas.centreView();
        canvas.repaint();
        updateStatusLabels();
    }

    // Starts the animation loop.
    private void startSimulation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            return;
        }
        int delay = Math.max(1, controlPanel.getSpeedSlider().getValue());
        lastFpsTimestampNs = System.nanoTime();
        framesSinceLastFps = 0;

        animationTimer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!stepInFlight.compareAndSet(false, true)) {
                    return;
                }
                try {
                    int delayMs = controlPanel.getSpeedSlider().getValue();
                    int stepsPerFrame = computeStepsPerFrame(delayMs);
                    for (int i = 0; i < stepsPerFrame; i++) {
                        engine.step();
                    }
                } finally {
                    stepInFlight.set(false);
                }

                canvas.repaint();
                framesSinceLastFps++;

                long now = System.nanoTime();
                if (now - lastFpsTimestampNs >= 500_000_000L) {
                    double seconds = (now - lastFpsTimestampNs) / 1_000_000_000.0;
                    double fps = framesSinceLastFps / Math.max(0.001, seconds);
                    performancePanel.setFps(fps);
                    framesSinceLastFps = 0;
                    lastFpsTimestampNs = now;
                }
                performancePanel.setStepCount(engine.getCurrentStep());
            }
        });
        animationTimer.start();
    }

    // Picks how many ticks per frame based on slider speed.
    private static int computeStepsPerFrame(int delayMs) {
        if (delayMs <= 1) return 200;
        if (delayMs <= 5) return 50;
        if (delayMs <= 20) return 10;
        if (delayMs <= 50) return 4;
        return 1;
    }

    // Pauses the animation.
    private void pauseSimulation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    // Stops and resets the simulation.
    private void stopSimulation() {
        pauseSimulation();
        engine.reset();
        canvas.setEngine(engine);
        canvas.repaint();
        updateStatusLabels();
    }

    // Runs exactly one tick.
    private void stepOnce() {
        engine.step();
        canvas.repaint();
        performancePanel.setStepCount(engine.getCurrentStep());
    }

    // Runs N ticks as fast as possible on a background thread.
    private void runMaxSpeed() {
        if (maxSpeedThread != null && maxSpeedThread.isAlive()) {
            return;
        }
        pauseSimulation();

        final int steps = parseFieldOrDefault(
                controlPanel.getNumStepsField().getText(), 10_000);
        if (steps <= 0) {
            controlPanel.getBenchmarkStatus().setText("Steps must be > 0");
            return;
        }

        controlPanel.getMaxSpeedButton().setEnabled(false);
        controlPanel.getStartButton().setEnabled(false);
        controlPanel.getStepOnceButton().setEnabled(false);
        controlPanel.getRunBenchmarkButton().setEnabled(false);
        controlPanel.getBenchmarkStatus().setText(
                "Running " + steps + " steps at max speed...");

        maxSpeedThread = new Thread(() -> {
            long elapsedNs = 0L;
            Throwable error = null;
            try {
                elapsedNs = engine.runSteps(steps);
            } catch (Throwable t) {
                error = t;
            }
            final long el = elapsedNs;
            final Throwable err = error;
            SwingUtilities.invokeLater(() -> {
                canvas.repaint();
                performancePanel.setStepCount(engine.getCurrentStep());
                controlPanel.getMaxSpeedButton().setEnabled(true);
                controlPanel.getStartButton().setEnabled(true);
                controlPanel.getStepOnceButton().setEnabled(true);
                controlPanel.getRunBenchmarkButton().setEnabled(true);
                if (err != null) {
                    controlPanel.getBenchmarkStatus().setText(
                            "Max-speed run failed: " + err.getMessage());
                    err.printStackTrace();
                } else {
                    double ms = el / 1_000_000.0;
                    double stepsPerSec = ms == 0 ? 0 : steps / (ms / 1000.0);
                    controlPanel.getBenchmarkStatus().setText(String.format(
                            "%,d steps in %.2f ms  (%,.0f steps/s)",
                            steps, ms, stepsPerSec));
                }
            });
        }, "MaxSpeedWorker");
        maxSpeedThread.setDaemon(true);
        maxSpeedThread.start();
    }

    // Resets the grid.
    private void resetGrid() {
        stopSimulation();
        canvas.repaint();
    }

    // Opens the config dialog.
    private void openNewConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(frame,
                engine.getGridWidth(), engine.getGridHeight(),
                engine.getNumRegions(), engine.getAntsPerRegion());
        ConfigDialog.ConfigResult cfg = dialog.showDialog();
        if (cfg == null) {
            return;
        }
        stopSimulation();
        controlPanel.getGridWidthField().setText(String.valueOf(cfg.getGridWidth()));
        controlPanel.getGridHeightField().setText(String.valueOf(cfg.getGridHeight()));
        controlPanel.getNumRegionsSpinner().setValue(cfg.getNumRegions());
        controlPanel.getAntsPerRegionSpinner().setValue(cfg.getAntsPerRegion());
        engine.initialize(cfg.getGridWidth(), cfg.getGridHeight(),
                cfg.getNumRegions(), cfg.getAntsPerRegion());
        canvas.setEngine(engine);
        canvas.centreView();
        canvas.repaint();
        updateStatusLabels();
        printConfigSummary();
    }

    // Runs the benchmark suite on a background thread.
    private void startBenchmark() {
        if (benchmarkThread != null && benchmarkThread.isAlive()) {
            return;
        }
        pauseSimulation();
        performancePanel.clearResults();
        controlPanel.getRunBenchmarkButton().setEnabled(false);
        controlPanel.getBenchmarkProgress().setValue(0);
        controlPanel.getBenchmarkStatus().setText("Starting benchmark...");

        BenchmarkRunner runner = new BenchmarkRunner(metrics);
        benchmarkThread = new Thread(() -> {
            try {
                runner.runFullSuite(
                        (progress, label) -> SwingUtilities.invokeLater(() -> {
                            controlPanel.getBenchmarkProgress().setValue((int) (progress * 100));
                            controlPanel.getBenchmarkStatus().setText(label);
                        }),
                        (BenchmarkResult r) -> performancePanel.addResult(r)
                );
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        controlPanel.getBenchmarkStatus().setText("Benchmark failed: " + ex.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    controlPanel.getRunBenchmarkButton().setEnabled(true);
                    controlPanel.getBenchmarkProgress().setValue(100);
                });
            }
        }, "BenchmarkRunner");
        benchmarkThread.setDaemon(true);
        benchmarkThread.start();
    }

    // Shows the About dialog.
    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                "CMP6011 - Parallel and Distributed Systems\n\n"
                        + "A Swing simulation of multi-agent Langton's Ant featuring "
                        + "sequential and parallel execution engines, region-based "
                        + "decomposition over a ForkJoinPool, and a full benchmark suite.\n\n"
                        + "Use mouse scroll to zoom; click and drag to pan.",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // Refreshes the status labels.
    private void updateStatusLabels() {
        performancePanel.setStepCount(engine.getCurrentStep());
        performancePanel.setAntCount(engine.getAnts().size());
        performancePanel.setModeLabel(engine.getMode() == Mode.PARALLEL
                ? "Parallel (" + engine.getParallelism() + " threads)"
                : "Sequential");
    }

    // Prints config info to the console.
    private void printConfigSummary() {
        System.out.println("=== Langton's Ant Configuration ===");
        System.out.printf("Grid:        %d x %d%n", engine.getGridWidth(), engine.getGridHeight());
        System.out.printf("Regions:     %d%n", engine.getNumRegions());
        System.out.printf("Ants/region: %d (total %d)%n",
                engine.getAntsPerRegion(), engine.getAnts().size());
        System.out.printf("Cores:       %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println();
    }

    // Safely parses an int, returning fallback on failure.
    private static int parseFieldOrDefault(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    // Stops everything before exit.
    public void shutdown() {
        if (animationTimer != null) animationTimer.stop();
        engine.shutdown();
        if (benchmarkThread != null) benchmarkThread.interrupt();
        if (maxSpeedThread != null) maxSpeedThread.interrupt();
    }
}
