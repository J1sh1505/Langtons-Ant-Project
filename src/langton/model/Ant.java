package langton.model;

import java.util.concurrent.atomic.AtomicBoolean;

// One Langton's Ant: position, direction and region.
public class Ant {

    private int x;
    private int y;
    private Direction direction;
    private int regionId;
    private final int id;

    // Region rectangle the ant is locked into.
    private final int regionStartX;
    private final int regionEndX;
    private final int regionStartY;
    private final int regionEndY;

    // On/off flag used during region transfers.
    private final AtomicBoolean active = new AtomicBoolean(true);

    private long stepCount = 0L;

    // Sets up the ant inside its region.
    public Ant(int id, int x, int y, Direction direction, int regionId,
               int regionStartX, int regionEndX,
               int regionStartY, int regionEndY) {
        this.id = id;
        this.direction = direction;
        this.regionId = regionId;
        this.regionStartX = regionStartX;
        this.regionEndX = regionEndX;
        this.regionStartY = regionStartY;
        this.regionEndY = regionEndY;
        this.x = clampToRegionX(x);
        this.y = clampToRegionY(y);
    }

    // Flips the cell, turns, then moves forward one step.
    public synchronized void step(Grid grid) {
        Cell previous = grid.flipCell(x, y, regionId + 1);

        if (previous == Cell.WHITE) {
            direction = direction.turnRight();
        } else {
            direction = direction.turnLeft();
        }

        int nx = x + direction.getDx();
        int ny = y + direction.getDy();
        x = wrapPacMan(nx, regionStartX, regionEndX);
        y = wrapPacMan(ny, regionStartY, regionEndY);

        stepCount++;
    }

    // Predicts the next position without moving.
    public synchronized int[] getNextPosition(Grid grid) {
        Cell here = grid.getCell(x, y);
        Direction next = (here == Cell.WHITE) ? direction.turnRight() : direction.turnLeft();

        int nx = wrapPacMan(x + next.getDx(), regionStartX, regionEndX);
        int ny = wrapPacMan(y + next.getDy(), regionStartY, regionEndY);
        return new int[]{nx, ny};
    }

    // Wraps a value around the region bounds.
    private static int wrapPacMan(int v, int minBound, int maxBoundExclusive) {
        int regionSize = maxBoundExclusive - minBound;
        return minBound + (((v - minBound) % regionSize) + regionSize) % regionSize;
    }

    // Keeps x inside the region.
    private int clampToRegionX(int v) {
        if (v < regionStartX) return regionStartX;
        if (v >= regionEndX)  return regionEndX - 1;
        return v;
    }

    // Keeps y inside the region.
    private int clampToRegionY(int v) {
        if (v < regionStartY) return regionStartY;
        if (v >= regionEndY)  return regionEndY - 1;
        return v;
    }

    // Returns x.
    public synchronized int getX() {
        return x;
    }

    // Returns y.
    public synchronized int getY() {
        return y;
    }

    // Returns the direction.
    public synchronized Direction getDirection() {
        return direction;
    }

    // Returns the region id.
    public synchronized int getRegionId() {
        return regionId;
    }

    // Sets the region id.
    public synchronized void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    // Returns the active flag.
    public AtomicBoolean activeFlag() {
        return active;
    }

    // True if the ant is active.
    public boolean isActive() {
        return active.get();
    }

    // Returns the ant id.
    public int getId() {
        return id;
    }

    // Returns the total step count.
    public synchronized long getStepCount() {
        return stepCount;
    }

    @Override
    public String toString() {
        return "Ant#" + id + "{(" + x + "," + y + ")," + direction + ",region=" + regionId + "}";
    }
}
