package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.os.Bundle;

public class GpsSetup extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    public static int main_started = 0;
    public String gsv;
    public Intent I;
    public int cap=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.waitgpsok);
        mContext = this;

        // on initialise le receiver de service/broadcast
        receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);

        // on lance le service
        Intent I = new Intent(mContext, com.samblancat.finder.LocService.class);
        mContext.startService(I);

        //Lance le Countdown de 10sec minimum avant lancement main...
        new CountDownTimer(18000, 50) {
            public void onTick(long millisUntilFinished) {
                TextView t=findViewById(R.id.progBarText);
                int sec=Math.round(millisUntilFinished / 1000);
       /*         if ((sec % 3)==0)
                    t.setText("        ");
                else
                    t.setText("Waiting GPS fix"); */

                //Tourne la flèche
                ImageView imgview;
                imgview = (ImageView) findViewById(R.id.faisceau);
                imgview.setRotation((float) cap);
                cap+=8;
            }
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
            //le toast est invisible a cause du killprocess
           //  Toast.makeText(mContext, "Bye...", Toast.LENGTH_LONG).show();
            stopService(new Intent(getBaseContext(), LocService.class));
            //ASSURE FIN DU PROCESS !!!
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

    //Reçoit les datas du LocService (Lat, Lon, Gsv)
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        private Intent i;
        @Override
        public void onReceive(Context context, Intent intent) {
            gsv = intent.getStringExtra("Sat");
            TextView txt = findViewById(R.id.gsvtxt);
            txt.setText(gsv+" sats");

            String la =  intent.getStringExtra("Lat");
            String lo = intent.getStringExtra("Lon");
            if ((la!=null)&&(lo!=null))  {
                //Flag pour assurer un seul Main-activity
                if (main_started == 0) {
                    main_started = 1;
                    I = new Intent(GpsSetup.this, MainActivity.class);
                    startActivity(I);
                    finish();
                }
            }
        }
    }
}
