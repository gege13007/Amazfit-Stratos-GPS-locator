package com.samblancat.finder;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
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

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    public double mylat0=0, mylng0=0;
    public double mylat=0, mylng=0, alt=0;
    private float x1,x2;
    static final int MIN_DISTANCE = 75;         // long swipe gauche avant finish()
    SharedPreferences sharedPref;
    public int blinking=1;

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

        //Fait clignoter "Waiting pos" tant que pas de fix
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(250);  // blinking time
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        TextView ptxt = (TextView) findViewById(R.id.precistxt);
        ptxt.startAnimation(anim);
        blinking=1;
    }


    //Detection du swipe sur gauche ou autre .... pour finish ?
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch(ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = ev.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    finish();
                }
                break;
        }
        return  super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //toujours arrete le receiver !
        unregisterReceiver(receiver);
        //Stoppe le service broadcast !
        stopService(new Intent(getBaseContext(), LocService.class));
        // Toast.makeText(mContext, "Bye...", Toast.LENGTH_LONG).show();   // le toast est caché par le killprocess
        //ASSURE FIN DU PROCESS !!!
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    // Fermeture close
    public void closecmd(View view) {
        finish();
    }

    // Réglages généraux
    public void setcmd(View view) {
        //Lance les reglages
        Intent intent = new Intent(MainActivity.this, reglages.class);
        startActivity(intent);
    }


    //Store & Recherche Immédiate position en cours
    public void setposcmd(View view) {
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        //Sauve la Position Départ !
        final Intent intent = new Intent(MainActivity.this, Scan.class);

        new AlertDialog.Builder(mContext)
                .setTitle("Create Wpt ?")
                .setCancelable(false)
                .setMessage("Save position to Wpt ?")
                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(R.string.oksave, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Confirme sauver new wpt
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                        String formatdate = df.format(c.getTime());
                        //prend mylat0/lng0 qui sont filtrés du 0.0
                        appendGPX(mylat0, mylng0, alt, formatdate);
                        Toast.makeText(mContext, "Position saved to "+formatdate, Toast.LENGTH_LONG).show();
                        //Sauve la Position Voulue et Go sur SCAN !
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putFloat("lat0", (float) mylat0);
                        editor.putFloat("lng0", (float) mylng0);
                        editor.putString("nom", formatdate);
                        editor.apply();
                        //Start Scanning Finder activity
                        startActivity(intent);
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.nosave,  new DialogInterface.OnClickListener() {
                    @SuppressLint("SimpleDateFormat")
                    public void onClick(DialogInterface dialog, int which) {
                        //Sauve la Position Voulue et Go sur SCAN !
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df;
                        df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                        String formatdate = df.format(c.getTime());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putFloat("lat0", (float) mylat0);
                        editor.putFloat("lng0", (float) mylng0);
                        editor.putString("nom", formatdate);
                        editor.apply();
                        //Start Scanning Finder activity
                        startActivity(intent);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    //Va chercher une position dans la liste et va suivre dans le GPX
    public void gotosavedpos(View view) {
        //Liste des Wpts & choix
        Intent intent = new Intent(MainActivity.this, Selectpos.class);
        startActivity(intent);
    }


    public void appendGPX(Double la, Double lo, Double ele, String nom){
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = path0 + "/" + sharedPref.getString("gpxini","gpxlocator.gpx");

        File gpx = new File(path);
        String path2 = path0 + "/gpslocator.tmp";
        File gpx2 = new File(path2);

        FileWriter fw = null;
        try {
            fw = new FileWriter(gpx2.getAbsoluteFile(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);

        try {
            BufferedReader br = new BufferedReader(new FileReader(gpx));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length()>8) {
                    if (line.substring(0, 8).equals("</trkseg")) {
                        //Insertion du wpt a la fin
                        bw.write("<trkpt lat=\""+la.toString()+"\" lon=\""+lo.toString()+"\">\r\n");
                        bw.write("<ele>"+ele.toString()+"</ele>\r\n");
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                        String formatdate = df.format(c.getTime());
                        df = new SimpleDateFormat("HH:mm:ss");
                        formatdate = formatdate+"T"+df.format(c.getTime());
                        bw.write("<time>"+formatdate+"Z</time>\r\n");
                        bw.write("<name>"+nom+"</name>\r\n");
                        bw.write("</trkpt>\r\n");
                    }
                }
                bw.write(line+"\r\n");
            }
            br.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
        //efface le source
        gpx.delete();
        //renomme le tmp en Gps
        gpx2.renameTo(gpx);
        if (gpx2.exists()) gpx2.delete();

        //Sync la Media-connection pour visu sur Windows usb
        MediaScannerConnection.scanFile(mContext,
                new String[]{gpx.getAbsolutePath()}, null, null);
    }


//Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo, dop, fix;
            int satok = 0;
            double precis;

            //Capte Qualité
            fix = intent.getStringExtra("Fix");
            dop = intent.getStringExtra("Hdop");

            TextView ptxt = findViewById(R.id.precistxt);

            if (fix!=null) {
                try {
                    satok = Integer.parseInt(fix);
                } catch (Exception e) {
                    satok = 0;
                }
                if (satok > 0) {
                    //ici ne pas faire la.isempty() ou la.length>0 !!!
                    try { precis = 7*Double.parseDouble(dop); } catch (Exception e) { precis = 99.0; }
                    String t = new DecimalFormat("##0.0").format(precis);
                    ptxt.setText("Precision " + t + "m");
                } else
                    ptxt.setText("Waiting Gps fix");

                if ((satok > 0) && (blinking > 0)) {
                    ptxt.clearAnimation();
                    blinking = 0;
                    Animation aniRotateClk = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
                    ImageButton img = findViewById(R.id.gotocmd);
                    img.startAnimation(aniRotateClk);
                }
                if ((satok < 1) && (blinking < 1)) {
                    //Fait clignoter "Waiting pos" tant que pas de fix
                    Animation anim = new AlphaAnimation(0.0f, 1.0f);
                    anim.setDuration(250);  // blinking time
                    anim.setStartOffset(120);
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setRepeatCount(Animation.INFINITE);
                    ptxt.startAnimation(anim);
                    blinking = 1;

                    ImageButton img = findViewById(R.id.gotocmd);
                    img.clearAnimation();
                }
            }

            //essaie de capter Altitude
            la = intent.getStringExtra("Alt");
            try { alt = Double.parseDouble(la); } catch (Exception e) {  }

            //essaie de capter Position
            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            try { mylat = Double.parseDouble(la); } catch (Exception e) {  }
            try { mylng = Double.parseDouble(lo); } catch (Exception e) {  }

            if ((mylat!=0)&&(mylng!=0)) {
                mylat0 = mylat;
                mylng0 = mylng;
            }
        }
    }
}