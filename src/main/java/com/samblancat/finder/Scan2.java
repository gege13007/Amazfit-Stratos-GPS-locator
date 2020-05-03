package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.List;

public class Scan2 extends AppCompatActivity  implements SensorEventListener {
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    // device sensor manager
    private SensorManager mSensorManager;
    double xx0=0, yy0=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        xx0=-99; yy0=-99;

        setContentView(R.layout.scan2);
        mContext=this;

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Sensor Type - 3 ,Sensor Name - Orientation  Sensor
        Sensor magnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, magnetSensor , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        Sensor magnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.unregisterListener(this, magnetSensor);

        Intent i = new Intent(mContext, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    //POUR GESTION MAGNETIC SENSOR
    @Override
    public void onSensorChanged(SensorEvent event) {
        long xx;
        long yy;
        //   String xz=Double.toString(Math.round(event.values[0]))+" "+Double.toString(Math.round(event.values[1]));
        //Test si valeur 0 start
        if ((xx0==-99) && (yy0==-99)) {
            xx0 = event.values[0];
            yy0 = event.values[1];
            String str=Double.toString(xx0)+"!"+Double.toString(yy0);;
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }

        xx=(Math.round (155-3*(event.values[0]-xx0)));
        yy=(Math.round (155+3*(event.values[1]-yy0)));
        if (xx<0) xx=0;
        if (yy<0) yy=0;
        RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.olayout);
        FrameLayout.LayoutParams relativeParams = (FrameLayout.LayoutParams)relativeLayout.getLayoutParams();
        relativeParams.leftMargin= (int) xx;
        relativeParams.topMargin= (int) yy;
        relativeLayout.setLayoutParams(relativeParams);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    private void getSensorList() {
        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        StringBuilder strLog = new StringBuilder();
        int iIndex = 1;
        for (Sensor item : sensors) {
            strLog.append(iIndex + ".");
            strLog.append(" Sensor Type - " + item.getType() + "\r\n");
            strLog.append(" Sensor Name - " + item.getName() + "\r\n");
            strLog.append(" Maximum Range - " + item.getMaximumRange() + "\r\n");
            strLog.append(" Resolution - " + item.getResolution() + "\r\n");
            strLog.append("\r\n");
            iIndex++;
        }
        System.out.println(strLog.toString());
    }
}