package de.tum.iot.locationbasedtrigger;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class LocationTrigger extends Application implements BootstrapNotifier, BeaconConsumer, RangeNotifier {
    //Used for Logging
    protected static final String TAG = "LocationTrigger";

    //Used by AltBeacon
    private BeaconManager beaconManager;
    private Region region; //Identifies certain Beacon, such as RaspPi
    private RegionBootstrap regionBootstrap; //Registers Callback for Background Scanning
    private BackgroundPowerSaver backgroundPowerSaver; //Enables Battery Saver

    //Used to provide Notifications
    private NotificationManagerCompat notificationManager;
    private String notificationChannelId = "0";
    private int lastNotificationId;
    private long lastNotificationTime;
    private DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    //Used to update Current Distance GUI
    public double currentDistance = -1;

    //Setting for trigger Frequency
    private double triggerDistance = .4;//Distance in meters when to trigger
    private long triggerInterval = 10 * 1000; //Interval in milliseconds

    //Switch Background Scanning On/Off
    public boolean isBackgroundScanning;
    public boolean isScanning;

    @Override
    public void onCreate() {
        super.onCreate();

        //Permission Checks will be done by MainActivity, once launched

        createNotificationChannel(); //Create Channel an Android 8+ to use for notifications
        notificationManager = NotificationManagerCompat.from(this);
        lastNotificationId = 0;
        lastNotificationTime = 0;

        //AltBeacon usually only detects AltBeacons. In order to detect Eddystone & iBeacons
        //they need to be added to the parser
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().addAll(getBeaconParsers());

        //Set Region
        region = getRaspiRegion();

        //Enable Debugging of BeaconManager
        beaconManager.setDebug(true);

        //Enable PowerSaver
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        //Update Status on Background Scanning
        isBackgroundScanning = beaconManager.getForegroundServiceNotificationId() != -1;

        Log.i(TAG, "App started up");
    }

    /**
     * Switches the Background Scanning on or off. This has no effect on the current scanning
     * until the app is stopped
     */
    public void switchBackgroundScanning(){
        if(isBackgroundScanning){
            disableBackgroundScanning();
        }else{
            enableBackgroundScanning();
        }

    }

    /**
     * Stops Monitoring for Beacons
     */
    public void stopScanning(){
        if(!isBackgroundScanning){
            stopRanging();
            regionBootstrap.disable();
            beaconManager.disableForegroundServiceScanning();
            isScanning = false;
        }
    }

    /**
     * Starts Monitoring for Beacons
     */
    public void startScanning(){
        if(isScanning){
            return;
        }
        isScanning = true;

        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(5000);
        beaconManager.setBackgroundScanPeriod(1100);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.near_me);
        builder.setContentTitle("Scanning for Beacons");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("D",
                    "Persistent Notification", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Persistent notification for background detection");
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        beaconManager.enableForegroundServiceScanning(builder.build(), 456);

        regionBootstrap = new RegionBootstrap(this, region);
    }

    public void disableBackgroundScanning(){
        isBackgroundScanning = false;
    }

    public void enableBackgroundScanning(){
        isBackgroundScanning = true;
    }

    /**
     * Changes the Application to Ranging mode used to estimate the distance to the beacon
     * and also reduces the time between scans to 0s allowing for continues scanning and
     * a faster reaction
     */
    public void startRanging(){
        Log.i(TAG, "Started Ranging");

        try {
            //Add this class as callback for ranging
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.addRangeNotifier(this);

            //reduce the time between scans for faster reaction
            beaconManager.setBackgroundBetweenScanPeriod(0);
            beaconManager.setBackgroundScanPeriod(1100);
            beaconManager.updateScanPeriods();
        } catch (RemoteException e) {  e.printStackTrace(); }
    }

    /**
     * Stops Ranging, only the detection of the presence of beacons will continue. Also increasing
     * the time between scans to save power and resources
     */
    public void stopRanging(){
        Log.i(TAG, "Stopped Ranging");

        try {
            //Stop Ranging for beacons
            beaconManager.stopRangingBeaconsInRegion(region);
            beaconManager.removeRangeNotifier(this);

            //increase the time between scans for faster reaction
            beaconManager.setBackgroundBetweenScanPeriod(5000);
            beaconManager.setBackgroundScanPeriod(1100);
            beaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        this.currentDistance = -1;
    }

    /**
     * Callback when the BeaconManager connected. Not needed since the BootStrap Notifier
     * will do this for us
      */
    @Override
    public void onBeaconServiceConnect() {
        Log.i(TAG, "Beacon Service connected");
    }

    /**
     * Determins the State of the region when application is launched.
     * @param arg0 state of the region 1 = present; 0 = absent
     * @param arg1 region / ID of the beacon
     */
    @Override
    public void didDetermineStateForRegion(int arg0, Region arg1) {
        Log.i(TAG, "Did Determine State for Region. Current state: "+arg0);

        if(arg0 == 1){
            startRanging();
        }
    }

    /**
     * Called when the beacon is with in range. Then we will switch to ranging to measure
     * the distance to the beacon
     * @param arg0 region / ID of the beacon
     */
    @Override
    public void didEnterRegion(Region arg0) {
        Log.i(TAG, "Got a didEnterRegion call");
        startRanging();
    }

    /**
     * Called when the beacon is not insight any more. Then we will disable ranging
     * @param arg0 region / ID of the beacon
     */
    @Override
    public void didExitRegion(Region arg0) {
        Log.i(TAG, "Did Exit Region");
        stopRanging();
    }

    /**
     * Creates a notification channel to be used for devices using Android 8.0+
     * Taken from https://developer.android.com/training/notify-user/build-notification#builder
     */
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

    /**
     * Publishes a notification
     * @param title
     * @param content
     */
    public void publishNotification(String title, String content){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, notificationChannelId)
                .setSmallIcon(R.drawable.near_me)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Add as notification
        notificationManager.notify(++lastNotificationId, mBuilder.build());
    }

    /**
     * Replaces the last sent notification with the given notification
     * @param title
     * @param content
     */
    public void replaceNotification(String title, String content){
        notificationManager.cancel(lastNotificationId);

        publishNotification(title, content);
    }

    /***
     * Returns the region containing the Namespace & Intstance to find the RaspberryPi Beacon
     *
     * @return Region
     */
    public static Region getRaspiRegion(){
        String raspiNamespaceId = "0x0eaea79793961d290fa4";
        String raspiInstanceId = "0x3dfe4b5e89b9";
        return new Region("raspberrypi",
                Identifier.parse(raspiNamespaceId),
                Identifier.parse(raspiInstanceId),
                null);
    }

    /**
     * Returns Beacon Parsers for Eddystone & iBeacon
     *
     * @return List of BeaconParsers
     */
    public static List<BeaconParser> getBeaconParsers(){
        return new ArrayList<BeaconParser>(){{
            add(new BeaconParser().
                    setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)); //Eddystone, e.g. RaspberryPi)
            add(new BeaconParser().
                    setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); //iBeacon, e.g. of university)
        }};
    }

    /**
     * Called when Ranging for beacons containing the distance to the beacons
     * @param beacons
     * @param region
     */
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon b:beacons
                ) {
            Log.i(TAG, "The first beacon " + b.toString() + " is about " + b.getDistance() + " meters away.");
            currentDistance = b.getDistance();

            if(b.getDistance() < triggerDistance &&
                    lastNotificationTime + triggerInterval <= System.currentTimeMillis()){
                lastNotificationTime = System.currentTimeMillis();
                publishNotification("Trigger",
                        "trigger was activated at "+dateFormat.format(new Date(lastNotificationTime))+
                                " at "+String.format("%.2f", b.getDistance())+"m");
                Log.i(TAG, "trigger was activated at "+dateFormat.format(new Date(lastNotificationTime))+
                        " at "+String.format("%.2f", b.getDistance())+"m");
            }
        }
    }
}
