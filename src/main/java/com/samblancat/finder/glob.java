package com.samblancat.finder;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.os.Environment;
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
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

//Variables Globales pour enregistrement des Wpt de la trace en live !!!
public class glob {
    //Si en cours de stockage nav ?
    static public boolean tracking = false;
    //no du nouveau point stocké
    static public int nbtracking = 0;
    //Track list en cours d'acquisition
    static public List<Location> gpxList = null;

    //info sur le GPX en cours
    static public int NbPts=0;
    static public int NbTrk=0;   // si 1 c'est un TRKSEG

    //Centre de l'écran map (sauvé ou changé avec decodeGPX)
    static public double maplat0, maplon0;
    static public int mapzoom0;

    //dernière position sauvée
    static public double lastlat = 0, lastlon = 0, lastalt=0;
    //dernier point pour calc Distance
    static public double oldlat, oldlon, oldalt;
    // =1 Fix GPS ok , =0 no gps !
    static public int gpsfix = 0;
    // Facteur HDOP de precision GPS
    static public Double hdop = 99.0;
    // Nb de satellites en vue
    static public String gsv = "";
    //Vitesse en km/h de VTG
    static public double vtg = 0;

    //Changement mode vue carto - 0 carte seule - 1 carte incrust - 2 sortie map
    static public int modevue=0;

    //Flag si show Wpt names sur Maps
    static public int shownames=0;

    // Fichier gpx en cours de visu (sauvé en sharedpref)
    static public String gpxini;

    //Pour calc distance entre 2 last GPS points
    static public double realspeed=0;         // vitesse
    static public double realdist=0;          // distance cumulée du track

    //pour progress bar -> decodeGPX
    static public int gpx_progress=0;

    //pour tests divers
    static public int debug=0;

    //Transforme le time long en sec -> duree en texte
    static public String timeotext(long duree) {
        String tt="";
        int secs = (int) (duree % 60);
        int mins = (int) (((duree) / 60) % 60);
        int hours = (int) (((duree) / 60) / 60);
        if (hours>0) tt = String.format("%02dh", hours);
        if (mins>0) tt += String.format("%02dm", mins);
        tt +=  String.format("%02ds", secs);
        return(tt);
    }


    //Transforme la distance dk en texte (m ou km)
    static public String dtotext(double dk) {
        String tp;

        if (dk < 1000) {
            tp = new DecimalFormat("###0").format(dk);
            return(tp + " m");
        } else {
            // en km
            dk = dk / 1000;
            if (dk < 10) tp = new DecimalFormat("0.00").format(dk);
            else {
                if (dk < 100)
                    tp = new DecimalFormat("#0.0").format(dk);
                else
                    tp = new DecimalFormat("###0").format(dk);
            }
            return(tp + " km");
        }
    }


    //Ajoute un wpt au fichier gpx
    static public void appendGPX(Context mContext, Double la, Double lo, Double ele, String nom){
       SharedPreferences sharedPref;
        sharedPref = mContext.getSharedPreferences("POSPREFS", MODE_PRIVATE);

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
            if (fw != null) fw.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
        //efface le source
        boolean b =gpx.delete();
        //renomme le tmp en Gps
        b = gpx2.renameTo(gpx);
        if (gpx2.exists()) b=gpx2.delete();

        //Sync la Media-connection pour visu sur Windows usb
        MediaScannerConnection.scanFile(mContext,
                new String[]{gpx.getAbsolutePath()}, null, null);
    }
}