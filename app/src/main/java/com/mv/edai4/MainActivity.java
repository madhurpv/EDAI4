package com.mv.edai4;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import top.defaults.colorpicker.ColorPickerView;

public class MainActivity extends AppCompatActivity {

    private static final int pic_id = 123;

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    EditText textinputInstruction;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    boolean isPressed;

    public static TextView measuredValuesTextView;
    public static String measuredValuesString;
    public static View colourDisplayView;
    ColorPickerView colourPicker;
    Button measureButton;
    FloatingActionButton floatingActionButtonOpenCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        final Button buttonToggle = findViewById(R.id.buttonToggle);
        textinputInstruction = findViewById(R.id.text_input_instructions);
        buttonToggle.setEnabled(false);


        measuredValuesTextView = findViewById(R.id.measuredValuesTextView);
        colourDisplayView = findViewById(R.id.colourDisplayView);
        colourPicker = findViewById(R.id.colourPicker);
        measureButton = findViewById(R.id.measureButton);

        measureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("C");
            }
        });

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

            try {
                connectedThread.write("R" + Color.red(color));
                Thread.sleep(100);
                connectedThread.write("G" + Color.green(color));
                Thread.sleep(100);
                connectedThread.write("B" + Color.blue(color));
            } catch (Exception e) {
                e.printStackTrace();
            }


        });


        View viewColour = new View((getApplicationContext()));
        viewColour.setBackgroundColor(Color.parseColor("#FFE3AD"));
        textinputInstruction.setBackground(viewColour.getBackground());

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();

        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                buttonToggle.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()) {
                            case "led is turned on":
                                //imageView.setBackgroundColor(getResources().getColor(R.color.colorOn));
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":
                                //imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });


        /*buttonToggle.setOnTouchListener((view, event) -> {
            // stop the thread
            view.performClick();
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                doPressDown();


                // start the thread
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP)
            {
                doPressRelease();
                return true;
            };

            return true;
        });*/

        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState) {
                    case "submit":
                        //buttonToggle.setText("Turn Off");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = textinputInstruction.getText().toString();
                        //cmdText = "As";
                        break;
                    case "turn off":
                        //buttonToggle.setText("Turn On");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                connectedThread.write(cmdText);
                //connectedThread.run();
                Toast.makeText(MainActivity.this, "Submitted", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, cmdText, Toast.LENGTH_SHORT).show();
            }
        });


        Button buttonControl = findViewById(R.id.buttonControl);
        buttonControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(MainActivity.this, MainControllerActivity.class);
                intent.putExtra("deviceAddress", deviceAddress);
                startActivity(intent);*/

                showCustomDialog(MainActivity.this);


            }
        });


        floatingActionButtonOpenCamera = findViewById(R.id.floatingActionButtonOpenCamera);
        floatingActionButtonOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, pic_id);
            }
        });


    }


    public void showCustomDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View customView = LayoutInflater.from(context).inflate(R.layout.popup_controller, null);
        builder.setView(customView);

        /*measuredValuesTextView = customView.findViewById(R.id.measuredValuesTextView);
        colourDisplayView = customView.findViewById(R.id.colourDisplayView);
        colourPicker = customView.findViewById(R.id.colourPicker);
        Button measureButton = customView.findViewById(R.id.measureButton);
        measureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThread.write("C");
            }
        });

        //measuredValuesTextView.setText(measuredValuesString);

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
        });*/

        // Set any additional properties for the dialog (e.g., title)
        //builder.setTitle("Custom Dialog");

        // Find views within the custom layout
        /*TextView textViewMessage = customView.findViewById(R.id.textViewMessage);
        Button buttonClose = customView.findViewById(R.id.buttonClose);*/

        // Set text for the message
        /*textViewMessage.setText("Hello from custom dialog!");

        // Set OnClickListener for the Close button
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close the dialog
                dialog.dismiss();
            }
        });*/

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();


        /*if (createConnectThread.isAlive() == false) {
            createConnectThread.start();
        }*/


        final Handler handler = new Handler();
        final int delay = 1000; // 1000 milliseconds == 1 second

        handler.postDelayed(new Runnable() {
            public void run() {
                System.out.println("myHandler: here!"); // Do your work here

                handler.postDelayed(this, delay);
            }
        }, delay);


    }






    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Match the request 'pic id with requestCode
        if (requestCode == pic_id) {
            // BitMap is data structure of image file which store the image in memory
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            // Set the image in imageview for display
            //click_image_id.setImageBitmap(photo);
            int avgColour = getAverageColour(photo);
            Toast.makeText(this, Color.red(avgColour) + "," + Color.green(avgColour) + "," + Color.blue(avgColour), Toast.LENGTH_SHORT).show();
            colourDisplayView.setBackgroundColor(avgColour);
            colourPicker.setInitialColor(avgColour);
        }
    }


    public int getAverageColour(Bitmap bitmap) {
        long redBucket = 0;
        long greenBucket = 0;
        long blueBucket = 0;
        long pixelCount = 0;

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                int c = bitmap.getPixel(x, y);

                pixelCount++;
                redBucket += Color.red(c);
                greenBucket += Color.green(c);
                blueBucket += Color.blue(c);
            }
        }

        // Calculate average colors
        int averageRed = (int)(redBucket / pixelCount);
        int averageGreen = (int)(greenBucket / pixelCount);
        int averageBlue = (int)(blueBucket / pixelCount);

        // Return the average color as an integer
        return Color.rgb(averageRed, averageGreen, averageBlue);
    }


















    private void doPressRelease() {
        isPressed = false;
    }

    private void doPressDown() {
        isPressed = true;
        new Thread(() -> {
            while (isPressed == true) {
                // here we increment till user release the button
                //connectedThread.write("As");
                connectedThread.write(textinputInstruction.getText().toString());
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("HHHH", "Pressed");
            }
        }).start();
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
            }
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.

                }
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run(MainActivity.measuredValuesTextView, MainActivity.colourDisplayView);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(TextView measuredValuesTextView, View colourDisplayView) {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message", readMessage);
                        String[] colourValues = readMessage.split("\\|");
                        int rgb = 0xFF<<8 + Integer.parseInt(colourValues[0]);
                        rgb = (rgb << 8) + Integer.parseInt(colourValues[1]);
                        rgb = (rgb << 8) + Integer.parseInt(colourValues[2]);
                        //SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref",MODE_PRIVATE);
                        measuredValuesString = "Measured Value : " + readMessage;
                        try{
                            int finalRgb = rgb;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    //do stuff like remove view etc
                                    measuredValuesTextView.setText(measuredValuesString);
                                    colourDisplayView.setBackgroundColor(finalRgb);
                                    colourDisplayView.setBackgroundColor(Color.rgb(Integer.parseInt(colourValues[0]), Integer.parseInt(colourValues[1]), Integer.parseInt(colourValues[2])));
                                    Log.e("QWER", "Values written");
                                }
                            });

                        } catch (Exception e) {
                            Log.e("QWER", "Error in writing!!");
                            e.printStackTrace();
                        }


                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                        //break;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        super.onBackPressed();
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
