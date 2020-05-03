package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;

public class Scan extends AppCompatActivity  implements SensorEventListener {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    // device sensor manager
    private SensorManager mSensorManager;
    double mylat=0, mylng=0;
    double mylat0=0, mylng0=0;
    double cap, cap0, compas=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retrouve Position Départ
        Intent intent = getIntent();
        String myl = intent.getStringExtra("lat0");
        if (myl!=null) mylat0 = Double.parseDouble(myl);
        myl = intent.getStringExtra("lng0");
        if (myl!=null) mylng0 = Double.parseDouble(myl);

        setContentView(R.layout.scan);
        mContext=this;

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Sensor Type - 3 ,Sensor Name - Orientation  Sensor
        Sensor magnetSensor = mSensorManager.getDefaultSensor(3);
        mSensorManager.registerListener(this, magnetSensor , SensorManager.SENSOR_DELAY_NORMAL);

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
        mSensorManager.unregisterListener(this);

        finish();
        Intent i = new Intent(mContext, MainActivity.class);
        startActivity(i);
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

    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String tp, la, lo, spd;
            TextView dtxt;

            //essaie de capter la Speed
            spd = intent.getStringExtra("Rmc");
            if (spd!=null) {
                Double kmh =  1.852 * Double.parseDouble(spd);
                spd = new DecimalFormat("##0.0").format(kmh);
                dtxt = (TextView) findViewById(R.id.speedtxt);
                dtxt.setText(spd+"kmh");
            }

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            mylat =  (la!=null)?Double.parseDouble(la):0;
            mylng = (lo!=null)?Double.parseDouble(lo):0;

            if ((mylat!=0)&&(mylng!=0)) {
                //Calc distance à mylat0/lng0
                double dk = Math.pow(Math.abs(mylat - mylat0), 2) + Math.pow(Math.abs(mylng - mylng0), 2);
                dk = 1000 * 111.12 * Math.sqrt(dk);
                if (dk < 1000) {
                    tp = new DecimalFormat("###0").format(dk);
                    dtxt = (TextView) findViewById(R.id.todisttxt);
                    dtxt.setText(tp + "m");
                } else {
                    // en km
                    dk = dk / 1000;
                    if (dk < 10)
                        tp = new DecimalFormat("#0.0").format(dk);
                    else
                        tp = new DecimalFormat("###0").format(dk);
                    dtxt = (TextView) findViewById(R.id.todisttxt);
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