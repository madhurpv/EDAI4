package com.mv.edai4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import top.defaults.colorpicker.ColorPickerView;

public class MainControllerActivity extends AppCompatActivity {




    View colourDisplayView;
    ColorPickerView colourPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);


        colourDisplayView = findViewById(R.id.colourDisplayView);
        colourPicker = findViewById(R.id.colourPicker);

        colourPicker.subscribe((color, fromUser, shouldPropagate) -> {
            colourDisplayView.setBackgroundColor(color);
            /*colorHex.setText(colorHex(color));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(color);
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setBackgroundDrawable(new ColorDrawable(color));
            }*/
        });


    }
}