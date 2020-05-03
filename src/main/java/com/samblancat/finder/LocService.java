package com.samblancat.finder;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
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

// Crée un service en tache de fond pour le Location Listener & ne pas arrêter les Updates ...
public class LocService extends Service {
    public static final String BROADCAST_ACTION = "com.samblancat";
    public LocationManager locationManager;
    public MyLocationListener listener;
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
        Log.e("Locservice:", "oncreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Locservice:", "onStartc");
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

    //    Toast.makeText(this, "Bye Bye...", Toast.LENGTH_LONG).show();
        locationManager=null;
        listener=null;
        Log.e("Locservice:", "ondestroy");
    }


    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction("com.samblancat");
            lat = loc.getLatitude();
            //Important ! faire toString du Double
            broadCastIntent.putExtra("Lat", Double.toString(lat));
            lon = loc.getLongitude();
            broadCastIntent.putExtra("Lon", Double.toString(lon));
            sendBroadcast(broadCastIntent);
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
                String txt=nmeaSplit[2]+"/"+nmeaSplit[1]+"  "+nmeaSplit[3]+" sats";
                Intent broadCastIntent = new Intent();
                broadCastIntent.setAction("com.samblancat");
                broadCastIntent.putExtra("Gsv", txt);
                sendBroadcast(broadCastIntent);
            }
            //Capte la Speed de RMC
            if (nmeaSplit[0].equalsIgnoreCase("$GPRMC")) {
                String txt=nmeaSplit[7];
                Intent broadCastIntent = new Intent();
                broadCastIntent.setAction("com.samblancat");
                broadCastIntent.putExtra("Rmc", txt);
                sendBroadcast(broadCastIntent);
            }
        }
    };
}