package com.heslihop.wesley.gameoflife;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* This class is designed to be a singleton.
 * It holds all the important data and logic for the actual game. Having this data encapsulated here instead of
 * being stored in the GameScreen Activity prevents loss on screen rotation.
 */

public class GameData implements Serializable {
    private static GameData instance = new GameData();

    boolean toroidal = true;
    int rowsTotal=15, columnsTotal=15;
    int cellRadius=35;
    int generationCounter = 0;
    transient float cellWidth, cellHeight, oldCellWidth, oldCellHeight;

    transient private DatabaseHelper dbh;
    transient private String lastID; // Held here for DatabaseHelper so information is not lost on screen rotations.

    // The fundamental rules for Conway's The Game of Life
    // Created this way to allow users to edit later on
    public static List<Integer> need2Survive, need2BeBorn;
    // Static so individual cells can access other cells using their own instance methods without the need to have
    // a reference to GameData in each Cell.
    public static List<Cell> petriDish;

    private GameData () {
        // Initialize rules.
        // These will be the default rules. Changeable by user later.
        need2Survive = new ArrayList<Integer>();
        need2Survive.add(2); need2Survive.add(3);
        need2BeBorn = new ArrayList<Integer>();
        need2BeBorn.add(3);

        // Initialize world
        petriDish = new ArrayList<Cell>();
    }

    public static GameData getInstance () {
        return instance;
    }

    // Randomizes entire grid to whatever desired ratio is.
    public void randomize (float ratio) {
        for (Cell c : petriDish) {
            if (Math.random() < ratio) c.setAlive(true);
            else c.setAlive(false);
        }
        allCellsCountNeighbours();
        calcNextGen();
        generationCounter = 0;

        // Save to database as "last_random"
        if (dbh != null) dbh.saveCurrent("last_random", true);
    }

    // Do you really need a comment to understand this method?!
    public void shutDownTheMatrixAndKillAllCells () {
        generationCounter = 0;
        for (Cell c : petriDish) {
            c.setAlive(false);
            c.setNextGen(false);
            c.setNeighbourCount(0);
        }
    }

    public void calcNextGen () {
        allCellsCountNeighbours();
        for (Cell c : petriDish) {
            c.determineNextGen();
        }
    }

    // Returns an array of integers reflecting the current state of petriDish's cells (whether alive
    //   or not). This is to be used for Serialization
    public int[] getMeArrayOfLiveStates () {
        int[] arrayOfStates = new int[petriDish.size()];

        for (int i = 0; i < arrayOfStates.length; i++) {
            arrayOfStates[i] = (petriDish.get(i).isAlive()) ? 1 : 0 ;
        }

        return arrayOfStates;
    }

    // Takes data and inflates a matrix state that was pulled from the database
    public void inflateThis (int rows, int cols, List<Integer> n2bBorn, List <Integer> n2Survive, int[] matrix) {
        // According to Occams Razor the simplest way is the best. Instead of trying to preserve and resize petriDish we
        // will destroy and create anew. On the other hand if the size of the list is the same as the size of the old then we can
        // keep it. This will be handy when restarting from last random or from last load

        need2Survive = n2Survive;
        need2BeBorn = n2bBorn;

        // If same dimensions then just go with it
        if (rows == rowsTotal && cols == columnsTotal && matrix.length == petriDish.size()) {
//            this.toroidal = (toroidal == 1) ? true : false;   // ** No longer recording Toroidal in saved states

            for (int i = 0; i < matrix.length; i++) {
                petriDish.get(i).setAlive((matrix[i]==1) ? true : false);
            }
        }
        else { // re-do everything
            rowsTotal = rows;
            columnsTotal = cols;

            petriDish = new ArrayList<>();
            for (int z = 0; z < matrix.length; z++) {
                petriDish.add(new Cell());
                petriDish.get(z).setAlive((matrix[z]==1) ? true : false);
            }

            giveNeighbourIndexesToCells();
        }

        setGenerationCounter(0);
    }

    // Initialize coordinates on first load or change in grid size
    public void resetCellCoordinates (int maxX, int maxY) {
        oldCellHeight = cellHeight;
        oldCellWidth = cellWidth;

        cellWidth = (float) maxX / columnsTotal;
        cellHeight = (float) maxY / rowsTotal;
        cellRadius = (int) (cellWidth * 0.4);

        for (int c = 0; c < columnsTotal; c++) {
            for (int r = 0; r < rowsTotal; r++) {
                petriDish.get(c + r* columnsTotal).setXandY ((c+0.5f) * cellWidth, (r+0.5f) * cellHeight);
            }
        }
    }

    // The previous version of the method the advance generations before nextGen was introduced.
    public void advanceGenerationForAll () {
        generationCounter++;
        for (Cell c : petriDish) {
            c.advanceGeneration();
        }
    }

    // Advances generation using a the nextGen value in cells - i.e. using the the nextGen info the cells have
    // already calculated
    public void advanceGenerationUsingNextGen () {
        generationCounter++;
        for (Cell c : petriDish) {
            c.setAliveToNextGen ();
        }
    }

    public void drawSelfForAll (Canvas canvas, Paint g, Paint b, boolean showNextGen, boolean markForDeath) {
        for (Cell c : petriDish) {
            c.drawSelf(canvas, g, b, showNextGen, markForDeath);
        }
    }

    public void initializeCells () {
        petriDish.clear();
        for (int z = 0; z < columnsTotal * rowsTotal; z++) {
            petriDish.add(new Cell());
        }
    }

    public void allCellsCountNeighbours () {
        for (Cell c : petriDish) {
            c.countLiveNeighbours();
        }

    }

    // Rotates the matrix clockwise or counterclockwise depending on the boolean.
    // To be called after a screen rotation
    public void rotatePetriDish (boolean clockwise) {
        List<Cell> newPetri = new ArrayList<> ();

        if (clockwise) {
            for (int c = 0; c < columnsTotal; c++) {
                for (int r = rowsTotal - 1; r >= 0; r--) {
                    newPetri.add(petriDish.get((r* columnsTotal) + c));
                }
            }
        }
        else {
            for (int c = columnsTotal - 1; c >= 0; c--) {
                for (int r = 0; r < rowsTotal; r++) {
                    newPetri.add(petriDish.get((r* columnsTotal) + c));
                }
            }
        }
        int temp = rowsTotal;
        rowsTotal = columnsTotal;
        columnsTotal = temp;

        petriDish = newPetri;
    }


    // Resizes the array. Needs to fill in or remove cells as needed.
    // Uses the power of Math to determine correct indexes for adding / removing cells.
    public void resizePetriDish (int newNumOfRows, int newNumOfCols) {

        // When the new board is smaller than the total size of board.
        if (newNumOfRows <= rowsTotal) {
            // I'm not even going to try to explain what these do. Just trust it's Math and it works.
            int rowsBefDel, colsBefDel, colsAfterDel;
            rowsBefDel = (rowsTotal - newNumOfRows) / 2;
            colsBefDel = (columnsTotal - newNumOfCols) / 2;
            colsAfterDel = columnsTotal - newNumOfCols - colsBefDel;

            petriDish.subList(0, columnsTotal * rowsBefDel + colsBefDel).clear();
            for (int i = 1; i < newNumOfRows; i++) {
                petriDish.subList (newNumOfCols * i, newNumOfCols * i + colsBefDel + colsAfterDel).clear();
            }
            petriDish.subList(newNumOfCols * newNumOfRows, petriDish.size()).clear();
        }

        // Else we are increasing the size. Similar procedure but exactly the opposite and nothing like it.
        else {
            int rowsBef, rowsAfter, colsBef, colsAfter, index;
            rowsBef = (newNumOfRows - rowsTotal) / 2;
            rowsAfter = (newNumOfRows - rowsTotal) - rowsBef;
            colsBef = (newNumOfCols - columnsTotal) / 2;
            colsAfter = (newNumOfCols - columnsTotal) - colsBef;

            for (int i = 0; i < rowsBef * newNumOfCols + colsBef; i++) {
                petriDish.add(0, new Cell());
            }
            for (int r = 0; r < rowsTotal - 1; r++) {
                index = ((rowsBef+r) * newNumOfCols) + colsBef + columnsTotal;
                for (int i = 0; i < colsAfter + colsBef; i++) {
                    petriDish.add (index + 0, new Cell());
                }
            }
            for (int r = 0; r < rowsAfter * newNumOfCols + colsAfter; r++) {
                petriDish.add (new Cell());
            }
        }

        rowsTotal = newNumOfRows;
        columnsTotal = newNumOfCols;
        giveNeighbourIndexesToCells();
    }

    // Does what the name suggests -> Lets each cell know what the neighbour indexes of it's neighbours are.
    // Should only be needed on initialization of the grid, and if the grid size changes for any reason
    // Toroidal is true when the universe 'wraps around'.
    //   i.e. a cell on the extreme right will be a neighbour to the corresponding one on the extreme left
    //        as well as extreme top neighbouring with bottom.
    public void giveNeighbourIndexesToCells () {
        if (petriDish.size() == 0) {
            Log.d ("Error!", "petriDish size was 0");
            return;
        }

        for (Cell c : petriDish) {
            c.resetNeighbours();
        }

        int index;
        for (int r = 0; r < rowsTotal; r++) {
            for (int c = 0; c < columnsTotal; c++) {
                index = (r* columnsTotal) + c;

                // Add neighbours to the right and left
                if (c < columnsTotal - 1) {
                    petriDish.get(index).addNeighbour(index + 1, 3);
                    petriDish.get(index+1).addNeighbour(index, 7);
                }
                else if (toroidal) {
                    petriDish.get(index).addNeighbour(r* columnsTotal, 3);
                    petriDish.get(r* columnsTotal).addNeighbour(index, 7);
                }
                // if neighbour is offscreen and toroidal is not in effect, set neighbour value to -1
                else {
                    petriDish.get(index).addNeighbour(-1, 3);
                    petriDish.get(r* columnsTotal).addNeighbour(-1, 7);
                }

                // Add neighbours on top and bottom
                if (r < rowsTotal - 1) {
                    petriDish.get(index).addNeighbour(index + columnsTotal, 5);
                    petriDish.get(index+ columnsTotal).addNeighbour(index, 1);
                }
                else if (toroidal) {
                    petriDish.get(index).addNeighbour(c, 5);
                    petriDish.get(c).addNeighbour(index, 1);
                }
                // if neighbour is offscreen and toroidal is not in effect, set neighbour value to -1
                else {
                    petriDish.get(index).addNeighbour(-1, 5);
                    petriDish.get(c).addNeighbour(-1, 1);
                }

                // Add neighbours to top right and bottom left
                if (r > 0 && c < columnsTotal - 1) { // not first row and not last column
                    petriDish.get(index).addNeighbour( (((r-1)* columnsTotal) + (c+1)) , 2);
                    petriDish.get(((r-1)* columnsTotal) + (c+1)).addNeighbour(index, 6);
                }
                else if (r > 0 && c == columnsTotal - 1) { // is last column excluding top right
                    if (toroidal) {
                        petriDish.get(index).addNeighbour((r - 1) * columnsTotal, 2);
                        petriDish.get((r - 1) * columnsTotal).addNeighbour(index, 6);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 2);
                        petriDish.get((r - 1) * columnsTotal).addNeighbour(-1, 6);
                    }
                }
                else if (r == 0 && c != columnsTotal - 1){ // is first row, excluding last column
                    if (toroidal) {
                        petriDish.get(index).addNeighbour((rowsTotal - 1) * columnsTotal + c + 1, 2);
                        petriDish.get((rowsTotal - 1) * columnsTotal + c + 1).addNeighbour(index, 6);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 2);
                        petriDish.get((rowsTotal - 1) * columnsTotal + c + 1).addNeighbour(-1, 6);
                    }
                }
                else { // is top right
                    if (toroidal) {
                        petriDish.get(index).addNeighbour((rowsTotal -1) * columnsTotal, 2);
                        petriDish.get((rowsTotal -1) * columnsTotal).addNeighbour(index, 6);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 2);
                        petriDish.get((rowsTotal -1) * columnsTotal).addNeighbour(-1, 6);
                    }
                }

                // Add neighbours to top left and bottom right
                if (r > 0 && c > 0) { // not first row or first column
                    petriDish.get(index).addNeighbour(((r-1)* columnsTotal) + (c-1), 0);
                    petriDish.get(((r-1)* columnsTotal) + (c-1)).addNeighbour(index, 4);
                }
                else if (r > 0 && c == 0) { // Is first column excluding top left cell.
                    if (toroidal) {
                        petriDish.get(index).addNeighbour(r* columnsTotal - 1, 0);
                        petriDish.get(r* columnsTotal - 1).addNeighbour(index, 4);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 0);
                        petriDish.get(r* columnsTotal - 1).addNeighbour(-1, 4);
                    }
                }
                else if (r == 0 && c > 0) { // Is first row excluding top left cell.
                    if (toroidal) {
                        petriDish.get(index).addNeighbour((rowsTotal -1)* columnsTotal + c - 1, 0);
                        petriDish.get((rowsTotal -1)* columnsTotal + c - 1).addNeighbour(index, 4);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 0);
                        petriDish.get((rowsTotal -1)* columnsTotal + c - 1).addNeighbour(-1, 4);
                    }
                }
                else { // top left cell
                    if (toroidal) {
                        petriDish.get(index).addNeighbour(rowsTotal * columnsTotal - 1, 0);
                        petriDish.get(rowsTotal * columnsTotal - 1).addNeighbour(index, 4);
                    }
                    else {
                        petriDish.get(index).addNeighbour(-1, 0);
                        petriDish.get(rowsTotal * columnsTotal - 1).addNeighbour(-1, 4);
                    }
                }
            }
        }
    }


    // Only getters and setters after this point //

    public int getGenerationCounter() {
        return generationCounter;
    }

    public void setGenerationCounter(int generationCounter) {
        this.generationCounter = generationCounter;
    }

    public Cell getCell (int r, int c) {
        return petriDish.get(columnsTotal * r + c);
    }

    public boolean isToroidal() {
        return toroidal;
    }

    public void setToroidal(boolean toroidal) {
        this.toroidal = toroidal;
        // When changing toroidal it's necessary to recalculate who is neighbours with who
        giveNeighbourIndexesToCells();
    }

    public int getRowsTotal() {
        return rowsTotal;
    }

    public void setRowsTotal(int rowsTotal) {
        this.rowsTotal = rowsTotal;
    }

    public int getColumnsTotal() {
        return columnsTotal;
    }

    public void setColumnsTotal(int columnsTotal) {
        this.columnsTotal = columnsTotal;
    }

    public static List<Integer> getNeed2Survive() {
        return need2Survive;
    }

    public static void setNeed2Survive(List<Integer> need2Survive) {
        GameData.need2Survive = need2Survive;
    }

    public static List<Integer> getNeed2BeBorn() {
        return need2BeBorn;
    }

    public static void setNeed2BeBorn(List<Integer> need2BeBorn) {
        GameData.need2BeBorn = need2BeBorn;
    }

    public void setDBH (DatabaseHelper dbh) { this.dbh = dbh; }

    public void setLastID (String id) { lastID = id; }

    public String getLastID () { return lastID; }
}
