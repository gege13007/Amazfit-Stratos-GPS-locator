package com.samblancat.finder;

import android.content.Context;
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
    public String gpxini;
    public CheckBox compchk, autowptchk, sortbydist;
    int ok=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reglage);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        gpxini = sharedPref.getString("gpxini","gpxlocator.gpx");

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
        //Test si gpxdata existe
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        String path0 = dir0.toString();
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }

        File[] filelist = dir0.listFiles();
      //  Log.d("Fsiz= ", Integer.toString(filelist.length));
        //Met en premier la valeur du gpx initial
        String[] thefiles = new String[filelist.length];
        thefiles[0] = gpxini;
        //ecrit en décalé !  sur un array de long
        for (int i = 0, pt=1; i < thefiles.length; i++) {
            //ne met pas deux fois le gpxini
          //  Log.d("Fil= ", filelist[i].getName()+" i="+Integer.toString(i));
            if (!gpxini.equals(filelist[i].getName())) {
                //!!! Sécurité anti-crash si gpxini est FAUX !!!
                if (pt>=filelist.length) pt=0;
                thefiles[pt] = filelist[i].getName();
           //     Log.d("Fil= ", filelist[i].getName()+" pt="+Integer.toString(i));
                pt++;
            }
        }
        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_dropdown_item_1line, thefiles);
        final Spinner spin = (Spinner) findViewById(R.id.gpxlist);
        spin.setAdapter(adapter);

        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Object o=spin.getSelectedItem();
                //Sauve le GPX ini !
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("gpxini", o.toString());
                editor.putFloat("zoom", (float) 0);
                editor.putFloat("shx", (float) 0);
                editor.putFloat("shy", (float) 0);
                editor.apply();
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
