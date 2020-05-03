package com.samblancat.finder;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    double mylat0=0, mylng0=0;
    double mylat=0, mylng=0;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mContext=this;

        // on initialise le receiver de service/broadcast
        receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void closecmd(View view) {
        unregisterReceiver(receiver);
        stopService(new Intent(getBaseContext(), LocService.class));
        finish();
        //ASSURE FIN DU PROCESS !!!
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    //Recherche Immédiate position en cours
    public void setposcmd(View view) {
        //Sauve la Position Départ !
        Intent intent = new Intent(MainActivity.this, Scan.class);
        intent.putExtra("lat0", String.valueOf(mylat0));
        intent.putExtra("lng0", String.valueOf(mylng0));

        String tp = new DecimalFormat("#0.00").format(mylat0)+" / "+new DecimalFormat("##0.00").format(mylng0);
        Toast.makeText(this, "Goto :"+ tp, Toast.LENGTH_LONG).show();

        //Start Scanning Finder activity
        startActivity(intent);

        finish();
    }

    public void testjeu(View view) {
        //Lance jeu
        Intent intent = new Intent(MainActivity.this, Scan2.class);
        //Start Jeu Scan2
        startActivity(intent);
        finish();
    }

    //Goto la Position sauvegardée en xml shared pref
    public void gotosavedpos(View view) {
        //Récup la position en mém.
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        mylat = sharedPref.getFloat("mylat", 0);
        mylng = sharedPref.getFloat("mylng", 0);

        String tp = new DecimalFormat("#0.00").format(mylat)+" / "+new DecimalFormat("##0.00").format(mylng);
        Toast.makeText(this, "Goto old pos :"+tp, Toast.LENGTH_LONG).show();

        //Sauve la Position Voulue et Go !
        Intent intent = new Intent(MainActivity.this, Scan.class);
        intent.putExtra("lat0", String.valueOf(mylat));
        intent.putExtra("lng0", String.valueOf(mylng));
        //Start scanning
        startActivity(intent);
        finish();
    }

    //Sauve la Position actuelle pour Recherche Future
    public void storepos(View view) {
        //Sauve la Position Départ !
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("mylat", (float)mylat0);
        editor.putFloat("mylng", (float)mylng0);
        editor.apply();
        //Store ok !
        Toast.makeText(this, "Position saved!", Toast.LENGTH_LONG).show();
        String tp = new DecimalFormat("#0.00").format(mylat0)+" / "+new DecimalFormat("##0.00").format(mylng0);
        Toast.makeText(this, tp, Toast.LENGTH_LONG).show();
    }

    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la,lo,dist;

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            mylat =  (la!=null)?Double.parseDouble(la):0;
            mylng = (lo!=null)?Double.parseDouble(lo):0;

            if ((mylat!=0)&&(mylng!=0)) {
                double dk = Math.pow(Math.abs(mylat - mylat0), 2) + Math.pow(Math.abs(mylng - mylng0), 2);
                dk = 1000 * 111.12 * Math.sqrt(dk);
                mylat0 = mylat;
                mylng0 = mylng;
                if (dk < 100) dist = new DecimalFormat("#0.0").format(dk);
                else dist = "-";
                TextView ptxt = (TextView) findViewById(R.id.precistxt);
                ptxt.setText("Précision " + dist + " m");
            }
        }
    }
}