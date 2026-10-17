package br.com.florestaviva.livewallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
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
    private final Rect capeSource=new Rect();
    private final RectF capeDestination=new RectF();
    private final float[] candleX={45,207,251,302,356,603,628,694,753,812,870};
    private final float[] candleY={579,710,1459,565,536,534,555,720,1310,963,1154};
    private boolean sword=true,eyes=true,movingCape=true,candles=true,fog=true,tilt=true,awakening=true,battery=false;
    private float intensity=1.3f;
    private int screenW=1, screenH=1;
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
        intensity=.75f+Math.min(100,Math.max(0,p.getInt("knight_intensity",65)))/100f*1.65f;
    }
    void onTouch(float nx,float ny,boolean doubleTap){
        float x=(nx*screenW-offsetX)/scale,y=(ny*screenH-offsetY)/scale;
        if(x>325&&x<602&&y>545&&y<1045){
            eyeFlash=1f;
            wind=(x<W*.5f?-1f:1f);
            if(doubleTap)tryAwaken();
        }
    }
    void tryAwaken(){
        long now=SystemClock.uptimeMillis();
        if(awakening && now-lastAwaken>5500){awake=4.2f;lastAwaken=now;eyeFlash=1.5f;}
    }
    void draw(Canvas c,int width,int height,float dt,float xTilt,float yTilt){
        if(background==null)return;
        screenW=width;screenH=height;
        elapsed+=dt;
        eyeFlash=Math.max(0,eyeFlash-dt*.85f);
        awake=Math.max(0,awake-dt);
        wind*=Math.max(0,1-dt*1.0f);
        tiltX+=(xTilt-tiltX)*Math.min(1f,dt*8f);
        tiltY+=(yTilt-tiltY)*Math.min(1f,dt*8f);
        // Crop-fill artwork and leave 5% bleed for sensor-driven movement.
        scale=Math.max(width/(float)W,height/(float)H)*1.06f;
        offsetX=(width-W*scale)*.5f+(tilt?tiltX*width*.023f:0);
        offsetY=(height-H*scale)*.5f+(tilt?tiltY*height*.013f:0);
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
        // Subtle, but measurable: 4-6 original artwork pixels of vertical breathing.
        float breathe=(float)Math.sin(elapsed*2.1f);
        float uplift=(2.5f+3.5f*breathe)*intensity;
        c.save();
        c.translate(468,838-uplift);
        c.scale(1f+breathe*.008f*intensity,1f+breathe*.014f*intensity);
        art.setAlpha(235);
        c.drawBitmap(chest,-138,-178,art);
        art.setAlpha(255);
        c.restore();
    }
    private void drawCape(Canvas c){
        if(cape==null)return;
        // Strip-deformed 2D cloth. Rows are animated continuously rather than jumping
        // between still sprites; the shoulder is anchored while the torn ends swing.
        final int cw=cape.getWidth(),ch=cape.getHeight(), seam=327;
        final int strip=9;
        float gust=wind*7f;
        float speed=elapsed*3.25f;
        art.setAlpha(245);
        for(int top=0;top<ch;top+=strip){
            int bottom=Math.min(ch,top+strip);
            float depth=top/(float)ch;
            float anchor=depth*depth;
            float wave=(float)Math.sin(speed-depth*5.7f);
            float wave2=(float)Math.sin(speed*.71f+depth*8.3f);
            float flutter=(wave*16f+wave2*7f+gust)*anchor*intensity;
            if(awake>0f) flutter*=1.65f;
            float dy=(float)Math.sin(speed*.82f+depth*4.3f)*3.0f*anchor*intensity;
            // The hanging cloth on either side is drawn separately to avoid
            // dragging the knight torso with the cape.
            capeSource.set(0,top,seam,bottom);
            capeDestination.set(200-flutter*.74f,725+top+dy,
                    200+seam-flutter*.74f,725+bottom+dy+.3f);
            c.drawBitmap(cape,capeSource,capeDestination,art);
            capeSource.set(seam,top,cw,bottom);
            capeDestination.set(200+seam+flutter,725+top+dy,
                    200+cw+flutter,725+bottom+dy+.3f);
            c.drawBitmap(cape,capeSource,capeDestination,art);
        }
        art.setAlpha(255);
    }
    private void drawSword(Canvas c){
        float power=(awake>0f?1.7f:1f)*intensity;
        float pulse=.78f+.22f*(float)Math.sin(elapsed*5.0f);
        c.save();
        // Stationary ambient aura keeps the blade embedded in the artwork.
        c.translate(434,1141);c.scale(.85f,7.3f);
        light.setAlpha(Math.min(255,(int)(145*pulse*power)));
        c.drawCircle(0,0,40,light);c.restore();

        // Distinct bright energy travelling visibly from hilt to sword tip.
        float length=409f, displacement=(elapsed*285f)%length;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int line=0;line<7;line++){
            float y=957+(displacement+line*length/7f)%length;
            float xx=433f+(float)Math.sin(elapsed*6.0f+line*.9f)*4.5f*intensity;
            paint.setStrokeWidth(line%3==0?4.5f:2.25f);
            paint.setColor(Color.argb(Math.min(250,(int)(225*pulse*power)),255,
                    line%3==0?218:73,line%3==0?146:59));
            c.drawLine(xx,y,xx+(float)Math.sin(elapsed*6f+line)*2f,Math.min(1365,y+30f),paint);
        }
        paint.setStyle(Paint.Style.FILL);
        // Animated magical sparks orbit the blade instead of staying fixed.
        for(int i=0;i<12;i++){
            float y=955+(i*47f+elapsed*(i%2==0?160f:230f))%415f;
            float x=434+(float)Math.sin(elapsed*(2.3f+i*.15f)+i)*((i%2==0?16f:27f)*intensity);
            int alpha=(int)(135+70*(.5f+.5f*(float)Math.sin(elapsed*5.2f+i)));
            paint.setColor(Color.argb(alpha,255,i%3==0?214:54,45));
            float side=i%4==0?3.3f:2f;
            c.drawRect(x,y,x+side,y+side,paint);
        }
        c.save();c.translate(434,1385);c.scale(3.0f,.60f);
        light.setAlpha(Math.min(255,(int)(125*pulse*power)));
        c.drawCircle(0,0,40,light);c.restore();
    }
    private void drawEyes(Canvas c){
        float glow=.65f+.30f*(float)Math.sin(elapsed*3.8f)+eyeFlash*1.3f+(awake>0?1.5f:0);
        glow*=intensity;
        for(int i=0;i<2;i++){
            float x=i==0?458f:485f;
            c.save();c.translate(x,640);c.scale(.23f,.13f);
            light.setAlpha(Math.min(255,(int)(155*glow)));
            c.drawCircle(0,0,40,light);c.restore();
            paint.setColor(Color.argb(Math.min(255,(int)(160*glow)),255,60,42));
            c.drawRect(x-3,639,x+3,641,paint);
        }
    }
    private void drawCandles(Canvas c){
        for(int i=0;i<candleX.length;i++){
            float flicker=(float)Math.sin(elapsed*(6.4f+(i%4)*1.2f)+i*1.19f);
            float x=candleX[i]+flicker*3.0f*intensity;
            float y=candleY[i]+(float)Math.cos(elapsed*5.1f+i)*3.0f;
            float factor=.64f+.29f*flicker+(awake>0?.30f:0);
            // Shifting pool of illumination below each moving flame.
            c.save();c.translate(x,y+24f);c.scale(1.4f,.42f);
            light.setAlpha((int)(72*factor*intensity));c.drawCircle(0,0,40,light);c.restore();
            c.save();c.translate(x,y);c.scale(.37f,.47f);
            light.setAlpha(Math.min(195,Math.max(15,(int)(130*factor))));
            c.drawCircle(0,0,40,light);c.restore();
            paint.setColor(Color.argb(Math.min(255,Math.max(80,(int)(210*factor))),255,175,108));
            c.drawRect(x-1.1f,y-3.2f,x+1.1f,y+3.2f,paint);
        }
    }
    private void drawFog(Canvas c){
        for(int i=0;i<6;i++){
            float drift=(elapsed*(17f+i*3f)+i*157f)%(W+330f);
            float x=-140f+drift;
            float y=1380+i*36+(float)Math.sin(elapsed*.95f+i)*13f*intensity;
            c.save();c.translate(x,y);c.scale(2.8f,.76f);
            haze.setAlpha((int)(190+(awake>0?50:0)));
            c.drawCircle(0,0,120,haze);c.restore();
        }
    }
    private void drawParticles(Canvas c,float dt){
        int maximum=battery?18:48;
        for(int i=0;i<maximum;i++){
            Particle p=particles[i];
            p.x+=p.dx*dt*intensity; p.y+=p.dy*dt*intensity;
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
        p.dx=-34+random.nextFloat()*68;
        p.dy=-33-random.nextFloat()*53;
        p.phase=random.nextFloat()*6.28f;
    }
    private static final class Particle{float x,y,dx,dy,phase;}
}
