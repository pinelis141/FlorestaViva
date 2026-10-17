package br.com.florestaviva.livewallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;

import java.util.Random;

/**
 * Renderizador de Cavaleiro Amaldiçoado. Coordenadas de composição estão na imagem
 * original, 941 x 1672, e não dependem da resolução do aparelho.
 * A animação acontece em tempo real, não utiliza vídeo nem áudio.
 */
final class KnightScene {
    private static final int W=941, H=1672;
    private static final int N=52;
    private final Random random=new Random(6219);
    private final Particle[] particles=new Particle[N];
    private final Bitmap background, cape, chest;
    private final Paint art=new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haze=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] candleX={45,207,251,302,356,603,628,694,753,812,870};
    private final float[] candleY={579,710,1459,565,536,534,555,720,1310,963,1154};
    private boolean sword=true,eyes=true,movingCape=true,candles=true,fog=true,tilt=true,awakening=true,battery=false;
    private float elapsed=0,eyeFlash=0,awake=0,wind=0,tiltX=0,tiltY=0;
    private float offsetX,offsetY,scale;
    private long lastAwaken=0;

    KnightScene(Bitmap background,Bitmap cape,Bitmap chest){
        this.background=background;
        this.cape=cape;
        this.chest=chest;
        for(int i=0;i<particles.length;i++) {
            Particle p=new Particle();resetParticle(p,true);
            particles[i]=p;
        }
        light.setShader(new RadialGradient(0,0,40,
                new int[]{0xB3FF4B3C,0x55EF1818,0x00DF1111},
                new float[]{0,.32f,1}, Shader.TileMode.CLAMP));
        haze.setShader(new RadialGradient(0,0,120,
                new int[]{0x224F6A87,0x104F6A87,0x004F6A87},
                new float[]{0,.5f,1},Shader.TileMode.CLAMP));
    }
    void load(SharedPreferences p){
        sword=p.getBoolean("knight_sword",true);
        eyes=p.getBoolean("knight_eyes",true);
        movingCape=p.getBoolean("knight_cape",true);
        candles=p.getBoolean("knight_candles",true);
        fog=p.getBoolean("knight_fog",true);
        tilt=p.getBoolean("knight_tilt",true);
        awakening=p.getBoolean("knight_awakening",true);
        battery=p.getBoolean("battery",false);
    }
    void onTouch(float nx,float ny,boolean doubleTap){
        float x=nx*W,y=ny*H;
        if(x>325&&x<602&&y>545&&y<1045){
            eyeFlash=1f;
            wind=(x<W*.5f?-1f:1f);
            if(doubleTap)tryAwaken();
        }
    }
    void tryAwaken(){
        long now=SystemClock.uptimeMillis();
        if(awakening && now-lastAwaken>5000){awake=4.2f;lastAwaken=now;eyeFlash=1.5f;}
    }
    void draw(Canvas c,int width,int height,float dt,float xTilt,float yTilt){
        if(background==null)return;
        elapsed+=dt;
        eyeFlash=Math.max(0,eyeFlash-dt*.65f);
        awake=Math.max(0,awake-dt);
        wind*=Math.max(0,1-dt*.4f);
        tiltX+=(xTilt-tiltX)*.075f;
        tiltY+=(yTilt-tiltY)*.075f;
        // Crop-fill artwork and leave 5% bleed for sensor-driven movement.
        scale=Math.max(width/(float)W,height/(float)H)*1.06f;
        offsetX=(width-W*scale)*.5f+(tilt?tiltX*width*.010f:0);
        offsetY=(height-H*scale)*.5f+(tilt?tiltY*height*.006f:0);
        c.save(); c.translate(offsetX,offsetY);c.scale(scale,scale);
        art.setAlpha(255);
        c.drawBitmap(background,0,0,art);
        if(movingCape){drawBreathing(c);drawCape(c);}
        if(candles)drawCandles(c);
        if(sword)drawSword(c);
        if(eyes)drawEyes(c);
        if(fog)drawFog(c);
        drawParticles(c,dt);
        if(awake>0)drawAwakening(c);
        c.restore();
    }
    private void drawBreathing(Canvas c){
        if(chest==null)return;
        float breathe=(float)Math.sin(elapsed*1.4f)*.0040f;
        c.save();
        c.translate(468,838);c.scale(1+breathe,1+breathe*1.4f);
        art.setAlpha(190);
        c.drawBitmap(chest,-138,-178,art); // crop: 330,660 - 620,1040
        art.setAlpha(255);c.restore();
    }
    private void drawCape(Canvas c){
        if(cape==null)return;
        float flutter=(float)Math.sin(elapsed*1.05f)*1.3f+(float)Math.sin(elapsed*.53f)*.9f+wind;
        if(awake>0)flutter*=1.7f;
        c.save();
        c.translate(555,789);
        c.rotate(flutter*.9f);
        art.setAlpha(232);
        c.drawBitmap(cape, -355, -64, art); // crop: 200,725 - 915,1265
        c.restore();art.setAlpha(255);
    }
    private void drawSword(Canvas c){
        float awaken=awake>0?1.7f:0f;
        float pulse=.68f+.25f*(float)Math.sin(elapsed*2.6f)+awaken;
        // Sword extends from the glowing jewel (430,944) to the stone (433,1382).
        c.save();
        light.setAlpha(Math.min(255,(int)(142*pulse)));
        c.translate(434,1141);c.scale(.82f,6.9f);
        c.drawCircle(0,0,40,light);
        c.restore();
        paint.setStrokeWidth(2.9f);paint.setColor(Color.argb(190,255,70,50));
        c.drawLine(433,957,434,1358,paint);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.argb(170,255,214,163));
        float lineY=950+(elapsed*125)%420;
        for(int i=0;i<5;i++){
            float y=950+((lineY-950+i*67)%420);
            c.drawLine(431+(float)Math.sin(y)*2,y,434,y+14,paint);
        }
        // Persistent glow in the stone and puddles below the sword tip.
        c.save();c.translate(434,1385);c.scale(2.6f,.6f);
        light.setAlpha(Math.min(255,(int)(105*pulse)));
        c.drawCircle(0,0,40,light);c.restore();
    }
    private void drawEyes(Canvas c){
        float intensity=.36f+.19f*(float)Math.sin(elapsed*2.25f)+eyeFlash*.9f+(awake>0?1.1f:0);
        for(float x:new float[]{458,485}){
            c.save();c.translate(x,640);c.scale(.23f,.13f);
            light.setAlpha(Math.min(255,(int)(155*intensity)));
            c.drawCircle(0,0,40,light);c.restore();
            paint.setColor(Color.argb(Math.min(255,(int)(110*intensity)),255,60,42));
            c.drawRect(x-3,639,x+3,641,paint);
        }
    }
    private void drawCandles(Canvas c){
        for(int i=0;i<candleX.length;i++){
            float flicker=(float)Math.sin(elapsed*(4.0f+(i%4)*.7f)+i*1.19f);
            float x=candleX[i]+flicker*1.0f;
            float y=candleY[i]+(float)Math.cos(elapsed*3.1f+i)*2.2f;
            float factor=.48f+.22f*flicker+(awake>0?.16f:0);
            c.save();c.translate(x,y);c.scale(.37f,.47f);
            light.setAlpha(Math.min(195,Math.max(15,(int)(130*factor))));
            c.drawCircle(0,0,40,light);c.restore();
            paint.setColor(Color.argb(Math.min(255,Math.max(80,(int)(210*factor))),255,175,108));
            c.drawRect(x-1.1f,y-3.2f,x+1.1f,y+3.2f,paint);
        }
    }
    private void drawFog(Canvas c){
        for(int i=0;i<5;i++){
            float phase=elapsed*.15f+i*1.6f;
            float x=112+i*192+(float)Math.sin(phase)*70+tiltX*i*2;
            float y=1410+i*17+(float)Math.cos(phase*.4f)*10;
            c.save();c.translate(x,y);c.scale(2.2f,.55f);
            haze.setAlpha((int)(140+(awake>0?50:0)));
            c.drawCircle(0,0,120,haze);c.restore();
        }
    }
    private void drawParticles(Canvas c,float dt){
        int maximum=battery?12:38;
        for(int i=0;i<maximum;i++){
            Particle p=particles[i];
            p.x+=p.dx*dt; p.y+=p.dy*dt;
            if(p.y<40||p.y>1680||p.x<0||p.x>941) resetParticle(p,false);
            float shimmer=.5f+.5f*(float)Math.sin(elapsed*2+p.phase);
            int alpha=Math.min(215,(int)((75+100*shimmer)*(awake>0?1.25f:1f)));
            paint.setColor(Color.argb(alpha,255,64,48));
            float size=(i%4==0?2.5f:1.3f);
            c.drawRect(p.x,p.y,p.x+size,p.y+size,paint);
        }
    }
    private void drawAwakening(Canvas c){
        float stage=4.2f-awake;
        float radius=Math.min(820,stage*320);
        float alpha=Math.max(0,.25f-stage*.065f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.4f+stage*.6f);
        paint.setColor(Color.argb((int)(alpha*255),214,25,52));
        c.drawCircle(432,1030,radius,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void resetParticle(Particle p,boolean initial){
        p.x=random.nextInt(W);
        p.y=initial?random.nextInt(H):H-120+random.nextInt(150);
        p.dx=-14+random.nextFloat()*28;
        p.dy=-9-random.nextFloat()*24;
        p.phase=random.nextFloat()*6.28f;
    }
    private static final class Particle{float x,y,dx,dy,phase;}
}
