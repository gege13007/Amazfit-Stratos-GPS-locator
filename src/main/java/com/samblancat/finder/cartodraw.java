package com.samblancat.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import static android.content.Context.MODE_PRIVATE;
import static com.samblancat.finder.Selectpos.decodeGPX;

public class cartodraw extends View {
    public Double wx , wy, cx, cy;
    public double altmax, altmin, altspan;
    public double altmoy;
    public double pixdegX, pixdegY;
    //dernière position Réelle
    public double dlat, dlon;
    //Position géo centre de l'écran
    public double clat, clon;
    //pos si nav
    public  double mylat0, mylng0;
    public String wptname="";
    public SharedPreferences sharedPref;
    public String gpxini;
    List<Location> gpxList=null;
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
        gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

        try { zoom = sharedPref.getInt("zoom", 7);
        //Retrouve last Position courante pour tri / distances
        clat = sharedPref.getFloat("clat", 43);
        clon = sharedPref.getFloat("clon",5);
        } catch (Exception e) {
            zoom = 8;
            clat = 43;
            clon = 5;
        }

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) return;
        String path = path0 +"/" + gpxini;
        final File gpxFile = new File(path);

        wx = (double) getResources().getDisplayMetrics().widthPixels;
        wy = (double) getResources().getDisplayMetrics().heightPixels;

        cx = wx/2;
        cy = wy/2;

        //Retrouve last position réelle
        dlat = sharedPref.getFloat("dlat", 43);
        dlon = sharedPref.getFloat("dlng",5);
        mylat0 = sharedPref.getFloat("lat0", 0);
        mylng0 = sharedPref.getFloat("lng0", 0);
        wptname = sharedPref.getString("nom", "");

        // Extrait la liste Array des 'name'
        gpxList = decodeGPX(gpxFile, dlat, dlon, 0);  // pas de tri !

        altmin=9999; altmax=-9999;
        //Calc echelle & altit moyenne
        for (int nn = 0; nn < gpxList.size(); nn++) {
            Location loc = gpxList.get(nn);
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
        float oldx= (float) 0;
        float oldy= (float) 0;
        int first=0;
        double x, y, z;

        //------------  AFFICHAGE DES TILES OSMap / Centre de l'Ecran  ---------------
        // Calcul des Dimensions tiles & scale
        double n = Math.pow(2, zoom);
        wtilex = 360 / n;
        wtiley = wtilex * Math.cos(Math.toRadians(dlat)); //  clat ? * 0.94444444

        //ReCalc de scale 'pixdeg' 256*2 -> 512 pix
        pixdegX = (bmp_x*256) / wtilex;
        pixdegY = (bmp_x*256) / wtiley;

        //Trouve tile centrale du centre écran
        double xt = Math.floor((n*(clon+180))/360);
        xtile = (int) xt;
        double la = Math.toRadians(clat);
        double yt = Math.tan(la)+(1/Math.cos(la));
        yt = 1 - ((Math.log(yt))/Math.PI);
        yt = (yt*n)/2;
        ytile = (int)(Math.rint(yt));

//        Log.d("Screen?", " ");
//        Log.d("Screen?", "xtile="+ xtile +" ytile="+ ytile);

        //Charge la Tile 0 principale du Centre écran
        getTile(canvas, xtile, ytile);

        //Test si manque un côté ?
        if (x0+(bmp_x*256) < wx)             // NOIR à Droite
            drawTile(canvas,xtile + 1, ytile,x0+(bmp_x*256),y0);
        if (x0 > 1)                           // NOIR à Gauche
            drawTile(canvas,xtile - 1, ytile,x0-(bmp_x*256),y0);
        if (y0+(bmp_x*256)<wy)              // NOIR en Bas
            drawTile(canvas, xtile, ytile + 1, x0,y0+(bmp_x*256));
        if (y0 > 1)                          // NOIR en Haut
            drawTile(canvas, xtile , ytile - 1, x0,y0-(bmp_x*256));
        // Si Manque un bloc en Coin ?
        if ((x0+(bmp_x*256)<wx) && (y0>1))              // NOIR en Haut à Droite
            drawTile(canvas,xtile + 1, ytile-1, x0+(bmp_x*256),y0-(bmp_x*256));
        if ((x0 > 1) && (y0 > 1))                       // NOIR en Haut à Gauche
            drawTile(canvas,xtile - 1, ytile-1, x0-(bmp_x*256),y0-(bmp_x*256));
        if ((x0 > 1) && (y0+(bmp_x*256)<wy))            // NOIR en Bas à Gauche
            drawTile(canvas,xtile-1, ytile + 1, x0-(bmp_x*256),y0+(bmp_x*256));
        if ((x0+(bmp_x*256)<wx)&&(y0+(bmp_x*256)<wy))    // NOIR en bas à Droite
            drawTile(canvas,xtile+1 , ytile + 1, x0+(bmp_x*256),y0+(bmp_x*256));

        //---------------   TRACE    DES   GPX   -----------------
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(1);

        //Display points
        int nbwpt = gpxList.size();
        //Increment de point à point
        int wptmodulo = 1;
        //Reduit le nb de points affichés à 200 maxi
        if (nbwpt>100) wptmodulo = (int) Math.floor(nbwpt / 200.0);
        double[] wptX = new double[nbwpt];
        double[] wptY = new double[nbwpt];
        double[] wptZ = new double[nbwpt];
        int nwptvis = 0;
        for (int nn = 0, countname=0; nn < nbwpt; nn+=wptmodulo, countname++) {
            Location loc = gpxList.get(nn);
            wptX[nn] = cx + (loc.getLongitude()-clon)*pixdegX;
            wptY[nn] = cy - (loc.getLatitude()-clat)*pixdegY;
            wptZ[nn] = loc.getAltitude();
            //compte les pts vraiment visibles
            if ( (wptX[nn]>0)&&(wptX[nn]<wx)&&(wptY[nn]>0)&&(wptY[nn]<wy) ) nwptvis++;
        }

        //Adapte nb display de wptnames max 1/8 nb de wpt visibles
        int namodulo = Math.round(nwptvis >> 3);
        //Dessin des points définitif
        for (int nn = 0, countname=0; nn < nbwpt; nn+=wptmodulo, countname++) {
            Location loc = gpxList.get(nn);
            x = wptX[nn];
            y = wptY[nn];
            z = wptZ[nn];
            //Pset le waypoint
            if ( (x>0)&&(x<wx)&&(y>0)&&(y<wy) ) {
                int rr=0, bb=0, gg;
                gg = (int) (255*((altspan - Math.abs(z-altmoy))/altspan));
                if (z>altmoy) rr = (int) (255*(z-altmoy)/(altspan));
                else bb = (int) (255*(altmoy-z)/(altspan));
                paint.setColor(Color.rgb(rr,gg,bb));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(Math.round(x), Math.round(y), 3, paint);
            }
            //Trace une droite ?
            paint.setStyle(Paint.Style.STROKE);
            if ( first>0 ) canvas.drawLine(oldx, oldy, (float)x, (float)y, paint);
            oldx=(float)x; oldy=(float)y;
            first=1;
            //Met le wptname ? (10 au maximum)
            if (countname > namodulo) countname=0;
            if (countname<1) {
                String wn = loc.getProvider();
                //N'AFFICHE PAS les noms du type "43.2210/5.2550"
                if (!wn.contains("/")) {
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                    paint.setTextSize(19);
                    canvas.drawText(wn, (float) x - 20, (float) y - 19, paint);
                }
            }
        }

        //Met POINT sur la position courante ?
        x = cx + (dlon - clon) * pixdegX;
        y = cy - ((dlat - clat) * pixdegY);
        if ( (x>0)&&(x<wx)&&(y>0)&&(y<wy) ) {
            //Pset le point actuel réel
            if (dlat!=0) paint.setColor(Color.BLUE); else paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawCircle(Math.round(x), Math.round(y), 7, paint);
            canvas.drawLine((float) (x - 9), (float) (y), (float) (x + 9), (float) (y), paint);
            canvas.drawLine((float) (x), (float) (y - 9), (float) (x), (float) (y + 9), paint);
        }

        //Dessine Trait à Destination ?
        if (!wptname.equals("")) {
            double x2 = cx + (mylng0 - clon) * pixdegX;
            double y2 = cy - ((mylat0 - clat) * pixdegY);
            //Pset le point actuel réel
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(2);
            canvas.drawLine((float) (x), (float) (y), (float) (x2), (float) (y2), paint);
            canvas.drawCircle(Math.round(x2), Math.round(y2), 5, paint);
        }

        Rect rt = new Rect(0, (int)Math.round(wy-68), (int)Math.round(wx), (int)Math.round(wy));
        paint.setColor(Color.WHITE);
        paint.setAlpha(105);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rt, paint);

        //Dessine boutons zooms & set center au milieu
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(45);
        canvas.drawText("-          +", (float) (cx-66), (float) (wy - 8), paint);
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
        canvas.drawText(tp, (float) 50, (float) (wy - 49), paint);
        canvas.drawText(Integer.toString(zoom), (float) (wx-74), (float) (wy - 49), paint);

        //Trace la règle
        paint.setStrokeWidth(2);
        float r0 = 53f;
        float r1 = (float)(r0+regle);
        canvas.drawLine(r0, (float)(wy-46), r1, (float)(wy-46), paint);
        canvas.drawLine(r0, (float)(wy-46), r0, (float)(wy-40), paint);
        canvas.drawLine(r1, (float)(wy-46), r1, (float)(wy-40), paint);
    }


    //Va charger la tile0 et retourne le Bitmap resizé !
    //Calcule les x0/y0 coin haut & gauche de la tile centrale
    public boolean getTile(Canvas canvas, int xtil, int ytil) {
        Bitmap myBitmap;
        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/OSMaps");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'OSMaps' directory !", Toast.LENGTH_LONG).show();
            return false;
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

        // Retrouve lat/lon coin haut/gauche de la tile (glat / glon)
        double n = Math.pow(2, zoom);
        double glon = ((xtil / n) * 360.0) - 180.0;
        double m = Math.PI - (2 * Math.PI * ytil) / n;
        double glat = Math.atan((0.5 * (Math.exp(m) - Math.exp(-m))));
        glat = Math.toDegrees(glat);

        //Calc pos Pixels de la tile0 (haut/gauche) (256*2 -> 512*512 pixels png)
        x0 = (float) (cx - 256 *(bmp_x*(clon - glon))/wtilex );
        y0 = (float) (cy + 256 *(bmp_x*(clat - glat))/wtiley );
        //Dessine la Tile 0
        canvas.drawBitmap(sizedBitmap, x0, y0, null);
        return true;
    }


    //Va charger et affiche la tile en Bitmap resizé !
    public int drawTile(Canvas canvas, int xtil, int ytil, float x0, float y0) {
        Bitmap myBitmap;
        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/OSMaps");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'OSMaps' directory !", Toast.LENGTH_LONG).show();
            return 0;
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
        return 1;
    }


    public void updateData(double dy, double dx) {
        dlat = dy;
        dlon = dx;
        invalidate();
    }


    public void StoreNewWpt(double dx, double dy) {
    final double nX, nY;

        nX = clon + ((dx - cx) / pixdegX);
        nY = clat + ((cy - dy) / pixdegY);

 //       String togo = new DecimalFormat("#0.0000").format(nY);
  //      togo += "/" + new DecimalFormat("##0.0000").format(nX);
  //      Toast.makeText(mContext, "Goto " + togo, Toast.LENGTH_LONG).show();

        //Demande confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.myALERT);
        builder.setTitle("Set mark");
        builder.setCancelable(false);
        builder.setMessage("New dest or new Wpt ?");
        builder.setPositiveButton("New Nav", new DialogInterface.OnClickListener() {
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
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("New Waypoint", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Store new Wpt
                mylat0 = nY;
                mylng0 = nX;
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //Annuler
                dialog.dismiss();
            }
        });
        AlertDialog alert11 = builder.create();
        alert11.show();

        invalidate();
    }


    public void centermap() {
        clat = dlat;
        clon = dlon;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("clat", (float) clat);
        editor.putFloat("clon", (float) clon);
        editor.apply();
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
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("clat", (float) clat);
        editor.putFloat("clon", (float) clon);
        editor.apply();
        invalidate();
    }

    public void zoomin() {
        //Max zoom 16 (aux environs de 10m)
        if (zoom < 16) {
            //Zoome
            zoom++;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("zoom", zoom);
            editor.apply();
            //ReTrace
            invalidate();
        }
    }

    public void zoomout() {
        //Zoom 3 minimum (pays)
        if (zoom > 3) {
            //dézoome
            zoom--;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("zoom", zoom);
            editor.apply();
            //ReTrace
            invalidate();
        }
    }

}
