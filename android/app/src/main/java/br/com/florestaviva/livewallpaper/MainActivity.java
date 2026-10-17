package br.com.florestaviva.livewallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(6, 22, 29));
        root.setPadding(dp(20), dp(28), dp(20), dp(18));
        ImageView artwork = new ImageView(this);
        artwork.setImageResource(R.drawable.floresta);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(artwork,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        TextView title = new TextView(this);
        title.setText("Floresta Viva"); title.setTextColor(Color.rgb(230,246,218));
        title.setTextSize(26); title.setGravity(Gravity.CENTER);title.setPadding(0,dp(15),0,dp(5));
        root.addView(title);
        TextView desc = new TextView(this);
        desc.setText("Wallpaper animado em pixel art. Toque na floresta para espantar os vaga-lumes.");
        desc.setTextColor(Color.rgb(188,210,207));desc.setTextSize(14);desc.setGravity(Gravity.CENTER);
        root.addView(desc);
        Button apply = new Button(this);apply.setText("APLICAR WALLPAPER");
        root.addView(apply,new LinearLayout.LayoutParams(-1,dp(58)));
        apply.setOnClickListener(v -> applyWallpaper());
        Button config = new Button(this);config.setText("CONFIGURAÇÕES");
        root.addView(config,new LinearLayout.LayoutParams(-1,dp(52)));
        config.setOnClickListener(v -> startActivity(new Intent(this,SettingsActivity.class)));
        setContentView(root);
    }
    private void applyWallpaper() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this,ForestWallpaperService.class));
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)); }
            catch (Exception x) { Toast.makeText(this,"Não foi possível abrir a seleção de wallpapers",Toast.LENGTH_LONG).show(); }
        }
    }
    private int dp(float dp){return (int)(dp*getResources().getDisplayMetrics().density+.5f);}
}
