package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.samblancat.finder.Selectpos.decodeGPX;

public class Scan extends AppCompatActivity implements SensorEventListener {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    private SensorManager mSensorManager;
    SharedPreferences sharedPref;
    double mylat=0, mylng=0, alt=0;
    double mylat0=0, mylng0=0;
    String wptname;
    double cap, cap0, compas=0;
    //Flag si compas bouge
    int compasok=0;
    //Flag si next wpt auto
    int autonext=0, counter=0;
    //Flag état de la recherche 0:rien,  1:scan rapprochement, 2:proche arrivé, 3:s'éloigne
    int arrived=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView dtxt;

        setContentView(R.layout.scan);
        mContext=this;

        //Récup la config.
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        compasok = sharedPref.getInt("compas", 0);
        autonext = sharedPref.getInt("autonext", 0);
        glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

        //Retrouve Position de Destination
        mylat0 = sharedPref.getFloat("lat0", (float) mylat);
        mylng0 = sharedPref.getFloat("lng0", (float) mylng);
        wptname = sharedPref.getString("nom"," ");
        dtxt = findViewById(R.id.wptnomtxt);
        dtxt.setText(wptname);

        //Fait clignoter "Wptname" tant que pas de fix
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(250);  // blinking time
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        dtxt.startAnimation(anim);

        if (compasok>0) {
            // initialize your android device sensor capabilities
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            // Sensor Type - 3 ,Sensor Name - Orientation  Sensor
            Sensor magnetSensor = mSensorManager.getDefaultSensor(3);
            mSensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // on initialise le receiver de service/broadcast
        receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);

        // Click Long pour Save New Waypoint ?
        ImageView imgView= findViewById(R.id.boussole);
        imgView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                // Confirme sauver new wpt
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                String formatdate = df.format(c.getTime());
                //prend mylat/lng qui sont filtrés du 0.0
                if ((mylat!=0)&&(mylng!=0)) {
                    glob.appendGPX(mContext, mylat, mylng, alt, formatdate);
                    String togo = new DecimalFormat("#0.0000").format(mylat);
                    togo += "/" + new DecimalFormat("#0.0000").format(mylng);
                    Toast.makeText(mContext, "Position "+togo+" saved to " + glob.gpxini, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        Toast.makeText(getApplicationContext(), "Long Click to create a Waypoint !", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //Arrete le Sensor listener
        if (compasok>0) mSensorManager.unregisterListener(this);
        unregisterReceiver(receiver );
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //Arrete le Sensor listener
        if (compasok>0) mSensorManager.unregisterListener(this);
        //Log.d("TAG : ", "onPause: ");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (compasok>0) {
            // initialize your android device sensor capabilities
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            // Sensor Type - 3 ,Sensor Name - Orientation  Sensor
            Sensor magnetSensor = mSensorManager.getDefaultSensor(3);
            mSensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
       // Log.d("TAG : ", "onResume");
    }

    //POUR GESTION MAGNETIC SENSOR
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        compas = Math.round(event.values[0]);
        //Tourne la boussole
        ImageView imgview;
        imgview = findViewById(R.id.gradcompas);
        //Angle inverse pour redresser la Rosace !
        imgview.setRotation((float)-compas);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }


    public Location getNextWpt(Double lat, Double lng) {
        int tri;
        // Extrait la liste Array des 'name'
        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        tri = sharedPref.getInt("sortbydist", 1);
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + sharedPref.getString("gpxini","gpxlocator.gpx");

        File gpx = new File(path);
        final List<Location> gpxList = decodeGPX(gpx, lat, lng, tri);        // liste des Pos des Wpts
        //Si Aucun Waypoints -> efface - recree
        if (gpxList.size() > 1) {
            //Va chercher le wptname qui suit l'actuel
            for (int nn = 0; nn < -1+gpxList.size(); nn++) {
                String tt = (gpxList.get(nn)).getProvider();
                if (wptname.equals(tt))
                    return (gpxList.get(nn+1));
            }
        }
        return(null);
    }


    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo, fix;
            AlertDialog.Builder builder;
            int satok;
            double al;

            TextView  dtxt = findViewById(R.id.todisttxt);
            TextView atxt = findViewById(R.id.speedtxt);
            ImageView imb = findViewById(R.id.boussole);

            //write your code here to be executed after 1 second
            TextView timtxt = findViewById(R.id.timetxt);
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            String formattime = df.format(c.getTime());
            timtxt.setText(formattime);

            //Capte Qualité
            fix = intent.getStringExtra("Fix");
            if (fix!=null) {
                try { satok = Integer.parseInt(fix);
                } catch (Exception e) { satok = 0; }
                if (satok == 0) {
                    dtxt.setText("-.-");
                    atxt.setText("alt-");
                    imb.setVisibility(View.INVISIBLE);
                    return;
                }
            }

            //essaie de capter Altitude
            String alti = intent.getStringExtra("Alt");
            if (alti!=null) {
                try { alt = Double.parseDouble(alti); }
                catch (Exception e) { alt=0; }
                DecimalFormat altprec = new DecimalFormat("###0");
                atxt.setText("alt "+altprec.format(alt)+"m");
            }

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            try { mylat = Double.parseDouble(la); } catch (Exception ignored) {  }
            try { mylng = Double.parseDouble(lo); } catch (Exception ignored) {  }

            if ( (mylat!=0)&&(mylng!=0)&&(!wptname.equals("")) ) {
                //Pos gps ok -> arrete clignotement
                TextView ptxt = findViewById(R.id.wptnomtxt);
                ptxt.clearAnimation();

                //Calc distance à mylat0/lng0
                double dk = Math.pow(Math.abs(mylat - mylat0), 2);
                dk += Math.pow(Math.cos(Math.toRadians(mylat))*Math.abs(mylng - mylng0), 2);
                dk = 1000 * 60 * 1.852 * Math.sqrt(dk);
                //--------- Test état de l'approche ---------
                //Si On s'éloigne ?
                if ((arrived == 2) && (dk > 25)) {
                    //Ajoute un peu d'inertie
                    if (counter++ > 5) {
                        counter = 0;
                        //on est arrivé et on s'éloigne
                        arrived = 3;
                        dtxt.setTextColor(Color.GREEN);

                        //Si Changement Auto de next Wpt
                        if (autonext > 0) {
                            //Va deja chercher le next wpt !
                            final Location nloc = getNextWpt(mylat, mylng);
                            final String wptname2 = nloc.getProvider();

                            //Demande confirmation
                            builder = new AlertDialog.Builder(mContext, R.style.myALERT);
                            builder.setTitle("Goto next Wpt ?");
                            builder.setCancelable(false);
                            builder.setMessage(wptname2);
                            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                mylat0 = nloc.getLatitude();
                                mylng0 = nloc.getLongitude();   // !!!!!
                                //Met le wptname
                                wptname = wptname2;
                                TextView ntxt = findViewById(R.id.wptnomtxt);
                                ntxt.setText(wptname);
                                //Raz flag en approche
                                arrived = 1;
                                }
                            });
                            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                //Raz flag en approche
                                arrived = 1;
                                }
                            });
                            builder.setIcon(android.R.drawable.ic_dialog_alert);
                            builder.show();
                        }
                    }
                }

                //Si on est arrivé ?
                if ((arrived == 1) && (dk <= 20)) {
                    //Ajoute un peu d'inertie
                    if (counter++>5) {
                        counter=0;
                        arrived = 2;
                        dtxt.setTextColor(Color.RED);

                        //Confirmation si ARRET de NAV ?
                        builder = new AlertDialog.Builder(mContext, R.style.myALERT);
                        builder.setTitle("Stop the nav ?");
                        builder.setCancelable(false);
                        builder.setMessage("Arrived at destination");
                        builder.setPositiveButton("Stop", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Raz et Stop Nav + flag en approche
                                wptname = "";
                                TextView wtxt = findViewById(R.id.wptnomtxt);
                                wtxt.setText("");
                                TextView  dtxt = findViewById(R.id.todisttxt);
                                TextView atxt = findViewById(R.id.speedtxt);
                                ImageView imb = findViewById(R.id.boussole);
                                dtxt.setText("-.-");
                                dtxt.setTextColor(Color.GREEN);
                                atxt.setText("alt-");
                                imb.setVisibility(View.INVISIBLE);
                                //Raz la Position de Nav & sort du Scan !
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putFloat("lat0", (float) 0.0);
                                editor.putFloat("lng0", (float) 0.0);
                                editor.putString("nom", wptname);
                                editor.apply();
                                arrived = 0;
                                finish();
                            }
                        });
                        builder.setNegativeButton("Continue", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Raz flag en approche
                                arrived = 1;
                            }
                        });
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.show();

                    }
                }
                //Si En approche ?
                if ((arrived == 0) && (dk > 30)) {
                    arrived = 1;
                    counter=0;
                }
                //Affiche dist en m ou km?
                dtxt.setText(glob.dtotext(dk));

                //Calcul du cap
                cap = 0;
                double dx = (mylng - mylng0);
                double dy = (mylat - mylat0);
                if (dy != 0) {
                    if (dy < 0)
                        cap = Math.PI + Math.atan(dx / dy);
                    else
                        cap = Math.atan(dx / dy);
                }
                if (cap < 0) cap += 2 * Math.PI;
                cap = cap * (180.0 / Math.PI);
                //petite moyenne / 2 val
                cap = (cap + cap0) / 2;
                cap0 = cap;

                //Tourne la flèche
                //Retranche (retour arrière) le cap Compas
                cap -= compas;
                imb.setRotation((float) cap);
                imb.setVisibility(View.VISIBLE);
            }
        }
    }
}