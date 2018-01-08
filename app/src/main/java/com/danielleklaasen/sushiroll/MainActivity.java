// File: MainActivity.java
// Class with 2 sensor implementations saving data to Firebase
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.lang.reflect.Field;
import java.util.UUID;

/**
 * This is a the Main Activity class implementing the SensorEvenListener to work with light sensor and accelerometer
 * @see android.hardware.SensorEventListener
 * @author Danielle Klaasen
 */
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
    TextView sushiTextView;

    // Array holding confirmation text variations
    String confirmationText[] = {
            "You make miso happy.",
            "Sashimi Rollin.. They Hatin..",
            "Miso hungry.",
            "Rice to meet you."
    };
    int lastConfirmationText;

    // db
    FirebaseDatabase database;
    /**
     * onCreate method to gain access to the system's sensors and set listeners to changes. Also, getting an instance of Firebase database and set listener for Data changes.
     *
     * @see #onSensorChanged(SensorEvent)
     * @see #DBlistener()
     */
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
        sushiTextView = (TextView)findViewById(R.id.latest_sushi); // text view with info from db

        // set db listener to update text
        database = FirebaseDatabase.getInstance();
        DBlistener();
    }

    // two required SensorEventListener methods (implemented in this class)
    /**
     * Method to detect changes in the sensor
     * @param event is the sensor event
     * */
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

    /**
     * Unregisters the sensors stored in senSensorManager, on pause.
     */
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    /**
     * Registers the light sensor + accelerometer again when the application resumes.
     */
    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Replaces a sushi cartoon randomly from drawables starting with defined prefix.
     * @see #SUSHI_PREFIX
     */
    public void replaceSushi(){
        if(curTime - timeImageUpdated > 1000){ // update picture, only if not updated in the last second
            Random rand = new Random();
            int  n = rand.nextInt(numSushiDrawables) + 1; // range 1 - number of sushi drawables

            // making sure new image != old image
            for (int i = 0; i < numSushiDrawables;i++){
                if(n != sushiTagNr){
                    break;
                }
                n = rand.nextInt(numSushiDrawables) + 1;
            }

            String uri = "@drawable/"+SUSHI_PREFIX+n; // make new sushi drawable
            sushiTagNr = n; // update nr

            int imageResource = getResources().getIdentifier(uri, null, getPackageName());
            Drawable res = getResources().getDrawable(imageResource);
            imageView.setImageDrawable(res); // assign drawable to imageview

            timeImageUpdated = curTime; // update time
        }
    }

    /**
     * Returns number of drawables starting with a specific string.
     * @param string to check for drawables starting with given string
     * @return number of drawables
     */
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

    /**
     * On click function for the save button.
     * @param v takes the view which was pressed
     */
    public void onClickSaveBtn(View v) {
        // retrieving data to send
        String uniqueID = UUID.randomUUID().toString(); // create unique ID
        String currentSushi = SUSHI_PREFIX + Integer.toString(sushiTagNr); // current sushi
        String strSunglassesOn = String.valueOf(sunglassesOn); // boolean sunglasses

        if(saveToDB(uniqueID, currentSushi, strSunglassesOn)){
            confirmToUser();
        }

    }

    /**
     * Saving to firebase database.
     * @param uniqueID
     * @param currentSushi the sushi drawable on screen
     * @param strSunglassesOn
     * @return true if successful
     */
    private Boolean saveToDB(String uniqueID, String currentSushi, String strSunglassesOn){
        // create child with unique id
        DatabaseReference myRef = database.getReference(uniqueID);

        // filling map with data
        Map<String,Object> sushiMap = new HashMap<>();
        sushiMap.put("sushi", currentSushi);
        sushiMap.put("sunglasses", strSunglassesOn);

        myRef.setValue(sushiMap); // send data to db

        return true;
    }

    /**
     * Show random confirmation text to user, different from previous one.
     */
    private void confirmToUser(){
        // getting random confirmation text from array and show user feedback (toast)
        Random rand = new Random();
        final int min = 0;
        final int max = confirmationText.length-1;
        int random = rand.nextInt((max - min) + 1) + min;

        // making sure new text != repeating old text
        if(confirmationText!=null){
            for (int i = 0; i < max;i++){
                if(random != lastConfirmationText){
                    break;
                }
                random = rand.nextInt((max - min) + 1) + min;
            }
        }

        String message = confirmationText[random];
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        lastConfirmationText = random;
    }

    /**
     * Listens to changes in the database.
     */
    private void DBlistener(){
        // DB
        DatabaseReference myRef = database.getReference();

        // Read from the database
       myRef.addChildEventListener(new ChildEventListener() {
           @Override
           public void onChildAdded(DataSnapshot dataSnapshot, String s) {
               String sushi = dataSnapshot.child("sushi").getValue().toString();
               String sunglasses = dataSnapshot.child("sunglasses").getValue().toString();

               updateText(sushi, sunglasses);
           }

           @Override
           public void onChildChanged(DataSnapshot dataSnapshot, String s) {

           }

           @Override
           public void onChildRemoved(DataSnapshot dataSnapshot) {

           }

           @Override
           public void onChildMoved(DataSnapshot dataSnapshot, String s) {

           }

           @Override
           public void onCancelled(DatabaseError databaseError) {
                // Failed to read value
               Log.w("db", "Failed to read value.", databaseError.toException());
           }
       });
    }

    /**
     * Method to update text with data from Firebase database
     * @param sushi the sushi drawable which is saved
     * @param sunglasses to define if the sunglasses are on or off
     */
    public void updateText(String sushi, String sunglasses){
        String content = "Sushi: " + sushi + ". Sunglasses: " + sunglasses + ".";
        sushiTextView.setText(content);
    }
}
