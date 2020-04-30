# Amazfit-Stratos-GPS-locator
Simple &amp; effective radar GPS locator app for Amazfit Xiaomi android wear
------------------------------------------------------------------------------
Hello guys, after a very hard work on Android Studio, here is my first version of a simple GPS locator for my Amazfit Stratos 2.
With this simple app , you can store your current location (for now or for later), and see the direction and distance to your point.
This could be useful if don't want to forget your car on a parking ! or if you want quickly store an interesting position for fututre uses...

Here's the first gps startup screen. A small info about satellites in view must appear on the top of the screen.
You can wait sometimes 10-30 seconds for this first fix. 
<img src="/gpsapp_271818537.jpg" alt="gps startup fix"/>

Once the gps fix is done, the main menu appear.
From up left to right : first button save the current position on the shared preferences of the wear.
Second button recall a previous position and go to the radar.
Third (bottom) is to exit, and the arrow is immediate radar return on the current position without save.
(** a small green buton is hidden on the right - this launches a test of magnetic sensors).
<img src="/gpsapp_31818537.jpg" alt="gps main menu amazfit"/>


In radar operation - the distance displayed on the center - the compass and direction rotates when you move.
<img src="/gpsapp_171818537.jpg" alt="gps radar stratos 2"/>

How that work
-------------
The first activity (gpsetup) launches a service with the locationManager and locationListener. This avoid to stop and re-run the gps listener between activities, and so just ONE first fix delay is necessary when you start the app.
The lat & long are broadcasted to the other activities with 'sendBroadcast & broadcastReceiver'.
The location service is only stopped when the app is destroyed (if not your battery don't last as long)...

What's left to do ?
------------------
==> Implement a simple selection system for labels of multiples places...
