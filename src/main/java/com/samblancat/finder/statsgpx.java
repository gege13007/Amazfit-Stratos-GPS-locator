package com.samblancat.finder;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.DateFormat;
import java.util.List;

import static com.samblancat.finder.Selectpos.decodeGPX;

public class statsgpx extends AppCompatActivity {
    Context mContext;
    SharedPreferences sharedPref;
    //Track visualisée
    List<Location> gpxList=null;
    //dernière position Réelle
    public double dlat, dlon;
    public double totdist=0, altmax, altmin;
    //infos duree
    public long topdepart=0, topfin=0, duree=0;

    @Override
    //Calcule et Affiche les Stats de glob.gpxini
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);

        //Retrouve last position réelle
        dlat = sharedPref.getFloat("dlat",  (float) 43.3);
        dlon = sharedPref.getFloat("dlng",(float) 5.2);

        //Fait la liste des gpx
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = dir0.toString() +"/" + glob.gpxini;
        final File gpxFile = new File(path);

        // Extrait la liste Array des 'name'
        gpxList = decodeGPX(gpxFile, dlat, dlon, 0);  // pas de tri !

        //Affiche la distance totale
        totdist=CalcDistance(gpxList);
        TextView dtxt = findViewById(R.id.disttxt);
        dtxt.setText("Dist "+glob.dtotext(totdist));

        dtxt = findViewById(R.id.altmaxtxt);
        dtxt.setText("Alt max "+Math.round(altmax)+"m");
        dtxt = findViewById(R.id.altmintxt);
        dtxt.setText("Alt min "+Math.round(altmin)+"m");
    }


    //Calcule distance du tracé mygpxList
    public double CalcDistance(List<Location> thegpx) {
        int nb;
        double llat, llon;
        double dist=0, d0, x0, y0;

        nb = thegpx.size();
        if (nb < 2) return(0);

        //fixe first point
        Location loc = thegpx.get(0);
        llat = loc.getLatitude();
        llon = loc.getLongitude();

        altmax=-99999;
        altmin=99999;

        //Gestion durée
        loc=thegpx.get(0);
        topdepart = loc.getTime();

        TextView dtxt = findViewById(R.id.datetxt);
        dtxt.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(topdepart));

        for (int nn = 0; nn < nb; nn++) {
            loc = thegpx.get(nn);
            //calc distance
            x0=Math.abs(llon - loc.getLongitude());
            x0*=Math.cos(Math.toRadians(llat));

            //gestion altitude
            double a=loc.getAltitude();
            if (a>altmax) altmax=a;
            if (a<altmin) altmin=a;

            y0=Math.abs(llat - loc.getLatitude());

            d0 = Math.sqrt((x0*x0)+(y0*y0));
            dist += d0;
            llat = loc.getLatitude();
            llon = loc.getLongitude();
        }

        //Fait la durée
        topfin = loc.getTime();
        duree=(topfin-topdepart)/1000;
        dtxt = findViewById(R.id.duratxt);
        dtxt.setText("Time "+glob.timeotext(duree));

        //retourne en m (ou km si >1000)
        dist *= 60*1852;

        //Fait Vitesse moyenne
        double vmoy = (3.6*dist) / duree;
        dtxt = findViewById(R.id.vmoytxt);
        dtxt.setText("Vmoy "+String.format("%.2f",vmoy)+" km/h");

        return(dist);
    }


    // Fermeture
    public void closestats(View view)
    {
        finish();
    }

}
