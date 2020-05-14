package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import static com.samblancat.finder.Selectpos.decodeGPX;

public class Scan extends AppCompatActivity implements SensorEventListener {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    private SensorManager mSensorManager;
    SharedPreferences sharedPref;
    double mylat=0, mylng=0;
    double mylat0=0, mylng0=0;
    String wptname;
    double cap, cap0, compas=0;
    Integer compasok=1;
    Integer autonext=0, counter=0;
    //Flag état de la recherche 0:rien,  1:scan rapprochement, 2:proche arrivé, 3:s'éloigne
    Integer arrived=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView dtxt;

        setContentView(R.layout.scan);
        mContext=this;

        //Retrouve Position de Destination
        Intent intent = getIntent();
        String myl = intent.getStringExtra("lat0");
        if (myl!=null) mylat0 = Double.parseDouble(myl);
        myl = intent.getStringExtra("lng0");
        if (myl!=null) mylng0 = Double.parseDouble(myl);
        //Met le wptname
        wptname = intent.getStringExtra("nom");
        dtxt = (TextView) findViewById(R.id.wptnomtxt);
        dtxt.setText(wptname);

        //Fait clignoter "Wptname" tant que pas de fix
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(250);  // blinking time
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        dtxt.startAnimation(anim);

        //Récup la position en mém.
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        compasok = sharedPref.getInt("compas", 1);
        autonext = sharedPref.getInt("autonext", 0);

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
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        //Arrete le Sensor listener
        if (compasok>0) mSensorManager.unregisterListener(this);
        unregisterReceiver((BroadcastReceiver)receiver );
        finish();
    }

    //POUR GESTION MAGNETIC SENSOR
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        compas = Math.round(event.values[0]);
        //Tourne la boussole
        ImageView imgview;
        imgview = (ImageView)findViewById(R.id.gradcompas);
        //Angle inverse pour redresser la Rosace !
        imgview.setRotation((float)-compas);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    public void getNextWpt() {
        // Extrait la liste Array des 'name'
        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + sharedPref.getString("gpxini","gpxlocator.gpx");
        File gpx = new File(path);
        final List<Location> gpxList = decodeGPX(gpx);        // liste des Pos des Wpts
        //Si Aucun Waypoints -> efface - recree
        if (gpxList.size() > 1) {
            //Va chercher le wptname qui suit l'actuel
            for (int nn = 0; nn < -1+gpxList.size(); nn++) {
                String tt = ((Location) gpxList.get(nn)).getProvider();
                if (wptname.equals(tt)) {
                    tt = ((Location) gpxList.get(nn+1)).getProvider();
                    Toast.makeText(mContext, "Goto "+tt, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String tp, la, lo, alt;
            TextView dtxt;
            dtxt = (TextView) findViewById(R.id.todisttxt);

            //essaie de capter Altitude
            alt = intent.getStringExtra("Alt");
            if (alt!=null) {
                TextView atxt = (TextView) findViewById(R.id.speedtxt);
                atxt.setText(alt+" m");
            }

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            try { mylat = Double.parseDouble(la); }
            catch (Exception e) { mylat=0; }
            try { mylng = Double.parseDouble(lo); }
            catch (Exception e) { mylng=0; }

            if ((mylat!=0)&&(mylng!=0)) {
                //Pos gps ok -> arrete clignotement
                TextView ptxt = findViewById(R.id.wptnomtxt);
                ptxt.clearAnimation();

                //Calc distance à mylat0/lng0
                double dk = Math.pow(Math.abs(mylat - mylat0), 2) + Math.pow(Math.abs(mylng - mylng0), 2);
                dk = 1000 * 111.12 * Math.sqrt(dk);
                //Test état de l'approche
                //Si On s'éloigne ?
                if ((arrived == 2) && (dk > 30)) {
                    //Ajoute un peu d'inertie
                    if (counter++ > 10) {
                        counter = 0;
                        arrived = 3;
                        dtxt.setTextColor(Color.GREEN);
                        if (autonext > 0) getNextWpt();
                    }
                }
                //Si on est arrivé ?
                if ((arrived == 1) && (dk <= 25)) {
                    //Ajoute un peu d'inertie
                    if (counter++>10) {
                        counter=0;
                        arrived = 2;
                        dtxt.setTextColor(Color.RED);
                    }
                }
                //Si En approche ?
                if ((arrived == 0) && (dk > 30)) {
                    arrived = 1;
                    counter=0;
                }
                //test si en km?
                if (dk < 1000) {
                    tp = new DecimalFormat("###0").format(dk);
                    dtxt.setText(tp + "m");
                } else {
                    // en km
                    dk = dk / 1000;
                    if (dk < 10)
                        tp = new DecimalFormat("0.00").format(dk);
                    else
                        tp = new DecimalFormat("###0").format(dk);
                    dtxt.setText(tp + "km");
                }

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
                ImageView imgview;
                imgview = (ImageView) findViewById(R.id.boussole);
                //Retranche (retour arrière) le cap Compas
                cap -= compas;
                imgview.setRotation((float) cap);
            }
        }
    };
}