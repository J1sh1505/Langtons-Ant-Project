package langton.simulation;

import langton.model.Ant;
import langton.model.Direction;
import langton.model.Grid;
import langton.model.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Owns the grid, ants, regions, and switches between sequential and parallel modes.
public class SimulationEngine {

    // Which mode to run in.
    public enum Mode {
        SEQUENTIAL,
        PARALLEL
    }

    private Grid grid;
    private final List<Ant> ants = new ArrayList<>();
    private final List<Region> regions = new ArrayList<>();

    private SequentialSimulation sequential;
    private ParallelSimulation parallel;

    private Mode mode = Mode.SEQUENTIAL;
    private long currentStep = 0L;

    private int gridWidth = 1000;
    private int gridHeight = 1000;
    private int numRegions = 4;
    private int antsPerRegion = 1;

    // Builds the world from scratch with the given settings.
    public void initialize(int width, int height, int numRegions, int antsPerRegion) {
        if (parallel != null) {
            parallel.shutdown();
            parallel = null;
        }

        this.gridWidth = width;
        this.gridHeight = height;
        this.numRegions = numRegions;
        this.antsPerRegion = antsPerRegion;
        this.currentStep = 0L;

        this.grid = new Grid(width, height);
        this.ants.clear();
        this.regions.clear();

        int[] layout = computeRegionLayout(numRegions);
        int cols = layout[0];
        int rows = layout[1];

        int regionId = 0;
        int antIdSeq = 0;
        for (int ry = 0; ry < rows; ry++) {
            for (int rx = 0; rx < cols; rx++) {
                int sx = (rx * width) / cols;
                int ex = ((rx + 1) * width) / cols;
                int sy = (ry * height) / rows;
                int ey = ((ry + 1) * height) / rows;

                Region region = new Region(regionId, sx, sy, ex, ey);
                regions.add(region);

                int cx = region.getCenterX();
                int cy = region.getCenterY();
                for (int a = 0; a < antsPerRegion; a++) {
                    int offsetX = (a % 4) - 2;
                    int offsetY = (a / 4) - 2;
                    int ax = clampInto(cx + offsetX, sx, ex);
                    int ay = clampInto(cy + offsetY, sy, ey);

                    Ant ant = new Ant(antIdSeq++, ax, ay, Direction.NORTH,
                            regionId, sx, ex, sy, ey);
                    region.addAnt(ant);
                    ants.add(ant);
                }
                regionId++;
            }
        }

        this.sequential = new SequentialSimulation(grid, ants);
        this.parallel = new ParallelSimulation(grid, regions);
    }

    // Keeps a value inside [lo, hi).
    private static int clampInto(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v >= hi) return hi - 1;
        return v;
    }

    // Picks rows and columns for laying out regions.
    static int[] computeRegionLayout(int n) {
        int best = 1;
        for (int i = 1; i * i <= n; i++) {
            if (n % i == 0) {
                best = i;
            }
        }
        int rows = best;
        int cols = n / best;
        if (cols < rows) {
            int tmp = cols;
            cols = rows;
            rows = tmp;
        }
        return new int[]{cols, rows};
    }

    // Runs one tick using the chosen mode.
    public void step() {
        if (mode == Mode.SEQUENTIAL) {
            sequential.step();
        } else {
            parallel.step();
        }
        currentStep++;
    }

    // Runs many ticks and returns elapsed time.
    public long runSteps(int steps) {
        long elapsed;
        if (mode == Mode.SEQUENTIAL) {
            elapsed = sequential.runSteps(steps);
        } else {
            elapsed = parallel.runSteps(steps);
        }
        currentStep += steps;
        return elapsed;
    }

    // Resets the world.
    public void reset() {
        initialize(gridWidth, gridHeight, numRegions, antsPerRegion);
    }

    // Stops the worker pool.
    public void shutdown() {
        if (parallel != null) {
            parallel.shutdown();
        }
    }

    // Returns the grid.
    public Grid getGrid() {
        return grid;
    }

    // Returns all ants.
    public List<Ant> getAnts() {
        return Collections.unmodifiableList(ants);
    }

    // Returns all regions.
    public List<Region> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    // Returns the current mode.
    public Mode getMode() {
        return mode;
    }

    // Switches mode.
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    // Returns the tick count.
    public long getCurrentStep() {
        return currentStep;
    }

    // Returns the grid width.
    public int getGridWidth() {
        return gridWidth;
    }

    // Returns the grid height.
    public int getGridHeight() {
        return gridHeight;
    }

    // Returns the region count.
    public int getNumRegions() {
        return numRegions;
    }

    // Returns ants per region.
    public int getAntsPerRegion() {
        return antsPerRegion;
    }

    // Returns the thread count.
    public int getParallelism() {
        return parallel == null ? 0 : parallel.getParallelism();
    }
}
