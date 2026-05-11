package langton.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

// A rectangular slice of the grid owned by one worker thread.
public class Region {

    private final int regionId;
    private final int startX;
    private final int startY;
    private final int endX;
    private final int endY;

    // Ants currently inside this region.
    private final List<Ant> ants;

    // Lock for safe ant transfers.
    private final ReentrantLock lock = new ReentrantLock();

    // Inbox for ants coming from other regions.
    private final ConcurrentLinkedQueue<Ant> pendingTransfers = new ConcurrentLinkedQueue<>();

    // Sets up the region rectangle.
    public Region(int regionId, int startX, int startY, int endX, int endY) {
        this.regionId = regionId;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.ants = new ArrayList<>();
    }

    // Returns the region id.
    public int getRegionId() {
        return regionId;
    }

    // Returns the left edge.
    public int getStartX() {
        return startX;
    }

    // Returns the top edge.
    public int getStartY() {
        return startY;
    }

    // Returns the right edge.
    public int getEndX() {
        return endX;
    }

    // Returns the bottom edge.
    public int getEndY() {
        return endY;
    }

    // Returns the width.
    public int getWidth() {
        return endX - startX;
    }

    // Returns the height.
    public int getHeight() {
        return endY - startY;
    }

    // Returns the middle x.
    public int getCenterX() {
        return startX + (endX - startX) / 2;
    }

    // Returns the middle y.
    public int getCenterY() {
        return startY + (endY - startY) / 2;
    }

    // True if (x,y) is inside this region.
    public boolean contains(int x, int y) {
        return x >= startX && x < endX && y >= startY && y < endY;
    }

    // Returns the live ant list.
    public List<Ant> getAnts() {
        return ants;
    }

    // Returns a safe copy of the ant list.
    public synchronized List<Ant> getAntsSnapshot() {
        return new ArrayList<>(ants);
    }

    // Adds an ant.
    public void addAnt(Ant ant) {
        ants.add(ant);
    }

    // Removes an ant.
    public boolean removeAnt(Ant ant) {
        return ants.remove(ant);
    }

    // Returns the region lock.
    public ReentrantLock getLock() {
        return lock;
    }

    // Returns the inbox queue.
    public ConcurrentLinkedQueue<Ant> getPendingTransfers() {
        return pendingTransfers;
    }

    @Override
    public String toString() {
        return "Region#" + regionId + "[" + startX + "," + startY + " → " + endX + "," + endY
                + " ants=" + ants.size() + "]";
    }
}
