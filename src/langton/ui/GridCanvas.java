package langton.ui;

import langton.model.Ant;
import langton.model.Direction;
import langton.model.Grid;
import langton.model.Region;
import langton.simulation.SimulationEngine;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.Map;

// Draws the grid, regions and ants. Supports zoom and pan.
@SuppressWarnings({"this-escape", "serial"})
public class GridCanvas extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final double MIN_CELL_SIZE = 0.5;
    private static final double MAX_CELL_SIZE = 60.0;
    private static final double DEFAULT_CELL_SIZE = 4.0;

    // Colours for each region's trail.
    private static final Color[] TRAIL_COLOURS = {
            new Color(46, 204, 113),
            new Color(52, 152, 219),
            new Color(230, 126, 34),
            new Color(155, 89, 182),
            new Color(26, 188, 156),
            new Color(255, 105, 180),
            new Color(241, 196, 15),
            new Color(255, 0, 255),
            new Color(0, 128, 128),
            new Color(128, 0, 128),
            new Color(0, 100, 0),
            new Color(70, 130, 180),
            new Color(184, 134, 11),
            new Color(64, 64, 64),
            new Color(255, 140, 0),
            new Color(75, 0, 130)
    };

    private SimulationEngine engine;

    private double cellSize = DEFAULT_CELL_SIZE;
    // Top-left position in grid coords.
    private double offsetX = 0;
    private double offsetY = 0;

    private int lastDragX = -1;
    private int lastDragY = -1;

    // Sets up the canvas and adds zoom/pan listeners.
    public GridCanvas(SimulationEngine engine) {
        this.engine = engine;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleZoom(e);
            }
        });

        MouseInputAdapter pan = new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastDragX = e.getX();
                lastDragY = e.getY();
                requestFocusInWindow();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDragX < 0) return;
                int dx = e.getX() - lastDragX;
                int dy = e.getY() - lastDragY;
                lastDragX = e.getX();
                lastDragY = e.getY();
                offsetX -= dx / cellSize;
                offsetY -= dy / cellSize;
                clampOffset();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastDragX = -1;
                lastDragY = -1;
            }
        };
        addMouseListener(pan);
        addMouseMotionListener(pan);
    }

    // Swaps in a new engine and resets the view.
    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }

    // Repaints the canvas.
    public void render() {
        repaint();
    }

    // Paints cells, region borders, then ants.
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (engine == null || engine.getGrid() == null) {
                drawHelpOverlay(g, w, h, "No simulation initialised — click 'Apply Configuration'");
                return;
            }

            Grid grid = engine.getGrid();
            drawBlackCells(g, grid, w, h);
            drawRegionBorders(g, w, h);
            drawAnts(g);
        } finally {
            g.dispose();
        }
    }

    // Draws all coloured cells visible on screen.
    private void drawBlackCells(Graphics2D g, Grid grid, int w, int h) {
        int firstX = (int) Math.floor(offsetX);
        int firstY = (int) Math.floor(offsetY);
        int lastX = firstX + (int) Math.ceil(w / cellSize) + 1;
        int lastY = firstY + (int) Math.ceil(h / cellSize) + 1;

        Map<Long, Integer> cells = grid.getCellMap();
        for (Map.Entry<Long, Integer> e : cells.entrySet()) {
            long k = e.getKey();
            int x = grid.decodeX(k);
            int y = grid.decodeY(k);
            if (x < firstX || x > lastX || y < firstY || y > lastY) {
                continue;
            }
            int px = (int) ((x - offsetX) * cellSize);
            int py = (int) ((y - offsetY) * cellSize);
            int size = (int) Math.max(1.0, cellSize);
            g.setColor(colorFor(e.getValue()));
            g.fillRect(px, py, size, size);
        }
    }

    // Picks a colour based on the cell's stored value.
    private static Color colorFor(int cellValue) {
        if (cellValue <= 0) {
            return Color.WHITE;
        }
        int idx = (cellValue - 1) % TRAIL_COLOURS.length;
        return TRAIL_COLOURS[idx];
    }

    // Draws dashed borders around each region.
    private void drawRegionBorders(Graphics2D g, int w, int h) {
        List<Region> regions = engine.getRegions();
        if (regions == null || regions.isEmpty()) {
            return;
        }
        Stroke previous = g.getStroke();
        g.setColor(new Color(0x888888));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, new float[]{4f, 4f}, 0f));
        for (Region r : regions) {
            int x = (int) ((r.getStartX() - offsetX) * cellSize);
            int y = (int) ((r.getStartY() - offsetY) * cellSize);
            int rw = (int) (r.getWidth() * cellSize);
            int rh = (int) (r.getHeight() * cellSize);
            if (x + rw < 0 || y + rh < 0 || x > w || y > h) continue;
            g.drawRect(x, y, rw, rh);
        }
        g.setStroke(previous);
    }

    // Draws every ant.
    private void drawAnts(Graphics2D g) {
        List<Ant> ants = engine.getAnts();
        for (Ant ant : ants) {
            paintAntMarker(g, ant);
        }
    }

    // Draws one ant as a red dot with a direction arrow.
    private void paintAntMarker(Graphics2D g, Ant ant) {
        double dotSize = Math.max(4.0, cellSize * 0.95);
        double cx = (ant.getX() - offsetX) * cellSize + cellSize / 2.0;
        double cy = (ant.getY() - offsetY) * cellSize + cellSize / 2.0;

        g.setColor(Color.RED);
        g.fillOval((int) (cx - dotSize / 2), (int) (cy - dotSize / 2),
                (int) dotSize, (int) dotSize);

        double inner = Math.max(2.0, dotSize * 0.45);
        g.setColor(Color.WHITE);
        g.fillOval((int) (cx - inner / 2), (int) (cy - inner / 2),
                (int) inner, (int) inner);

        drawDirectionArrow(g, cx, cy, ant.getDirection(), inner, Color.RED);
    }

    // Draws a triangle pointing in the ant's direction.
    private void drawDirectionArrow(Graphics2D g, double cx, double cy,
                                    Direction dir, double size, Color colour) {
        double half = size * 0.9;
        int[] xs = new int[3];
        int[] ys = new int[3];
        switch (dir) {
            case NORTH -> {
                xs[0] = (int) cx;            ys[0] = (int) (cy - half);
                xs[1] = (int) (cx - half/2); ys[1] = (int) (cy + half/4);
                xs[2] = (int) (cx + half/2); ys[2] = (int) (cy + half/4);
            }
            case EAST -> {
                xs[0] = (int) (cx + half);   ys[0] = (int) cy;
                xs[1] = (int) (cx - half/4); ys[1] = (int) (cy - half/2);
                xs[2] = (int) (cx - half/4); ys[2] = (int) (cy + half/2);
            }
            case SOUTH -> {
                xs[0] = (int) cx;            ys[0] = (int) (cy + half);
                xs[1] = (int) (cx - half/2); ys[1] = (int) (cy - half/4);
                xs[2] = (int) (cx + half/2); ys[2] = (int) (cy - half/4);
            }
            case WEST -> {
                xs[0] = (int) (cx - half);   ys[0] = (int) cy;
                xs[1] = (int) (cx + half/4); ys[1] = (int) (cy - half/2);
                xs[2] = (int) (cx + half/4); ys[2] = (int) (cy + half/2);
            }
        }
        g.setColor(colour.darker());
        g.fillPolygon(xs, ys, 3);
    }

    // Shows a centred help message.
    private void drawHelpOverlay(Graphics2D g, int w, int h, String message) {
        g.setColor(new Color(0x555555));
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(message);
        g.drawString(message, (w - tw) / 2, h / 2);
    }

    // Zooms in or out around the mouse cursor.
    private void handleZoom(MouseWheelEvent e) {
        double mouseGridX = offsetX + e.getX() / cellSize;
        double mouseGridY = offsetY + e.getY() / cellSize;

        double factor = e.getWheelRotation() < 0 ? 1.2 : 1.0 / 1.2;
        double newSize = clamp(cellSize * factor, MIN_CELL_SIZE, MAX_CELL_SIZE);
        if (newSize == cellSize) {
            return;
        }
        cellSize = newSize;

        offsetX = mouseGridX - e.getX() / cellSize;
        offsetY = mouseGridY - e.getY() / cellSize;

        clampOffset();
        repaint();
    }

    // Stops the user panning too far off the grid.
    private void clampOffset() {
        if (engine == null || engine.getGrid() == null) return;
        Grid grid = engine.getGrid();
        double maxX = grid.getWidth();
        double maxY = grid.getHeight();
        if (offsetX < -maxX) offsetX = -maxX;
        if (offsetY < -maxY) offsetY = -maxY;
        if (offsetX > maxX * 2) offsetX = maxX * 2;
        if (offsetY > maxY * 2) offsetY = maxY * 2;
    }

    // Keeps a value inside [lo, hi].
    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // Centres the view on the middle of the grid.
    public void centreView() {
        if (engine == null || engine.getGrid() == null) return;
        Grid grid = engine.getGrid();
        int w = getWidth();
        int h = getHeight();
        if (w <= 0) w = getPreferredSize().width;
        if (h <= 0) h = getPreferredSize().height;
        offsetX = grid.getWidth() / 2.0 - w / (2.0 * cellSize);
        offsetY = grid.getHeight() / 2.0 - h / (2.0 * cellSize);
        repaint();
    }
}
