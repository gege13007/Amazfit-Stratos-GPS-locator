package com.samblancat.finder;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.os.Bundle;

public class GpsSetup extends AppCompatActivity  {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    public static int main_started = 0;
    public String gsv;

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
        Intent msgIntent = new Intent(mContext, com.samblancat.finder.LocService.class);
        mContext.startService(msgIntent);

        Log.e("Gpsetup:", "onCreate");
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        //Ne stoppe le service que si encore rien reçu !!! -> fermeture anticipée
        if (main_started==0) {
            stopService(new Intent(getBaseContext(), LocService.class));
            //ASSURE FIN DU PROCESS !!!
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        Log.e("Gpsetup:", "onDestroy");

        unregisterReceiver((BroadcastReceiver)receiver );
        receiver=null;
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    //Reçoit les datas du LocService (Lat, Lon, Gsv)
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        private Intent i;
        @Override
        public void onReceive(Context context, Intent intent) {
            gsv = intent.getStringExtra("Gsv");
            TextView txt = (TextView) findViewById(R.id.gsvtxt);
            txt.setText(gsv);

            String la =  intent.getStringExtra("Lat");
            String lo = intent.getStringExtra("Lon");
            if ((la!=null)&&(lo!=null))  {
                //Flag pour assurer un seul Main-activity
                if (main_started == 0) {
                    main_started = 1;
                    i = new Intent(GpsSetup.this, MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        }
    }
}
