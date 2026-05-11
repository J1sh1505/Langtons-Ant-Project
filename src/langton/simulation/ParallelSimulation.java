package langton.simulation;

import langton.model.Ant;
import langton.model.Grid;
import langton.model.Region;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.concurrent.RecursiveAction;

// Runs the simulation in parallel, one thread per region.
public class ParallelSimulation {

    private final Grid grid;
    private final List<Region> regions;
    private final BoundaryHandler boundaryHandler;
    private final ForkJoinPool pool;

    // Sync barrier used per tick.
    private final Phaser tickBarrier;

    // Sets up the thread pool and barrier.
    public ParallelSimulation(Grid grid, List<Region> regions) {
        this.grid = grid;
        this.regions = regions;
        this.boundaryHandler = new BoundaryHandler(regions);
        int parallelism = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.pool = new ForkJoinPool(parallelism);
        this.tickBarrier = new Phaser(regions.size() + 1);
    }

    // Returns the number of worker threads.
    public int getParallelism() {
        return pool.getParallelism();
    }

    // Runs one tick across all regions, then handles transfers.
    public void step() {
        for (Region region : regions) {
            final Region target = region;
            pool.execute(new RecursiveAction() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void compute() {
                    try {
                        stepRegion(target);
                    } finally {
                        tickBarrier.arrive();
                    }
                }
            });
        }

        tickBarrier.arriveAndAwaitAdvance();

        boundaryHandler.processPendingTransfers();
    }

    // Steps every ant inside one region.
    private void stepRegion(Region region) {
        List<Ant> snapshot = region.getAntsSnapshot();
        for (Ant ant : snapshot) {
            if (!ant.isActive()) {
                continue;
            }
            ant.step(grid);

            if (!region.contains(ant.getX(), ant.getY())) {
                Region dest = boundaryHandler.findRegionFor(ant.getX(), ant.getY());
                if (dest != null && dest != region) {
                    boundaryHandler.scheduleTransfer(ant, dest);
                }
            }
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

    // Shuts down the thread pool.
    public void shutdown() {
        pool.shutdown();
    }
}
