package com.samblancat.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import static android.content.Context.MODE_PRIVATE;
import static com.samblancat.finder.Selectpos.decodeGPX;

public class cartodraw extends View {
    public Double wx , wy, cx, cy;
    public double lamax, lomax, lamin, lomin, altmax, altmin, altspan;
    public double lamoy, lomoy, altmoy;
    public double mylat0=0, mylng0=0;
    public double pixdeg;
    public double shiftx=0, shifty=0;
    public double dlat, dlon;
    public int compasok=0, angle=0;
    public SharedPreferences sharedPref;
    public String gpxini;
    List<Location> gpxList=null;
    Context mContext;
    public int counterpos=0;

    public cartodraw(Context context) {
        super(context);
        double x, y, z;

        mContext = context;

        //Reprend le Gpx de base
        sharedPref = cartodraw.this.getContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");
        compasok = sharedPref.getInt("compas", 0);

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(cartodraw.this.getContext(), "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = path0 +"/" + gpxini;
        final File gpxFile = new File(path);

        wx = (double) getResources().getDisplayMetrics().widthPixels;
        wy = (double) getResources().getDisplayMetrics().heightPixels;
        cx = wx/2;
        cy = wy/2;

        //Retrouve last Position courante pour tri / distances
        mylat0 = sharedPref.getFloat("dlat", 43);
        mylng0 = sharedPref.getFloat("dlng",5);

        // Extrait la liste Array des 'name'
        gpxList = decodeGPX(gpxFile, mylat0, mylng0, 0);  // pas de tri !

        lamax=-99; lomax=-999;
        lamin=99; lomin=999;
        altmin=9999; altmax=-9999;
        //Calc echelle
        for (int nn = 0; nn < gpxList.size(); nn++) {
            Location loc = (Location)gpxList.get(nn);
            x= loc.getLongitude();
            y= loc.getLatitude();
            z= loc.getAltitude();  // !!!!

            if (x>lomax) lomax=x;   if (x<lomin) lomin=x;
            if (y>lamax) lamax=y;   if (y<lamin) lamin=y;
            if (z>altmax) altmax=z; if (z<altmin) altmin=z;
        }
        lamoy=(lamax+lamin)/2;
        lomoy=(lomax+lomin)/2;
        altmoy=(altmax+altmin)/2;
        altspan=(altmax-altmin)/2;

        //pixdeg = pixel / °
        if ((lamax-lamin)>(lomax-lomin)) {
            pixdeg = (wy)/(lamax-lamin);
        } else {
            pixdeg = (wx)/(lomax-lomin);
        }
        double dsc = sharedPref.getFloat("zoom",0);
        if (dsc!=0) { pixdeg = dsc; }

        //Reprend eventuel décalage déja réglé
        shiftx = sharedPref.getFloat("shx",0);
        shifty = sharedPref.getFloat("shy",0);
        setKeepScreenOn(true);
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        float oldx= (float) 0;
        float oldy= (float) 0;
        int first=0;
        double x, y, z;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(1);

        //Display points
        int nbwpt = gpxList.size();

        double[] wptX = new double[nbwpt];
        double[] wptY = new double[nbwpt];
        double[] wptZ = new double[nbwpt];

        int nwptvis = 0;
        for (int nn = 0, countname=0; nn < nbwpt; nn++, countname++) {
            Location loc = (Location)gpxList.get(nn);
            wptX[nn] = shiftx + cx + (loc.getLongitude()-lomoy)*pixdeg;
            wptY[nn] = shifty + cy - (loc.getLatitude()-lamoy)*pixdeg;
            wptZ[nn] = loc.getAltitude();
            //compte les pts vraiment visibles
            if ( (wptX[nn]>0)&&(wptX[nn]<wx)&&(wptY[nn]>0)&&(wptY[nn]<wy) ) nwptvis++;
        }

        if (compasok>0) canvas.rotate(-angle, (float) (cx - 0), (float) (cy - 0));

        //Adapte nb display de wptnames max 1/8 nb de wpt
        int namodulo = Math.round(nwptvis >> 3);
        //Dessin des points définitif
        for (int nn = 0, countname=0; nn < nbwpt; nn++, countname++) {
            Location loc = (Location)gpxList.get(nn);

            x = wptX[nn];
            y = wptY[nn];
            z = wptZ[nn];

            //Pset le waypoint
            if ( (x>0)&&(x<wx)&&(y>0)&&(y<wy) ) {
                int rr=0, gg=0, bb=0;
                gg = (int) (255*((altspan - Math.abs(z-altmoy))/altspan));

                if (z>altmoy) rr = (int) (255*(z-altmoy)/(altspan));    // alspan = max-min / 2
                else bb = (int) (255*(altmoy-z)/(altspan));

                paint.setColor(Color.rgb(rr,gg,bb));

                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(Math.round(x), Math.round(y), 4, paint);
            }

            //Trace une droite ?
            paint.setStyle(Paint.Style.STROKE);
            if ( first>0 ) canvas.drawLine(oldx, oldy, (float)x, (float)y, paint);
            oldx=(float)x; oldy=(float)y;
            first=1;

            //Met le wptname ? (10 au maximum)
            if (countname > namodulo) countname=0;
            if (countname<1) {
                paint.setColor(Color.LTGRAY);
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                paint.setTextSize(18);
                canvas.drawText(loc.getProvider(), (float) x - 20, (float) y - 18, paint);
            }
        }

        //Met POINT sur la position courante ?
        if ( (dlat < lamax)&&(dlat > lamin)&&(dlon < lomax)&&(dlon > lomin) ) {
            x = shiftx + cx + (dlon - lomoy) * pixdeg;
            y = shifty + cy - (dlat - lamoy) * pixdeg;
            //Pset le waypoint
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(Math.round(x), Math.round(y), 7, paint);
            canvas.drawLine((float) (x-9), (float)(y), (float)(x+9), (float)(y), paint);
            canvas.drawLine((float) (x), (float)(y-9), (float)(x), (float)(y+9), paint);
        }

        if (compasok>0) canvas.rotate(angle, (float) (cx - 0), (float) (cy - 0));

        //Dessine boutons zooms
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(45);
        canvas.drawText("-       +", (float) (cx - 48), (float) (wy - 8), paint);
        paint.setColor(Color.LTGRAY);
        canvas.drawLine((float)(cx-1), (float)(wy-41),(float)(cx-1), (float)(wy-1), paint);

        //Ecris l'échelle
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        paint.setTextSize(24);
        //Distance pour 90 pixels = règle
        double regle = 212;
        Double km=111133*(regle/pixdeg);
        String tp;
        if (km < 1000) {
            tp = new DecimalFormat("###0").format(km)+" m";
        } else {
            // en km
            km = km / 1000;
            if (km < 10) tp = new DecimalFormat("0.00").format(km)+" km";
            else tp = new DecimalFormat("###0").format(km)+" km";
        }
        canvas.drawText(tp, (float) 55, (float) (wy - 49), paint);

        //Trace la règle
        paint.setStrokeWidth(2);
        float r0 = 53f;
        float r1 = (float)(r0+regle);
        canvas.drawLine(r0, (float)(wy-46), r1, (float)(wy-46), paint);
        canvas.drawLine(r0, (float)(wy-46), r0, (float)(wy-50), paint);
        canvas.drawLine(r1, (float)(wy-46), r1, (float)(wy-50), paint);
    }


    public void updateData(double dy, double dx) {
        dlat = dy;
        dlon = dx;
        invalidate();
    }

    public void shift(double dx, double dy) {
        shiftx += dx;
        shifty += dy;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("shx", (float) shiftx);
        editor.putFloat("shy", (float) shifty);
        editor.apply();
        invalidate();
    }

    public void zoomin() {
        //Max zoom aux environs de 10m
        if (pixdeg < 990000) {
            //calc position la0/lo0 au centre de l'écran
            double lo0=lomoy-(shiftx/pixdeg);
            double la0=lamoy-(shifty/pixdeg);
            //Zoome
            pixdeg *= 1.24;
            //Recale sur le centre
            shiftx=(lomoy-lo0)*pixdeg;
            shifty=(lamoy-la0)*pixdeg;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat("zoom", (float) pixdeg);
            editor.putFloat("shx", (float) shiftx);
            editor.putFloat("shy", (float) shifty);
            editor.apply();
            invalidate();
        }
    }

    public void zoomout() {
        //calc position la0/lo0 au centre de l'écran
        double lo0=lomoy-(shiftx/pixdeg);
        double la0=lamoy-(shifty/pixdeg);
        //dézoome
        pixdeg /= 1.24;
        //Recale sur le centre
        shiftx=(lomoy-lo0)*pixdeg;
        shifty=(lamoy-la0)*pixdeg;

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("zoom", (float) pixdeg);
        editor.putFloat("shx", (float) shiftx);
        editor.putFloat("shy", (float) shifty);
        editor.apply();
        invalidate();
    }

    public void rotation(int ang) {
        if (Math.abs(angle-ang)>15) angle=ang;
        invalidate();
    }

    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
/*    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo;

            la =  intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            //Retrouve Position de Destination
            double dlat = sharedPref.getFloat("dlat", (float) 43);
            double dlon = sharedPref.getFloat("dlng", (float) 5);

            if ((la!=null)&&(lo!=null)) {
                try { dlat = Double.parseDouble(la); } catch (Exception ignored) { ; }
                try { dlon = Double.parseDouble(lo); } catch (Exception ignored) { ; }
                //Attend 10 sec avant repaint !
                if (counterpos==0) updateData(dlat, dlon);
                if (counterpos++ > 6) counterpos=0;
            }
        }
    };
*/
}
