package com.samblancat.finder;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.text.DateFormat;

public class statsgpx extends AppCompatActivity  {
    Context mContext;

    //dernière position Réelle
    public double totdist=0, altmax, altmin;
    //infos duree
    public long topdepart=0, topfin=0, duree=0;

    @Override
    //Calcule et Affiche les Stats de glob.gpxini
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats);
        mContext = this;

        //Affiche la distance totale
        totdist=CalcDistance();

        TextView dtxt = findViewById(R.id.disttxt);
        dtxt.setText("Dist "+glob.dtotext(totdist));

        dtxt = findViewById(R.id.altit);
        dtxt.setText("Alt max "+Math.round(altmax)+"m /min "+Math.round(altmin)+"m");

        dtxt = findViewById(R.id.nbpoints);
        dtxt.setText(String.valueOf(glob.gpxList.size())+" points");
    }


    //Calcule distance du tracé mygpxList
    public double CalcDistance() {
        int nb;
        double llat, llon;
        double dist=0, d0, x0, y0;

        nb = glob.gpxList.size();
        if (nb < 2) return(0);

        //fixe first point
        Location loc = glob.gpxList.get(0);
        llat = loc.getLatitude();
        llon = loc.getLongitude();

        altmax=-99999;
        altmin=99999;

        //Gestion durée
        loc = glob.gpxList.get(0);
        topdepart = loc.getTime();

        TextView dtxt = findViewById(R.id.datetxt);
        dtxt.setText(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(topdepart));

        for (int nn = 0; nn < nb; nn++) {
            loc = glob.gpxList.get(nn);
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

        //Fait la durée & vmoy
        dtxt = findViewById(R.id.duratxt);
        TextView vtxt = findViewById(R.id.vmoytxt);

        if (topdepart!=0) {
            topfin = loc.getTime();
            duree = (topfin - topdepart) / 1000;
            dtxt.setText("Time " + glob.timeotext(duree));

            //Fait Vitesse moyenne
            double vmoy = (3.6*dist) / duree;
            vtxt.setText("Vmoy "+String.format("%.2f",vmoy)+" km/h");
        } else {
            dtxt.setVisibility(View.GONE);
            vtxt.setVisibility(View.GONE);
        }

        //retourne en m (ou km si >1000)
        dist *= 60*1852;
        return(dist);
    }


    // Fermeture
    public void closestats(View view)
    {
        finish();
    }

}
