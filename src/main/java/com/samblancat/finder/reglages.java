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
import android.widget.ListView;
import android.widget.Spinner;

import java.io.File;

public class reglages extends AppCompatActivity {
    Context mContext;
    SharedPreferences sharedPref;
    public String gpxini;
    public CheckBox compchk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reglage);
        mContext = this;

        compchk = (CheckBox) findViewById(R.id.setcompasschk);

        //Reprend réglage compas & fichier GPX de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        if (sharedPref.getInt("compas", 1) > 0) compchk.setChecked(true);
        else compchk.setChecked(false);

        //Reprend le Gpx de base
        gpxini=sharedPref.getString("gpxini","gpxlocator.gpx");

        compchk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer ok;
                if (((CheckBox) v).isChecked()) ok = 1;
                else ok = 0;
                //Sauve la Position Départ !
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("compas", ok);
                editor.apply();
            }
        });

        //Fait la liste des gpx
        String path0 = Environment.getExternalStorageDirectory().toString()+"/gpxdata";
        File dir = new File(path0);
        File[] filelist = dir.listFiles();
        //Met en premier la valeur du gpx initial
        String[] thefiles = new String[1+filelist.length];
        thefiles[0] = gpxini;
        //ecrit en décalé ! +1 sur un array de long +1 !!
        for (int i = 0; i < thefiles.length-1; i++) {
            Log.d("gpxini: ", filelist[i].getName()+ " i="+String.valueOf(i));
            thefiles[i+1] = filelist[i].getName();
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
