package langton.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

// Left-side panel with all the control buttons. MainUI hooks up the actions.
@SuppressWarnings({"this-escape", "serial"})
public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField gridWidthField;
    private final JTextField gridHeightField;
    private final JButton applyGridSizeButton;

    private final JSpinner numRegionsSpinner;
    private final JSpinner antsPerRegionSpinner;
    private final JButton applyConfigButton;

    private final JTextField numStepsField;
    private final JSlider speedSlider;
    private final JButton maxSpeedButton;
    private final JButton startButton;
    private final JButton pauseButton;
    private final JButton stopButton;
    private final JButton stepOnceButton;
    private final JToggleButton modeToggle;
    private final JButton resetButton;

    private final JButton runBenchmarkButton;
    private final JProgressBar benchmarkProgress;
    private final JLabel benchmarkStatus;

    // Creates all the widgets and lays them out in sections.
    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(0xf4f4f4));
        setPreferredSize(new Dimension(300, 600));
        setMinimumSize(new Dimension(280, 400));

        gridWidthField = new JTextField("1000", 8);
        gridHeightField = new JTextField("1000", 8);
        applyGridSizeButton = new JButton("Apply Grid Size");

        numRegionsSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        antsPerRegionSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        applyConfigButton = new JButton("Apply Configuration");

        numStepsField = new JTextField("10000", 8);
        speedSlider = new JSlider(0, 500, 50);
        speedSlider.setMajorTickSpacing(100);
        speedSlider.setMinorTickSpacing(25);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        maxSpeedButton = new JButton("⚡ Max Speed (instant)");
        maxSpeedButton.setToolTipText(
                "Run the configured number of Steps as fast as possible "
                + "on a background thread, then redraw once.");
        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        stopButton = new JButton("⏹ Stop");
        stepOnceButton = new JButton("Step Once");
        modeToggle = new JToggleButton("Sequential");
        resetButton = new JButton("↻ Reset Grid");

        runBenchmarkButton = new JButton("▶ Run Benchmark");
        benchmarkProgress = new JProgressBar(0, 100);
        benchmarkProgress.setStringPainted(true);
        benchmarkStatus = new JLabel("Idle");

        for (Component c : new Component[]{
                gridWidthField, gridHeightField, applyGridSizeButton,
                numRegionsSpinner, antsPerRegionSpinner, applyConfigButton,
                numStepsField, speedSlider, maxSpeedButton,
                startButton, pauseButton, stopButton, stepOnceButton,
                modeToggle, resetButton,
                runBenchmarkButton, benchmarkProgress, benchmarkStatus}) {
            stretchHorizontal(c);
        }

        add(sectionHeader("Grid Configuration"));
        add(label("Width:"));
        add(gridWidthField);
        add(verticalGap(4));
        add(label("Height:"));
        add(gridHeightField);
        add(verticalGap(4));
        add(applyGridSizeButton);
        add(verticalGap(8));
        add(separator());

        add(sectionHeader("Region Configuration"));
        add(label("Number of Regions (1-16):"));
        add(numRegionsSpinner);
        add(verticalGap(4));
        add(label("Ants per Region (1-20):"));
        add(antsPerRegionSpinner);
        add(verticalGap(4));
        add(applyConfigButton);
        add(verticalGap(8));
        add(separator());

        add(sectionHeader("Simulation Control"));
        add(label("Steps:"));
        add(numStepsField);
        add(verticalGap(4));
        add(label("Speed (ms delay):"));
        add(speedSlider);
        add(verticalGap(4));
        add(maxSpeedButton);
        add(verticalGap(4));
        add(startButton);
        add(verticalGap(2));
        add(pauseButton);
        add(verticalGap(2));
        add(stopButton);
        add(verticalGap(2));
        add(stepOnceButton);
        add(verticalGap(2));
        add(modeToggle);
        add(verticalGap(2));
        add(resetButton);
        add(verticalGap(8));
        add(separator());

        add(sectionHeader("Performance"));
        add(runBenchmarkButton);
        add(verticalGap(4));
        add(benchmarkProgress);
        add(verticalGap(4));
        add(benchmarkStatus);
        add(Box.createVerticalGlue());
    }

    // Builds a bold section header label.
    private static JLabel sectionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setForeground(new Color(0x333333));
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // Builds a small caption label.
    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(11f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // Builds a thin horizontal separator line.
    private static JSeparator separator() {
        JSeparator s = new JSeparator(SwingConstants.HORIZONTAL);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    // Builds a vertical spacer.
    private static Component verticalGap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    // Makes a component stretch horizontally but stay one row tall.
    private static void stretchHorizontal(Component c) {
        Dimension pref = c.getPreferredSize();
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
        if (c instanceof JPanel || c instanceof JLabel
                || c instanceof JTextField || c instanceof JButton
                || c instanceof JToggleButton || c instanceof JSpinner
                || c instanceof JSlider || c instanceof JProgressBar) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    // Returns the width field.
    public JTextField getGridWidthField() { return gridWidthField; }
    // Returns the height field.
    public JTextField getGridHeightField() { return gridHeightField; }
    // Returns the apply-grid-size button.
    public JButton getApplyGridSizeButton() { return applyGridSizeButton; }

    // Returns the region count spinner.
    public JSpinner getNumRegionsSpinner() { return numRegionsSpinner; }
    // Returns the ants-per-region spinner.
    public JSpinner getAntsPerRegionSpinner() { return antsPerRegionSpinner; }
    // Returns the apply-config button.
    public JButton getApplyConfigButton() { return applyConfigButton; }

    // Returns the steps field.
    public JTextField getNumStepsField() { return numStepsField; }
    // Returns the speed slider.
    public JSlider getSpeedSlider() { return speedSlider; }
    // Returns the max-speed button.
    public JButton getMaxSpeedButton() { return maxSpeedButton; }
    // Returns the start button.
    public JButton getStartButton() { return startButton; }
    // Returns the pause button.
    public JButton getPauseButton() { return pauseButton; }
    // Returns the stop button.
    public JButton getStopButton() { return stopButton; }
    // Returns the step-once button.
    public JButton getStepOnceButton() { return stepOnceButton; }
    // Returns the mode toggle.
    public JToggleButton getModeToggle() { return modeToggle; }
    // Returns the reset button.
    public JButton getResetButton() { return resetButton; }

    // Returns the run-benchmark button.
    public JButton getRunBenchmarkButton() { return runBenchmarkButton; }
    // Returns the benchmark progress bar.
    public JProgressBar getBenchmarkProgress() { return benchmarkProgress; }
    // Returns the benchmark status label.
    public JLabel getBenchmarkStatus() { return benchmarkStatus; }
}
