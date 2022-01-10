package com.samblancat.finder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver receiver;
    public static final String BROADCAST_ACTION = "com.samblancat";
    public Context mContext;
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

        wptname = sharedPref.getString("nom", "");

        glob.shownames = sharedPref.getInt("shownames", 1);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
        Log.e("Versions", "manufacturer " + manufacturer
                + " \n model " + model
                + " \n version " + version
                + " \n versionRelease " + versionRelease
        );

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

        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        wptname = sharedPref.getString("nom", "");

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
 //       Log.e("TAG", "Activity Minimized nepasfermer="+nepasfermer);
        if (nepasfermer<1) {
            Log.e("TAG", "Activity Minimized");
            finish();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
 //       Log.e("TAG", "onPause nepasfermer="+nepasfermer);
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
        //ASSURE FIN DU PROCESS !!!
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    //Renvoie la Liste des Wpts contenus dans le File gpxini
    // La distance à (lat0/lng0) est stockée dans .accuracy
    public static void decodeGPX(File file, Double lat0, Double lng0) {

        //Reset Liste des positions
        glob.gpxList = new ArrayList<>();

        //raz first point
        glob.maplat0 = 0;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            FileInputStream fileInputStream = new FileInputStream(file);
            Document document = documentBuilder.parse(fileInputStream);
            Element elementRoot = document.getDocumentElement();
            //Liste des blocs 'trkpt'
            NodeList nodelist_trkpt = elementRoot.getElementsByTagName("trkpt");
            int nbWpts = nodelist_trkpt.getLength();
            if (nbWpts > 0) {
                glob.NbPts = nbWpts;
                glob.NbTrk = 1;
                Log.e("dcod: ", "nbtrkpt =" + nbWpts);
            }
            if (nbWpts<1) {
                nodelist_trkpt = elementRoot.getElementsByTagName("wpt");
                nbWpts = nodelist_trkpt.getLength();
                Log.e("dcod: ", "nbWpt =" + nbWpts);
                glob.NbPts = nbWpts;
                glob.NbTrk = 0;
            }

            for(int i = 0; i < nbWpts; i++ ){
                String wptname="";
                String wpttime="";
                double newAlt_double=0;
                Node node = nodelist_trkpt.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String newLat = attributes.getNamedItem("lat").getTextContent();
                double newLat_double = Double.parseDouble(newLat);
                String newLon = attributes.getNamedItem("lon").getTextContent();
                double newLon_double = Double.parseDouble(newLon);

                //sauve le premier point du gpx
                if (glob.maplat0==0) {
                    glob.maplat0=newLat_double;
                    glob.maplon0=newLon_double;
                }
                //Calc pseudo distance à lat0/lng0
                Double dist1=Math.pow(Math.abs(lat0-newLat_double),2);
                Double dist2=Math.pow(Math.abs(lng0-newLon_double),2);
                float distance = (float) Math.sqrt(dist1 + dist2);

                //strip les champs inclus au trkpt
                NodeList nList = node.getChildNodes();
                for(int j=0; j<nList.getLength(); j++) {
                    Node el = nList.item(j);
                    //capte le 'time'
                    if(el.getNodeName().equals("time"))  wpttime = el.getTextContent();
                    //capte le 'name'
                    if(el.getNodeName().equals("name"))  wptname = el.getTextContent();
                    //capte 'ele' altitude
                    if(el.getNodeName().equals("ele")) {
                        String newAlt = el.getTextContent();
                        newAlt_double = Double.parseDouble(newAlt);
                    }
                }
                //Si Pas de nom -> affiche extrait de la position
                if ( wptname.equals("") ) {
                    if (newLat.length()>6) wptname= newLat.substring(0,7); else wptname= newLat;
                    if (newLon.length()>6) wptname+= "/"+newLon.substring(0,7); else wptname+= "/"+ newLon;
                }
                //stoc la Location dans la 'list'
                Location newLocation = new Location(wptname);
                newLocation.setLatitude(newLat_double);
                newLocation.setLongitude(newLon_double);
                newLocation.setAltitude(newAlt_double);
                //sauve le timestamp
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try {
                    Date date = dateFormat.parse(wpttime);
                    newLocation.setTime(date.getTime());
                } catch (ParseException ignored) {

                }

                //Sauve la distance en 'Accuracy'
                newLocation.setAccuracy(distance);

                glob.gpxList.add(newLocation);

                glob.gpx_progress = i;
            }

            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    //Attention ! Les données arrive ici par paquets de Locservice,
    //un coup du Lat/lon... il peut donc y avoir du null...
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String la, lo;
            double precis;

            TextView ptxt = findViewById(R.id.precistxt);
            ImageButton img = findViewById(R.id.gotocmd);
            TextView gsvtxt = findViewById(R.id.posittxt);

            if (glob.gpsfix > 0) {
                    //ici ne pas faire la.isempty() ou la.length>0 !!!
                    try { precis = 4 * (glob.hdop);    // 6 ?
                    } catch (Exception e) { precis = 99.0; }
                    String t = new DecimalFormat("##0").format(precis);
                    ptxt.setText("Precision " + t + "m");
                    img.setBackgroundResource(R.drawable.finder32);
                    t = new DecimalFormat("##0.0000").format(mylat);
                    t += "°/" + new DecimalFormat("##0.0000").format(mylng) + "°";
                    gsvtxt.setText(t);
                } else {
                    ptxt.setText(R.string.waitgps);
                    gsvtxt.setText(glob.gsv);
                    img.setBackgroundResource(R.drawable.finder32red);
                }

                if ((glob.gpsfix > 0) && (blinking > 0)) {
                    ptxt.clearAnimation();
                    blinking = 0;
                }

                if ((glob.gpsfix < 1) && (blinking < 1)) {
                    //Fait clignoter "Waiting pos" tant que pas de fix
                    Animation anim = new AlphaAnimation(0.0f, 1.0f);
                    anim.setDuration(250);  // blinking time
                    anim.setStartOffset(120);
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setRepeatCount(Animation.INFINITE);
                    ptxt.startAnimation(anim);
                    blinking = 1;
                }

            //essaie de capter Position
            la = intent.getStringExtra("Lat");
            lo = intent.getStringExtra("Lon");

            if ((la!=null)&&(lo!=null)) {
                try { mylat = Double.parseDouble(la); } catch (Exception ignored) {  }
                try { mylng = Double.parseDouble(lo); } catch (Exception ignored) {  }

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