package langton.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// Dialog for entering grid/region/ant settings.
@SuppressWarnings({"this-escape", "serial"})
public class ConfigDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField widthField;
    private final JTextField heightField;
    private final JSpinner regionsSpinner;
    private final JSpinner antsSpinner;

    private ConfigResult result;

    // Builds the form and wires the OK/Cancel buttons.
    public ConfigDialog(Frame owner, int initialWidth, int initialHeight,
                        int initialRegions, int initialAntsPerRegion) {
        super(owner, "New Configuration", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        widthField = new JTextField(String.valueOf(initialWidth), 8);
        heightField = new JTextField(String.valueOf(initialHeight), 8);
        regionsSpinner = new JSpinner(new SpinnerNumberModel(initialRegions, 1, 16, 1));
        antsSpinner = new JSpinner(new SpinnerNumberModel(initialAntsPerRegion, 1, 20, 1));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, 0, "Grid Width:",        widthField);
        addRow(form, c, 1, "Grid Height:",       heightField);
        addRow(form, c, 2, "Number of Regions:", regionsSpinner);
        addRow(form, c, 3, "Ants per Region:",   antsSpinner);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int w = parseOrDefault(widthField.getText(), initialWidth);
                int h = parseOrDefault(heightField.getText(), initialHeight);
                int r = ((Number) regionsSpinner.getValue()).intValue();
                int a = ((Number) antsSpinner.getValue()).intValue();
                w = clamp(w, 10, 10_000);
                h = clamp(h, 10, 10_000);
                result = new ConfigResult(w, h, r, a);
                dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = null;
                dispose();
            }
        });

        JPanel buttons = new JPanel();
        buttons.add(okButton);
        buttons.add(cancelButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        pack();
        setLocationRelativeTo(owner);
    }

    // Adds a label + field row to the form.
    private static void addRow(JPanel form, GridBagConstraints c, int row,
                               String labelText, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        form.add(new JLabel(labelText), c);
        c.gridx = 1; c.weightx = 1.0;
        form.add(field, c);
    }

    // Shows the dialog and returns the result (null if cancelled).
    public ConfigResult showDialog() {
        setVisible(true);
        return result;
    }

    // Returns the last result.
    public ConfigResult getResult() {
        return result;
    }

    // Safely parses an int, returning fallback on failure.
    private static int parseOrDefault(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Keeps a value inside [lo, hi].
    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // Holds the four chosen values.
    public static final class ConfigResult {
        private final int gridWidth;
        private final int gridHeight;
        private final int numRegions;
        private final int antsPerRegion;

        // Stores the chosen values.
        public ConfigResult(int gridWidth, int gridHeight, int numRegions, int antsPerRegion) {
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
            this.numRegions = numRegions;
            this.antsPerRegion = antsPerRegion;
        }

        // Returns the chosen grid width.
        public int getGridWidth() { return gridWidth; }
        // Returns the chosen grid height.
        public int getGridHeight() { return gridHeight; }
        // Returns the chosen region count.
        public int getNumRegions() { return numRegions; }
        // Returns the chosen ants per region.
        public int getAntsPerRegion() { return antsPerRegion; }
    }
}
