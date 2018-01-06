package com.danielleklaasen.sushiroll;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener { // setting sensor event listener here
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize the Accelerometer variables
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // used to access the system's sensors
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // reference to system's accelerometer
        // register accelerometer listener
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL); // deliver sensor events at normal rate (last arg)
    }

    // two required SensorEventListener methods (implemented in this class)

    @Override
    public void onSensorChanged(SensorEvent event) { // to detect a change in the sensor
        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) { // check for accelerometer sensor
            // retrieve values from x, y and z axis
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // only if it's more than 100 ms since onSensorChanged was invoked

            long curTime = System.currentTimeMillis(); // system's current time in ms

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime; // update curTime to now

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000; // calculate how quick the axis changed

                if (speed > SHAKE_THRESHOLD) {
                    Toast.makeText(MainActivity.this, "Shake gesture",
                            Toast.LENGTH_LONG).show();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        // good practice to unregister the sensor on pause
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        // register the sensor again when the application resumes
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

}