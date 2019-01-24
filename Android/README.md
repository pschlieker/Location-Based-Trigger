# Android Trigger
## Considerations for choosing framework
Over the years different standards for BLE Beacons have developed. The more common ones are iBeacon (Apple), Eddystone (Google) and AltBeacon (Radius Networks). Since we are using Android on the later two were considered. Both of the provide adequate libraries and APIs that include the ability to receive notifications when a beacon was detected. 
* AltBeacon uses the Android Beacon Library https://altbeacon.github.io/android-beacon-library/
* Eddystone is an opensource standard developed by Google. It is connected to the Nearby API of Google, which has the option to register Beacons with Google and receive among others notifications when you come in range with a beacon.
Since the Google Nearby API requires the Beacons to be registered with Google AltBeacon was chosen. In addition it does not require any servers or additional ressources. Last but not least it is opensource and covered by the Apache2 open source license.
## Settings
The API to be triggered, as well as the distance at which to trigger, can be set in LocationTrigger.java
