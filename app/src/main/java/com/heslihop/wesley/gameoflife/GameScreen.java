package com.heslihop.wesley.gameoflife;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The activity that holds the SurfaceView that actually does the drawing.
// Logic and important data are held in GameData

public class GameScreen extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{
    AnimationSurfaceView animationSV;
    ConstraintLayout CL;
    GameData gd;
    DatabaseHelper dbh; // used for storing and retrieving data from the database.

    private boolean paused = false, GUIHidden = false;
    private ImageButton advanceOneButton, randomButton, clearButton;
    private ImageButton settingsButton, saveButton, pauseButton, redoButton;
    private ImageView backgroundIV;
    private SeekBar speedSeekBar, sizeSeekBar;
    private TextView speedTextView, sizeTextView, generationCounter;
    private int currentGenPerSec = 10; // Current Generations-per-seconds value.
    private View[] HIDEABLE_VIEWS = new View [12];
    private static final String[] BACKGROUND_IMAGE_LIST = {"bacteria_811861_960_720", "microbial_handprint",
                "drew_hays_206414_unsplash", "tiphaine_27140_unsplash", "salmonella_549608_960_720",
                "koli_bacteria_123081_960_720"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        getSupportActionBar().hide();

        dbh = new DatabaseHelper(this);

        animationSV = new AnimationSurfaceView(this, _handler);
        CL = (ConstraintLayout) findViewById(R.id.game_screen_parent);
        CL.addView(animationSV);
        backgroundIV = (ImageView) findViewById(R.id.backgroundIV);
        randomly_change_background_image();

        gd = GameData.getInstance();
        gd.setDBH(dbh);
        speedTextView = (TextView) findViewById(R.id.speed);
        sizeTextView = (TextView) findViewById(R.id.size);
        generationCounter = (TextView) findViewById(R.id.GenerationCount);
        randomButton = (ImageButton) findViewById(R.id.randomButton);
        clearButton = (ImageButton) findViewById(R.id.clearButton);
        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        saveButton = (ImageButton) findViewById(R.id.saveButton);
        pauseButton = (ImageButton) findViewById(R.id.Pause);
        redoButton = (ImageButton) findViewById(R.id.redoButton);
        advanceOneButton = (ImageButton) findViewById(R.id.AdvanceOne);
        speedSeekBar = (SeekBar) findViewById(R.id.speedSeekBar);
        speedSeekBar.setOnSeekBarChangeListener(this);
        sizeSeekBar = (SeekBar) findViewById(R.id.sizeSeekBar);
        sizeSeekBar.setOnSeekBarChangeListener(this);

        // This is so primitive. "How to program like a neanderthal 101"
        HIDEABLE_VIEWS[0] = randomButton;
        HIDEABLE_VIEWS[1] = advanceOneButton;
        HIDEABLE_VIEWS[2] = clearButton;
        HIDEABLE_VIEWS[3] = settingsButton;
        HIDEABLE_VIEWS[4] = pauseButton;
        HIDEABLE_VIEWS[5] = redoButton;
        HIDEABLE_VIEWS[6] = speedSeekBar;
        HIDEABLE_VIEWS[7] = sizeSeekBar;
        HIDEABLE_VIEWS[8] = speedTextView;
        HIDEABLE_VIEWS[9] = sizeTextView;
        HIDEABLE_VIEWS[10] = generationCounter;
        HIDEABLE_VIEWS[11] = saveButton;

        // Restore instance state after screen rotation
        if (savedInstanceState != null) {
            GUIHidden = savedInstanceState.getBoolean("GUI_Hidden");
            if (GUIHidden) hideAllWidgets();
            paused = savedInstanceState.getBoolean("Paused");
            if (paused) {
                advanceOneButton.setClickable(true);
                pauseButton.setBackgroundResource(R.drawable.play);
            }
            currentGenPerSec = savedInstanceState.getInt("Current GPS");
            speedSeekBar.setProgress(savedInstanceState.getInt("Speed progress"));
            sizeSeekBar.setProgress(savedInstanceState.getInt("Size seekBar progress"));
        }
    }

    // Saving instance state for GUI on screen rotation.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("GUI_Hidden", GUIHidden);
        outState.putBoolean("Paused", paused);
        outState.putInt("Current GPS", currentGenPerSec);
        outState.putInt("Speed progress", speedSeekBar.getProgress());
        outState.putInt("Size seekbar progress", sizeSeekBar.getProgress());
    }

    @Override
    protected void onPause() {
        Log.d("Activity", "onPause() called");
        super.onPause();
        animationSV.pause();
    }

    @Override
    protected void onResume() {
        Log.d("Activity", "onResume() called");
        super.onResume();
        checkPreferences();

        if (!paused) animationSV.resume();
        else animationSV.redrawButDontAdvanceGen();
        animationSV.setGPS (currentGenPerSec);
        speedSeekBar.setProgress(currentGenPerSec);
        refreshSizeBar ();
    }

    // Gets stored user preferences and refreshes them.
    private void checkPreferences () {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean isToroidal = SP.getBoolean("pref_toroidal", true);
        Log.d("Toroidal is", isToroidal + "");
        gd.setToroidal(isToroidal);

        boolean isNextGenVisible = SP.getBoolean("pref_next_gen", false);
        animationSV.setShowNextGen(isNextGenVisible);

        boolean markForDeath = SP.getBoolean("pref_mark_for_death", false);
        animationSV.setMarkCellsForDeath(markForDeath);

        Set<String> prefCreationValues = new HashSet<String>();
        prefCreationValues.add("3");
        prefCreationValues = SP.getStringSet("prefs_creation_rules", prefCreationValues);
        List<Integer> need2BeBorn = new ArrayList<Integer>();

        for (String s : prefCreationValues) {
            need2BeBorn.add(Integer.parseInt(s));
        }
        gd.setNeed2BeBorn(need2BeBorn);

        Set<String> prefSurvivalValues = new HashSet<String>();
        prefSurvivalValues.add("2"); prefSurvivalValues.add("3");
        prefSurvivalValues = SP.getStringSet("prefs_survival_rules", prefSurvivalValues);
        List<Integer> need2Survive = new ArrayList<Integer>();

        for (String s : prefSurvivalValues) {
            need2Survive.add(Integer.parseInt(s));
        }
        gd.setNeed2Survive(need2Survive);
    }

    // This is setup to receive messages from AnimationSurfaceView
    // TO-DO This method will be removed when screen change is handled
    // via a separate thread launched by this class
    public Handler _handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg2 < 0) {
                changeBackground();
            }
            setGenerationText(gd.getGenerationCounter());
            super.handleMessage(msg);
        }
    };

    private void changeBackground() {
        fadeInAndOut(backgroundIV, 2000);
    }

    public void pauseButton(View v) {
        if (!paused) {
            animationSV.pause();
            paused = true;
            pauseButton.setBackgroundResource(R.drawable.play);
            advanceOneButton.setClickable(true);
        } else {
            animationSV.resume();
            paused = false;
            pauseButton.setBackgroundResource(R.drawable.pause);
            advanceOneButton.setClickable(false);
        }

    }

    public void onSaveButton (View v) {
        if (!paused) {
            pauseButton(v);
        }

        final EditText saveText = new EditText(this);

        // TO-DO -> Pick hint from a pool of hints. If user presses save without entering text then
        //          use hint as save name.
        saveText.setHint("Exploding Star of Wonder");

        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.AlertDialogTheme);

        new AlertDialog.Builder(ctw).setTitle("Save")
                .setTitle(getResources().getString(R.string.save_title))
                .setMessage(getResources().getString(R.string.save_title_hint))
                .setView(saveText)
                .setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String saveName = saveText.getText().toString();
                dbh.saveCurrent(saveName, false);
            }

        }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        }).show();
    }

    public void settingsButton (View v) {
        boolean wasPaused = true;
        if (!paused) {
            pauseButton(v);
            wasPaused = false;
        }

        Intent i = new Intent (this, MainMenu.class);
        startActivity (i);

        refreshSizeBar ();

        if (!wasPaused) {
            pauseButton(v);
        }
    }
    public void advanceOneButton(View v) {
        animationSV.advanceOneGen();
    }

    public void redoButton (View v) {
        boolean wasPaused = false;
        if (!paused) {
            pauseButton(v);
        } else {
            wasPaused = true;
        }

        dbh.getData("0");
        // Causes neighbourships and nextGen to be recalculated before redrawing (maybe) ((definitely))
        animationSV.cellsHaveChanged = true;
        refreshSizeBar ();

        if (!wasPaused) {
            pauseButton(v);
        } else {
            animationSV.redrawButDontAdvanceGen();
        }
    }

    public void clearButton(View v) {
        gd.shutDownTheMatrixAndKillAllCells();
        if (paused) {
            // The extra calls to advanceOneGen here is a workaround for a bug. When the screen was cleared while paused,
            // and then play pressed before drawing any cells manually, the old configuration of cells would flash briefly
            // on the screen. The solution isn't pretty, but it does work.
            for (int i = 0; i < 3; i++) {
                animationSV.advanceOneGen();
            }
        }
    }

    public void randomizeButton(View v) {
        boolean wasPaused = false;
        if (!paused) {
            pauseButton(v);
        } else {
            wasPaused = true;
        }

        gd.randomize(0.3F);

        if (!wasPaused) {
            pauseButton(v);
        } else {
            animationSV.advanceOneGen();
        }
    }

    // Handles both the speed and size seekbar input
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Handles speedSeekBar changes
        if (seekBar == findViewById(R.id.speedSeekBar) && fromUser) {
            // Don't allow speedSeekBar to be set to 0
            // (If user wants GPS = 0, they should Pause)
            int speed = progress;
            if (speed < 1) {
                speed = 1;
                seekBar.setProgress(1);
            }
            animationSV.setGPS(speed);
            currentGenPerSec = speed;
        }
        // Handles sizeSeekBar changes
        else if (seekBar == findViewById(R.id.sizeSeekBar) && fromUser) {
            boolean wasPaused = true;
            if (!paused) {
                wasPaused = false;
                animationSV.pause();
            }
            // shortSide is the number of cells along the short side of the screen
            int shortSide;
            switch (progress) {
                case 0:
                    shortSide = 7;
                    break;
                case 1:
                    shortSide = 11;
                    break;
                case 2:
                    shortSide = 15; // initial value
                    break;
                case 3:
                    shortSide = 22;
                    break;
                default:
                    shortSide = 30;
            }
            // shortSide could be columns or rows depending on screen orientation.
            // Finds out which and calculates number of cells on long side based on current ratio.
            if (animationSV.maxX > animationSV.maxY) { // horizontal mode
                gd.resizePetriDish(shortSide, shortSide * animationSV.maxX / animationSV.maxY);
            } else { // portrait mode
                gd.resizePetriDish(shortSide * animationSV.maxY / animationSV.maxX, shortSide);
            }

            gd.resetCellCoordinates(animationSV.maxX, animationSV.maxY);

            // Refresh screen to reflect change if wasPaused. (If not it will refresh with next gen anyway)
            if (wasPaused) animationSV.redrawButDontAdvanceGen();
            else animationSV.resume();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void showHideGUI(View v) {
        if (!GUIHidden) {
            GUIHidden = true;
            hideAllWidgets();
        } else {
            GUIHidden = false;
            showAllWidgets();
        }
    }

    // All but the bacteria that toggles this behaviour
    public void hideAllWidgets() {
        for (View v : HIDEABLE_VIEWS) {
            fadeOutAndHideImage(v);
        }
    }

    public void showAllWidgets() {
        for (View v : HIDEABLE_VIEWS) {
            v.setVisibility(View.VISIBLE);
        }
    }

    // Clever method I found on Stack Overflow
    private void fadeOutAndHideImage(final View img) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(500);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                img.setVisibility(View.INVISIBLE);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        img.startAnimation(fadeOut);
    }

    // A slightly more general version of above method
    private void fadeInAndOut(final View img, int duration) {

        Animation fadeOut = new AlphaAnimation(1, 0);
        final Animation fadeIn = new AlphaAnimation(0, 1);

        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(duration);
        fadeIn.setDuration(duration);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                randomly_change_background_image ();
                img.startAnimation(fadeIn);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        img.startAnimation(fadeOut);
    }

    public void setGPSText(int gps) {
        currentGenPerSec = gps;
        speedTextView.setText(getResources().getString(R.string.speed) + "  " + currentGenPerSec + "  " + getResources().getString(R.string.genpersec));
    }

    public void setGenerationText (int count) {
        generationCounter.setText (getResources().getString(R.string.generations) + "  " + count);
    }

    // Useful when seekbar changes but not directly from user, such as hitting the redo button or
    // from loading a save that was taken with a different grid size.
    private void refreshSizeBar () {
        int rows = gd.getRowsTotal();
        int cols = gd.getColumnsTotal();
        int shortSide = (rows<cols) ? rows : cols;
        int setting;
        switch (shortSide) {
            case 7:
                setting = 0;
                break;
            case 11:
                setting = 1;
                break;
            case 15:
                setting = 2; // initial value
                break;
            case 22:
                setting = 3;
                break;
            default:
                setting = 4;
        }
        sizeSeekBar.setProgress(setting);
    }

    private void randomly_change_background_image () {
        int rand = (int) (Math.random() * BACKGROUND_IMAGE_LIST.length);
        backgroundIV.setImageDrawable(getResources().getDrawable(getResourceID (BACKGROUND_IMAGE_LIST[rand],
                "drawable", getApplicationContext())));
    }

    // Credit given where it's due -> This method was taken from an answer on stackoverflow by a user
    // named "Kling Klang", Dec 2013.  https://stackoverflow.com/questions/20549705/how-to-display-random-images-on-image-view
    protected final static int getResourceID (final String resName, final String resType, final Context ctx) {
        final int ResourceID = ctx.getResources().getIdentifier(resName, resType, ctx.getApplicationInfo().packageName);

        if (ResourceID == 0) {
            throw new IllegalArgumentException ("No resource string found with name " + resName);
        }
        else {
            return ResourceID;
        }
    }

    public void deleteDatabase (View v) {
        dbh.deleteDatabase();
    }
}
