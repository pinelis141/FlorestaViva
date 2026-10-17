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
    public static final String PREFS="forest_prefs";
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);
        boolean knight="knight".equals(p.getString("scene","forest"));
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.rgb(8,13,21));
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22),dp(32),dp(22),dp(30));scroll.addView(column);
        TextView title=new TextView(this);title.setText(knight?"Cavaleiro Amaldiçoado":"Floresta Viva");
        title.setTextSize(24);title.setTextColor(Color.WHITE);title.setPadding(0,0,0,dp(15));column.addView(title);
        if(knight){
            option(column,p,"knight_sword","Espada amaldiçoada",true);
            option(column,p,"knight_eyes","Olhos interativos",true);
            option(column,p,"knight_cape","Capa e respiração",true);
            option(column,p,"knight_candles","Velas e luz dinâmica",true);
            option(column,p,"knight_fog","Névoa rasteira",true);
            option(column,p,"knight_tilt","Profundidade ao inclinar",true);
            option(column,p,"knight_awakening","Despertar (toque duplo ou longo)",true);
            option(column,p,"knight_60fps","Alta fluidez (até 60 FPS)",true);
            TextView movement=new TextView(this);
            movement.setTextColor(0xFFDCEDD6);movement.setTextSize(17);
            movement.setPadding(0,dp(18),0,dp(6));column.addView(movement);
            SeekBar intensityBar=new SeekBar(this);intensityBar.setMax(100);
            intensityBar.setProgress(Math.max(0,Math.min(100,p.getInt("knight_intensity",65))));
            column.addView(intensityBar);
            movement.setText("Intensidade do movimento: "+intensityBar.getProgress()+"%");
            intensityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar b,int value,boolean fromUser){
                    movement.setText("Intensidade do movimento: "+value+"%");
                    if(fromUser)p.edit().putInt("knight_intensity",value).apply();
                }
                public void onStartTrackingTouch(SeekBar b){}
                public void onStopTrackingTouch(SeekBar b){}
            });
        }else{
            option(column,p,"fire","Vaga-lumes interativos",true);
            option(column,p,"fog","Névoa em movimento",true);
            option(column,p,"leaves","Folhagem ao vento",true);
            option(column,p,"water","Cachoeira e água em movimento",true);
            option(column,p,"tilt","Profundidade ao inclinar",true);
            TextView label=new TextView(this);label.setTextColor(0xFFDCEDD6);label.setTextSize(17);
            label.setPadding(0,dp(18),0,dp(5));column.addView(label);
            SeekBar bar=new SeekBar(this);bar.setMax(75);
            bar.setProgress(Math.max(0,Math.min(75,p.getInt("count",48)-10)));column.addView(bar);
            label.setText("Vaga-lumes: "+(bar.getProgress()+10));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar b,int v,boolean user){label.setText("Vaga-lumes: "+(v+10));if(user)p.edit().putInt("count",v+10).apply();}
                public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
            });
        }
        option(column,p,"battery","Economia de bateria (até 24 FPS)",false);
        TextView note=new TextView(this);note.setText("Alterações são aplicadas automaticamente, inclusive quando o wallpaper já está instalado. O Android pode reservar alguns gestos para ícones ou widgets.");
        note.setTextColor(0xFFADBFBE);note.setTextSize(13);note.setGravity(Gravity.START);
        note.setPadding(0,dp(24),0,0);column.addView(note);
        setContentView(scroll);
    }
    private void option(LinearLayout c,SharedPreferences p,String key,String label,boolean defaultValue){
        CheckBox b=new CheckBox(this);b.setText(label);b.setTextColor(0xFFE6F0E7);b.setTextSize(16);
        b.setPadding(0,dp(6),0,dp(6));b.setChecked(p.getBoolean(key,defaultValue));
        b.setOnCheckedChangeListener((button,checked)->p.edit().putBoolean(key,checked).apply());c.addView(b);
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
