package com.samblancat.finder;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// Crée un service en tache de fond pour le Location Listener & ne pas arrêter les Updates ...
public class LocService extends Service {
    public static final String BROADCAST_ACTION = "com.samblancat";
    public LocationManager locationManager;
    public MyLocationListener listener;
    SharedPreferences sharedPref;
    double lat, lon;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Listener de position
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        //Listener de NMEA
        locationManager.addNmeaListener(nmeaListener);

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        locationManager.removeUpdates(listener);
        //Remove NMEA listener
        locationManager.removeNmeaListener(nmeaListener);
    }


    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat fdate = new SimpleDateFormat("yy-MM-dd");
            SimpleDateFormat ftime = new SimpleDateFormat("HH:mm:ss");

            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction("com.samblancat");
            lat = loc.getLatitude();
            //Important ! faire toString du Double
            broadCastIntent.putExtra("Lat", Double.toString(lat));
            lon = loc.getLongitude();
            broadCastIntent.putExtra("Lon", Double.toString(lon));
            sendBroadcast(broadCastIntent);

            //Sauve la Position Voulue et Go sur SCAN !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat("dlat", (float) lat);
            editor.putFloat("dlng", (float) lon);
            editor.apply();

            //Test si new point si Tracking en cours !
            if (glob.tracking) {
                //Fait à peu près 5m d'écart
                double x = 10000 * (Math.abs(lat-glob.lastlat)+Math.abs(lon-glob.lastlon));
                if (x > 1) {
                    //test si calc segment de distance
                    x = Math.pow(Math.abs(glob.lastlat - lat), 2);
                    x += Math.pow(Math.cos(Math.toRadians(lat)) * Math.abs(glob.lastlon - lon), 2);
                    //distance des deux points en m
                    x = 111120 * Math.sqrt(x);
                    //Distance cumulée track
                    glob.realdist += x;

                    String locname = Integer.toString(glob.nbtracking++);
                    Location newpt = new Location(locname);
                    newpt.setLatitude(lat);
                    newpt.setLongitude(lon);
                    newpt.setAltitude(loc.getAltitude());
                    //stocke date+heure dans le 'Provider'
                    newpt.setProvider(fdate.format(c.getTime())+"T"+ftime.format(c.getTime())+"Z");
                    glob.mygpxList.add(newpt);
                }
            }

            glob.lastlat=lat;
            glob.lastlon=lon;
            glob.lastalt=loc.getAltitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }
    };

    GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
        public void onNmeaReceived(long timestamp, String nmea) {
            String[] nmeaSplit = nmea.split(",");
            //Capte le nb de sats de GGA
            if (nmeaSplit[0].equalsIgnoreCase("$GPGSV")) {
                try {
                    String txt = nmeaSplit[3];
                    Intent broadCastIntent = new Intent();
                    broadCastIntent.setAction("com.samblancat");
                    broadCastIntent.putExtra("Sat", txt);
                    sendBroadcast(broadCastIntent);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Capte la Speed de GGA
            if (nmeaSplit[0].equalsIgnoreCase("$GPGGA")) {

                Intent broadCastIntent = new Intent();
                broadCastIntent.setAction("com.samblancat");

                //Extrait Fix (0=no 1=ok 2=dgps)
                String txt=nmeaSplit[6];
                broadCastIntent.putExtra("Fix", txt);
                if (txt.equals("0")) glob.gpsfix=0; else glob.gpsfix=1;

                //Extrait HDOP
                txt=nmeaSplit[8];
                broadCastIntent.putExtra("Hdop", txt);

                //Extrait Altitude
                txt=nmeaSplit[9];
                broadCastIntent.putExtra("Alt", txt);

                sendBroadcast(broadCastIntent);
            }
        }
    };
}