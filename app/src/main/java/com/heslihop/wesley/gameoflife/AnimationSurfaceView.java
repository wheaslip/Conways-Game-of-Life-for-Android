package com.heslihop.wesley.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AnimationSurfaceView extends SurfaceView implements Runnable {
    SurfaceHolder surfaceHolder;
    Canvas canvas;
    Thread thread;
    GameData gd;

    int maxX, maxY; // Maximum game drawing area in pixels
    boolean canDraw, paused=false, changeBackground, showNextGen=true, markCellsForDeath=false, cellsHaveChanged=false;

    int currentGPS = 10; // Current Generations/second. (i.e. the speed)

    Message msg = Message.obtain();
    Handler gameScreenhandler;

    boolean advanceOne = false, justRedraw = false;

    long startTimer, timeLoopTook, sleepTime, backgroundTimer;

    // To keep track of touch events
    float touchX=0, touchY=0;

    Paint green_paintbrush_fill, black_paintbrush_stroke;

    public AnimationSurfaceView (Context c, Handler handler) {
        super (c);
        gd = GameData.getInstance();

        surfaceHolder = getHolder();

        // These next two work together to make any drawing go ontop of the background, but
        // areas not drawn on allow the background to show.
        this.setZOrderOnTop(true);
        surfaceHolder.setFormat (PixelFormat.TRANSPARENT);

        prepPaintBrushes();

        // Used for sending messages back to the GameScreen Activity
        gameScreenhandler = handler;
    }


    // Only receives touch events, doesn't record swipes
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        touchX = event.getX();
        touchY = event.getY();

        // Add (or remove) a cell
        getCell((int) touchX, (int) touchY).reverseAlivenessWithDampener();

        cellsHaveChanged = true; // warn animation loop that something has changed

        if (paused) {
            // Causes cells to be redrawn but no neighbourships calculated
            redrawButDontAdvanceGen();
        }

        return true;
    }

    // Takes coordinates and returns Cell at those coordinates
    private Cell getCell (float x, float y) {
        int row, col;
        row = (int) (y * gd.getRowsTotal() / maxY);
        col = (int) (x * gd.getColumnsTotal() / maxX);
        return gd.getCell (row, col);
    }

    /* Handles updating the background and doing any transition animations that might be needed
    * to switch from one background to another. Incidently also clears each previous drawing
    * before each frame is repainted.
    *
    * I didn't find a way to make a simple fade out fade in for swapping backgrounds so I made my
    * own way. So there!
    *
    * TO-DO: Based on my recent studies, changing background could easily be handled by using a separate
    * thread launched from GameScreen. This would simplify things.
    */
    private void updateBackground () {
        backgroundTimer += 1000 / currentGPS;
        if (backgroundTimer > 15000) {
            changeBackground = true;
            backgroundTimer = 0;
        }
        else {
            changeBackground = false;
        }
    }

    @Override
    public void run () {
        // This loop is to get the screen parameters and then scale the drawing to the screen.
        // Includes other tasks that don't need to be repeated in the main loop
        // Specifically kept out of the main loop as it doesn't need to be recalculated every cycle.
        while (canDraw && !advanceOne) {
            if (surfaceHolder.getSurface().isValid()) {
                canvas = surfaceHolder.lockCanvas();

                maxX = canvas.getWidth();
                maxY = canvas.getHeight();

                // Having this get refreshed on every initial run cycle fixes a bug that was causing all
                // cells to be drawn at (0,0) when in horizontal mode and loading a saved config from a different
                // grid size.
                if (gd.petriDish.size() != 0 && maxX != 0 && maxY != 0) {
                    gd.resetCellCoordinates(maxX, maxY);
                }

                // This condition should only be true if the screen has just been rotated to horizontal,
                // or a pattern saved in vertical is loaded in horizontal
                // *** Note: Could improve this later by finding out which way it's been rotated
                //      and then rotating the grid in the same direction to make sure up is still on
                ///     the same side. ***
                if (maxX > maxY && gd.getRowsTotal() > gd.getColumnsTotal()) {
                    gd.rotatePetriDish(true);
                    gd.resetCellCoordinates(maxX, maxY);
                    gd.giveNeighbourIndexesToCells();
                }
                // else if just rotated from horizontal to potrait
                else if (maxX < maxY && gd.getRowsTotal() < gd.getColumnsTotal()) {
                    gd.rotatePetriDish(false); // assumes proportions of drawing area are the same
                    gd.resetCellCoordinates(maxX, maxY);
                    gd.giveNeighbourIndexesToCells();
                }

                // Make cells uniform size based on ratio of drawing area width to height
                // Only if it hasn't been done already (rows == columns)
                else if (gd.rowsTotal == gd.columnsTotal) {
                    if (maxX > maxY) { // horizontal mode
                        gd.setColumnsTotal(gd.getRowsTotal() * maxX / maxY);
                    }
                    else { // portrait mode
                        gd.setRowsTotal(gd.getColumnsTotal() * maxY / maxX);
                    }
                }

                surfaceHolder.unlockCanvasAndPost(canvas);

                // If it hasn't been initialized yet
                // I was going to do this in the constructor but it wasn't giving me the canvas.
                if (gd.petriDish.size() == 0) {
                    gd.initializeCells ();
                    gd.giveNeighbourIndexesToCells();
                    gd.resetCellCoordinates(maxX, maxY);
                    gd.randomize(0.5f);
                }

                break;
            }
        }

        // Main animation loop
        while (canDraw) {
            startTimer = System.currentTimeMillis();

            if (surfaceHolder.getSurface().isValid()) {
                canvas = surfaceHolder.lockCanvas(); // gets canvas for drawing on
                maxX = canvas.getWidth();
                maxY = canvas.getHeight();

                // advance generation
                if (cellsHaveChanged) { // for refreshing when drawing on a paused screen
                    gd.allCellsCountNeighbours();
                    gd.calcNextGen();
                    cellsHaveChanged = false;
                }

                if (!justRedraw) {
                    gd.advanceGenerationUsingNextGen();
                }

                gd.allCellsCountNeighbours();
                gd.calcNextGen();

                // Resets canvas to transparent pixels
                canvas.drawColor (Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                updateBackground ();

                // Have each cell draw itself
                // TO-DO: Refactor - Store Paint objects in GameData and let cells pull from there. No
                //    need to have them here. (Or allow each cell to have it's own paint objects.)
                gd.drawSelfForAll(canvas, green_paintbrush_fill, black_paintbrush_stroke, showNextGen, markCellsForDeath);

                surfaceHolder.unlockCanvasAndPost(canvas); // draws the canvas to the screen

                msg = Message.obtain();

                if (changeBackground) msg.arg2 = -1;
                else msg.arg2 = 0; //maxGPS; // auto-adjusts speed Seekbar

                gameScreenhandler.sendMessage(msg);

                // Stop here if intention is to advance just one gen, or just redrawing
                if (advanceOne || justRedraw) {
                    canDraw = false;
                    advanceOne = false;
                    justRedraw = false;
                    pause();
                }
            }

            // Sleep time calculation
            timeLoopTook = System.currentTimeMillis() - startTimer;
            sleepTime = (1000 / currentGPS) - timeLoopTook;
            //Log.d ("Sleep time percentage", "" + ((float) sleepTime / (float) (1000/currentGPS) * 100) + "%");

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // If GameScreen Activity closes or pause button hit
    public void pause () {
        // This paused boolean prevents a bug caused by rotating the screen twice while animation is paused.
        if (!paused) {
            canDraw = false;
            paused = true;

            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            thread = null;
        }
    }

    // If GameScreen Activity has been restarted or resume button hit
    public void resume () {
        canDraw = true;
        paused = false;
        thread = new Thread (this);
        thread.start();
    }

    public void advanceOneGen () {
        advanceOne = true;
        resume ();
    }

    // Also called when game is paused but needs to be redrawn (i.e. size of board is changed)
    public void redrawButDontAdvanceGen () {
        justRedraw = true;
        resume ();
    }

    public void setShowNextGen(boolean showNextGen) {
        this.showNextGen = showNextGen;
    }

    public void setMarkCellsForDeath(boolean markCellsForDeath) {
        this.markCellsForDeath = markCellsForDeath;
    }


    public void setCellsHaveChanged (boolean cellsHaveChanged) {
        this.cellsHaveChanged = cellsHaveChanged;
    }

    public void setGPS (int gps) {
        currentGPS = gps;
    }

    private void prepPaintBrushes() {
        green_paintbrush_fill = new Paint();
        green_paintbrush_fill.setColor(Color.GREEN);
        green_paintbrush_fill.setStyle(Paint.Style.FILL);

        black_paintbrush_stroke = new Paint();
        black_paintbrush_stroke.setColor(Color.BLACK);
        black_paintbrush_stroke.setStyle(Paint.Style.FILL);
        black_paintbrush_stroke.setStrokeWidth(7);
        black_paintbrush_stroke.setTextSize(30);

    }
}
