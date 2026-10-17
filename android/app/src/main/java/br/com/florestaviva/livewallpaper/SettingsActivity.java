package br.com.florestaviva.livewallpaper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    public static final String PREFS = "forest_prefs";
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        SharedPreferences p = getSharedPreferences(PREFS,MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.rgb(7,26,32));
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(22),dp(32),dp(22),dp(30));scroll.addView(column);
        TextView title=new TextView(this);title.setText("Floresta Viva");title.setTextSize(26);title.setTextColor(Color.WHITE);title.setPadding(0,0,0,dp(15));column.addView(title);
        option(column,p,"fire","Vaga-lumes interativos",true);
        option(column,p,"fog","Névoa em movimento",true);
        option(column,p,"leaves","Folhagem ao vento",true);
        option(column,p,"water","Cachoeira e água em movimento",true);
        option(column,p,"tilt","Profundidade ao inclinar",true);
        option(column,p,"battery","Economia de bateria (15 FPS)",false);
        TextView label=new TextView(this);label.setTextColor(Color.rgb(220,238,214));label.setTextSize(17);label.setPadding(0,dp(18),0,dp(5));column.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMax(75);bar.setProgress(p.getInt("count",48)-10);column.addView(bar);
        label.setText("Vaga-lumes: "+(bar.getProgress()+10));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int v,boolean user){label.setText("Vaga-lumes: "+(v+10));if(user)p.edit().putInt("count",v+10).apply();}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        TextView note=new TextView(this);note.setText("As mudanças são aplicadas automaticamente. As animações param quando o wallpaper não está visível. Alguns launchers reservam os toques para os ícones, então o efeito aparece ao tocar em áreas livres.");
        note.setTextColor(Color.rgb(172,193,190));note.setTextSize(13);note.setGravity(Gravity.START);note.setPadding(0,dp(22),0,0);column.addView(note);
        setContentView(scroll);
    }
    private void option(LinearLayout c,SharedPreferences p,String id,String title,boolean defaultValue){
        CheckBox b=new CheckBox(this);b.setText(title);b.setTextColor(Color.rgb(228,239,228));b.setTextSize(16);b.setPadding(0,dp(6),0,dp(6));b.setChecked(p.getBoolean(id,defaultValue));
        b.setOnCheckedChangeListener((v,checked)->p.edit().putBoolean(id,checked).apply());c.addView(b);
    }
    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+.5f);}
}
