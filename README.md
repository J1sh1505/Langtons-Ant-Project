# Langton's Ant — Parallel Simulation

**CMP6011 — Parallel and Distributed Systems**

A Java Swing implementation of Langton's Ant that supports both sequential
and parallel execution modes. The grid is partitioned into independent
regions so that multiple ants can be advanced in parallel on a
`ForkJoinPool`, coordinated by a `Phaser` tick barrier. A built-in
benchmark suite measures speed-up and efficiency across grid sizes,
region counts, ant counts, and step counts.

---

## Key Parts of the Project

```
src/langton/
├── Main.java                       # Entry point — launches the Swing UI
├── model/                          # Pure domain model (no threading, no UI)
│   ├── Grid.java                   #   Sparse grid backed by ConcurrentHashMap
│   ├── Cell.java                   #   Cell colour state
│   ├── Ant.java                    #   Ant position, direction, region bounds
│   ├── Direction.java              #   N/E/S/W with turn-left / turn-right
│   └── Region.java                 #   Sub-grid owned by one worker thread
│
├── simulation/                     # Execution engines
│   ├── SimulationEngine.java       #   Facade that hides Sequential vs Parallel
│   ├── SequentialSimulation.java   #   Single-threaded baseline
│   ├── ParallelSimulation.java     #   ForkJoinPool + Phaser barrier per tick
│   └── BoundaryHandler.java        #   Routes ants between regions (dormant
│                                   #   today — kept for future cross-region
│                                   #   migration)
│
├── ui/                             # Swing front-end
│   ├── MainUI.java                 #   Top-level window, menus, animation loop
│   ├── GridCanvas.java             #   Renders the grid + ant markers
│   ├── ControlPanel.java           #   Start/Pause/Step/Reset + mode toggle
│   ├── ConfigDialog.java           #   Grid size / regions / ants per region
│   └── PerformancePanel.java       #   Live benchmark results table & FPS
│
└── metrics/                        # Performance measurement
    ├── BenchmarkRunner.java        #   Sweeps configurations, writes CSV
    └── PerformanceMetrics.java     #   Speed-up & efficiency calculations
```

Additional files in the repo root:

- `diagrams.md` — full system documentation: use-case, class, sequence and
  ant-step flow diagrams, plus analysis of the benchmark results.
- `out/` — pre-compiled `.class` files (regenerate with `javac`, see below).

---

## Requirements

- **JDK 11 or newer** (uses only standard library — `javax.swing`,
  `java.util.concurrent.ForkJoinPool`, `Phaser`, `ConcurrentHashMap`).
- No third-party dependencies, no build tool required.

---

## How to Run

All commands assume you are in the project root
(`Langtons-Ant-Project-main/`).

### 1. Compile

PowerShell / Windows:

```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

Bash / macOS / Linux:

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
```

### 2. Launch the interactive UI

```bash
java -cp out langton.Main
```

This opens the main window. Use the **Control Panel** to:

- Choose grid size, number of regions, and ants per region.
- Click **Apply Configuration** to build the world.
- **Start / Pause / Step / Reset** the simulation.
- Toggle between **Sequential** and **Parallel** mode at any time.
- Open **File → Run Benchmark** to populate the performance table.

### 3. Run the benchmark suite from the command line

```bash
java -cp out langton.metrics.BenchmarkRunner
```

This sweeps grid sizes, region counts, ants per region, and step counts,
performs warm-up + measured runs, and writes `benchmark-results.csv` to
the working directory. The numbers underpinning the charts in
`diagrams.md` come from this runner.

---

## Architecture at a Glance

- **Model** (`langton.model`) — pure data and rules. The grid is sparse:
  only flipped cells are stored, keyed by a packed `long` (x, y).
- **Simulation** (`langton.simulation`) — `SimulationEngine` is the
  facade. The UI only ever calls `engine.step()`; the engine delegates
  to `SequentialSimulation` or `ParallelSimulation` depending on the
  current mode.
- **Parallelism** — each region is advanced by an independent worker
  task submitted to a `ForkJoinPool`. Ants wrap inside their own region
  (Pac-Man style), so two workers can never write to the same cell key,
  removing the need for cell-level locks. Workers rendezvous on a
  `Phaser` at the end of every tick.
- **UI** (`langton.ui`) — Swing only, no domain logic. A `javax.swing.Timer`
  drives the animation; the canvas reads engine state on the EDT.
- **Metrics** (`langton.metrics`) — independent of the UI; can be invoked
  either from the menu bar or as a standalone `main()`.

See `diagrams.md` for class, sequence, and flow diagrams plus a full
analysis of the benchmark results.
