# Android Wear Amazfit GPS Direction Finder
Simple &amp; effective GPS locator app for Amazfit Xiaomi android wear
------------------------------------------------------------------------------
Hello guys, after a very hard work ( ͠° ͟ʖ ͡°) on Android Studio, here is my first version of a simple GPS locator for Xiaomi Wear Android  Amazfit Stratos 2.
With this simple app , you can store your current location (for now or for later), and see the direction and distance to your point.
This could be useful if don't want to forget your car on a parking ! or if you want quickly store an interesting position for fututre uses...

Here's the first gps startup screen. A small info about satellites in view must appear on the top of the screen.
You can wait sometimes 10-30 seconds for this first fix.

<center><img src="/gpsapp_271818537.jpg" alt="gps startup fix"/></center>

Once the gps fix is done, the main menu appear.
From up left to right : first button save the current position on the shared preferences of the wear.
Second button recall a previous position and go to the gps radar.
Third (bottom) is to exit, and the arrow is immediate radar return on the current position without save.

(** a small green button is hidden on the right - this launches a test of magnetic sensors).

<center><img src="/gpsapp_31818537.jpg" alt="gps main menu amazfit"/></center>


In radar operation - the distance displayed on the center - the compass and direction rotates when you move.

<center><img src="/gpsapp_171818537.jpg" alt="gps radar stratos 2"/></center>

How this works
--------------
The first activity (gpsetup) launches a service who assumes the background task of getting GPS location (with a locationManager and locationListener). This avoid to stop and re-run the gps updates between each activities, and assures that just ONE first fix delay is necessary when you start the app.
The lat & long are broadcasted to the other activities with 'sendBroadcast & broadcastReceiver'.
The first screen uses a 'nmealistener' to get & display a snapshot of the $GPGSV frames.
The location service and its broadcats is only stopped, when the app is destroyed (if not - your battery don't last so long)...
We use the 'magnetic orientation sensor type3' to rotate the compass graduations, with a sensorManager.registerListener.

Change log
----------
30/4/2020 First upload v1.

1/5/2020  Location & NMEA broadcasted in the same manner by Locservice. Many checks to do.
          Important Fix !!! Impossible to kill the entire process (sinking the battery). No explication. Find a solution with ' android.os.Process.killProcess(android.os.Process.myPid());' in OnDestroy().
          RMC speed added on the scan screen.
         
14/5/2020  Version 2 , with GPX file compatibility, and 'autonext' waypoint detection.


What's left to do ?
------------------
==> Implement a simple cartography display of the tracks.
