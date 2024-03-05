package com.mv.edai4;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import top.defaults.colorpicker.ColorPickerView;

public class MainControllerActivity extends AppCompatActivity {




    View colourDisplayView;
    ColorPickerView colourPicker;
    TextView measuredValuesTextView;

    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        //String deviceAddress = getIntent().getExtras().getString("getIntent().getExtras()");


        colourDisplayView = findViewById(R.id.colourDisplayView);
        colourPicker = findViewById(R.id.colourPicker);
        measuredValuesTextView = findViewById(R.id.measuredValuesTextView);

        colourPicker.subscribe((color, fromUser, shouldPropagate) -> {
            colourDisplayView.setBackgroundColor(color);
            measuredValuesTextView.setText("Measured Values : (" + Color.red(color) + ", " + Color.green(color) + ", "  + Color.blue(color) + ")");

            float[] hsv = new float[3];
            Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
            //Log.d("QWER", "HSV : " + Arrays.toString(hsv));
            hsv[1] = hsv[1]/8;
            getWindow().setStatusBarColor(Color.HSVToColor(hsv));
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setBackgroundDrawable(new ColorDrawable(Color.HSVToColor(hsv)));
            }
        });



        /*BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CreateConnectThread createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress, handler, MainControllerActivity.this);
        createConnectThread.start();*/


    }
}