package com.staysafe.staysafe;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.staysafe.staysafe.models.Alert;
import com.staysafe.staysafe.util.JsonAsyncTask;
import com.staysafe.staysafe.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, SensorEventListener {

    private static final int LOCATION_PERMISSION_CODE = 234;
    private static final String MAIN_ACTIVITY = "Main Activity";
    private static final double ACCELERATION_THRESHOLD = 10;
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private boolean mRequestingLocationUpdates = true;
    private LocationRequest mLocationRequest;
    private FrameLayout warningBackground;
    private TextView accelText;
    private String accidentType = "accident";
    private TextView locationText;

    private enum RunningTypes {NOT_RUNNING, RUNNING, ENDED}

    private RunningTypes TIMER_STATUS = RunningTypes.NOT_RUNNING;

    private TextView warningText;
    private ImageView warningImage;
    private CountDownTimer timer;

    private SensorManager senSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        warningBackground = (FrameLayout) findViewById(R.id.warning_background);
        accelText = (TextView) findViewById(R.id.accel_text);
        accelText.setText("");
        accelText.setVisibility(View.INVISIBLE);

        locationText = (TextView) findViewById(R.id.location_text);
        locationText.setText("...");
        locationText.setVisibility(View.INVISIBLE);

        warningText = (TextView) findViewById(R.id.warning_text);
        warningImage = (ImageView) findViewById(R.id.warning_image);
        warningImage.setVisibility(View.INVISIBLE);
        warningText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (timer != null) {
                    if (TIMER_STATUS == RunningTypes.NOT_RUNNING) {
                        timer.start();
                        warningBackground.setBackgroundResource(R.drawable.background_dangerous);
                        warningImage.setVisibility(View.INVISIBLE);
                        warningText.setText("!");
                        TIMER_STATUS = RunningTypes.RUNNING;
                    } else if (TIMER_STATUS == RunningTypes.RUNNING) {
                        timer.cancel();
                        warningText.setText("!");
                        warningBackground.setBackgroundResource(R.drawable.background_normal);
                        TIMER_STATUS = RunningTypes.NOT_RUNNING;
                    } else if (TIMER_STATUS == RunningTypes.ENDED) {
                        warningText.setText("!");
                        warningBackground.setBackgroundResource(R.drawable.background_normal);
                        warningImage.setVisibility(View.INVISIBLE);
                        TIMER_STATUS = RunningTypes.NOT_RUNNING;
                    }
                }
            }
        });
        warningText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (mCurrentLocation != null && TIMER_STATUS == RunningTypes.NOT_RUNNING){
                    locationText.setVisibility(View.VISIBLE);
                    String url = "https://os.smartcommunitylab.it/core.geocoder/spring/location?latlng="+
                            mCurrentLocation.getLatitude()+","+mCurrentLocation.getLongitude()+"&rows=1";
                    JsonAsyncTask asyncTask = new JsonAsyncTask();
                    asyncTask.setOnJsonListener(new JsonAsyncTask.OnJsonDownloadListener() {
                        @Override
                        public void OnPreExecute() {

                        }

                        @Override
                        public void OnPostExecute(String result) {
                            try {
                                JSONObject obj = new JSONObject(result);
                                String name = obj.getJSONObject("response").getJSONArray("docs").getJSONObject(0).getString("name");
                                locationText.setText(name);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    asyncTask.execute(url);
                    return true;
                }
                return false;
            }
        });

        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                warningText.setText(String.valueOf((int) millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                warningImage.setVisibility(View.VISIBLE);
                warningText.setText("");
                warningBackground.setBackgroundResource(R.drawable.background_critical);
                TIMER_STATUS = RunningTypes.ENDED;
                DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("alert");
                if (mCurrentLocation != null){
                    alertRef.push().setValue(new Alert("+39 349 3333334", mCurrentLocation.getLatitude(),
                            mCurrentLocation.getLongitude(), accidentType));
                    accidentType = "accident";
                } else {
                    CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator);
                    Snackbar.make(coordinatorLayout, "No location available", Snackbar.LENGTH_SHORT)
                            .setAction("dismiss",null)
                            .show();
                }
            }
        };

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("accelerometer_enabled", true))
            setupAccelerometer();
    }

    public void setupAccelerometer(){
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        accelText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_PERMISSION_CODE);
            return;
        }
        if (!Utils.isLocationServiceEnabled(this)){
            //enable location
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("You should enable location in order to use this app")
                    .setTitle("Warning")
                    .setPositiveButton("enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        }
                    })
                    .setNegativeButton("exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            builder.create().show();
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mRequestingLocationUpdates = true;
    }


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        Log.d(MAIN_ACTIVITY,"location: "+ location.toString());
        Log.d(MAIN_ACTIVITY,"speed: "+ location.getSpeed());
    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;


        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] linear_acceleration = addSamples(event.values);

            //text.setText("x="+linear_acceleration[0]+"\ny="+linear_acceleration[1]+"\nz="+linear_acceleration[2]);
            double accel = Math.sqrt(Math.pow(linear_acceleration[0], 2) + Math.pow(linear_acceleration[1], 2) + Math.pow(linear_acceleration[2], 2));
            //text.setText(text.getText().toString().concat("\naccel = "+accel));
            accelText.setText(String.format("%.2f", accel) +" m/s^2");
            if (accel >= ACCELERATION_THRESHOLD) {
                accelText.setText("CAR CRASH");
                if (TIMER_STATUS != RunningTypes.RUNNING) {
                    timer.start();
                    warningBackground.setBackgroundResource(R.drawable.background_dangerous);
                    warningImage.setVisibility(View.INVISIBLE);
                    warningText.setText("!");
                    TIMER_STATUS = RunningTypes.RUNNING;
                    accidentType = "car crash";
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        stopLocationUpdates();
        if (senSensorManager != null)
            senSensorManager.unregisterListener(this);
        accelText.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains("accelerometer_enabled") && prefs.getBoolean("accelerometer_enabled", true))
            setupAccelerometer();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        throw new NullPointerException("Google Play Services not properly installed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setMessage("You should grant location permissions in order to use this app")
                            .setTitle("Error")
                            .setPositiveButton("retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    onConnected(null);
                                }
                            })
                            .setNegativeButton("exit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                    builder.create().show();
                }
            }
        }
    }

    // Constants for the low-pass filters
    float timeConstant = 0.18f;
    float alpha = 0.9f;
    float dt = 0;

    // Timestamps for the low-pass filters
    float timestamp = System.nanoTime();
    float timestampOld = System.nanoTime();

    // Gravity and linear accelerations components for the
    // Wikipedia low-pass filter
    float[] gravity = new float[]
            { 0, 0, 0 };

    float[] linearAcceleration = new float[]
            { 0, 0, 0 };

    // Raw accelerometer data
    float[] input = new float[]
            { 0, 0, 0 };

    int count = 0;

    /**
     * Add a sample.
     *
     * @param acceleration
     *            The acceleration data.
     * @return Returns the output of the filter.
     */
    public float[] addSamples(float[] acceleration)
    {
        // Get a local copy of the sensor values
        System.arraycopy(acceleration, 0,input, 0, acceleration.length);

        timestamp = System.nanoTime();

        // Find the sample period (between updates).
        // Convert from nanoseconds to seconds
        dt = 1 / (count / ((timestamp - timestampOld) / 1000000000.0f));

        count++;

        alpha = timeConstant / (timeConstant + dt);

        gravity[0] = alpha * gravity[0] + (1 - alpha) * input[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * input[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * input[2];

        linearAcceleration[0] = input[0] - gravity[0];
        linearAcceleration[1] = input[1] - gravity[1];
        linearAcceleration[2] = input[2] - gravity[2];

        return linearAcceleration;
    }

}
