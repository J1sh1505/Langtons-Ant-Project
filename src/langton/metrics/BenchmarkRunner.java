package langton.metrics;

import langton.metrics.PerformanceMetrics.BenchmarkResult;
import langton.simulation.SimulationEngine;
import langton.simulation.SimulationEngine.Mode;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// Runs benchmark sweeps and collects results.
public class BenchmarkRunner {

    private final PerformanceMetrics metrics;

    // Settings for the quick UI suite.
    private static final int[] DEFAULT_ANT_COUNTS = {1, 2, 4, 8};
    private static final int[] DEFAULT_REGION_COUNTS = {1, 2, 4};
    private static final int[] DEFAULT_STEP_COUNTS = {10_000, 100_000};

    private static final int BENCHMARK_GRID_WIDTH = 1000;
    private static final int BENCHMARK_GRID_HEIGHT = 1000;

    // Settings for the full comprehensive suite.
    private static final int[] COMPREHENSIVE_GRID_SIZES =
            {100, 500, 1000, 5000, 10_000};
    private static final int[] COMPREHENSIVE_REGION_COUNTS =
            {1, 2, 4, 8, 16};
    private static final int[] COMPREHENSIVE_ANTS_PER_REGION =
            {1, 2, 5, 10};
    private static final int[] COMPREHENSIVE_STEP_COUNTS =
            {10_000, 100_000, 1_000_000};

    // Warm-up and measured run counts.
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 5;

    // Default CSV filename.
    private static final String DEFAULT_CSV_PATH = "benchmark-results.csv";

    // Stores the metrics aggregator.
    public BenchmarkRunner(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    // Runs the quick benchmark suite for the UI.
    public void runFullSuite(BiConsumer<Double, String> progress,
                             Consumer<BenchmarkResult> resultListener) {

        int totalConfigurations = DEFAULT_ANT_COUNTS.length
                * DEFAULT_REGION_COUNTS.length
                * DEFAULT_STEP_COUNTS.length;
        int completed = 0;

        System.out.println();
        System.out.println("=== Langton's Ant Benchmark Suite ===");
        System.out.printf("Grid size: %d x %d%n", BENCHMARK_GRID_WIDTH, BENCHMARK_GRID_HEIGHT);
        System.out.printf("Available cores: %d%n", Runtime.getRuntime().availableProcessors());
        System.out.println();
        printHeader();

        for (int regions : DEFAULT_REGION_COUNTS) {
            for (int totalAnts : DEFAULT_ANT_COUNTS) {
                int antsPerRegion = Math.max(1, (int) Math.ceil((double) totalAnts / regions));
                int actualAnts = antsPerRegion * regions;

                for (int steps : DEFAULT_STEP_COUNTS) {
                    String label = String.format(
                            "ants=%d regions=%d steps=%d", actualAnts, regions, steps);
                    if (progress != null) {
                        progress.accept((double) completed / totalConfigurations, label);
                    }

                    BenchmarkResult result = runSingleQuick(actualAnts, regions, antsPerRegion, steps);
                    metrics.addResult(result);
                    printRow(result);
                    if (resultListener != null) {
                        resultListener.accept(result);
                    }

                    completed++;
                }
            }
        }

        if (progress != null) {
            progress.accept(1.0, "Benchmark complete");
        }
        System.out.println();
        System.out.println("=== Benchmark complete ===");
    }

    // Runs one quick benchmark config.
    public BenchmarkResult runSingleQuick(int numAnts, int numRegions, int antsPerRegion, int steps) {
        return runSingle(BENCHMARK_GRID_WIDTH, BENCHMARK_GRID_HEIGHT,
                numRegions, antsPerRegion, steps, 1, 1);
    }

    // Forwards to the quick version. Kept for older callers.
    public BenchmarkResult runSingle(int numAnts, int numRegions, int antsPerRegion, int steps) {
        return runSingleQuick(numAnts, numRegions, antsPerRegion, steps);
    }

    // Runs the full comprehensive sweep and writes a CSV.
    public List<BenchmarkResult> runComprehensiveSuite(
            Path csvPath,
            BiConsumer<Double, String> progress,
            Consumer<BenchmarkResult> resultListener) throws IOException {

        List<BenchmarkResult> all = new ArrayList<>();

        long total = (long) COMPREHENSIVE_GRID_SIZES.length
                * COMPREHENSIVE_REGION_COUNTS.length
                * COMPREHENSIVE_ANTS_PER_REGION.length
                * COMPREHENSIVE_STEP_COUNTS.length;
        long completed = 0;

        System.out.println();
        System.out.println("=== Langton's Ant Comprehensive Benchmark Suite ===");
        System.out.printf("Configurations: %d%n", total);
        System.out.printf("Warm-up runs:   %d (discarded)%n", WARMUP_RUNS);
        System.out.printf("Measured runs:  %d (median reported)%n", MEASURED_RUNS);
        System.out.printf("Cores:          %d%n", Runtime.getRuntime().availableProcessors());
        System.out.printf("CSV output:     %s%n", csvPath.toAbsolutePath());
        System.out.println();
        printHeader();

        try (PrintWriter csv = new PrintWriter(
                Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
            csv.println("gridSize,regions,antsPerRegion,totalAnts,steps,"
                    + "seqTimeMs,parTimeMs,speedup,efficiency,threads");

            for (int gridSize : COMPREHENSIVE_GRID_SIZES) {
                for (int regions : COMPREHENSIVE_REGION_COUNTS) {
                    for (int antsPerRegion : COMPREHENSIVE_ANTS_PER_REGION) {
                        int totalAnts = regions * antsPerRegion;
                        for (int steps : COMPREHENSIVE_STEP_COUNTS) {
                            String label = String.format(
                                    "grid=%d regions=%d ants/r=%d steps=%d",
                                    gridSize, regions, antsPerRegion, steps);
                            if (progress != null) {
                                progress.accept((double) completed / total, label);
                            }

                            BenchmarkResult result = runSingle(
                                    gridSize, gridSize,
                                    regions, antsPerRegion, steps,
                                    WARMUP_RUNS, MEASURED_RUNS);

                            metrics.addResult(result);
                            all.add(result);
                            printRow(result);
                            double efficiencyByRegions = result.getNumRegions() == 0
                                    ? 0.0
                                    : result.getSpeedup() / result.getNumRegions();
                            csv.printf(java.util.Locale.ROOT,
                                    "%d,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%d%n",
                                    gridSize, regions, antsPerRegion, totalAnts, steps,
                                    result.getSeqTimeMs(), result.getParTimeMs(),
                                    result.getSpeedup(), efficiencyByRegions,
                                    result.getParallelism());
                            csv.flush();

                            if (resultListener != null) {
                                resultListener.accept(result);
                            }
                            completed++;
                        }
                    }
                }
            }
        }

        if (progress != null) {
            progress.accept(1.0, "Comprehensive benchmark complete");
        }
        System.out.println();
        System.out.println("=== Comprehensive benchmark complete ===");
        printSummary(all);
        return all;
    }

    // Runs one configuration with warm-up and median-of-N timing.
    public BenchmarkResult runSingle(int gridWidth, int gridHeight,
                                     int numRegions, int antsPerRegion,
                                     int steps,
                                     int warmupRuns, int measuredRuns) {
        int totalAnts = numRegions * antsPerRegion;
        SimulationEngine engine = new SimulationEngine();

        for (int w = 0; w < warmupRuns; w++) {
            engine.initialize(gridWidth, gridHeight, numRegions, antsPerRegion);
            engine.setMode(Mode.SEQUENTIAL);
            engine.runSteps(steps);
        }
        long[] seqRuns = new long[measuredRuns];
        for (int m = 0; m < measuredRuns; m++) {
            engine.initialize(gridWidth, gridHeight, numRegions, antsPerRegion);
            engine.setMode(Mode.SEQUENTIAL);
            seqRuns[m] = engine.runSteps(steps);
        }
        long seqMedian = medianOf(seqRuns);

        for (int w = 0; w < warmupRuns; w++) {
            engine.initialize(gridWidth, gridHeight, numRegions, antsPerRegion);
            engine.setMode(Mode.PARALLEL);
            engine.runSteps(steps);
        }
        long[] parRuns = new long[measuredRuns];
        int parallelism = 1;
        for (int m = 0; m < measuredRuns; m++) {
            engine.initialize(gridWidth, gridHeight, numRegions, antsPerRegion);
            engine.setMode(Mode.PARALLEL);
            parRuns[m] = engine.runSteps(steps);
            parallelism = Math.max(1, engine.getParallelism());
        }
        long parMedian = medianOf(parRuns);

        engine.shutdown();
        return new BenchmarkResult(totalAnts, numRegions, steps,
                seqMedian, parMedian, parallelism);
    }

    // Returns the median of an array.
    private static long medianOf(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    // Prints the best-speedup row at the end.
    private static void printSummary(List<BenchmarkResult> all) {
        if (all.isEmpty()) return;
        double bestSpeedup = 0;
        BenchmarkResult bestRow = all.get(0);
        for (BenchmarkResult r : all) {
            if (r.getSpeedup() > bestSpeedup) {
                bestSpeedup = r.getSpeedup();
                bestRow = r;
            }
        }
        System.out.println();
        System.out.printf(
                "Best speed-up: %.2fx (ants=%d regions=%d steps=%d, threads=%d)%n",
                bestSpeedup, bestRow.getNumAnts(), bestRow.getNumRegions(),
                bestRow.getNumSteps(), bestRow.getParallelism());
    }

    // Prints the table header.
    private static void printHeader() {
        System.out.printf("%-6s %-8s %-9s %-15s %-15s %-9s %-10s%n",
                "Ants", "Regions", "Steps", "Seq Time (ms)", "Par Time (ms)",
                "Speedup", "Efficiency");
        System.out.println("------------------------------------------------------------------------------");
    }

    // Prints one table row.
    private static void printRow(BenchmarkResult r) {
        System.out.printf("%-6d %-8d %-9d %-15.2f %-15.2f %-9.2f %-10.2f%n",
                r.getNumAnts(), r.getNumRegions(), r.getNumSteps(),
                r.getSeqTimeMs(), r.getParTimeMs(),
                r.getSpeedup(), r.getEfficiency());
    }

    // CLI entry point. Runs the comprehensive suite.
    public static void main(String[] args) throws IOException {
        Path csvPath = Paths.get(args.length > 0 ? args[0] : DEFAULT_CSV_PATH);
        BenchmarkRunner runner = new BenchmarkRunner(new PerformanceMetrics());
        runner.runComprehensiveSuite(
                csvPath,
                (progress, label) -> {
                    int pct = (int) (progress * 100);
                    System.out.printf("[%3d%%] %s%n", pct, label);
                },
                null);
        System.out.printf("Results written to %s%n", csvPath.toAbsolutePath());
    }
}
