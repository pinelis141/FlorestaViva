package br.com.florestaviva.livewallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int back=Color.rgb(8,13,21);
    private final int highlight=Color.rgb(189,53,61);
    private SharedPreferences preferences;
    private LinearLayout forestCard,knightCard,galaxyCard;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences=getSharedPreferences(SettingsActivity.PREFS,MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(back);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(26),dp(16),dp(30));scroll.addView(root);

        TextView title=new TextView(this);title.setText("WALLPAPERS VIVOS");title.setTextColor(Color.WHITE);
        title.setTextSize(22);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView hint=new TextView(this);hint.setText("Escolha um cenário. Um único aplicativo, três mundos animados.");
        hint.setTextColor(0xFFB5BEC5);hint.setTextSize(13);hint.setGravity(Gravity.CENTER);
        hint.setPadding(0,dp(6),0,dp(22));root.addView(hint);

        HorizontalScrollView galleryScroll=new HorizontalScrollView(this);
        galleryScroll.setHorizontalScrollBarEnabled(false);
        galleryScroll.setFillViewport(false);
        LinearLayout gallery=new LinearLayout(this);
        gallery.setOrientation(LinearLayout.HORIZONTAL);
        gallery.setGravity(Gravity.TOP);
        galleryScroll.addView(gallery,new HorizontalScrollView.LayoutParams(-2,-1));
        root.addView(galleryScroll,new LinearLayout.LayoutParams(-1,dp(322)));
        forestCard=addCard(gallery,"forest",R.drawable.floresta,"FLORESTA VIVA", "Água • névoa • vaga-lumes");
        knightCard=addCard(gallery,"knight",R.drawable.cavaleiro,"DARK FANTASY", "Espada • fogo • despertar");
        galaxyCard=addCard(gallery,"galaxy",R.drawable.galaxy_space,"GALÁXIA", "Fluxo • loop • partículas");
        refresh();

        Button apply=new Button(this);apply.setText("APLICAR WALLPAPER");
        root.addView(apply,new LinearLayout.LayoutParams(-1,dp(55)));
        apply.setOnClickListener(v->applyWallpaper());
        Button settings=new Button(this);settings.setText("CONFIGURAR EFEITOS");
        root.addView(settings,new LinearLayout.LayoutParams(-1,dp(53)));
        settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        TextView footer=new TextView(this);footer.setText("O tema selecionado substitui o anterior no mesmo wallpaper. Não instala outro aplicativo.");
        footer.setTextSize(12);footer.setTextColor(0xFF9CABB5);footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8),dp(15),dp(8),0);root.addView(footer);
        setContentView(scroll);
    }
    private LinearLayout addCard(LinearLayout gallery,String key,int resource,String title,String description){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(5),dp(5),dp(5),dp(12));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(158),-1);
        params.setMargins(dp(3),0,dp(3),0);gallery.addView(box,params);
        ImageView picture=new ImageView(this);picture.setImageResource(resource);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(picture,new LinearLayout.LayoutParams(-1,0,1f));
        TextView name=new TextView(this);name.setText(title);name.setTextColor(Color.WHITE);
        name.setGravity(Gravity.CENTER);name.setTextSize(14);name.setPadding(0,dp(11),0,dp(5));box.addView(name);
        TextView sub=new TextView(this);sub.setText(description);sub.setTextSize(11);
        sub.setTextColor(0xFFB4C1C9);sub.setGravity(Gravity.CENTER);box.addView(sub);
        box.setOnClickListener(v->{preferences.edit().putString("scene",key).apply();refresh();});
        return box;
    }
    private void refresh(){
        String selected=preferences.getString("scene","forest");
        decorate(forestCard,"forest".equals(selected));
        decorate(knightCard,"knight".equals(selected));
        decorate(galaxyCard,"galaxy".equals(selected));
    }
    private void decorate(LinearLayout view,boolean selected){
        GradientDrawable b=new GradientDrawable();
        b.setColor(selected?0xFF20242E:0xFF121923);b.setCornerRadius(dp(9));
        b.setStroke(dp(selected?2:1), selected?highlight:0xFF394451);
        view.setBackground(b);
    }
    private void applyWallpaper(){
        try{
            Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this,ForestWallpaperService.class));
            startActivity(i);
        }catch(Exception e){
            try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}
            catch(Exception x){Toast.makeText(this,"Não foi possível abrir o seletor de wallpapers",Toast.LENGTH_LONG).show();}
        }
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
}
