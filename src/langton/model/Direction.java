package langton.model;

// Four compass directions.
public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    // Stores the step offset for each direction.
    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    // Returns horizontal step.
    public int getDx() {
        return dx;
    }

    // Returns vertical step.
    public int getDy() {
        return dy;
    }

    // Turns 90° right.
    public Direction turnRight() {
        return values()[(this.ordinal() + 1) % 4];
    }

    // Turns 90° left.
    public Direction turnLeft() {
        return values()[(this.ordinal() + 3) % 4];
    }
}
