package com.heslihop.wesley.gameoflife;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
    }

    public void aboutGameOfLife (View v) {
        Intent i = new Intent(this, AboutGameOfLife.class);
        startActivity(i);
    }

    public void aboutThisApp (View v) {
        Intent i = new Intent(this, AboutApp.class);
        startActivity(i);
    }

    public void loadButton(View v) {
        Intent i = new Intent(this, SavedStateList.class);
        startActivity(i);
        finish();
    }

    public void settings(View v) {
        Intent i = new Intent(this, SettingsPreferencesActivity.class);
        startActivity(i);
    }

    public void doYouEvenCode (View v) {
        Intent i = new Intent(this, DoYouEvenCode.class);
        startActivity(i);
    }

    public void attribution (View V) {
        Intent i = new Intent(this, Attribution.class);
        startActivity(i);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
