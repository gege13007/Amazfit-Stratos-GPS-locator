package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    Context mContext;
    public double mylat0=0, mylng0=0;
    public double mylat=0, mylng=0, alt=0;
    public int satok=0;
    public String wptname;
    private float x1,x2;
    static final int MIN_DISTANCE = 75;         // long swipe gauche avant finish()
    SharedPreferences sharedPref;
    public int blinking=1;
    public int nepasfermer=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mContext = this;

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        //Retrouve last Position courante destination en cours ?
        mylat0 = sharedPref.getFloat("lat0", 0);
        mylng0 = sharedPref.getFloat("lng0", 0);
        wptname = sharedPref.getString("nom", "");

        ImageButton img = findViewById(R.id.gotocmd);
        assert wptname != null;
        if (!wptname.isEmpty()) {
            Animation aniRotateClk = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
            img.startAnimation(aniRotateClk);
        } else
            img.clearAnimation();

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
        TextView ptxt = findViewById(R.id.precistxt);
        ptxt.startAnimation(anim);
        blinking=1;
    }


    @Override
    protected void onResume() {
        //Auto-generated method stub
        super.onResume();

        //l'appli peut etre fermée !
        nepasfermer = 0;
        Log.d("main", "onResume: nepasfermer="+nepasfermer);

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        wptname = sharedPref.getString("nom", "");
        mylat0 = sharedPref.getFloat("lat0", 0);
        mylng0 = sharedPref.getFloat("lng0", 0);
        ImageButton img = findViewById(R.id.gotocmd);
        if (!wptname.equals("")) {
            Animation aniRotateClk = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
            img.startAnimation(aniRotateClk);
        }
        else
            img.clearAnimation();
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
                    nepasfermer = 0;
                    finish();
                }
                break;
        }
        return  super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
 //       Log.d("TAG", "Activity Minimized nepasfermer="+nepasfermer);
        if (nepasfermer<1) {
            Log.d("TAG", "Activity Minimized");
            finish();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
 //       Log.d("TAG", "onPause nepasfermer="+nepasfermer);
        if (nepasfermer<1) {
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //toujours arrete le receiver !
        unregisterReceiver(receiver);
        //Stoppe le service broadcast !
        stopService(new Intent(getBaseContext(), LocService.class));
 //       Log.d("main", "onDestroy");
        //ASSURE FIN DU PROCESS !!!
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    // Réglages généraux
    public void setcmd(View view) {
        //Lance les reglages
        nepasfermer = 1;
        Intent intent = new Intent(MainActivity.this, reglages.class);
        startActivity(intent);
    }


    //Store & Recherche Immédiate position en cours
    public void setposcmd(View view) {
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);

        //Save wpt que si sat ok
        if ( mylat!=0 ) {
            //Si recherche en cours demande ...continue / new ?
            if (!wptname.equals("")) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.myALERT));
                builder1.setMessage("New destination or continue ?");
                builder1.setCancelable(true);
                builder1.setPositiveButton("Continue",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //Stop dialog
                                dialog.dismiss();
                                //Start Scanning Finder activity
                                final Intent intent = new Intent(MainActivity.this, Scan.class);
                                startActivity(intent);
                                nepasfermer = 1;
                            }
                        });
                builder1.setNegativeButton("Goto New",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //Sauve la Position Voulue et Go sur SCAN !
                                Calendar c = Calendar.getInstance();
                                SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                                String formatdate = df.format(c.getTime());
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putFloat("lat0", (float) mylat);
                                editor.putFloat("lng0", (float) mylng);
                                editor.putString("nom", formatdate);
                                editor.apply();
                                String togo = new DecimalFormat("#0.0000").format(mylat);
                                togo += "/" + new DecimalFormat("#0.0000").format(mylng);
                                Toast.makeText(mContext, "Goto " + togo, Toast.LENGTH_LONG).show();
                                //stop dialog
                                dialog.dismiss();
                                //Start Scanning Finder activity
                                final Intent intent = new Intent(MainActivity.this, Scan.class);
                                startActivity(intent);
                                nepasfermer = 1;
                            }
                        });
                builder1.setNeutralButton("Stop nav",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("nom", "");
                                editor.apply();
                                wptname = "";
                                //Stop animation goto
                                ImageButton img = findViewById(R.id.gotocmd);
                                img.clearAnimation();
                                //Stop dialog
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            } else {
                //Pas de Nav en cours -> New destination !
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df;
                df = new SimpleDateFormat("yy-MM-dd/HH:mm:ss");
                String formdate = df.format(c.getTime());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat("lat0", (float) mylat);
                editor.putFloat("lng0", (float) mylng);
                editor.putString("nom", formdate);
                editor.apply();
                String togo = new DecimalFormat("#0.0000").format(mylat);
                togo += "/" + new DecimalFormat("#0.0000").format(mylng);
                Toast.makeText(mContext, "Goto " + togo, Toast.LENGTH_LONG).show();
                //Start Scanning Finder activity
                final Intent intent = new Intent(MainActivity.this, Scan.class);
                startActivity(intent);
                nepasfermer = 1;
            }
        }

        //Pas de Pos -> peut juste stoppper
        if ( mylat==0 ) {
            //Si recherche en cours demande ...continue / stop ?
            if (!wptname.equals("")) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.myALERT));
                builder1.setTitle("Navigation");
                builder1.setMessage("Continue or Stop ?");
                builder1.setCancelable(false);
                builder1.setPositiveButton("Continue",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //Stop dialog
                                dialog.dismiss();
                                //Start Scanning Finder activity
                                final Intent intent = new Intent(MainActivity.this, Scan.class);
                                startActivity(intent);
                                nepasfermer = 1;
                            }
                        });
                builder1.setNeutralButton("Stop nav",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("nom", "");
                                editor.apply();
                                wptname = "";
                                //Stopanimation goto
                                ImageButton img = findViewById(R.id.gotocmd);
                                img.clearAnimation();
                                //Stop dialog
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
            else {
                //Start Quand meme empty Finder activity
                final Intent intent = new Intent(MainActivity.this, Scan.class);
                startActivity(intent);
                nepasfermer = 1;
            }
        }

    }


    //Lance la CARTO MAPS + GPX
    public void cartomaps(View view) {
        Intent intent = new Intent(MainActivity.this, Carto.class);
        startActivity(intent);
        nepasfermer = 1;
    }


    //Va chercher une position dans la liste et va suivre dans le GPX
    public void gotosavedpos(View view) {
        //Liste des Wpts & choix
        Intent intent = new Intent(MainActivity.this, Selectpos.class);
        startActivity(intent);
        nepasfermer = 1;
    }


//Attention ! Les données arrive ici par paquets : un coup du gsv,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP ="com.samblancat";
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo, dop, fix;
            double precis;

            //Capte Qualité
            fix = intent.getStringExtra("Fix");
            dop = intent.getStringExtra("Hdop");

            TextView ptxt = findViewById(R.id.precistxt);
            ImageButton img = findViewById(R.id.gotocmd);

            if (fix!=null) {
                try { satok = Integer.parseInt(fix); } catch (Exception e) { satok = 0; }

                if (satok > 0) {
                    //ici ne pas faire la.isempty() ou la.length>0 !!!
                    try { precis = 4 * Double.parseDouble(dop);    // 6 ?
                    } catch (Exception e) { precis = 99.0; }
                    String t = new DecimalFormat("##0").format(precis);
                    ptxt.setText("Precision " + t + "m");
                    img.setBackgroundResource(R.drawable.finder32);
                } else {
                    ptxt.setText(R.string.waitgps);
                    img.setBackgroundResource(R.drawable.finder32red);
                }

                if ((satok > 0) && (blinking > 0)) {
                    ptxt.clearAnimation();
                    blinking = 0;
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
                }
            }

            //essaie de capter Altitude
            la = intent.getStringExtra("Alt");
            try { alt = Double.parseDouble(la); } catch (Exception ignored) {  }

            //essaie de capter Position
            la = intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            if ((la!=null)&&(lo!=null)) {
                try { mylat = Double.parseDouble(la); } catch (Exception ignored) {  }
                try { mylng = Double.parseDouble(lo); } catch (Exception ignored) {  }

                mylat0 = mylat;
                mylng0 = mylng;

                TextView postxt = findViewById(R.id.posittxt);
                if (satok > 0) {
                    String t = new DecimalFormat("##0.0000").format(mylat);
                    t += "°/" + new DecimalFormat("##0.0000").format(mylng) + "°";
                    postxt.setText(t);
                }
                else
                    postxt.setText("...");
            }
        }
    }
}