package com.samblancat.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import static android.content.Context.MODE_PRIVATE;
import static android.graphics.Color.rgb;
import static com.samblancat.finder.MainActivity.decodeGPX;


public class cartodraw extends View {
    public double wx , wy, cx, cy;
    public double altmax, altmin, altspan, altmoy;
    public double pixdegX, pixdegY;
    //dernière position Réelle
    public double dlat, dlon;
    //Position géo centre de l'écran
    public double clat, clon;
    //pos si nav
    public double mylat0, mylng0;
    public String wptname="";
    public SharedPreferences sharedPref;
    //nb de wpt dans liste en cours
    public int nbwpt=0;
    Context mContext;
    public int xtile, ytile, zoom;
    int bmp_x = 2;
    static float x0, y0;
    static double wtilex,  wtiley;

    public cartodraw(Context context) {
        super(context);
        double x, y, z;

        mContext = context;

        //Reprend le Gpx de base
        sharedPref = mContext.getSharedPreferences("POSPREFS", MODE_PRIVATE);
        glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

        //Retrouve last Position de la carte
        zoom = glob.mapzoom0;       // sharedPref.getInt("zoom", 9);
        clat = glob.maplat0;        // sharedPref.getFloat("clat", (float) 43.3);
        clon = glob.maplon0;        // sharedPref.getFloat("clon", (float) 5.2);
        if (glob.maplat0==0) {
            clat = 43;
            clon = 5;
        }
        if (glob.mapzoom0==0) {
            zoom = 9;
        }

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) return;
        String path = path0 +"/" + glob.gpxini;
        final File gpxFile = new File(path);
        if ( !gpxFile.exists() ) {
            Toast.makeText(mContext, "File not found !", Toast.LENGTH_LONG).show();
            return;
        }

        //Largeur et centre de l'écran
        wx = (double) getResources().getDisplayMetrics().widthPixels;
        wy = (double) getResources().getDisplayMetrics().heightPixels;
        cx = wx/2;
        cy = wy/2;

        //Retrouve last position réelle
        dlat = sharedPref.getFloat("dlat",  (float) 43.3);
        dlon = sharedPref.getFloat("dlng",(float) 5.2);

        mylat0 = sharedPref.getFloat("lat0",  (float) 43.3);
        mylng0 = sharedPref.getFloat("lng0", (float) 5.2);
        wptname = sharedPref.getString("nom", "");

        // La liste des Wpts est préchargée dans 'gpxlist'

        Log.e("TAG ", String.valueOf(glob.gpxList.size()));
        altmin=9999; altmax=-9999;
        //Calc echelle & altit moyenne
        for (int nn = 0; nn < glob.gpxList.size(); nn++) {
            Location loc = glob.gpxList.get(nn);
            z= loc.getAltitude();
            if (z>altmax) altmax=z;
            if (z<altmin) altmin=z;
        }
        altmoy=(altmax+altmin)/2;
        altspan=(altmax-altmin)/2;
        setKeepScreenOn(true);
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        float oldx = (float) 0;
        float oldy = (float) 0;
        int first = 0;
        double x, y, z;
        Calendar c;

        //------------  AFFICHAGE DES TILES OSMap / Centre de l'Ecran  ---------------
        // Calcul des Dimensions tiles & scale
        double n = Math.pow(2, zoom);
        wtilex = 360 / n;
        wtiley = wtilex * Math.cos(Math.toRadians(dlat)); //  clat ? * 0.94444444

        //ReCalc de scale 'pixdeg' 256*2 -> 512 pix
        pixdegX = (bmp_x * 256) / wtilex;
        pixdegY = (bmp_x * 256) / wtiley;

        //Trouve tile centrale du centre écran
        double xt = Math.floor((n * (clon + 180)) / 360);
        xtile = (int) xt;
        double la = Math.toRadians(clat);
        double yt = Math.tan(la) + (1 / Math.cos(la));
        yt = 1 - ((Math.log(yt)) / Math.PI);
        yt = (yt * n) / 2;
        ytile = (int) (Math.rint(yt));

        //Charge la Tile 0 principale du Centre écran
        getTile(canvas, xtile, ytile);

        //Limites en lat°/lon° de l'écran
        double lonG = clon + (0 - cx)/pixdegX;
        double lonD = clon + (wx - cx)/pixdegX;
        double latN = clat + (wy - cy)/pixdegY;
        double latS = clat + (0 - cy)/pixdegY;

        //Test si manque un côté ?
        if (x0 + (bmp_x * 256) < wx)             // NOIR à Droite
            drawTile(canvas, xtile + 1, ytile, x0 + (bmp_x * 256), y0);
        if (x0 > 1)                           // NOIR à Gauche
            drawTile(canvas, xtile - 1, ytile, x0 - (bmp_x * 256), y0);
        if (y0 + (bmp_x * 256) < wy)              // NOIR en Bas
            drawTile(canvas, xtile, ytile + 1, x0, y0 + (bmp_x * 256));
        if (y0 > 1)                          // NOIR en Haut
            drawTile(canvas, xtile, ytile - 1, x0, y0 - (bmp_x * 256));
        // Si Manque un bloc en Coin ?
        if ((x0 + (bmp_x * 256) < wx) && (y0 > 1))              // NOIR en Haut à Droite
            drawTile(canvas, xtile + 1, ytile - 1, x0 + (bmp_x * 256), y0 - (bmp_x * 256));
        if ((x0 > 1) && (y0 > 1))                       // NOIR en Haut à Gauche
            drawTile(canvas, xtile - 1, ytile - 1, x0 - (bmp_x * 256), y0 - (bmp_x * 256));
        if ((x0 > 1) && (y0 + (bmp_x * 256) < wy))            // NOIR en Bas à Gauche
            drawTile(canvas, xtile - 1, ytile + 1, x0 - (bmp_x * 256), y0 + (bmp_x * 256));
        if ((x0 + (bmp_x * 256) < wx) && (y0 + (bmp_x * 256) < wy))    // NOIR en bas à Droite
            drawTile(canvas, xtile + 1, ytile + 1, x0 + (bmp_x * 256), y0 + (bmp_x * 256));

        //---------------  TRACE en mode VISU GPX  -----------------
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);

        if ((!glob.tracking) && (glob.gpxList != null)) {
            //Display points
            nbwpt = glob.gpxList.size();
            //Increment de point à point
            int wptmodulo = 1;
            //Reduit le nb de points affichés à 200 maxi
            if (nbwpt > 100) wptmodulo = (int) Math.floor(nbwpt / 200.0);
            if (wptmodulo < 1) wptmodulo = 1;

            //Dessin des points définitifs
            int nbwptvis = 0;
            for (int nn = 0; nn < nbwpt; nn++) {
                Location loc = glob.gpxList.get(nn);
                //Pset le waypoint
                if ((loc.getLongitude() > lonG) && (loc.getLongitude() < lonD)) {
                    if ((loc.getLatitude() > latS) && (loc.getLatitude() < latN)) {
                        nbwptvis++;
                    }
                }
            }
            //Adapte nb display de wptnames modulo = 1/8 nb des pt visibles
            int namodulo = Math.round(nbwptvis / 5);
            int nbwptvu=0;
            for (int nn = 0, countname = 0; nn < nbwpt; nn++, countname++) {
                Location loc = glob.gpxList.get(nn);
                //Pset le waypoint
                if ( (loc.getLongitude() > lonG) && (loc.getLongitude() < lonD) ) {
                    if ( (loc.getLatitude() > latS) && (loc.getLatitude() < latN) ) {
                        x = cx + (loc.getLongitude() - clon) * pixdegX;
                        y = cy - (loc.getLatitude() - clat) * pixdegY;
                        z = loc.getAltitude();
                        int rr = 0, bb = 0, gg;
                        if (z==0)
                            paint.setColor(rgb(0xfc, 0x80, 0));
                        else {
                            gg = (int) (255 * ((altspan - Math.abs(z - altmoy)) / altspan));
                            if (z > altmoy) rr = (int) (255 * (z - altmoy) / (altspan));
                            else bb = (int) (255 * (altmoy - z) / (altspan));
                            paint.setColor(rgb(rr, gg, bb));
                        }
                        paint.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(Math.round(x), Math.round(y), 4, paint);

                        //Trace une droite ?
                        if (glob.NbTrk > 0) {
                            paint.setColor(rgb(0x70, 0x70, 0x70));
                            if (first > 0) {
                                double dd = Math.abs(oldx - x) + Math.abs(oldy - y);
                                if (dd < 200)
                                    canvas.drawLine(oldx, oldy, (float) x, (float) y, paint);
                            }
                        }

                        oldx = (float) x;
                        oldy = (float) y;
                        first = 1;

                        //Met le wptname ? (10 au maximum)
                        if (countname > namodulo) countname = 0;
                        if ( (countname < 1) && (glob.shownames>0) ) {
                            String wn = loc.getProvider();
                            //N'AFFICHE PAS les noms du type "43.2210/5.2550"
                            if (!wn.contains("/")) {
                                paint.setColor(rgb(0x70, 0x70, 0x70));
                                paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                                paint.setTextSize(19);
                                canvas.drawText(wn, (float) x - 20, (float) y - 19, paint);
                                nbwptvu++;
                            }
                        }
                    }
                }
            }
            Log.e("nbwptvis= ", String.valueOf(nbwptvis));
            Log.e("nbwptvu= ", String.valueOf(nbwptvu));
        }
        //------------------ TRACE Tracking en LIVE ---------------------------
        else if (glob.gpxList != null) {
            //Display points
            nbwpt = glob.gpxList.size();
            if (nbwpt > 1) {
                //Increment de point à point
                int wptmodulo = 1;
                //Reduit le nb de points affichés à 200 maxi
                if (nbwpt > 100) wptmodulo = (int) Math.floor(nbwpt / 200.0);
                if (wptmodulo < 1) wptmodulo = 1;
                double[] wptX = new double[nbwpt];
                double[] wptY = new double[nbwpt];
                int nwptvis = 0;
                for (int nn = 0; nn < nbwpt; nn += wptmodulo) {
                    Location loc = glob.gpxList.get(nn);
                    wptX[nn] = cx + (loc.getLongitude() - clon) * pixdegX;
                    wptY[nn] = cy - (loc.getLatitude() - clat) * pixdegY;
                    //compte les pts vraiment visibles
                    if ((wptX[nn] > 0) && (wptX[nn] < wx) && (wptY[nn] > 0) && (wptY[nn] < wy))
                        nwptvis++;
                }

                //Adapte nb display de wptnames max 1/8 nb de wpt visibles
                int namodulo = Math.round(nwptvis / 5);
                //Dessin des points
                for (int nn = 0; nn < nbwpt; nn += wptmodulo) {
                    Location loc = glob.gpxList.get(nn);
                    x = wptX[nn];
                    y = wptY[nn];
                    //Pset le waypoint
                    if ((x > 0) && (x < wx) && (y > 0) && (y < wy)) {
                        paint.setStrokeWidth(3);
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawCircle(Math.round(x), Math.round(y), 2, paint);
                    }
                    //Trace une droite ?
                    paint.setStyle(Paint.Style.FILL);
                    if (first > 0) canvas.drawLine(oldx, oldy, (float) x, (float) y, paint);
                    oldx = (float) x;
                    oldy = (float) y;
                    first = 1;
                }
            }
        }

        //Met POINT sur la position courante ?
        x = cx + (dlon - clon) * pixdegX;
        y = cy - ((dlat - clat) * pixdegY);
        if ((x > 0) && (x < wx) && (y > 0) && (y < wy)) {
            //Pset le point actuel réel
            if (glob.gpsfix > 0)
                paint.setColor(Color.GREEN);
            else
                paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(2);
            canvas.drawCircle(Math.round(x), Math.round(y), 8, paint);
            canvas.drawLine((float) (x - 12), (float) (y), (float) (x + 12), (float) (y), paint);
            canvas.drawLine((float) (x), (float) (y - 12), (float) (x), (float) (y + 12), paint);
        }

        //Recentrage auto en mode track saving
        if (glob.gpxList != null) {
//            if ( (Math.abs(x -cx) + Math.abs(y-cy)) > (cx+cy)/2.2 ) {
//                centermap();
//            }
        }

        //Dessine Trait à Destination ?
        if (!wptname.equals("")) {
            double x2 = cx + (mylng0 - clon) * pixdegX;
            double y2 = cy - ((mylat0 - clat) * pixdegY);
            //Pset le point actuel réel
            Paint paint2 = new Paint();
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setColor(Color.BLUE);
            paint2.setStrokeWidth(3);
   //         paint2.setPathEffect(new DashPathEffect(new float[]{10,10},20));
   //         paint2.setAntiAlias(true);
            canvas.drawLine((float) (x), (float) (y), (float) (x2), (float) (y2), paint2);
            canvas.drawCircle(Math.round(x2), Math.round(y2), 6, paint);
        }

        Rect rt = new Rect(0, (int)Math.round(wy-68), (int)Math.round(wx), (int)Math.round(wy));
        paint.setColor(Color.WHITE);
        paint.setAlpha(100);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rt, paint);

        //Dessine boutons zooms & set center au milieu
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(45);
        canvas.drawText("-          +", (float) (cx-66), (float) (wy - 15), paint);
        //Affiche l'heure en haut
        c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        paint.setTextSize(22);
        canvas.drawText(df.format(c.getTime()), (float) (cx-26), (float) (28), paint);

        //Center picto
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle((float) (cx+0), (float) (wy-20), 10, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle((float) (cx-0), (float) (wy-20), 6, paint);
        canvas.drawLine((float) (cx-14), (float) (wy-20), (float) (cx + 14), (float) (wy-20), paint);
        canvas.drawLine((float) (cx+0), (float) (wy-34), (float) (cx-0), (float) (wy-6), paint);

        //Ecris l'échelle
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        paint.setTextSize(24);
        //Distance pour 90 pixels = règle
        double regle = 212;
        Double km=111319.5*(regle/pixdegX)*Math.cos(Math.toRadians(clat));
        String tp;
        if (km < 1000) {
            tp = new DecimalFormat("###0").format(km)+" m";
        } else {
            // en km
            km = km / 1000;
            if (km < 10) tp = new DecimalFormat("0.00").format(km)+" km";
            else tp = new DecimalFormat("###0").format(km)+" km";
        }

        //hauteur de la regle du bas
        float ry =  (float)((wx+wy)*0.408);
        canvas.drawText(tp, (float) 50, (float) (ry - 3), paint);
        canvas.drawText(Integer.toString(zoom), (float) (wx-74), ry - 3, paint);

        //Trace la règle du bas
        paint.setStrokeWidth(2);
        float r0 = (float)(cx-(regle / 2));
        float r1 = (float)(cx+(regle / 2));
        canvas.drawLine(r0, ry, r1, ry, paint);
        canvas.drawLine(r0, ry, r0, ry+4, paint);
        canvas.drawLine(r1, ry, r1, ry+4, paint);

        //-------------------------------------------------------
        //Aff données sur MAP incrustation 1
        if (glob.modevue==1) {
            paint.setColor(rgb(0, 0, 65));
            paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            //Affiche Vitesse km/h
            ry = (float) (wy * 0.36);
            paint.setTextSize(20);
            canvas.drawText("Spd", (float) (cx-100), ry-20, paint);
            paint.setTextSize(48);
            String vt = new DecimalFormat("0.0").format(glob.vtg);
            if (glob.gpsfix>0)
                canvas.drawText(vt+" km/h", (float) (cx-64), ry, paint);
            else
                canvas.drawText("- km/h", (float) (cx-64), ry, paint);

            //Distance cumulée track
            if (glob.tracking){
                paint.setTextSize(20);
                canvas.drawText("Dst", (float) (cx-95), ry+30, paint);
                paint.setTextSize(48);
                vt = glob.dtotext(glob.realdist);
                canvas.drawText(vt, (float) (cx - 60), ry+50, paint);
            }

            //Altitude
            paint.setTextSize(20);
            canvas.drawText("Alt", (float) (cx-95), ry+80, paint);
            paint.setTextSize(48);
            vt = glob.dtotext(glob.lastalt);
            if (glob.gpsfix>0)
              canvas.drawText(vt, (float) (cx - 60), ry+100, paint);
            else
              canvas.drawText("- m", (float) (cx - 60), ry+100, paint);
        }
    }


    //Va charger la tile0 et retourne le Bitmap resizé !
    //Calcule les x0/y0 coin haut & gauche de la tile centrale
    public void getTile(Canvas canvas, int xtil, int ytil) {
        Bitmap myBitmap;
        int cote;

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/OSMaps");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'OSMaps' directory !", Toast.LENGTH_LONG).show();
            return;
        }

        String tilename = path0 + "/" + zoom + "/" + xtil + "/" + ytil + ".png";
        File file = new File(tilename);
        if (file.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            myBitmap = (BitmapFactory.decodeFile(new File(tilename).getPath(), options));
            cote = 256;
        } else {
            //si rien met les carreaux
            myBitmap = (BitmapFactory.decodeResource(getContext().getResources(), R.drawable.emptytile256));
            cote = 256;
        }

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale((float) bmp_x, (float) bmp_x);
        Bitmap sizedBitmap = Bitmap.createBitmap(
                myBitmap, 0, 0, cote, cote, matrix, false);
        myBitmap.recycle();

        // Retrouve lat/lon coin haut/gauche de la tile (glat / glon)
        double n = Math.pow(2, zoom);
        double glon = ((xtil / n) * 360.0) - 180.0;
        double m = Math.PI - (2 * Math.PI * ytil) / n;
        double glat = Math.atan((0.5 * (Math.exp(m) - Math.exp(-m))));
        glat = Math.toDegrees(glat);

        //Calc pos Pixels de la tile0 (haut/gauche) (256*2 -> 512*512 pixels png)
        x0 = (float) (cx - cote *(bmp_x*(clon - glon))/wtilex );
        y0 = (float) (cy + cote *(bmp_x*(clat - glat))/wtiley );
        //Dessine la Tile 0
        canvas.drawBitmap(sizedBitmap, x0, y0, null);
    }


    //Va charger et affiche la tile en Bitmap resizé !
    public void drawTile(Canvas canvas, int xtil, int ytil, float x0, float y0) {
        Bitmap myBitmap;
        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/OSMaps");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'OSMaps' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String tilename = path0 + "/" + zoom + "/" + xtil + "/" + ytil + ".png";
        File file = new File(tilename);
        if (file.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            myBitmap = (BitmapFactory.decodeFile(new File(tilename).getPath(), options));
        } else
            myBitmap = (BitmapFactory.decodeResource(getContext().getResources(), R.drawable.emptytile256));
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale((float) bmp_x, (float) bmp_x);
        Bitmap sizedBitmap = Bitmap.createBitmap(
                myBitmap, 0, 0, 256, 256, matrix, false);
        myBitmap.recycle();

        //Dessine la Tile 0
        canvas.drawBitmap(sizedBitmap, x0, y0, null);
    }


   //Rafraichis la carte avec dlat / dlon
    public void updateData(double dy, double dx) {
        dlat = dy;
        dlon = dx;
        invalidate();
    }


    //MENU CHOIX si NEW WPT / NEW TRACK / Annuler
    public void StoreNewWpt(double dx, double dy) {
    final double nX, nY;
        nX = clon + ((dx - cx) / pixdegX);
        nY = clat + ((cy - dy) / pixdegY);
  //     Toast.makeText(mContext, "Goto " + togo, Toast.LENGTH_LONG).show();

        //Demande confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.myALERT);
        builder.setTitle("New track/wpt");
        builder.setCancelable(false);
        builder.setMessage("Store new Wpt or new Track ?");
        builder.setPositiveButton("New Wpt", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Met le wptname
                SharedPreferences.Editor editor = sharedPref.edit();
                mylat0 = nY;
                mylng0 = nX;
                editor.putFloat("lat0", (float) mylat0);
                editor.putFloat("lng0", (float) mylng0);
                //Sauve la Position Voulue et Go sur SCAN !
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                wptname = df.format(c.getTime());
                editor.putString("nom", wptname);
                editor.apply();
                glob.appendGPX(mContext, mylat0, mylng0, glob.lastalt, glob.gpxini);
                Toast.makeText(mContext, "New Wpt to go saved to " + glob.gpxini, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        if (!glob.tracking) {
            builder.setNegativeButton("Start new Track", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    glob.tracking = true;
                    //start list
                    glob.gpxList = new ArrayList<>();
                    //First point
                    glob.lastlat=dlat;
                    glob.lastlon=dlon;
                    //Sauve mode Tracking en cours
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putInt("track", 1);
                    editor.apply();
                    dialog.dismiss();
                }
            });
        } else {
            builder.setNegativeButton("Stop Track", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //Demande confirmation Stop tracking save ?
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.myALERT);
                    builder.setTitle("Stop track");
                    builder.setCancelable(false);
                    builder.setMessage("Save new Track ?");
                    builder.setPositiveButton("Save Gpx", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Sauve la track
                            glob.tracking = false;
                            //Store new GPX Wpt file
                            Calendar c = Calendar.getInstance();
                            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
                            String formatdate = df.format(c.getTime());
                            String nom = "gs" + "-" + formatdate + ".gpx";
                            WriteMyGPX(nom);

                            Toast.makeText(mContext, glob.gpxList.size()+" points", Toast.LENGTH_LONG).show();

                            //Sauve Fin de Tracking
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt("track", 0);
                            //sauve comme nouveau gpxini
                            editor.putString("gpxini", nom);
                            glob.gpxini = nom;
                            editor.apply();
                            dialog.dismiss();
                            //Aff les stats de track
                            Intent intent = new Intent(mContext, statsgpx.class);
                            mContext.startActivity(intent);
                        }
                    });
                    builder.setNegativeButton("Discard track", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            glob.tracking = false;
                            //Sauve Fin de Tracking
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt("track", 0);
                            editor.apply();
                            dialog.dismiss();
                        }
                    });
                    //Annuler
                    builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert11 = builder.create();
                    alert11.show();
                }
            });
        }
        builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            //Annuler
            dialog.dismiss();
            }
        });
        AlertDialog alert12 = builder.create();
        alert12.show();
        invalidate();
    }


    public void centermap() {
        clat = dlat;
        clon = dlon;
                                    // SharedPreferences.Editor editor = sharedPref.edit();
        glob.mapzoom0 = zoom;       // editor.putFloat("clat", (float) clat);
        glob.maplat0 = clat;        // editor.putFloat("clon", (float) clon);
        glob.maplon0 = clon;        //editor.apply();
        invalidate();
    }


    public void shift(double dx, double dy) {
        clat += dy/pixdegY;
        clon -= dx/pixdegX;
        //Check positions
        if (clat>89.5) clat=89.5;
        if (clat<-89.5) clat=-89.5;
        if (clon>180) clon=-179.999;
        if (clon<-180) clon=179.999;
        // SharedPreferences.Editor editor = sharedPref.edit();
        glob.mapzoom0 = zoom;       // editor.putFloat("clat", (float) clat);
        glob.maplat0 = clat;        // editor.putFloat("clon", (float) clon);
        glob.maplon0 = clon;        //editor.apply();
        invalidate();
    }


    public void zoomin() {
        //Max zoom = 17 (aux environs de 5m)
        if (zoom <= 16) {
            //Zoome
            zoom++;
            glob.mapzoom0 = zoom;
            //ReTrace
            invalidate();
        }
    }

    public void zoomout() {
        //Zoom 3 minimum (pays)
        if (zoom > 3) {
            //dézoome
            zoom--;
            glob.mapzoom0 = zoom;
            //ReTrace
            invalidate();
        }
    }

    //Ajoute un wpt au fichier gpx
    public void WriteMyGPX(String nom) {

        int nb = glob.gpxList.size();
        if (nb==0) return;

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formatdate = df.format(c.getTime());

        File dir0 = new File(Environment.getExternalStorageDirectory().toString() + "/gpxdata");
        String path0 = dir0.toString();
        if (!dir0.exists()) {
            Toast.makeText(mContext, "No gpxdata directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = path0 + "/" + nom;
        File gpx = new File(path);

        FileWriter fw = null;
        try {
            fw = new FileWriter(gpx.getAbsoluteFile(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);

        try {
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n");
            bw.write("<gpx xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/11.xsd\"\r\n");
            bw.write("xmlns=\"http://www.topografix.com/GPX/1/1\"\r\n");
            bw.write("xmlns:ns3=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"\r\n");
            bw.write("xmlns:ns2=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"\r\n");
            bw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
            bw.write("xmlns:ns1=\"http://www.cluetrust.com/XML/GPXDATA/1/0\"\r\n");
            bw.write("creator=\"Huami Amazfit Sports Watch\" version=\"1.1\">\r\n");
            bw.write("<metadata>\r\n");
            bw.write("<name>Amazfit GPS Tracker</name>\r\n");
            bw.write("<author>\r\n");
            bw.write("<name>Samblancat Stratos 3</name>\r\n");
            bw.write("</author>\r\n");
            bw.write("<time>"+formatdate+"</time>\r\n");
            bw.write("</metadata>\r\n");
            bw.write("<trk>\r\n");
            bw.write("<name>Track "+formatdate+"</name>\r\n");
            bw.write("<trkseg>\r\n");

            for (int nn = 0; nn < nb; nn++) {
                Location loc = glob.gpxList.get(nn);
                //Insertion du wpt a la fin
                bw.write("<trkpt lat=\"" + loc.getLatitude() + "\" lon=\"" + loc.getLongitude() + "\">\r\n");
                bw.write("<ele>" + loc.getAltitude() + "</ele>\r\n");
                bw.write("<time>" + loc.getProvider() + "</time>\r\n");
                bw.write("<name>" + nn + "</name>\r\n");
                bw.write("</trkpt>\r\n");
            }

            bw.write("</trkseg>\r\n");
            bw.write("</trk>\r\n");
            bw.write("</gpx>\r\n");
            bw.close();
            if (fw != null) fw.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        //Sync la Media-connection pour visu sur Windows usb
        MediaScannerConnection.scanFile(mContext,
                new String[]{gpx.getAbsolutePath()}, null, null);
    }
}
