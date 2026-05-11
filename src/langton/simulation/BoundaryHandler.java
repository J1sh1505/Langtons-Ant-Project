package langton.simulation;

import langton.model.Ant;
import langton.model.Region;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

// Moves ants between regions when they cross a boundary.
public class BoundaryHandler {

    private final List<Region> regions;

    // Stores the region list.
    public BoundaryHandler(List<Region> regions) {
        this.regions = regions;
    }

    // Finds which region contains (x,y).
    public Region findRegionFor(int x, int y) {
        for (Region r : regions) {
            if (r.contains(x, y)) {
                return r;
            }
        }
        return null;
    }

    // Queues an ant for transfer to a new region.
    public void scheduleTransfer(Ant ant, Region newRegion) {
        ant.activeFlag().set(false);
        newRegion.getPendingTransfers().offer(ant);
    }

    // Processes every queued transfer.
    public void processPendingTransfers() {
        for (Region dest : regions) {
            Ant ant;
            while ((ant = dest.getPendingTransfers().poll()) != null) {
                int sourceId = ant.getRegionId();
                Region source = regions.get(sourceId);
                transferAnt(ant, source, dest);
            }
        }
    }

    // Moves one ant from source to dest under both locks.
    private void transferAnt(Ant ant, Region source, Region dest) {
        if (source == dest) {
            ant.activeFlag().set(true);
            return;
        }

        Region first = source.getRegionId() < dest.getRegionId() ? source : dest;
        Region second = first == source ? dest : source;

        ReentrantLock lockA = first.getLock();
        ReentrantLock lockB = second.getLock();

        lockA.lock();
        try {
            lockB.lock();
            try {
                source.removeAnt(ant);
                ant.setRegionId(dest.getRegionId());
                dest.addAnt(ant);
                ant.activeFlag().set(true);
            } finally {
                lockB.unlock();
            }
        } finally {
            lockA.unlock();
        }
    }
}
