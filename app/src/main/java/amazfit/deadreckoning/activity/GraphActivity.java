package amazfit.deadreckoning.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import amazfit.deadreckoning.R;
import amazfit.deadreckoning.utils.Utils;
import amazfit.deadreckoning.graph.GraphDrawer;
import amazfit.deadreckoning.orientation.GyroscopeDeltaOrientation;
import amazfit.deadreckoning.orientation.GyroscopeEulerOrientation;
import amazfit.deadreckoning.orientation.MagneticFieldOrientation;
import amazfit.deadreckoning.stepcounting.DynamicStepCounter;

public class GraphActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private static final float GYROSCOPE_INTEGRATION_SENSITIVITY = 0.0025f;

    private DynamicStepCounter dynamicStepCounter;
    private GyroscopeDeltaOrientation gyroscopeDeltaOrientation;
    private GyroscopeEulerOrientation gyroscopeEulerOrientation;
    private GraphDrawer scatterPlot;

    private FloatingActionButton fabButton;
    private LinearLayout mLinearLayout;

    private SensorManager sensorManager;
    private LocationManager locationManager;

    float[] gyroBias;
    float[] magBias;
    float[] currGravity;
    float[] currMag; //current magnetic field

    private boolean isRunning;
    private boolean isCalibrated;
    private boolean usingDefaultCounter;
    private boolean areFilesCreated;
    private float strideLength;
    private float gyroHeading;
    private float magHeading;

    private long startTime;
    private boolean firstRun;

    private float initialHeading;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GraphActivity.this, new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },0);
            finish();
        }

        gyroBias = null;
        magBias = null;
        currGravity = null;
        currMag = null;

        String counterSensitivity;

        isRunning = isCalibrated = usingDefaultCounter = areFilesCreated = false;
        firstRun = true;
        strideLength = 0;
        initialHeading = gyroHeading = magHeading = 0;
        startTime = 0;

        strideLength = getIntent().getFloatExtra("stride_length", 2.5f);
        isCalibrated = getIntent().getBooleanExtra("is_calibrated", false);
        gyroBias = getIntent().getFloatArrayExtra("gyro_bias");
        magBias = getIntent().getFloatArrayExtra("mag_bias");

        counterSensitivity = getIntent().getStringExtra("preferred_step_counter");

        usingDefaultCounter = counterSensitivity.equals("default") &&
                getIntent().getBooleanExtra("step_detector", false);

        gyroscopeDeltaOrientation = new GyroscopeDeltaOrientation(GYROSCOPE_INTEGRATION_SENSITIVITY, gyroBias);
        if (usingDefaultCounter) //if using default TYPE_STEP_DETECTOR, don't need DynamicStepCounter
            dynamicStepCounter = null;
        else if (!counterSensitivity.equals("default"))
            dynamicStepCounter = new DynamicStepCounter(Double.parseDouble(counterSensitivity));
        else
            dynamicStepCounter = new DynamicStepCounter(1.0);

        fabButton = findViewById(R.id.fab);
        mLinearLayout = findViewById(R.id.linearLayoutGraph);

        scatterPlot = new GraphDrawer("Position");
        scatterPlot.addPoint(0, 0);
        mLinearLayout.addView(scatterPlot.getGraphView(getApplicationContext()));

        Toast.makeText(GraphActivity.this, "Stride Length: " + strideLength, Toast.LENGTH_SHORT).show();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GraphActivity.this);

        //starting sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(GraphActivity.this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);

        if (isCalibrated) {
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
                    SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        if (usingDefaultCounter) {
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            sensorManager.registerListener(GraphActivity.this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        //setting up buttons
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isRunning) {

                    isRunning = true;

                    float[][] initialOrientation = MagneticFieldOrientation.getOrientationMatrix(currGravity, currMag, magBias);
                    initialHeading = MagneticFieldOrientation.getHeading(currGravity, currMag, magBias);

                    gyroscopeEulerOrientation = new GyroscopeEulerOrientation(Utils.IDENTITY_MATRIX);

                    fabButton.setImageDrawable(ContextCompat.getDrawable(GraphActivity.this, R.drawable.ic_pause_black_24dp));

                } else {

                    firstRun = true;
                    isRunning = false;

                    fabButton.setImageDrawable(ContextCompat.getDrawable(GraphActivity.this, R.drawable.ic_play_arrow_black_24dp));

                }


            }
        });

        mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                float compHeading = Utils.calcCompHeading(magHeading, gyroHeading);


                float oPointX = scatterPlot.getLastYPoint();
                float oPointY = -scatterPlot.getLastXPoint();

                oPointX += Utils.getXFromPolar(strideLength, compHeading);
                oPointY += Utils.getYFromPolar(strideLength, compHeading);

                float rPointX = -oPointY;
                float rPointY = oPointX;

                scatterPlot.addPoint(rPointX, rPointY);

                mLinearLayout.removeAllViews();
                mLinearLayout.addView(scatterPlot.getGraphView(getApplicationContext()));

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isRunning) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(GraphActivity.this, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },0);
                finish();
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GraphActivity.this);

            if (isCalibrated) {
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED),
                        SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
                        SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                        SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                        SensorManager.SENSOR_DELAY_FASTEST);
            }

            if (usingDefaultCounter) {
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                        SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                sensorManager.registerListener(GraphActivity.this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                        SensorManager.SENSOR_DELAY_FASTEST);
            }

            fabButton.setImageDrawable(ContextCompat.getDrawable(GraphActivity.this, R.drawable.ic_pause_black_24dp));

        } else {

            fabButton.setImageDrawable(ContextCompat.getDrawable(GraphActivity.this, R.drawable.ic_play_arrow_black_24dp));

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(firstRun) {
            startTime = event.timestamp;
            firstRun = false;
        }

        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            currGravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ||
                event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
            currMag = event.values;
        }

        if (isRunning) {
            if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                ArrayList<Float> dataValues = Utils.arrayToList(event.values);
                dataValues.add(0, (float)(event.timestamp - startTime));
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD || event.sensor.getType() ==
                    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {

                magHeading = MagneticFieldOrientation.getHeading(currGravity, currMag, magBias);

                ArrayList<Float> dataValues = Utils.createList(
                        event.values[0], event.values[1], event.values[2],
                        magBias[0], magBias[1], magBias[2]
                );
                dataValues.add(0, (float)(event.timestamp - startTime));
                dataValues.add(magHeading);

            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE ||
                    event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {

                float[] deltaOrientation = gyroscopeDeltaOrientation.calcDeltaOrientation(event.timestamp, event.values);

                gyroHeading = gyroscopeEulerOrientation.getHeading(deltaOrientation);
                gyroHeading += initialHeading;


                ArrayList<Float> dataValues = Utils.createList(
                        event.values[0], event.values[1], event.values[2],
                        gyroBias[0], gyroBias[1], gyroBias[2]
                );
                dataValues.add(0, (float)(event.timestamp - startTime));
                dataValues.add(gyroHeading);

            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

                float norm = Utils.calcNorm(
                        event.values[0] +
                                event.values[1] +
                                event.values[2]
                );

                boolean stepFound = dynamicStepCounter.findStep(norm);

                if (stepFound) {

                    //saving linear acceleration data
                    ArrayList<Float> dataValues = Utils.arrayToList(event.values);
                    dataValues.add(0, (float)(event.timestamp - startTime));
                    dataValues.add(1f);

                    //complimentary filter
                    float compHeading = Utils.calcCompHeading(magHeading, gyroHeading);

                    //Log.d("comp_heading", "" + compHeading);

                    //getting and rotating the previous XY points so North 0 on unit circle
                    float oPointX = scatterPlot.getLastYPoint();
                    float oPointY = -scatterPlot.getLastXPoint();

                    //calculating XY points from heading and stride_length
                    oPointX += Utils.getXFromPolar(strideLength, gyroHeading);
                    oPointY += Utils.getYFromPolar(strideLength, gyroHeading);

                    //rotating points by 90 degrees, so north is up
                    float rPointX = -oPointY;
                    float rPointY = oPointX;

                    scatterPlot.addPoint(rPointX, rPointY);

                    //saving XY location data


                    mLinearLayout.removeAllViews();
                    mLinearLayout.addView(scatterPlot.getGraphView(getApplicationContext()));

                    //if step is not found
                } else {
                    //saving linear acceleration data
                    ArrayList<Float> dataValues = Utils.arrayToList(event.values);
                    dataValues.add(0, (float) event.timestamp);
                    dataValues.add(0f);
                }

            } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

                boolean stepFound = (event.values[0] == 1);

                if (stepFound) {

                    float oPointX = scatterPlot.getLastYPoint();
                    float oPointY = -scatterPlot.getLastXPoint();

                    oPointX += Utils.getXFromPolar(strideLength, gyroHeading);
                    oPointY += Utils.getYFromPolar(strideLength, gyroHeading);

                    float rPointX = -oPointY;
                    float rPointY = oPointX;

                    scatterPlot.addPoint(rPointX, rPointY);
                    mLinearLayout.removeAllViews();
                    mLinearLayout.addView(scatterPlot.getGraphView(getApplicationContext()));
                }

            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 0:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(GraphActivity.this, "Thank you for providing permission!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(GraphActivity.this, "Need location permission to create tour.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }


}

