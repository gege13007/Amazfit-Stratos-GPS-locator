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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class Selectpos extends AppCompatActivity  {
    Context mContext;
    double mylat=0, mylng=0;
    static double mylat0=0;
    static double mylng0=0;
    static Integer nbWpts=0;
    SharedPreferences sharedPref;
    public static String gpxini;
    String wptname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int nn;

        setContentView(R.layout.selposlist);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

        //Retrouve last Position courante pour tri / distances
        mylat0 = sharedPref.getFloat("dlat", (float) mylat);
        mylng0 = sharedPref.getFloat("dlng", (float) mylng);

  //      mylat0=43.20; mylng0=5.30;     // TEST ONLY !!!
   //     LireGpx();

        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + gpxini;
        final File gpxFile = new File(path);
        long ll = gpxFile.length();
        if ( ll <1 ) {
            Log.d("GPX :", "Creer file : " + path);
            CreerGpx();
        }
        // Extrait la liste Array des 'name'
        final List<Location> gpxList = decodeGPX(gpxFile, mylat0, mylng0);        // liste des Pos des Wpts
        //Si Aucun Waypoints -> efface - recree
        if (nbWpts < 1) {
            Toast.makeText(mContext, "No waypoints !", Toast.LENGTH_LONG).show();
            DeleteGpx();
            CreerGpx();
        }
        ArrayList<String> namesList = new ArrayList<String>();         // liste des Noms des Wpts
        for (nn = 0; nn < gpxList.size(); nn++) {
            String tt = ((Location) gpxList.get(nn)).getProvider();
            namesList.add(tt);
        }

        final ListView lv = (ListView) findViewById(R.id.listname);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, namesList);
        lv.setAdapter(adapter);

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Object Item = lv.getItemAtPosition(position);
                wptname = Item.toString();
                Log.d("TAG :", wptname);
                new AlertDialog.Builder(mContext)
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
                            final List<Location> gpxList = decodeGPX(gpxFile, mylat0, mylng0);        // liste des Pos des Wpts
                            //Si Aucun Waypoints -> efface - recree
                            if (nbWpts < 1) {
                                Toast.makeText(mContext, "No waypoints !", Toast.LENGTH_LONG).show();
                                DeleteGpx();
                                CreerGpx();
                            }
                            //Reconstruit la liste des Wpts
                            ArrayList<String> namesList = new ArrayList<String>();         // liste des Noms des Wpts
                            for (int nn = 0; nn < gpxList.size(); nn++) {
                                String tt = ((Location) gpxList.get(nn)).getProvider();
                                namesList.add(tt);
                            }
                            final ListView lv = (ListView) findViewById(R.id.listname);
                            ArrayAdapter adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, namesList);
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
                //   Log.d("TAG :", Item.toString());
                mylat = gpxList.get(position).getLatitude();
                mylng = gpxList.get(position).getLongitude();
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

    public void CreerGpx() {
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + gpxini;
        File gpx = new File(path);
        try {
            gpx.createNewFile();
            Log.d("TAG :", "create file ok");
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
            bw.write("</author><time>2020-05-05T10:52:19Z</time>\r\n");
            bw.write("</metadata>\r\n");
            bw.write("<trk>\r\n");
            bw.write("<name>GPS Locator</name>\r\n");
            bw.write("<trkseg>\r\n");

            bw.write("<trkpt lat=\"48.856\" lon=\"2.347\">\r\n");
            bw.write("<ele>35</ele>\r\n");
            bw.write("<time>2020-05-05T08:53:30Z</time>\r\n");
            bw.write("<name>Paris</name>\r\n");
            bw.write("</trkpt>\r\n");

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
        String path = path0 + "/" + gpxini;
        File gpx = new File(path);
        gpx.delete();
        Log.d("GPX :", "delete ok");
    }

    public void LireGpx() {
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + gpxini;
        File gpx = new File(path);
   //     Log.d("Rd :", "Read "+gpx.toString());
        try {
            BufferedReader br = new BufferedReader(new FileReader(gpx));
            String line;
            while ((line = br.readLine()) != null) {
                Log.d("Rd :", line);
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }
    }

    public void DeleteWptGPX(String nom){
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        String path = path0 + "/" + gpxini;
        File gpx = new File(path);
        String path2 = path0 + "/gpslocator.tmp";
        File gpx2 = new File(path2);

        String nametofind = "<name>"+nom+"</name>";

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
                    Log.d("del ", line);
                    //envoie vers le fichier tmp
                    bw.write(line + "\r\n");
                }
                //Teste si début d'un nouveau WPT
                if (line.length()>5)  if (line.substring(0, 6).equals("<trkpt")) {
                    //Stocke le wpt en tampon jusqu'au 'name'
                    ArrayList<String> lines = new ArrayList<String>();
                    while ( true ) {
                        //sauve une ligne
                        lines.add(line);
                        //Test si on rentre dans le Waypoint à supprimer (name = wptname ?)
                        if (line.length()>5) if (line.substring(0,6).equals("<name>")) {
                            if (line.equals(nametofind)) {
                                todelete=true;
                            }
                        }

                        //Teste la fin du Wpt ?
                        if (line.length()>6) if (line.substring(0, 7).equals("</trkpt")) break;

                        if ((line = br.readLine()) == null) break;
                    }
                    //Renvoie (si pas deleté) le wpt en memoire tampon
                    if (!todelete) {
                        for (int nl = 0; nl < lines.size(); nl++) {
                            Log.d("del ", lines.get(nl));
                            //envoie le NON supprimé vers le fichier tmp
                            bw.write(lines.get(nl)+"\r\n");
                        }
                    }
                }
                else {
                    //Pas dans un WPT <trkpt>
                    Log.d("del ", line);
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


    //Renvoie une Liste de nbWpts triés par distance croissante à (lat0/lng0)
    public static List<Location> decodeGPX(File file, Double lat0, Double lng0) {
        //Liste des positions
        List<Location> list = new ArrayList<Location>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            FileInputStream fileInputStream = new FileInputStream(file);
            Document document = documentBuilder.parse(fileInputStream);
            Element elementRoot = document.getDocumentElement();
            //Liste des blocs 'trkpt'
            NodeList nodelist_trkpt = elementRoot.getElementsByTagName("trkpt");
            nbWpts = nodelist_trkpt.getLength();
            for(int i = 0; i < nbWpts; i++){
                String wptname="";
                Node node = nodelist_trkpt.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String newLat = attributes.getNamedItem("lat").getTextContent();
                double newLat_double = Double.parseDouble(newLat);
                String newLon = attributes.getNamedItem("lon").getTextContent();
                double newLon_double = Double.parseDouble(newLon);
                //Calc pseudo distance à lat0/lng0
                Double dist1=Math.pow(Math.abs(lat0-newLat_double),2);
                Double dist2=Math.pow(Math.abs(lng0-newLon_double),2);
                float distance = (float) Math.sqrt(dist1 + dist2);
                //strip les champs inclus au trkpt
                NodeList nList = node.getChildNodes();
                for(int j=0; j<nList.getLength(); j++) {
                    Node el = nList.item(j);
                    if(el.getNodeName().equals("name"))  wptname = el.getTextContent();
                }
                //stoc name dans le Provider
                Location newLocation = new Location(wptname);
                newLocation.setLatitude(newLat_double);
                newLocation.setLongitude(newLon_double);
                //Sauve la distance en 'Accuracy'
                newLocation.setAccuracy(distance);
                list.add(newLocation);
            }
            fileInputStream.close();
        } catch (ParserConfigurationException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Tri Croissant de la Liste des positions / distance
        List<Location> list2 = new ArrayList<Location>();
        //a chaque iter de j, min0 est le nouveau minimum croissant
        Float min0=(float)0.0;
        int j,k,index=-1,oldindex=-1;
        for(j=0; j<list.size(); j++) {
            //Trouve le nouveau minimum de [accuracy] dans list
            Float min=(float)99999999.9;
            for(k=0; k<list.size(); k++) {
               Float a=list.get(k).getAccuracy();
               if ((a<min)&&(a>=min0)) {
                   if (oldindex==-1)
                      { min=a; index=k; }
                   else
                      if (oldindex!=k) { min=a; index=k; }
                }
            }
            //oldindex est l'index du dernier mini trouvé pour ne pas répéter si val=
            oldindex = index;
            //min0 est le dernier minimum trouvé (augmente)
            min0=min;
            list2.add(list.get(index));
        }

        //Debug only
        for(j=0; j<list2.size(); j++) {
            String tt = ((Location) list2.get(j)).getProvider();
            Log.d("nomTrie : ", Integer.toString(j)+" "+tt);
        }

        return list2;
    }

}