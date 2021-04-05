package com.samblancat.finder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.File;

public class reglages extends AppCompatActivity {
    Context mContext;
    SharedPreferences sharedPref;
    public CheckBox compchk, autowptchk, sortbydist;
    public int ok=0, firstbuiltshow=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         int pt, i, deja=0;

        setContentView(R.layout.reglage);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        try { glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx"); }
        catch (Exception e) {
            e.printStackTrace();
            glob.gpxini="<no file>";
         }

        compchk = (CheckBox) findViewById(R.id.setcompasschk);
        autowptchk = (CheckBox) findViewById(R.id.autonextchk);
        sortbydist = (CheckBox) findViewById(R.id.sortwptdistchk);

        //Set etat init du 'compas activé'
        try { ok=sharedPref.getInt("compas", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) compchk.setChecked(true); else compchk.setChecked(false);

        //Set état init de 'auto next wpt'
        try { ok=sharedPref.getInt("autonext", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) autowptchk.setChecked(true); else autowptchk.setChecked(false);

        //Set état init de 'tri par distance'
        try { ok=sharedPref.getInt("sortbydist", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) sortbydist.setChecked(true); else sortbydist.setChecked(false);

        //Set the 3 click chk listeners
        compchk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (((CheckBox) v).isChecked()) ok = 1;
            else ok = 0;
            //Sauve la Position Départ !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("compas", ok);
            editor.apply();
            }
        });

        autowptchk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (((CheckBox) v).isChecked()) ok = 1;
            else ok = 0;
            //Sauve la Position Départ !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("autonext", ok);
            editor.apply();
            }
        });

        sortbydist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (((CheckBox) v).isChecked()) ok = 1;
            else ok = 0;
            //Sauve la Position Départ !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("sortbydist", ok);
            editor.apply();
            }
        });

        //Fait la liste des gpx
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }

        File[] filelist = dir0.listFiles();
        //Met en premier la valeur du gpx initial (ajoute 1 pour '<no file>'
        String[] thefiles = new String[1 + filelist.length];
        //commence par le file en cours (si il existe !)
        File f_ini = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata/"+glob.gpxini);
        if (!f_ini.exists()) glob.gpxini="<no file>";
        thefiles[0] = glob.gpxini;

        //si y avait rien -> pas la peine de '<no file>' a la fin de liste
        if (glob.gpxini.equals("<no file>")) deja = 1;

        //ecrit en décalé !  sur un array de long
        for (i = 0, pt = 1; i < filelist.length; i++) {
            //ne met pas deux fois le gpxini & ne copie pas le '<no file>'
            if (!glob.gpxini.equals(filelist[i].getName())) {
                //!!! Sécurité anti-crash si gpxini est FAUX !!!
                if (pt >= 1+filelist.length) pt=0;
                thefiles[pt] = filelist[i].getName();
                pt++;
            }
        }
        //Ajoute 'pas de fichier'
        if (deja==0) thefiles[pt] ="<no file>";

        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, thefiles);    // simple_dropdown_item_1line
        final Spinner spin = findViewById(R.id.gpxlist);
        spin.setAdapter(adapter);

        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (firstbuiltshow!=0) {
                    Object o = spin.getSelectedItem();
                    //Sauve le GPX ini !
                    SharedPreferences.Editor editor = sharedPref.edit();
                    glob.gpxini = o.toString();
                    editor.putString("gpxini", glob.gpxini);
                    editor.apply();
                    //Aff les stats de track
                    if (!glob.gpxini.equals("<no file>")) {
                        Intent intent = new Intent(mContext, statsgpx.class);
                        mContext.startActivity(intent);
                    }
                }
                //set flag pour eviter adffich au Create()
                firstbuiltshow=1;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

    }

    // Fermeture close
    public void closesettings(View view)
    {
        finish();
    }
}
