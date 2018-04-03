package com.heslihop.wesley.gameoflife;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.io.Serializable;

public class Cell implements Serializable{
    // All things transient that don't need to be stored in database
    // Better to save space in Database and recalculate what's needed upon inflating
    boolean alive;
    transient boolean nextGen;
    transient int neighbourCount;
    transient int[] indexOfNeighbourCells;
    transient float x, y; // x and y coordinates
    transient GameData gd;
    transient int colour;
    transient long timestampForAliveDampenerOld=0, timestampForAliveDampenerNew;

    public Cell () {
        indexOfNeighbourCells = new int[8];
        alive = false;
        gd = GameData.getInstance();

        // Used to randomize cell colour for now.
        colour = Color.rgb ((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
    }

    public void advanceGeneration () {
        if (alive && gd.need2Survive.contains(neighbourCount)) {
            alive = true;
            // Do nothing. Is alive and stays alive.
        }
        else if (alive) {
            alive = false; // dies of loneliness or overcrowding
        }
        // If not alive and conditions are right to be born
        else if (gd.need2BeBorn.contains(neighbourCount)) {
            alive = true;
        }
        // Else continue empty
        else {
            alive = false;
        }
    }

    public void determineNextGen () {
        if (alive && gd.need2Survive.contains(neighbourCount)) {
            nextGen = true;
        }
        else if (alive) {
            nextGen = false; // dies of loneliness or overcrowding
        }
        // If not alive and conditions are right to be born
        else if (gd.need2BeBorn.contains(neighbourCount)) {
            nextGen = true;
        }
        // Else continue empty
        else {
            nextGen = false;
        }
    }

    public void setAliveToNextGen () {
        alive = nextGen;
    }

    public void drawSelf (Canvas c, Paint p, Paint black, boolean showNextGen, boolean markForDeath) {
        int radius = gd.cellRadius;

        if (alive) {
            p.setAlpha (250);
            p.setColor (colour);
            c.drawCircle(x, y, radius, p);
        }

        if (showNextGen) {
            // If no cell is there but one will be next generation
            if (!alive && nextGen) {
                // draw a semi-transparent, smaller than normal circle
                p.setColor (colour);
                p.setAlpha(120);
                c.drawCircle(x, y, radius * 0.65f, p);
            }
        }

        if (markForDeath) {
            // Cell is alive now but won't be in next generation
            // TO-DO -> Come up with a better (prettier) way to mark cells for death.
            if (alive && !nextGen) {
                // Draw a black X over it
                int r = (int) (radius * .85);
                c.drawLine(x-r, y-r, x+r, y+r, black);
                c.drawLine(x+r, y-r, x-r, y+r, black);
            }
        }
    }

    public void countLiveNeighbours() {
        neighbourCount = 0;
        for (int i : indexOfNeighbourCells) {
            if (i != -1 && gd.petriDish.get(i).isAlive()) {
                neighbourCount++;
            }
        }
    }

    // If you put the cell in the middle and count all the neighbours starting from the top left
    // and going clockwise, their associated number is the 'pos' variable
    public void addNeighbour (int neigh, int pos) {
        indexOfNeighbourCells[pos] = neigh;
    }

    public void resetNeighbours () {
        indexOfNeighbourCells = new int[8];
    }

    // Reverses aliveness, but only if it hasn't already been reversed in the last 1000 milliseconds.
    // This avoids problems with one touch event being reported many times.
    public void reverseAlivenessWithDampener () {
        // Supposedly currentTimeMillis uses less CPU cycles than nanoTime.
        timestampForAliveDampenerNew = System.currentTimeMillis();
        if (timestampForAliveDampenerNew - timestampForAliveDampenerOld > 1000) alive = !alive;

        timestampForAliveDampenerOld = timestampForAliveDampenerNew;
    }

    //// **** Only getters and setters after this point **** ////

    public void reverseAliveness () { alive = !alive; }

    public boolean isAlive() {
        return alive;
    }

    public void setGD (GameData gd) {
        this.gd = gd;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getNeighbourCount() {
        return neighbourCount;
    }

    public void setNeighbourCount(int neighbourCount) {
        this.neighbourCount = neighbourCount;
    }

    public void setNextGen (boolean nextGen) { this.nextGen = nextGen; }

    public float getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setXandY (float x, float y) {
        this.x = x;
        this.y = y;
    }
}
