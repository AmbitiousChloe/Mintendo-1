// TetrisBoard.java
package model;

import java.io.Serializable;
import java.util.Arrays;

/** Represents a Board class for Tetris.  
 * Based on the Tetris assignment in the Nifty Assignments Database, authored by Nick Parlante
 */
public class TetrisBoard implements Serializable{
    private int width; //board height and width
    private int height;
    protected boolean[][] tetrisGrid, secondTetrisGrid; //board grid
    boolean committed, secondCommited; //indicates if the board is in a 'committed' state, meaning can't undo!

    //In your implementation, you'll want to keep counts of filled grid positions in each column.
    //A completely filled column means the game is over!
    private int colCounts[];
    //You will also want to keep counts by row.
    //A completely filled row can be cleared from the board (and points are awarded)!
    private int rowCounts[];

    //In addition, you'll need to allocate some space to back up your grid data.
    //This will be important when you implement "undo".
    private boolean[][] backupGrid; //to back up your grid
    private int backupColCounts[]; //to back up your row counts
    private int backupRowCounts[]; //to back up your column counts

    //error types (to be returned by the place function)
    public static final int ADD_OK = 0;
    public static final int ADD_ROW_FILLED = 1;
    public static final int ADD_OUT_BOUNDS = 2;
    public static final int ADD_BAD = 3;

    /**
     * Constructor for an empty board of the given width and height measured in blocks.
     *
     * @param aWidth    width
     * @param aHeight    height
     */
    public TetrisBoard(int aWidth, int aHeight) {
        width = aWidth;
        height = aHeight;
        tetrisGrid = new boolean[width][height];
        secondTetrisGrid = new boolean[width][height];
        colCounts = new int[width];
        rowCounts = new int[height];

        //init backup storage, for undo
        backupGrid = new boolean[width][height];
        backupColCounts = new int[width];
        backupRowCounts = new int[height];
    }

    /**
     * Helper to fill new game grid with empty values
     */
    public void newGame() {
        for (int x = 0; x < tetrisGrid.length; x++) {
            for (int y = 0; y < tetrisGrid[x].length; y++) {
                tetrisGrid[x][y] = false;
                }
            }
        for (int a = 0; a < secondTetrisGrid.length; a++) {
            for (int b = 0; b < secondTetrisGrid[a].length; b++) {
                secondTetrisGrid[a][b] = false;
            }
        }
        Arrays.fill(colCounts, 0);
        Arrays.fill(rowCounts, 0);
        committed = true;
        secondCommited = true;
    }

    /**
     * Getter for board width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for board height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the max column height present in the board.
     * For an empty board this is 0.
     *
     * @return the y position of the last filled square in the tallest column
     */
    public int getMaxHeight() {
        return Arrays.stream(colCounts).max().getAsInt();
    }

    /**
     * Returns the height of the given column -- i.e. the y value of the highest block + 1.
     * The height is 0 if the column contains no blocks.
     *
     * @param x grid column, x
     *
     * @return the height of the given column, x
     */
    public int getColumnHeight(int x) {
        return colCounts[x];
    }

    /**
     * Returns the number of filled blocks in the given row.
     *
     * @param y grid row, y
     *
     * @return the number of filled blocks in row y
     */
    public int getRowWidth(int y) {
        return rowCounts[y];
    }

    /**
     * Returns true if the given block is filled in the board. Blocks outside of the
     * valid width/height area always return true (as we can't place anything there).
     *
     * @param x grid position, x
     * @param y grid position, y
     *
     * @return true if the given block at x,y is filled, else false
     */
    public boolean getGrid(int x, int y) {
        if (x >= width || x < 0 || y >= height || y < 0 || tetrisGrid[x][y])
            return true;
        return false;
    }
    // get the second board grid
    public boolean secondGetGrid(int x, int y) {
        if (x >= width || x < 0 || y >= height || y < 0 || secondTetrisGrid[x][y])
            return true;
        return false;
    }

    /**
     * Given a piece and an x, returns the y value where the piece will come to rest
     * if it were dropped straight down at that x.
     *
     * Use getLowestYVals and the col heights (getColumnHeight) to compute this quickly!
     *
     * @param piece piece to place
     * @param x column of grid
     *
     * @return the y value where the piece will come to rest
     */
    public int placementHeight(TetrisPiece piece, int x) {
        int yValue = 0;
        for(int i = 0; i < piece.getLowestYVals().length; i++){
            int curr = colCounts[x + i] - piece.getLowestYVals()[i];
            if(curr > yValue){
                yValue = curr;
            }
        }
        return yValue;
    }

    /**
     * Attempts to add the body of a piece to the board. Copies the piece blocks into the board grid.
     * Returns ADD_OK for a regular placement, or ADD_ROW_FILLED
     * for a regular placement that causes at least one row to be filled. 
     * 
     * Error cases:
     * A placement may fail in two ways. First, if part of the piece may fall out
     * of bounds of the board, ADD_OUT_BOUNDS is returned.
     * Or the placement may collide with existing blocks in the grid
     * in which case ADD_BAD is returned.
     * In both error cases, the board may be left in an invalid
     * state. The client can use undo(), to recover the valid, pre-place state.
     * 
     * @param piece piece to place
     * @param x placement position, x
     * @param y placement position, y
     * 
     * @return static int that defines result of placement
     */
    public int placePiece(TetrisPiece piece, int x, int y) {
        if (!committed){
            throw new RuntimeException("Commit problem");
        }
        committed = false;
        backupGrid();
        for (TetrisPoint points: piece.getBody()){
            int x_ = x + points.x;
            int y_ = y + points.y;
            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height){
                undo();
                return ADD_OUT_BOUNDS;
            }
            if (getGrid(x_, y_)){
                undo();
                return ADD_BAD;
            }
            tetrisGrid[x_][y_] = true;
        }
        makeHeightAndWidthArrays();
        for (int i = 0; i < height; i ++){
            if (rowCounts[i] == width){
                return ADD_ROW_FILLED;
            }
        }
        return ADD_OK;
    }

    //add second piece to second board
    public int secondPlacePiece(TetrisPiece piece, int x, int y) {
        for (int a = 0; a < secondTetrisGrid.length; a++) {
            for (int b = 0; b < secondTetrisGrid[a].length; b++) {
                secondTetrisGrid[a][b] = false;
            }
        }
        for (TetrisPoint points: piece.getBody()){
            int x_ = x + points.x;
            int y_ = y + points.y;
            if (x_ < 0 || x_ >= width || y_ < 0 || y_ >= height){
                undo();
                return ADD_OUT_BOUNDS;
            }
            if (secondGetGrid(x_, y_)){
                return ADD_BAD;
            }
            secondTetrisGrid[x_][y_] = true;
        }
        secondMakeHeightAndWidthArrays();
        for (int i = 0; i < height; i ++){
            if (rowCounts[i] == width){
                return ADD_ROW_FILLED;
            }
        }
        return ADD_OK;
    }

    /**
     * Deletes rows that are filled all the way across, moving
     * things above down. Returns the number of rows cleared.
     * 
     * @return number of rows cleared (useful for scoring)
     */
    public int clearRows() {
        int rowsCleared = 0;
        int maxHeight = getMaxHeight();
        if (committed)
            for (int i = 0; i < tetrisGrid.length; i++)
                System.arraycopy(tetrisGrid[i], 0, backupGrid[i], 0, tetrisGrid[i].length);
        committed = false;
        for (int toIndex = 0; toIndex <= maxHeight; toIndex++) {
            if (width == getRowWidth(toIndex)) {
                int fromIndex = toIndex + 1;
                while (fromIndex <= maxHeight && width == getRowWidth(fromIndex))
                    fromIndex++;
                for (int i = 0; i < width; i++) {
                    tetrisGrid[i][toIndex] = tetrisGrid[i][fromIndex];
                    tetrisGrid[i][fromIndex] = false;
                }
                int ceil = maxHeight-rowsCleared;
                while (fromIndex < ceil) {
                    for (int i=0; i < width; i++) {
                        tetrisGrid[i][fromIndex] = tetrisGrid[i][fromIndex+1];
                        tetrisGrid[i][fromIndex+1] = false;
                    }
                    fromIndex++;
                }
                rowsCleared++;
            }
        }
        return rowsCleared;
    }

    /**
     * Reverts the board to its state before up to one call to placePiece() and one to clearRows();
     * If the conditions for undo() are not met, such as calling undo() twice in a row, then the second undo() does nothing.
     * See the overview docs.
     */
    public void undo() {
        if (committed == true) return;  //a committed board cannot be undone!

        if (backupGrid == null) throw new RuntimeException("No source for backup!");  //a board with no backup source cannot be undone!

        //make a copy!!
        for (int i = 0; i < backupGrid.length; i++) {
            System.arraycopy(backupGrid[i], 0, tetrisGrid[i], 0, backupGrid[i].length);
        }

        //copy row and column tallies as well.
        System.arraycopy(backupRowCounts, 0, rowCounts, 0, backupRowCounts.length);
        System.arraycopy(backupColCounts, 0, colCounts, 0, backupColCounts.length);

        committed = true; //no going backwards now!
    }

    /**
     * Copy the backup grid into the grid that defines the board (to support undo)
     */
    private void backupGrid() {
        //make a copy!!
        for (int i = 0; i < tetrisGrid.length; i++) {
            System.arraycopy(tetrisGrid[i], 0, backupGrid[i], 0, tetrisGrid[i].length);
        }
        //copy row and column tallies as well.
        System.arraycopy(rowCounts, 0, backupRowCounts, 0, rowCounts.length);
        System.arraycopy(colCounts, 0, backupColCounts, 0, colCounts.length);
    }

    /**
     * Puts the board in the 'committed' state.
     */
    public void commit() {
        committed = true;
    }
    public void secondCommited(){
        secondCommited = true;
    }
    /**
     * Fills heightsOfCols[] and widthOfRows[].  Useful helper to support clearing rows and placing pieces.
     */
    private void makeHeightAndWidthArrays() {

        Arrays.fill(colCounts, 0);
        Arrays.fill(rowCounts, 0);

        for (int x = 0; x < tetrisGrid.length; x++) {
            for (int y = 0; y < tetrisGrid[x].length; y++) {
                if (tetrisGrid[x][y]) { //means is not an empty cell
                    colCounts[x] = y + 1; //these tallies can be useful when clearing rows or placing pieces
                    rowCounts[y]++;
                }
            }
        }
    }

    //find second board make height and width arrays
    private void secondMakeHeightAndWidthArrays() {

        Arrays.fill(colCounts, 0);
        Arrays.fill(rowCounts, 0);

        for (int x = 0; x < secondTetrisGrid.length; x++) {
            for (int y = 0; y < secondTetrisGrid[x].length; y++) {
                if (secondTetrisGrid[x][y]) { //means is not an empty cell
                    colCounts[x] = y + 1; //these tallies can be useful when clearing rows or placing pieces
                    rowCounts[y]++;
                }
            }
        }
    }


    /**
     * Print the board
     * 
     * @return a string representation of the board (useful for debugging)
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (int y = height-1; y>=0; y--) {
            buff.append('|');
            for (int x=0; x<width; x++) {
                if (getGrid(x,y)) buff.append('+');
                else buff.append(' ');
            }
            buff.append("|\n");
        }
        for (int x=0; x<width+2; x++) buff.append('-');
        return(buff.toString());
    }


}


