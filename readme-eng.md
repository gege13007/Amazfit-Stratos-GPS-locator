# Android Wear GPS OpenStreet Maps & GPX reader for Android wear     [french version](/readme.md)
----------------------------------------------------------------
Here is the last version 9.5 (december 2021) of GPS Maps & compass Locator for Wear Android (tested on Xiaomi Amazfit Stratos 2, Stratos 3, PACE, and SUUNTO...).
I made some changes with the GPX (big) files loading process. 
An large gpx file listing all water points in France is given for example (Water.gpx).

My first idea was to make a simple position reminder with a compass display, or a Golf Gps watch. We have now a Gpx file viewer with Open Street Maps.

This app includes 4 main windows :

1/ The Map viewer can display 'Open Street Maps' from zoom level 3 to 16. You can change the zoom level, and center the map on actual position with a simple click on the 3 buttons on the bottom of the screen. A distance scale and the osmap zoom level are displayed on the bottom of the screen. The scale (in km) is equal to the distance represented by the horizontal line. 
The selected GPX points are plotted on the map, with different colors depending on the altitude (blue is deep, and red is hight).
On nav mode, a line is plotted to the destination point.
A blue marker indicates your actual position.
It's possible to move the maps by simply swiping the screen.
A LONG ClICK on the map, let you select your new Destination point (or to save a new waypoint).
A simple click close the map.

Here is an example of 2 levels of zoom from 4 to 15 (maximum details).

<center><img src="/6-carto3.jpg" alt="gps carto osmaps"/></center>

2/ The compass is active when you firstly have selected a nav destination (long click on map or compas), or a waypoint in your Gpx file list.
The compass radar will display : the time, distance & altitude to your nav destination.
If you activate the magnetic sensor (with compatible wear) on the config window, the compass will turn to indicate the real direction.
Without magnetic sensor, the north is on the top of screen (at 12 hours).
A Long Click on the compass, let you mark the actual position as new Destination (or to save a new waypoint).
Swipe to close the map.

<center><img src="/5-scann.jpg" alt="gps radar stratos 2"/></center>

3/ The Gpx list view, allows you select a destination by simple click on list.
A 'LONG CLICK' remove the point. A 'simple click' select your destination.
You must have a 'GPXData' directory on the root of the wear (external storage memory).

<center><img src="/3-wpt-liste.jpg" alt="liste waypoint gps menu"/></center>

4/ The main menu appears even if the position is not ok. So it's possible to change the settings, or to find and select a waypoint to track.
From up left to right : first button start the settings, the second is for select a Gps waypoint from a gpx file.
Third (bottom) button is for the Map, and the arrow is for the tracking radar.

<center><img src="/2-main-screen.jpg" alt="gps main menu"/></center>

5/ The settings view, allows to select a gpx file (it can be an export of your sports activities).
To enable the magnetic sensor and compass mooving (not good for your battery!).
To enable the automatic next waypoint selection.
To enable automatic centering map when moving.

<center><img src="/4-settings.jpg" alt="gps radar stratos 2"/></center>

Here's the first gps startup screen. This animation just appear for 10 seconds showing the number of satellites in view.
The first gps fixing time depends of your situation, the meteo...

<center><img src="/1-startscreen.jpg" alt="gps startup fix"/></center>

Where are the MAPS ?
-------------------
You MUST create an 'OSMaps' directory on your wear.
Then in OSMaps, you will have a subdirectory for each zoom level - from '2' (country level) to '16' (to see your home).
In each subdirectory the tiles are stored with their X/Y coordinatess numbers.
Don't worry about that, just download and run 'MOBILE ATLAS CREATOR' ( https://mobac.sourceforge.io/ ).

<center><img src="/mobac1.jpg" alt="mobac osm maps"/></center>

Click on 'Nouvel Atlas' (OSMdroid ZIP). With the checkboxes on the left you must select the zoom level to include in your atlas.
The level 15 includes small details and is not available for every country or cities. The level 15 represent a 2X size of datas than zoom 14, and 4X than zoom 13. So don't try to select the entire world on zoom level 14 !

Click to valid the checkbox 'Recreer/Ajuster tuiles' !
Click 'Ajouter a la selection', and 'Creer Atlas'.

Unzip the file, and copy all repertories to your wear ("/OSMaps").
You must have something like this.

<center><img src="/osmaps-wear1.jpg" alt="wear osm maps"/></center>

How this works
--------------
The first activity launches a service who assumes the background task of getting GPS location (with a locationManager and locationListener). This avoid to stop and re-run the gps updates between each activities, and assures that just ONE first fix delay is necessary when you start the app.
The lat & long are broadcasted to the other activities with 'sendBroadcast & broadcastReceiver'.
The first screen uses a 'nmealistener' to get & display a snapshot of the $GPGSV & GPGGA frames.
The location service and its broadcats is only stopped, when the app is destroyed (if not - your battery don't last so long)...
We use the 'magnetic orientation sensor type3' to rotate the compass graduations and map, with a sensorManager.registerListener.

I made my own implementation of the tiles selection formula. Each OSM tile is a 256x256 png bitmap displayed with usual canvas drawing functions.
Each tile is resized on a 512x512 pixels bitmap, to get more visibility on small screens.

Change log
----------
30/4/2020 First upload v1.

1/5/2020  Location & NMEA broadcasted in the same manner by Locservice. Many checks to do.
          Important Fix !!! Impossible to kill the entire process (sinking the battery). No explication. Find a solution with ' android.os.Process.killProcess(android.os.Process.myPid());' in OnDestroy().
          RMC speed added on the scan screen.
         
14/5/2020  Version 2 , with GPX file compatibility, and 'autonext' waypoint detection.

24/05/2020 Version 3 - many fix & Map

2/06/2020  Version 4 - Long click on Maps and many fixs

05/06/2020 Version 4.2 - modif on stop app

07/06/2020 Version 4.4 - mini modif affich, alertdialog...

08/06/2020 V4.5 - Bug fix on start-up

12/06/2020 v5 - Improve display on different screen dimensions (SUUNTO)

13/06/2020 v6 - Possible bug fix with gpx display on cartodraw

15/06/2020 v7 - Modif 2 display on Suunto / bug fix on delete waypoint 

4/04/2021 v5.0 - Add speed & distance and direct track saving on the Maps.

12/2021 - v9 - Big changes on gpx read - only on start app, and changing gpx.
         Read the wpt xml or the trkseg for tracks...
