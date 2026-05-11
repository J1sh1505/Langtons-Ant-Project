package langton.simulation;

import langton.model.Ant;
import langton.model.Grid;

import java.util.List;

// Single-threaded version. Used as a baseline.
public class SequentialSimulation {

    private final Grid grid;
    private final List<Ant> ants;

    // Stores the grid and ant list.
    public SequentialSimulation(Grid grid, List<Ant> ants) {
        this.grid = grid;
        this.ants = ants;
    }

    // Steps every ant once.
    public void step() {
        for (int i = 0, n = ants.size(); i < n; i++) {
            ants.get(i).step(grid);
        }
    }

    // Runs N ticks and returns how long it took.
    public long runSteps(int steps) {
        long start = System.nanoTime();
        for (int s = 0; s < steps; s++) {
            step();
        }
        return System.nanoTime() - start;
    }
}
