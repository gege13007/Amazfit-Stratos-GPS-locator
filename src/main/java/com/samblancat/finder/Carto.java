package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class Carto extends AppCompatActivity {
    Context mContext;
    private cartodraw cartoview;
    public float x1, x2, y1, y2;
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    public int counterpos=0;
    public double wx , wy, cx, cy;
    public double dlat=0, dlon=0;
    public long touchtime = 0;
    public Timer mytime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext=this;

        wx = getResources().getDisplayMetrics().widthPixels;
        wy = getResources().getDisplayMetrics().heightPixels;
        cx = wx/2;
        cy = wy/2;

        //Mode de vue map - map+incrust - sortie
        glob.modevue = 0;

        //Lance timer toutes les 6 secondes pour calc vitesse
        mytime = new Timer();
        TimerTask timerTaskObj = new TimerTask() {
            public void run() {
                if (glob.oldlat!=0) {
                    //Calc distance à oldlat/oldlon
                    double x = Math.pow(Math.abs(glob.lastlat - glob.oldlat), 2);
                    x += Math.pow(Math.cos(Math.toRadians(glob.lastlat)) * Math.abs(glob.lastlon - glob.oldlon), 2);
                    x = Math.sqrt(x);
                    //distance des deux points en 5 sec
                    x = 111120 * x;
                    //Vitesse instant (moyenne pondérée / 3)
                    glob.realspeed += (1.2 * x);    // 2 * ( 3.600 / 6 )
                    glob.realspeed /= 3;
                }
                //sauve last posit
                glob.oldlat=glob.lastlat;
                glob.oldlon=glob.lastlon;
            }
        };
        mytime.schedule(timerTaskObj, 0, 6000);

        cartoview = new cartodraw(this);
        setContentView(cartoview);

        // on initialise le receiver de service/broadcast
        receiver = new Carto.MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(receiver, intentFilter);
    }


    //Detection du swipe sur gauche ou autre .... pour finish ?
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch(ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = ev.getX();
                y1 = ev.getY();
                //pour test detection Long Click
                touchtime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                x2 = ev.getX();
                y2 = ev.getY();
                //Test si long click ?
                touchtime = (System.currentTimeMillis()-touchtime);
                if ( (touchtime>1500) && (Math.abs(x2 - x1) < 4) && (Math.abs(y2 - y1) < 4) ) {
                    // Lance choix multiple !
                    cartoview.StoreNewWpt(x2, y2);
                }
                else {
                    //Traitement du Tap Rapide (zoom...ou changement de vue ?)
            //        Log.d("TAP", Integer.toString((int) (Math.abs(x2 - x1)+Math.abs(y2 - y1))));
                    if ((Math.abs(x2 - x1)<2) && (Math.abs(y2 - y1)<2)) {
                        //Test si ZOOM ?
                        if (y2 > wy - 65) {
                            if (x2 > cx + 30) {
                                cartoview.zoomin();
                            } else {
                                if (x2 < cx - 30) {
                                    cartoview.zoomout();
                                } else
                                    cartoview.centermap();
                            }
                        } else {
                            // Changement de vue -> sort ?
                            glob.modevue++;
                            glob.modevue=(glob.modevue % 3);
                            //si datas incrustation
                            if (glob.modevue==1) cartoview.updateData(dlat, dlon);
                            //si sortie
                            if (glob.modevue==2) finish();
                        }
                    }
                    //anti-rebond ?
                    x2=0; y2=0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                x2 = ev.getX();
                y2 = ev.getY();

                //SHIFT de l'écran
                if (Math.abs(x2 - x1) + Math.abs(y2 - y1) > 15) {
                    cartoview.shift((x2 - x1)/3, (y2 - y1)/3);
             //       x1 = x2-3;  // pour test anti-rebond ?
             //       y1 = y2-3;
                }
                break;
        }
        return true;   //super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
    //    if (nepasfermer<1) {
    //        Log.d("TAG", "Activity Minimized");
    //        finish();
    //    }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        mytime.cancel();

        try { unregisterReceiver(receiver );
        } catch(IllegalArgumentException e) { e.printStackTrace(); }
    }

    //Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo;

            la = intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            if ((la!=null)&&(lo!=null)) {
                try { dlat = Double.parseDouble(la); } catch (Exception ignored) {  }
                try { dlon = Double.parseDouble(lo); } catch (Exception ignored) {  }
                //Attend 10 sec avant repaint !
                if (counterpos==0) cartoview.updateData(dlat, dlon);
                if (counterpos++ > 3) counterpos=0;
            }
        }
    }

}