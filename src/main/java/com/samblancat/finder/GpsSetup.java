package com.samblancat.finder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.samblancat.finder.MainActivity.decodeGPX;

public class GpsSetup extends AppCompatActivity {
    SharedPreferences sharedPref;
    Context mContext;
    public static int main_started = 0;
    public int cap = 0, dcap = 30;
    public Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.waitgpsok);
        mContext = this;

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);

        //pour raz de la vitesse
        glob.oldlat = 0;
        glob.oldlon = 0;

        glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

        TextView ptxt = findViewById(R.id.gpxnametxt);
        ptxt.setText(glob.gpxini);

        startTimer();
        new LongOperation().execute("");
    }

    public void startTimer() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Tourne la flèche
                        ImageView imgview;
                        imgview = findViewById(R.id.faisceau);
                        imgview.setRotation((float) cap);
                        cap += dcap;
                        if (cap > 360) cap -= 360;
                        //affiche avancement chargement GPX
                        if (glob.gpx_progress>0) {
                            TextView ptxt = findViewById(R.id.progtxt);
                            ptxt.setText(String.valueOf(glob.gpx_progress)+" wpt");
                        }
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 300);
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        //Ne stoppe le service que si encore rien reçu !!! -> fermeture anticipée
        if (main_started == 0) {
            stopService(new Intent(getBaseContext(), LocService.class));
            //ASSURE FIN DU PROCESS !!!
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    private class LongOperation extends AsyncTask<String, Void, String> {
        private double lat, lon;

        @Override
        protected String doInBackground(String... params) {

            //Fait la liste des gpx
            File dir0 = new File(Environment.getExternalStorageDirectory().toString() + "/gpxdata");
            if (!dir0.exists()) {
                Toast.makeText( mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            }
            String path = dir0.toString() + "/" + glob.gpxini;
            final File gpxFile = new File(path);

            //Retrouve last position réelle
            lat = sharedPref.getFloat("dlat", (float) 43.3);
            lon = sharedPref.getFloat("dlng", (float) 5.2);

            // Extrait la liste Array des 'name'
            decodeGPX(gpxFile, lat, lon);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            timer.cancel();
            timer.purge();

            TextView ptxt = findViewById(R.id.progtxt);
            ptxt.setText(" ");
            ptxt = findViewById(R.id.gpxnametxt);
            ptxt.setText(" ");

            // on lance le service
            Intent i = new Intent(mContext, com.samblancat.finder.LocService.class);
            mContext.startService(i);

            //Lance le MainActivity
            main_started = 1;
            Intent mainI = new Intent(GpsSetup.this, MainActivity.class);
            startActivity(mainI);
            finish();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}