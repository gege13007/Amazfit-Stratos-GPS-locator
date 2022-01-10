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
    public CheckBox compchk, autowptchk, shownames;
    public int ok=0, firstbuiltshow=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reglage);
        mContext = this;

        //Reprend le Gpx de base
        sharedPref = getBaseContext().getSharedPreferences("POSPREFS", MODE_PRIVATE);
        try { glob.gpxini = sharedPref.getString("gpxini","gpxlocator.gpx"); }
        catch (Exception e) {
            e.printStackTrace();
            glob.gpxini="new-file.gpx";
         }

        compchk = findViewById(R.id.setcompasschk);
        autowptchk = findViewById(R.id.autonextchk);
        shownames = findViewById(R.id.shownameschk);

        //Set etat init du 'compas activé'
        try { ok=sharedPref.getInt("compas", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) compchk.setChecked(true); else compchk.setChecked(false);

        //Set état init de 'auto next wpt'
        try { ok=sharedPref.getInt("autonext", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) autowptchk.setChecked(true); else autowptchk.setChecked(false);

        //Set état init de 'montre les libellés'
        try { ok=sharedPref.getInt("shownames", 1); } catch (Exception e) { e.printStackTrace(); }
        if (ok > 0) shownames.setChecked(true); else shownames.setChecked(false);

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
            if (((CheckBox) v).isChecked()) ok = 1; else ok = 0;
            //Sauve la Position Départ !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("autonext", ok);
            editor.apply();
            }
        });

        shownames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (((CheckBox) v).isChecked()) ok = 1; else ok = 0;
            //Sauve la Position Départ !
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("shownames", ok);
            editor.apply();
            glob.shownames=ok;
            }
        });

        //Fait la liste des gpx
        File dir0 = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata");
        if ( !dir0.exists() ) {
            Toast.makeText(mContext, "No 'gpxdata' directory !", Toast.LENGTH_LONG).show();
            return;
        }

        File[] filelist = dir0.listFiles();
        //Met en premier la valeur du gpx initial
        String[] thefiles = new String[1+filelist.length];
        //commence par le file en cours (si il existe !)
        File f_ini = new File(Environment.getExternalStorageDirectory().toString()+"/gpxdata/"+glob.gpxini);
        Log.e("/f_ini=", String.valueOf(f_ini));

        if (!f_ini.exists()) glob.gpxini="new-file.gpx";
        Log.e("/gpxini=", String.valueOf(glob.gpxini));
        //Le fichier en cours en premier !
        thefiles[0] = glob.gpxini;

        //ecrit en décalé !  sur un array de long
        for (int i = 0; i < filelist.length; i++)
            //ne met pas deux fois le gpxini & ne copie pas le 'new-file'
            thefiles[i+1] = filelist[i].getName();

        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, R.layout.spinner_item, thefiles);    // simple_dropdown_item_1line
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
                    Intent intent = new Intent(mContext, waitloadgpx.class);
                    mContext.startActivity(intent);
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
