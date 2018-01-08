package com.danielleklaasen.sushiroll;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.lang.reflect.Field;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener { // setting sensor event listener here
    // sensors
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senLight;

    // timing
    private long lastUpdate = 0;
    private long timeImageUpdated = 0;
    private long curTime = 0;

    // motion
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 1200;

    // sushi variables
    private int numSushiDrawables = 0;
    private int sushiTagNr;
    ImageView imageView;
    ImageView sunglasses;
    private final String SUSHI_PREFIX = "sushi";
    Boolean sunglassesOn = true;

    // confirmation text array
    String confirmationText[] = {
            "Sashimi Rollin’.. They Hatin’..",
            "You make miso happy",
            "Rice to meet you",
            "Miso hungry"
    };
    int lastConfirmationText = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SENSOR SETUP
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // used to access the system's sensors

        // initialize the Accelerometer variables
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // reference to system's accelerometer
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL); // accelerometer listener: deliver sensor events at normal rate (last arg)

        // light sensor variables
        senLight = senSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); // reference to system's light sensor
        senSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_NORMAL);

        // find out how many sushi drawables there are in the resource folder
        numSushiDrawables = getNumDrawable(SUSHI_PREFIX);

        // fill variables with sushi info
        imageView = (ImageView)findViewById(R.id.main_image); // access image
        sunglasses = (ImageView)findViewById(R.id.sunglasses); // access sunglasses
        String sushiTag = (String) imageView.getTag(); // see which sushi nr it is
        sushiTagNr = Integer.parseInt(sushiTag); // set current tag nr sushi drawable
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

            // set up current time in ms
            curTime = System.currentTimeMillis();

            // SET UP SHAKE GESTURE
            if ((curTime - lastUpdate) > 100) { // after 100ms onSensorChanged was invokes (to recognize quick shake gesture, but not overload the system)
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime; // update curTime to now

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000; // calculate how quick the axis changed

                if (speed > SHAKE_THRESHOLD) {
                    // SHAKE GESTURE RECOGNIZED
                    replaceSushi();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

        if (mySensor.getType() == Sensor.TYPE_LIGHT) { // check for light sensor
            float lightValue = event.values[0]; // retrieve light value from sensor

            if (lightValue<15){
                sunglassesOn = false;
                sunglasses.setVisibility(View.INVISIBLE);
            } else {
                sunglassesOn = true;
                sunglasses.setVisibility(View.VISIBLE);
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
        // register the sensors again when the application resumes
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void replaceSushi(){
        if(curTime - timeImageUpdated > 1000){ // update picture, only if not updated in the last second
            String sushiTag = (String) imageView.getTag(); // see which sushi nr it is
            sushiTagNr = Integer.parseInt(sushiTag);
            Random rand = new Random();
            int  n = rand.nextInt(numSushiDrawables) + 1; // range 1 - number of sushi drawables

            // making sure new image != old image
            for (int i = 0; i < numSushiDrawables;i++){
                if(n != sushiTagNr){
                    break;
                }
                n = rand.nextInt(numSushiDrawables) + 1;
            }

            String uri = "@drawable/sushi"+n; // make new sushi drawable

            int imageResource = getResources().getIdentifier(uri, null, getPackageName());
            Drawable res = getResources().getDrawable(imageResource);
            imageView.setImageDrawable(res); // assign drawable to imageview

            imageView.setTag(Integer.toString(n));

            timeImageUpdated = curTime; // update time
        }
    }

    public int getNumDrawable(String string){
        int num = 0;
        Field[] drawables = R.drawable.class.getFields();
        for (Field f : drawables) {
            try {
                if(f.getName().startsWith(string)){
                    num++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return num;
    }
    public void onClickSaveBtn(View v) {
        // retrieving data to send
        String uniqueID = UUID.randomUUID().toString(); // create unique ID
        curTime = System.currentTimeMillis(); // setup current timestamp
        String currentTime = Long.toString(curTime);
        String currentSushi = SUSHI_PREFIX + Integer.toString(sushiTagNr); // current sushi
        String strSunglassesOn = String.valueOf(sunglassesOn); // boolean sunglasses

        if(saveToDB(uniqueID, currentSushi, strSunglassesOn)){
            confirmToUser();
            //updateTextFromDB(uniqueID);
        }

    }
    private Boolean saveToDB(String uniqueID, String currentSushi, String strSunglassesOn){
        // DB
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(uniqueID);

        // filling map with data
        Map<String,Object> sushiMap = new HashMap<>();
        sushiMap.put("sushi", currentSushi);
        sushiMap.put("sunglasses", strSunglassesOn);

        myRef.setValue(sushiMap); // send data to db

        return true;
    }
    private void confirmToUser(){
        // getting random confirmation text from array and show user feedback (toast)
        Random rand = new Random();
        final int min = 0;
        final int max = confirmationText.length-1;
        int random = rand.nextInt((max - min) + 1) + min;

        // making sure new text != repeating old text
        for (int i = 0; i < max;i++){
            if(random != lastConfirmationText){
                break;
            }
            random = rand.nextInt((max - min) + 1) + min;
        }
        String message = confirmationText[random];
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        lastConfirmationText = random;
    }
    private void updateTextFromDB(String uniqueID){
        // DB
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(uniqueID);
        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("debug", "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("debug", "Failed to read value.", error.toException());
            }
        });
    }
}
