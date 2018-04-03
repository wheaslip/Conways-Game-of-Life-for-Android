package com.heslihop.wesley.gameoflife;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.apache.commons.lang3.SerializationUtils;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "Database_of_Life";
    static final int DATABASE_VERSION = 4;
    static final String TABLE_NAME = "Saved_Game_States";
    static final String _ID = "_ID";
    static final String NAME_OF_STATE = "Name_of_State";
    static final String ROW_COUNT = "Number_of_rows";
    static final String COL_COUNT = "Number_of_columns";
//    static final String TOROIDAL = "Torus"; // Removed Toroidal for now. Only changes via settings.
    static final String NEED_2_SURVIVE = "Survivability_rules";
    static final String NEED_2_BE_BORN = "Birth_rules";
    static final String MATRIX_OF_ALIVE_STATES = "Matrix_of_booleans";
    static final String DATE = "Date_created";

    private String last_id;
    private GameData gd = GameData.getInstance();

    static final String[] COLUMNS = {_ID, NAME_OF_STATE, ROW_COUNT, COL_COUNT, NEED_2_SURVIVE, NEED_2_BE_BORN, MATRIX_OF_ALIVE_STATES, DATE};

    public DatabaseHelper (Context context) {
        super (context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    // Create a table in DB whenever this method is called
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + NAME_OF_STATE + " TEXT, " +
                ROW_COUNT + " INTEGER, " + COL_COUNT + " INTEGER, " + NEED_2_SURVIVE + " BLOB, " +
                NEED_2_BE_BORN + " BLOB, " +  DATE + " STRING, " + MATRIX_OF_ALIVE_STATES + " BLOB);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void deleteDatabase () {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d ("Insert dropping db", "");
        db.execSQL("DROP DATABASE IF EXISTS " + DATABASE_NAME);
        db.close();
    }

    // Saves current state. Checks if state already exists and database for specific name and if so replaces
    // Else adds in as a new line
    public boolean saveCurrent (String name, boolean last_random) {
        // Create needed blobs
        byte[] need2BeBorn = SerializationUtils.serialize((Serializable) gd.need2BeBorn);
        byte[] need2Survive = SerializationUtils.serialize((Serializable) gd.need2Survive);
        byte[] matrixOfAliveStates = SerializationUtils.serialize(gd.getMeArrayOfLiveStates());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if (last_random) contentValues.put (_ID, 1); // For returning to last random or load
        contentValues.put (NAME_OF_STATE, name);
        contentValues.put (ROW_COUNT, gd.rowsTotal);
        contentValues.put (COL_COUNT, gd.columnsTotal);
//        contentValues.put (TOROIDAL, (gd.toroidal) ? 1 : 0); // I'm surprised I found a use for this structure
        contentValues.put (NEED_2_SURVIVE, need2Survive);
        contentValues.put (NEED_2_BE_BORN, need2BeBorn);
        contentValues.put (MATRIX_OF_ALIVE_STATES, matrixOfAliveStates);

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        Date date = new Date();
        contentValues.put (DATE, df.format(date));

        //  Deletes row 1 if exists and saving new last_random
        if (last_random) {
            int returnValue = db.delete(TABLE_NAME, "_ID = 1", null);
            Log.d ("Row deleted", "" + returnValue);
        }
        
        last_id = String.valueOf(db.insert(TABLE_NAME, null, contentValues));
        gd.setLastID(last_id);
        Log.d ("Inserted at row", last_id);

        db.close();
        return true;
    }

    // Only used for troubleshooting
    public void printSaves () {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] stringArray = {NAME_OF_STATE, _ID};
        Cursor cursor = db.query(TABLE_NAME, stringArray, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String currentName = cursor.getString(cursor.getColumnIndex(NAME_OF_STATE));
            int id = cursor.getInt(cursor.getColumnIndex(_ID));
            Log.d ("Saved state name", currentName + " " + id);
        }
        db.close();
    }

    public List<SavedState> updateStateList () {
        List<SavedState> savedState = new ArrayList<>();
        SavedState newSavedState;

        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {NAME_OF_STATE, _ID, DATE};
        Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String currentName = cursor.getString(cursor.getColumnIndex(NAME_OF_STATE));
            int id = cursor.getInt(cursor.getColumnIndex(_ID));
            String date = cursor.getString(cursor.getColumnIndex(DATE));
            newSavedState = new SavedState(currentName, date, String.valueOf(id));
            savedState.add(newSavedState);
        }
        db.close();
        return savedState;
    }

    public void getData (String id) {
        if (id.equals("0")) id = gd.getLastID();
        else gd.setLastID(id);

        SQLiteDatabase db = this.getReadableDatabase();

        // Build query
        Cursor cursor = db.query(TABLE_NAME, COLUMNS, "_ID = " + id, null, null, null, null);

        // If we have results get the first one
        if (cursor != null && cursor.moveToFirst()) {

            // get info from database
            int rows = cursor.getInt(cursor.getColumnIndex(ROW_COUNT));
            int cols = cursor.getInt(cursor.getColumnIndex(COL_COUNT));
//            int toroidal = cursor.getInt(cursor.getColumnIndex(TOROIDAL));
            byte[] need2Survive = cursor.getBlob(cursor.getColumnIndex(NEED_2_SURVIVE));
            byte[] need2BeBorn = cursor.getBlob(cursor.getColumnIndex(NEED_2_BE_BORN));
            byte[] matrixOfAliveStates = cursor.getBlob(cursor.getColumnIndex(MATRIX_OF_ALIVE_STATES));

            List<Integer> n2bBorn = SerializationUtils.deserialize(need2BeBorn);
            List<Integer> n2Survive = SerializationUtils.deserialize(need2Survive);
            int[] matrix = SerializationUtils.deserialize(matrixOfAliveStates);

            // Call GameData method to inflate properly
            gd.inflateThis(rows, cols, n2bBorn, n2Survive, matrix);

            Log.d("Inflated", cursor.getString(cursor.getColumnIndex(NAME_OF_STATE)));
        }
        else {
            Log.d ("Id", id + " not found.");
            printSaves();
        }
        db.close();
    }

    public void removeEntry (String id) {
        Log.d ("Activity", "removeEntry, id: " + id);

        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE _ID = " + id);
        db.close();
    }
}