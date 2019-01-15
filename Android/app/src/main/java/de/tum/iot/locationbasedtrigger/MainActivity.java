package de.tum.iot.locationbasedtrigger;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.RemoteException;
import android.preference.TwoStatePreference;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    //Used for Logging
    protected static final String TAG = "LocationTrigger";
    private LocationTrigger lt;

    //Used to update UI
    private TextView lastDistance;
    private Thread uiUpdateThread;

    //Used for Location Permission Requeste
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lt = (LocationTrigger) getApplication();

        //Get Permissions & Checks
        verifyBluetooth();
        requestLocationPermission();

        //Update Button to Switch Background Scanning
        updateUIIsBackgroundScanning();

        //Start Updating the UI
        lastDistance = findViewById(R.id.distance);
        uiUpdateThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!uiUpdateThread.isInterrupted()) {
                        uiUpdateThread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lastDistance.setText(getLastDistance());
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        uiUpdateThread.start();

        lt.startScanning();
    }

    @Override
    protected void onResume(){
        super.onResume();
        lt.startScanning();
    }

    @Override
    protected void onStop(){
        super.onStop();
        lt.stopScanning();
    }

    /**
     * Get Last registered Distance to Beacon formated as String
     * @return
     */
    public String getLastDistance(){
        return (lt.currentDistance == -1 ? "-" : String.format("%.2f", lt.currentDistance) + "m");
    }

    /**
     * Requests Location Permission from the user
     * Taken from https://altbeacon.github.io/android-beacon-library/requesting_permission.html
     */
    public void requestLocationPermission(){
        //Request Location Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });
                builder.show();
            }
        }
    }

    /**
     * Handles the return values after requesting the location permission
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not work.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    /**
     * Verifies that the given Device has bluetooth enabled, as well as BLE capabilities
     * Taken from https://github.com/AltBeacon/android-beacon-library-reference/blob/master/app/src/
     * main/java/org/altbeacon/beaconreference/MonitoringActivity.java
     */
    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();

        }

    }

    /**
     * Switches the background Scanning and off
     * @param view
     */
    public void switchBackgroudScanning(View view){
        lt.switchBackgroundScanning();
        updateUIIsBackgroundScanning();
    }

    /**
     * Returns whether the background Scanning is enabled
     * @return
     */
    public boolean isBackgroundScanning(){
        return lt.isBackgroundScanning;
    }

    /**
     * Updates the UI depending on whether the background scanner is enabled or disabled
     */
    public void updateUIIsBackgroundScanning(){
        TextView bt = findViewById(R.id.switchBackgroundScanning);
        if(isBackgroundScanning()){
            bt.setText(R.string.disable_background);
        }else{
            bt.setText(R.string.enable_background);
        }
    }
}
