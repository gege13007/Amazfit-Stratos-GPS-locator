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

// Crée un service en tache de fond pour le Location Listener & ne pas arrêter les Updates ...
public class LocService extends Service {
    public static final String BROADCAST_ACTION = "com.samblancat";
    public LocationManager locationManager;
    public MyLocationListener listener;
    SharedPreferences sharedPref;
    double lat, lon;
    public static final int[] gsvnr= new int[32];

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

            //pour raz de la vitesse
            if (glob.oldlat==0) {
                glob.oldlat = lat;
                glob.oldlon = lon;
            }

            //Test si new point si Tracking en cours !
            if (glob.tracking) {
                //Fait à peu près 5m d'écart
                double dtlat = Math.abs(lat-glob.lastlat);
                double dtlon = Math.abs(lon-glob.lastlon);
                double x = 10000 * (dtlat + dtlon);
                if (x > 1) {
                    //test si calc segment de distance
                    x = Math.pow(dtlat, 2);
                    x += Math.pow(Math.cos(Math.toRadians(lat)) * dtlon, 2);
                    //distance des deux points en m
                    x = 111120 * Math.sqrt(x);
                    //Distance cumulée track
                    glob.realdist += x;

                    glob.lastlat=lat;
                    glob.lastlon=lon;

                    String locname = Integer.toString(glob.nbtracking++);
                    Location newpt = new Location(locname);
                    newpt.setLatitude(lat);
                    newpt.setLongitude(lon);
                    newpt.setAltitude(loc.getAltitude());
                    //stocke date+heure dans le 'Provider'
                    newpt.setProvider(fdate.format(c.getTime())+"T"+ftime.format(c.getTime())+"Z");
                    glob.gpxList.add(newpt);
                }
            }
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


    //------------- Réception d'un Message NMEA ------------
    GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
        public void onNmeaReceived(long timestamp, String nmea) {
            int tt, n, nn;
            String txt="";
            String[] nmeaSplit = nmea.split(",");

            String cmd = nmeaSplit[0].substring(3,6);
            Log.e("oncmd : ",cmd );
            //Capte le nb de sats de GSV
            //   0    1 2 3   4   5  6  7           11            15           19
            // $GPGSV,4,2,13, 12,75,283, , 15,17,175, , 17,07,034, , 19,26,046,00,0*60
            if (cmd.equalsIgnoreCase("GSV")) {
                Log.e("onGSV : ",nmea );
                //index de no de phrase GSV
                tt = 4 * (-1+Integer.parseInt(nmeaSplit[2]));
                //parcours la phrase
                for (n=7, nn=0; n<20; n+=4, nn++) {
                    try { if (Integer.parseInt(nmeaSplit[n])>0) gsvnr[tt+nn]=Integer.parseInt(nmeaSplit[n-3]); }  //Integer.parseInt(nmeaSplit[n]); }
                    catch (Exception e) { gsvnr[tt+nn]=0; }
                }
                //retranscris les SNR dans l'ordre et en string
                txt="";
                for (n=0; n<30; n++) {
                    nn=gsvnr[n];
                    if (nn>0) { txt += nn+" "; }
                }
                glob.gsv=txt;
                Log.e("onGSVtxt : ", txt);
            }
            //Capte le Fix / Alt / Dop de GGA
            else if (cmd.equalsIgnoreCase("GGA")) {
                //Extrait Fix (0=no 1=ok 2=dgps)
                txt=nmeaSplit[6];
                if (txt.equals("0")) {
                    glob.gpsfix = 0;
                    glob.oldalt=0;
                }
                else glob.gpsfix=1;

                //Extrait HDOP
                txt=nmeaSplit[8];
                try { glob.hdop = Double.parseDouble(txt); }
                catch (Exception e) { glob.hdop = 99.0; }
                //Extrait Altitude
                txt=nmeaSplit[9];
                glob.oldalt = Double.parseDouble(txt);
            }
            //Test si Autre...VTG...
            else if (cmd.equalsIgnoreCase("VTG")) {
                //Extrait VTG
                txt=nmeaSplit[7];
                Log.e("onVTG : ",txt);
                try { glob.vtg = Double.parseDouble(txt); }
                catch (Exception e) { glob.vtg = 0.0; }
            }

            //Envoie un broadcast quelconque pour Maj de MainActivity
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction("com.samblancat");
            broadCastIntent.putExtra("Sat", " ");
            sendBroadcast(broadCastIntent);
        }
    };
}