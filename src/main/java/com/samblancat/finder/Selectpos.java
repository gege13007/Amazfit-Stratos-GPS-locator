package com.samblancat.finder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.samblancat.finder.MainActivity.decodeGPX;

public class Selectpos extends AppCompatActivity  {
    Context mContext;
    double mylat=0, mylng=0;
    static double mylat0=0;
    static double mylng0=0;
    static Integer nbwpt=0;
    SharedPreferences sharedPref;
    String wptname;
    int tri;
    public ArrayList<String> namesList;
    public List<Location> list2 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int nn;

        setContentView(R.layout.selposlist);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");
        tri = sharedPref.getInt("sortbydist", 1);

        //Retrouve last Position courante pour tri / distances
        mylat0 = sharedPref.getFloat("dlat", (float) mylat);
        mylng0 = sharedPref.getFloat("dlng", (float) mylng);

        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }
        String path = path0 +"/" + glob.gpxini;

        final File gpxFile = new File(path);
        long ll = gpxFile.length();
        if ( ll <1 ) {
            Log.e("GPX :", "Creer file : " + path);
            Toast.makeText(mContext, "Empty file !", Toast.LENGTH_LONG).show();
            CreerGpx(path);
        }
        // la liste Array des 'name' est préchargée
        nbwpt = glob.gpxList.size();
        if (nbwpt < 1) {
            Toast.makeText(mContext, "No waypoints !", Toast.LENGTH_LONG).show();
         //   DeleteGpx();
         //   CreerGpx();
        }

        //list2 = gpx les + proches triés de la Liste des positions / distance
        list2 = new ArrayList<Location>();
        //a chaque iter de j, min0 est le nouveau minimum croissant
        Float min0 = (float) 0.0;
        int j, k, index = -1, oldindex = -1;
        for (j = 0; j < glob.gpxList.size(); j++) {
            //Trouve le nouveau minimum de [accuracy] dans list
            Float min = (float) 99999999.9;
            for (k = 0; k < glob.gpxList.size(); k++) {
                Float a = glob.gpxList.get(k).getAccuracy();
                if ((a < min) && (a >= min0)) {
                    if (oldindex == -1) {
                        min = a;
                        index = k;
                    } else if (oldindex != k) {
                        min = a;
                        index = k;
                    }
                }
            }
            //oldindex est l'index du dernier mini trouvé pour ne pas répéter si val=
            oldindex = index;
            //min0 est le dernier minimum trouvé (augmente)
            min0 = min;
            list2.add(glob.gpxList.get(index));
            //bloque à 50 points
            if (j > 50) break;
        }

        //Bloque à 50 points par liste
        namesList = new ArrayList<String>();         // liste des Noms des Wpts
        for (nn = 0; nn < list2.size(); nn++) {
            String tt = (list2.get(nn)).getProvider();
            namesList.add(tt);
            if (nn>50) break;
        }

        final ListView lv = findViewById(R.id.listname);
        ArrayAdapter adapter = new ArrayAdapter<>(this, R.layout.custom_listview, namesList);
        lv.setAdapter(adapter);

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object Item = lv.getItemAtPosition(position);
                wptname = Item.toString();
                Log.e("TAG :", wptname);
                new AlertDialog.Builder(mContext, R.style.myALERT)
                        .setTitle("Delete wpt ?")
                        .setCancelable(false)
                        .setMessage(wptname)
                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            // Efface le wpt du meme nom
                            DeleteWptGPX(wptname);
                            // Extrait la liste Array des 'name'
                            decodeGPX(gpxFile, mylat0, mylng0);        // liste des Pos des Wpts
                            //Si Aucun Waypoints -> efface - recree
                            nbwpt = glob.gpxList.size();
                            if (nbwpt < 1) {
                                Toast.makeText(mContext, "No waypoints !", Toast.LENGTH_LONG).show();
                  //              DeleteGpx();
                  //              CreerGpx();
                            }
                            //Reconstruit la liste des Wpts
                            ArrayList<String> namesList = new ArrayList<>();         // liste des Noms des Wpts
                            for (int nn = 0; nn < glob.gpxList.size(); nn++) {
                                String tt = (glob.gpxList.get(nn)).getProvider();
                                namesList.add(tt);
                            }
                            final ListView lv = findViewById(R.id.listname);
                            ArrayAdapter adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, namesList);
                            lv.setAdapter(adapter);
                            }
                        })
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });

        //Met le listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object Item = lv.getItemAtPosition(position);
                wptname = Item.toString();
                Log.e("select :", wptname);
                mylat = list2.get(position).getLatitude();
                mylng = list2.get(position).getLongitude();
                //Sauve la Position Voulue et Go sur SCAN !
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat("lat0", (float) mylat);
                editor.putFloat("lng0", (float) mylng);
                editor.putString("nom", wptname);
                editor.apply();
                //Lance le Scanning
                Intent intent = new Intent(Selectpos.this, Scan.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void CreerGpx(String path) {
        File gpx = new File(path);
        try {
            gpx.createNewFile();
            Log.e("TAG :", "create file ok");
            // true = append file
            FileWriter fw = new FileWriter(gpx.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n");
            bw.write("<gpx xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/11.xsd\"\r\n");
            bw.write("xmlns=\"http://www.topografix.com/GPX/1/1\"\r\n");
            bw.write("xmlns:ns3=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"\r\n");
            bw.write("xmlns:ns2=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"\r\n");
            bw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n");
            bw.write("xmlns:ns1=\"http://www.cluetrust.com/XML/GPXDATA/1/0\"\r\n");
            bw.write("creator=\"Huami Amazfit Sports Watch\" version=\"1.1\">\r\n");
            bw.write("<metadata>\r\n");
            bw.write("<name>Amazfit GPS Locator</name>\r\n");
            bw.write("<author>\r\n");
            bw.write("<name>Samblancat G</name>\r\n");
            bw.write("</author><time>2021-08-05T10:52:19Z</time>\r\n");
            bw.write("</metadata>\r\n");
            bw.write("<trk>\r\n");
            bw.write("<name>GPS Locator</name>\r\n");
            bw.write("<trkseg>\r\n");

 /*           bw.write("<trkpt lat=\"48.856\" lon=\"2.347\">\r\n");
            bw.write("<ele>35</ele>\r\n");
            bw.write("<time>2020-05-05T08:53:30Z</time>\r\n");
            bw.write("<name>Paris</name>\r\n");
            bw.write("</trkpt>\r\n");
*/
            bw.write("</trkseg>\r\n");
            bw.write("</trk>\r\n");
            bw.write("</gpx>\r\n");

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void DeleteGpx() {
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + glob.gpxini;
        File gpx = new File(path);
        gpx.delete();
        Log.e("GPX :", "delete ok");
    }

    public void LireGpx() {
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + glob.gpxini;
        File gpx = new File(path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(gpx));
            String line;
            while ((line = br.readLine()) != null) {
                Log.e("Rd :", line);
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    public void DeleteWptGPX(String nom){
        String noname;
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + glob.gpxini;
        File gpx = new File(path);
        String path2 = path0 + "/gpslocator.tmp";
        File gpx2 = new File(path2);

        String nametofind = "<name>"+nom+"</name>";
        Log.e("del ", "<name>"+nom+"</name>");

        FileWriter fw = null;
        try { fw = new FileWriter(gpx2.getAbsoluteFile(), true); }
        catch (IOException e) { e.printStackTrace(); }
        BufferedWriter bw = new BufferedWriter(fw);

        try {
            BufferedReader br = new BufferedReader(new FileReader(gpx));
            String line;

            while ( (line = br.readLine()) != null) {
                //flag si le wpt en cours est a supprimer !
                boolean todelete=false;

                //Teste si début d'un nouveau WPT
                //(envoie par defaut les '<trk>' plus petit que le '<trkpt' qui iest testé après
                if (line.length()<=5) {
                    //envoie vers le fichier tmp
                    bw.write(line + "\r\n");
                }
                //Teste si début d'un nouveau WPT
                if (line.length()>5)  if (line.substring(0, 6).equals("<trkpt")) {
                    //ReFait un wptname par défaut (au cas ou pas de name reel dans gpx)
                    String coords[] = line.split(" ");
                    String non=coords[1];
                    non=non.replace("\"", "");
                    non=non.replace("lat=", "");
                    if (non.length()>6) non= non.substring(0,7);
                    noname=non+"/";
                    non=coords[2];
                    non=non.replace("\"", "");
                    non=non.replace("lon=", "");
                    if (non.length()>6) non= non.substring(0,7);
                    noname +=non;

                    //Stocke le wpt en tampon jusqu'au 'name'
                    ArrayList<String> lines = new ArrayList<String>();
                    while ( true ) {
                        //sauve une ligne dans le fichier tampon
                        lines.add(line);
                        //Test si on rentre dans le Waypoint à supprimer (name = wptname ?)
                        if (line.length()>5) {
                            if (line.substring(0, 6).equals("<name>")) {
                                if (line.equals(nametofind)) {
                                    todelete = true;
                                }
                            } else {
                                //compare au nom brut (sans les <name>...</name>)
                                if (noname.equals(nom)) {
                                    todelete = true;
                                }
                            }
                        }

                        //Teste la fin du Wpt ?
                        if (line.contains("</trkpt")) break;

                        if ((line = br.readLine()) == null) break;
                    }
                    //Renvoie (si pas deleté) le wpt en memoire tampon
                    if (!todelete) {
                        for (int nl = 0; nl < lines.size(); nl++) {
                            //envoie le NON supprimé vers le fichier tmp
                            bw.write(lines.get(nl)+"\r\n");
                        }
                    }
                }
                else {
                    //Pas dans un WPT <trkpt>
                    //envoie le NON supprimé vers le fichier tmp
                    bw.write(line+"\r\n");
                }
            }
            br.close();
        } catch (IOException ignored) {
        }
        try { bw.close(); } catch (IOException e) { e.printStackTrace(); }
        try { if (fw != null) fw.close(); } catch (IOException e) { e.printStackTrace(); }

        //efface le source
        gpx.delete();
        //renomme le tmp en Gps
        gpx2.renameTo(gpx);
        if (gpx2.exists()) gpx2.delete();

        //Sync la Media-connection pour visu sur Windows usb
        MediaScannerConnection.scanFile(mContext,
                new String[]{gpx.getAbsolutePath()}, null, null);
        MediaScannerConnection.scanFile(mContext,
                new String[]{gpx2.getAbsolutePath()}, null, null);
    }

}