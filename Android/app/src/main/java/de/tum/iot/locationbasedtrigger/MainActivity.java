package de.tum.iot.locationbasedtrigger;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    //Used for Logging
    protected static final String TAG = "MonitoringActivity";

    //Used by AltBeacon
    private BeaconManager beaconManager;
    private Region region;

    //Used to provide Notifications
    private NotificationManagerCompat notificationManager;
    private String notificationChannelId = "0";
    private int lastNotificationId;
    private long lastNotificationTime;
    private DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    //Used to update Current Distance GUI
    private String currentDistance = "-";

    //Used for Location Permission Requeste
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    //Setting for trigger Frequency
    private double triggerDistance = .4;//Distance in meters when to trigger
    private long triggerInterval = 10 * 1000; //Interval in milliseconds


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get Permissions & Checks
        verifyBluetooth();
        requestLocationPermission();

        createNotificationChannel(); //Create Channel an Android 8+ to use for notifications
        notificationManager = NotificationManagerCompat.from(this);
        lastNotificationId = 0;
        lastNotificationTime = 0;

        beaconManager = BeaconManager.getInstanceForApplication(this);

        //AltBeacon usually only detects AltBeacons. In order to detect Eddystone & iBeacons
        //they need to be added to the parser
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)); //Eddystone, e.g. RaspberryPi
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); //iBeacon, e.g. of university

        //Setup RaspberryPi Beacon
        String raspiNamespaceId = "0x0eaea79793961d290fa4";
        String raspiInstanceId = "0x3dfe4b5e89b9";
        region = new Region("raspberrypi",
                Identifier.parse(raspiNamespaceId),
                Identifier.parse(raspiInstanceId),
                null);

        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        RangeNotifier rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon b:beacons
                     ) {
                    Log.i(TAG, "The first beacon " + b.toString() + " is about " + b.getDistance() + " meters away.");
                    updateCurrentDistanceGUI(b.getDistance());

                    if(b.getDistance() < triggerDistance &&
                            lastNotificationTime + triggerInterval <= System.currentTimeMillis()){
                        lastNotificationTime = System.currentTimeMillis();
                        replaceNotification("Trigger",
                                "trigger was activated at "+dateFormat.format(new Date(lastNotificationTime))+
                                        " at "+String.format("%.2f", b.getDistance())+"m");
                        Log.i(TAG, "trigger was activated at "+dateFormat.format(new Date(lastNotificationTime))+
                                " at "+String.format("%.2f", b.getDistance())+"m");
                    }
                }
            }

        };
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {   }
    }

    public void updateCurrentDistanceGUI(double d){
        currentDistance = String.format("%.2f", d)+" m";

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.distance)).setText(currentDistance);
            }
        });

    }

    //Taken from https://github.com/AltBeacon/android-beacon-library-reference/blob/master/app/src/main/java/org/altbeacon/beaconreference/MonitoringActivity.java
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

    //Taken from https://developer.android.com/training/notify-user/build-notification#builder
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(notificationChannelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void publishNotification(String title, String content){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, notificationChannelId)
                .setSmallIcon(R.drawable.near_me)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add as notification
        notificationManager.notify(++lastNotificationId, mBuilder.build());
    }

    public void replaceNotification(String title, String content){
        notificationManager.cancel(lastNotificationId);

        publishNotification(title, content);
    }

    //Taken from https://github.com/AltBeacon/android-beacon-library-reference/blob/master/app/src/main/java/org/altbeacon/beaconreference/MonitoringActivity.java
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
}
