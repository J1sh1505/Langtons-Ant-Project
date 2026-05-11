package langton.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The grid of cells. Only non-white cells are stored.
public class Grid {

    private final int width;
    private final int height;

    // Cell storage: key is (x,y), value is the colour code.
    private final ConcurrentHashMap<Long, Integer> cells;

    // Creates an empty grid.
    public Grid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new ConcurrentHashMap<>(1024);
    }

    // Returns the width.
    public int getWidth() {
        return width;
    }

    // Returns the height.
    public int getHeight() {
        return height;
    }

    // Gets the colour at a cell.
    public Cell getCell(int x, int y) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        Integer v = cells.get(key(wx, wy));
        return Cell.fromValue(v == null ? 0 : v);
    }

    // Gets the raw int value at a cell.
    public int getCellValue(int x, int y) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        Integer v = cells.get(key(wx, wy));
        return v == null ? 0 : v;
    }

    // Flips a cell black/white safely. Returns the old colour.
    public Cell flipCell(int x, int y, int colourIndex) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        Long k = key(wx, wy);

        final Cell[] previous = new Cell[]{Cell.WHITE};
        cells.compute(k, (unused, current) -> {
            if (current == null || current == 0) {
                previous[0] = Cell.WHITE;
                return colourIndex;
            } else {
                previous[0] = Cell.BLACK;
                return null;
            }
        });
        return previous[0];
    }

    // Sets a cell to a specific value.
    public void setCellValue(int x, int y, int value) {
        int wx = wrapX(x);
        int wy = wrapY(y);
        Long k = key(wx, wy);
        if (value == 0) {
            cells.remove(k);
        } else {
            cells.put(k, value);
        }
    }

    // Sets a cell to white or black.
    public void setCell(int x, int y, Cell cell) {
        setCellValue(x, y, cell == Cell.WHITE ? 0 : 1);
    }

    // Wipes all cells.
    public void clear() {
        cells.clear();
    }

    // Returns how many non-white cells exist.
    public int getBlackCellCount() {
        return cells.size();
    }

    // Returns the raw map for fast rendering.
    public Map<Long, Integer> getCellMap() {
        return cells;
    }

    // Wraps x into the grid.
    public int wrapX(int x) {
        int r = x % width;
        return r < 0 ? r + width : r;
    }

    // Wraps y into the grid.
    public int wrapY(int y) {
        int r = y % height;
        return r < 0 ? r + height : r;
    }

    // Packs (x,y) into a single long.
    public long key(int x, int y) {
        return (long) x * height + y;
    }

    // Unpacks x from a long key.
    public int decodeX(long k) {
        return (int) (k / height);
    }

    // Unpacks y from a long key.
    public int decodeY(long k) {
        return (int) (k % height);
    }
}
