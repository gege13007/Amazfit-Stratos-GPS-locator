package com.samblancat.finder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.samblancat.finder.MainActivity.decodeGPX;

public class waitloadgpx extends AppCompatActivity {
    Context mContext;
    SharedPreferences sharedPref;
    public double dlat, dlon;
    public Timer timer;
    public int cap=0, dcap=30;

    @Override
//Calcule et Affiche les Stats de glob.gpxini
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.waitgpsok);

        mContext=this;

        TextView ptxt = findViewById(R.id.progtxt);
        ptxt.setText(".");

        ptxt = findViewById(R.id.gpxnametxt);
        ptxt.setText(glob.gpxini);

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);

        startTimer();

        new LongOperation().execute("");
    }

    @Override
    protected void onPause() {
        //Auto-generated method stub
        super.onPause();

        TextView ptxt = findViewById(R.id.progtxt);
        ptxt.setText(".");
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

    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            //Fait la liste des gpx
            File dir0 = new File(Environment.getExternalStorageDirectory().toString() + "/gpxdata");
            if (!dir0.exists()) {
                Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            }
            String path = dir0.toString() + "/" + glob.gpxini;
            final File gpxFile = new File(path);

            //Retrouve last position réelle
            dlat = sharedPref.getFloat("dlat",  (float) 43.3);
            dlon = sharedPref.getFloat("dlng",(float) 5.2);

            // Extrait la liste Array des 'name'
            decodeGPX(gpxFile, dlat, dlon);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            timer.cancel();
            timer.purge();

            TextView ptxt = findViewById(R.id.progtxt);
            ptxt.setText(".");
            ptxt = findViewById(R.id.gpxnametxt);
            ptxt.setText(" ");

            finish();

            //Aff les stats de track
            Intent intent = new Intent(mContext, statsgpx.class);
            mContext.startActivity(intent);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

}
