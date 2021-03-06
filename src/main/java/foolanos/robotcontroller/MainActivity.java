package foolanos.robotcontroller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements  SensorEventListener{

    // Sensors & SensorManager
    private Sensor accelerometer;
    private Sensor magnetometer;
    private SensorManager mSensorManager;

    // Storage for Sensor readings
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;

    // Rotation around the Z axis
    private double mRotationInDegrees,lastRotation=Integer.MAX_VALUE,prevRotation=Integer.MAX_VALUE;


    private TextView angle;
    private Vibrator vibrator;

    private volatile boolean started =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        angle = (TextView) findViewById(R.id.angle);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Get a reference to the SensorManager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        angle.setText("waiting");
        new Thread(new WaitThread()).start();

        // Get a reference to the accelerometer
        accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        // Get a reference to the magnetometer
        magnetometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        // Exit unless both sensors are available
        if (null == accelerometer || null == magnetometer)
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();


        // Register for sensor updates

        mSensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister all sensors
        mSensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Acquire accelerometer event data

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            mGravity = new float[3];
            System.arraycopy(event.values, 0, mGravity, 0, 3);

        }

        // Acquire magnetometer event data

        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            mGeomagnetic = new float[3];
            System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);

        }

        // If we have readings from both sensors then
        // use the readings to compute the device's orientation
        // and then update the display.

        if (mGravity != null && mGeomagnetic != null) {

            float rotationMatrix[] = new float[9];

            // Users the accelerometer and magnetometer readings
            // to compute the device's rotation with respect to
            // a real world coordinate system

            boolean success = SensorManager.getRotationMatrix(rotationMatrix,
                    null, mGravity, mGeomagnetic);

            if (success) {

                float orientationMatrix[] = new float[3];

                // Returns the device's orientation given
                // the rotationMatrix

                SensorManager.getOrientation(rotationMatrix, orientationMatrix);

                // Get the rotation, measured in radians, around the Z-axis
                // Note: This assumes the device is held flat and parallel
                // to the ground

                float rotationInRadians = orientationMatrix[0];

                // Convert from radians to degrees
                mRotationInDegrees = Math.toDegrees(rotationInRadians)+180;

                if (!started) return;

                if (lastRotation==Integer.MAX_VALUE){
                    lastRotation = mRotationInDegrees;
                    prevRotation = mRotationInDegrees;
                }

                double diff = (prevRotation-mRotationInDegrees);
                if (Math.abs(diff)<10)
                    mRotationInDegrees = mRotationInDegrees+ 0.5*diff;
                prevRotation=mRotationInDegrees;


                if (mRotationInDegrees<90 && lastRotation>270){
                    diff = mRotationInDegrees+360-lastRotation;
                }
                else if (mRotationInDegrees>270 && lastRotation<90){
                    diff = lastRotation+360-mRotationInDegrees;
                }
                else {
                    diff = Math.abs(mRotationInDegrees-lastRotation);
                }


                if (diff >= 90) {
                    lastRotation=mRotationInDegrees;
                    vibrator.vibrate(500);
                }



                angle.setText(mRotationInDegrees +"");

                // Reset sensor event data arrays
                mGravity = mGeomagnetic = null;

            }
        }

    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // N/A
    }

    private class WaitThread implements Runnable{

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
                started =true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
