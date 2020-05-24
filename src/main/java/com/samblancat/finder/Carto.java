package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

public class Carto extends AppCompatActivity implements SensorEventListener {
    Context mContext;
    private cartodraw cartoview;
    public float x1,x2,y1,y2;
    private BroadcastReceiver receiver;
    SharedPreferences sharedPref;
    private SensorManager mSensorManager;
    public int compasok=0;
    public static final String BROADCAST_ACTION = "com.samblancat";
    public int counterpos=0;
    public double wx , wy, cx, cy;
    public double dlat, dlon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext=this;

        wx = (double) getResources().getDisplayMetrics().widthPixels;
        wy = (double) getResources().getDisplayMetrics().heightPixels;
        cx = wx/2;
        cy = wy/2;

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        compasok = sharedPref.getInt("compas", 0);

        //Retrouve Position de Destination
        dlat = sharedPref.getFloat("dlat", (float) 43);
        dlon = sharedPref.getFloat("dlng", (float) 5);

        cartoview = new cartodraw(this);
        setContentView(cartoview);

        if (compasok>0) {
            // initialize your android device sensor capabilities
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            // Sensor Type - 3 ,Sensor Name - Orientation  Sensor
            Sensor magnetSensor = mSensorManager.getDefaultSensor(3);
            mSensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // on initialise le receiver de service/broadcast
        receiver = new Carto.MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        //Arrete le Sensor listener
        if (compasok>0) mSensorManager.unregisterListener(this);
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
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (compasok>0) mSensorManager.unregisterListener(this);

        try { unregisterReceiver((BroadcastReceiver)receiver );
        } catch(IllegalArgumentException e) { e.printStackTrace(); }
    }


    //POUR GESTION MAGNETIC SENSOR
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        int compas = Math.round(event.values[0]);
        //Tourne la carte
        cartoview.rotation(compas);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }


    //Detection du swipe sur gauche ou autre .... pour finish ?
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch(ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = ev.getX();
                y1 = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = ev.getX();
                y2 = ev.getY();
                //      Toast.makeText(mContext, "x="+Double.valueOf(x2)+" y="+Double.valueOf(y2), Toast.LENGTH_LONG).show();
                if ((Math.abs(x2 - x1) < 1) && (Math.abs(y2 - y1) < 1)) {
                    //Test si ZOOM ?
                    if (y2 > wy - 60) {
                        if (x2 > cx+5) {
                            cartoview.zoomin();
                        } else {
                            cartoview.zoomout();
                        }
                    } else
                        finish();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x2 = ev.getX();
                y2 = ev.getY();
                //      Toast.makeText(mContext, "x="+Double.valueOf(x2)+" y="+Double.valueOf(y2), Toast.LENGTH_LONG).show();
                //SHIFT de l'écran
                //    Log.d("sh ", String.valueOf(x2-x1)+" "+String.valueOf(y2-y1));
                if (Math.abs(x2 - x1) + Math.abs(y2 - y1) > 30) {
                    cartoview.shift(x2 - x1, y2 - y1);
                    x1 = x2-3;  // pour test anti-rebond ?
                    y1 = y2-3;
                    break;
                }
        }
        return  false;   //super.dispatchTouchEvent(ev);
    }


    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo;

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            if ((la!=null)&&(lo!=null)) {
                try { dlat = Double.parseDouble(la); } catch (Exception ignored) { ; }
                try { dlon = Double.parseDouble(lo); } catch (Exception ignored) { ; }
                //Attend 10 sec avant repaint !
                if (counterpos==0) cartoview.updateData(dlat, dlon);
                if (counterpos++ > 3) counterpos=0;
            }
        }
    };

}