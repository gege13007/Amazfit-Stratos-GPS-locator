package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GpsSetup extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    public static int main_started = 0;
    public String gsv, lat, lon;
    public Intent I;
    public int cap=0, dcap=9;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder;
        setContentView(R.layout.waitgpsok);
        mContext = this;

        // on initialise le receiver de service/broadcast
        receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);

        // on lance le service
        Intent i = new Intent(mContext, com.samblancat.finder.LocService.class);
        mContext.startService(i);

        //Lance le Countdown de 10sec minimum avant lancement main...
        new CountDownTimer(3000, 50) {
            public void onTick(long millisUntilFinished) {
                TextView t=findViewById(R.id.progBarText);

                //Tourne la flèche
                ImageView imgview;
                imgview = findViewById(R.id.faisceau);
                imgview.setRotation((float) cap);

                cap += dcap;
                if (cap > 360) dcap=0;

   /*             if ((lat!=null)&&(lon!=null))  {
                    //Flag pour assurer un seul Main-activity
                    main_started = 1;
                    Intent mainI = new Intent(GpsSetup.this, MainActivity.class);
                    startActivity(mainI);
                    finish();
                }  */

            }

            //Fin du compte-a-rebours
            public void onFinish() {
                //Lance le MainActivity
                main_started = 1;
                Intent mainI = new Intent(GpsSetup.this, MainActivity.class);
                startActivity(mainI);
                finish();
            }
        }.start();
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        unregisterReceiver(receiver);

        //Ne stoppe le service que si encore rien reçu !!! -> fermeture anticipée
        if (main_started==0) {
            stopService(new Intent(getBaseContext(), LocService.class));
            //ASSURE FIN DU PROCESS !!!
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    //Reçoit les datas du LocService (Lat, Lon, Gsv)
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            gsv = intent.getStringExtra("Sat");
            if (gsv==null) gsv="00";
            TextView txt = findViewById(R.id.gsvtxt);
            txt.setText(gsv+" sats");

            lat =  intent.getStringExtra("Lat");
            lon = intent.getStringExtra("Lon");
        }
    }
}
