package langton.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Stores benchmark results and helpers for speedup/efficiency math.
public class PerformanceMetrics {

    private final List<BenchmarkResult> results = new ArrayList<>();

    // Adds a result row.
    public void addResult(BenchmarkResult result) {
        results.add(result);
    }

    // Returns all results.
    public List<BenchmarkResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    // Clears all results.
    public void clear() {
        results.clear();
    }

    // Speedup = seq time / par time.
    public static double computeSpeedup(long sequentialNs, long parallelNs) {
        if (parallelNs <= 0) {
            return 0.0;
        }
        return (double) sequentialNs / (double) parallelNs;
    }

    // Efficiency = speedup / threads.
    public static double computeEfficiency(double speedup, int numThreads) {
        if (numThreads <= 0) {
            return 0.0;
        }
        return speedup / numThreads;
    }

    // One row of benchmark data.
    public static class BenchmarkResult {
        private final int numAnts;
        private final int numRegions;
        private final int numSteps;
        private final long seqTimeNs;
        private final long parTimeNs;
        private final int parallelism;
        private final double speedup;
        private final double efficiency;

        // Stores config and timings, pre-computes speedup/efficiency.
        public BenchmarkResult(int numAnts, int numRegions, int numSteps,
                               long seqTimeNs, long parTimeNs, int parallelism) {
            this.numAnts = numAnts;
            this.numRegions = numRegions;
            this.numSteps = numSteps;
            this.seqTimeNs = seqTimeNs;
            this.parTimeNs = parTimeNs;
            this.parallelism = parallelism;
            this.speedup = computeSpeedup(seqTimeNs, parTimeNs);
            this.efficiency = computeEfficiency(this.speedup, parallelism);
        }

        // Returns ant count.
        public int getNumAnts() { return numAnts; }
        // Returns region count.
        public int getNumRegions() { return numRegions; }
        // Returns step count.
        public int getNumSteps() { return numSteps; }
        // Returns sequential time in ns.
        public long getSeqTimeNs() { return seqTimeNs; }
        // Returns parallel time in ns.
        public long getParTimeNs() { return parTimeNs; }
        // Returns speedup.
        public double getSpeedup() { return speedup; }
        // Returns efficiency.
        public double getEfficiency() { return efficiency; }
        // Returns thread count.
        public int getParallelism() { return parallelism; }

        // Sequential time in ms.
        public double getSeqTimeMs() { return seqTimeNs / 1_000_000.0; }
        // Parallel time in ms.
        public double getParTimeMs() { return parTimeNs / 1_000_000.0; }

        @Override
        public String toString() {
            return String.format(
                    "ants=%d regions=%d steps=%d seq=%.2fms par=%.2fms speedup=%.2f eff=%.2f",
                    numAnts, numRegions, numSteps,
                    getSeqTimeMs(), getParTimeMs(), speedup, efficiency);
        }
    }
}
