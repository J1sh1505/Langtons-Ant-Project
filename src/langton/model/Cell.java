package langton.model;

// A cell is either white or black.
public enum Cell {
    WHITE(0),
    BLACK(1);

    private final int value;

    // Stores the int code for the colour.
    Cell(int value) {
        this.value = value;
    }

    // Returns the int code.
    public int getValue() {
        return value;
    }

    // Turns a stored int back into a colour.
    public static Cell fromValue(int value) {
        return value != 0 ? BLACK : WHITE;
    }

    // Returns the opposite colour.
    public Cell flip() {
        return this == WHITE ? BLACK : WHITE;
    }
}
