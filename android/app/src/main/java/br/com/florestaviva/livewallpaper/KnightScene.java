package br.com.florestaviva.livewallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import java.util.Random;

/**
 * Dark Fantasy Remaster: stable source painting, animated locally reconstructed
 * transparent flow atlases, subframe crossfade, and vector light/particle effects.
 * Rendered on demand by the same FlorestaViva WallpaperService.
 */
final class KnightScene {
    static final int W=941,H=1672;
    private static final int PARTICLES=76;
    private final Random random=new Random(21331);
    private final Bitmap background,capeAtlas,breathAtlas;
    private final Paint art=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint pen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mist=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source=new Rect();
    private final RectF dest=new RectF();
    private final Particle[] ashes=new Particle[PARTICLES];
    private final float[] candleX={45,207,251,302,356,603,628,694,753,812,870};
    private final float[] candleY={579,710,1459,565,536,534,555,720,1310,963,1154};
    private boolean sword=true,eyes=true,movingCape=true,candles=true,fog=true,tilt=true,awakening=true,battery=false;
    private float intensity=1f;
    private float elapsed=0f,eyeFlash=0f,awake=0f,wind=0f,tiltX=0f,tiltY=0f;
    private float offsetX=0f,offsetY=0f,scale=1f;
    private int screenW=1,screenH=1;
    private long lastAwaken=0;

    KnightScene(Bitmap base,Bitmap cape,Bitmap breath){
        background=base; capeAtlas=cape; breathAtlas=breath;
        glow.setShader(new RadialGradient(0,0,40,
                new int[]{0xB3FF6456,0x55F51F23,0x00DB1414},
                new float[]{0,.32f,1},Shader.TileMode.CLAMP));
        mist.setShader(new RadialGradient(0,0,120,
                new int[]{0x3C718194,0x187B7D98,0x007B7D98},
                new float[]{0,.55f,1},Shader.TileMode.CLAMP));
        for(int i=0;i<ashes.length;i++){
            Particle p=new Particle();reset(p,true);ashes[i]=p;
        }
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
        // UI percentage now maps predictably to 0.8x through 1.6x.
        intensity=.8f+Math.max(0,Math.min(100,p.getInt("knight_intensity",65)))*.008f;
    }
    void onTouch(float nx,float ny,boolean doubleTap){
        float x=(nx*screenW-offsetX)/Math.max(.1f,scale);
        float y=(ny*screenH-offsetY)/Math.max(.1f,scale);
        if(x>330&&x<590&&y>570&&y<1120){
            eyeFlash=1f;
            wind=(x<475?-1f:1f);
            if(doubleTap)tryAwaken();
        }
    }
    void tryAwaken(){
        long now=SystemClock.uptimeMillis();
        if(awakening && now-lastAwaken>5600){
            awake=4.2f;lastAwaken=now;eyeFlash=1.4f;
        }
    }
    void draw(Canvas c,int width,int height,float dt,float xTilt,float yTilt){
        if(background==null)return;
        screenW=width;screenH=height;
        dt=Math.min(.05f,Math.max(0f,dt));
        elapsed+=dt;
        eyeFlash=Math.max(0,eyeFlash-dt*.8f);
        awake=Math.max(0,awake-dt);
        wind*=Math.max(0,1-dt*1.2f);
        float response=Math.min(1,dt*7f);
        tiltX+=(xTilt-tiltX)*response;
        tiltY+=(yTilt-tiltY)*response;
        scale=Math.max(width/(float)W,height/(float)H)*1.055f;
        offsetX=(width-W*scale)*.5f+(tilt?tiltX*width*.014f:0);
        offsetY=(height-H*scale)*.5f+(tilt?tiltY*height*.009f:0);
        c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);
        art.setAlpha(255);
        c.drawBitmap(background,0,0,art);
        if(movingCape){
            drawAtlas(c,breathAtlas,377,687,198,251,4,16,2.85f,0f);
            drawAtlas(c,capeAtlas,542,696,338,464,6,24,1.75f,wind);
        }
        if(candles)drawCandles(c);
        if(sword)drawSword(c);
        if(eyes)drawEyes(c);
        if(fog)drawFog(c);
        drawAshes(c,dt);
        if(awake>0)drawAwakening(c);
        c.restore();
    }
    /** Adjacent sprite poses are blended each rendered frame, including at wrap-around. */
    private void drawAtlas(Canvas c,Bitmap atlas,int x,int y,int fw,int fh,int columns,int n,float speed,float gust){
        if(atlas==null||atlas.isRecycled())return;
        float f=(elapsed*speed/(float)(2*Math.PI)+gust*.024f)*n;
        f=f%(float)n;if(f<0)f+=n;
        int first=(int)f;
        int second=(first+1)%n;
        float weight=f-first;
        drawPose(c,atlas,first,columns,fw,fh,x,y,1f-weight);
        drawPose(c,atlas,second,columns,fw,fh,x,y,weight);
    }
    private void drawPose(Canvas c,Bitmap atlas,int frame,int cols,int fw,int fh,int x,int y,float opacity){
        if(opacity<.003f)return;
        int sx=frame%cols*fw,sy=frame/cols*fh;
        source.set(sx,sy,sx+fw,sy+fh);
        dest.set(x,y,x+fw,y+fh);
        art.setAlpha(Math.min(255,(int)(255*opacity)));
        c.drawBitmap(atlas,source,dest,art);
        art.setAlpha(255);
    }
    private void drawEyes(Canvas c){
        float pulse=.73f+.27f*(float)Math.sin(elapsed*2.8f);
        float energy=(pulse+eyeFlash*1.5f+(awake>0?1.2f:0))*intensity;
        for(float x:new float[]{457f,483f}){
            c.save();c.translate(x,639);c.scale(.25f,.14f);
            glow.setAlpha(Math.min(255,(int)(168*energy)));
            c.drawCircle(0,0,40,glow);c.restore();
            pen.setColor(Color.argb(Math.min(255,(int)(193*energy)),255,82,62));
            c.drawRoundRect(x-3.4f,638,x+3.4f,641,1.1f,1.1f,pen);
        }
    }
    private void drawSword(Canvas c){
        float pulse=.68f+.32f*(float)Math.sin(elapsed*4.9f);
        float active=awake>0?1.5f:1f;
        float energy=pulse*intensity*active;
        // Base aura follows the long straight original blade; red and warm-white
        // cores remain aligned, unlike the v0.4 floating random streaks.
        c.save();c.translate(436,1166);c.scale(.63f,6.2f);
        glow.setAlpha(Math.min(255,(int)(120*energy)));
        c.drawCircle(0,0,40,glow);c.restore();
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeCap(Paint.Cap.ROUND);
        final float bladeStart=971,bladeEnd=1361;
        for(int lane=0;lane<3;lane++){
            Path path=new Path();
            float center=432+(lane-1)*4.2f;
            for(int i=0;i<=52;i++){
                float y=bladeStart+(bladeEnd-bladeStart)*i/52f;
                float wave=(float)Math.sin(y*.053f-elapsed*7.1f+lane*1.6f);
                float x=center+wave*(lane==1?2.2f:3.8f);
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            pen.setStrokeWidth(lane==1?2.7f:1.6f);
            pen.setColor(Color.argb(Math.min(225,(int)(125*energy)),
                    255,lane==1?166:46,lane==1?107:55));
            c.drawPath(path,pen);
        }
        for(int i=0;i<10;i++){
            float y=bladeStart+((elapsed*230+i*63f)%(bladeEnd-bladeStart));
            float dx=(float)Math.sin(elapsed*4.5f+i*2)*3f;
            pen.setStrokeWidth(i%3==0?3f:1.8f);
            pen.setColor(Color.argb(Math.min(255,(int)(200*energy)),255,185,129));
            c.drawLine(431+dx,y,438-dx,y+13,pen);
        }
        pen.setStyle(Paint.Style.FILL);
        c.save();c.translate(433,1378);c.scale(2.6f,.37f);
        glow.setAlpha(Math.min(250,(int)(138*energy)));
        c.drawCircle(0,0,40,glow);c.restore();
        // Small coherent sparks originate at the blade, not arbitrary scene locations.
        for(int i=0;i<22;i++){
            float orbit=elapsed*(25f+(i%5)*9f)+i*48f;
            float y=bladeEnd-(orbit%(bladeEnd-bladeStart));
            float x=435f+(float)Math.sin(elapsed*2.8+i*2f)*(8f+i%4*4f);
            pen.setColor(Color.argb(105+(i%4)*25,255,70+i%3*28,35));
            c.drawCircle(x,y,i%6==0?2.1f:1f,pen);
        }
    }
    private void drawCandles(Canvas c){
        for(int i=0;i<candleX.length;i++){
            float flick=(float)(.63*Math.sin(elapsed*(5.1+i%3*.7)+i*2.27)
                    +.37*Math.sin(elapsed*(8.1+i%5*.48)+i*.97));
            float x=candleX[i]+flick*2.4f;
            float y=candleY[i]+(float)Math.sin(elapsed*4.5+i)*1.2f;
            float strength=(.61f+.21f*flick)*(awake>0?1.45f:1f);
            c.save();c.translate(x,y);c.scale(.38f,.62f);
            glow.setAlpha(Math.min(245,(int)(152*strength)));
            c.drawCircle(0,0,40,glow);c.restore();
            pen.setColor(Color.argb(180,255,193,114));
            c.drawOval(x-1.1f,y-5.8f,x+1.1f,y+2.7f,pen);
            // Shadows track candle phase, but never paint opaque gray rectangles.
            pen.setColor(Color.argb(25,15,1,8));
            c.drawOval(x+8+flick*3f,y+23,x+43+flick*5f,y+30,pen);
        }
    }
    private void drawFog(Canvas c){
        for(int i=0;i<5;i++){
            float drift=(elapsed*(12f+i*3.2f)+i*250f)%(W+330);
            float x=-170+drift;
            float y=1410+i*38+(float)Math.sin(elapsed*.77+i)*15f;
            c.save();c.translate(x,y);c.scale(2.2f,.59f);
            mist.setAlpha(165+(awake>0?42:0));
            c.drawCircle(0,0,120,mist);c.restore();
        }
    }
    private void drawAshes(Canvas c,float dt){
        int max=battery?23:62;
        for(int i=0;i<max;i++){
            Particle a=ashes[i];a.x+=a.vx*dt*intensity;a.y+=a.vy*dt*intensity;
            if(a.x<0||a.x>W||a.y<55)reset(a,false);
            float shimmer=.65f+.35f*(float)Math.sin(elapsed*3f+a.phase);
            pen.setColor(Color.argb(Math.min(205,(int)(138*shimmer)),255,65+i%5*14,31));
            c.drawCircle(a.x,a.y,i%4==0?1.9f:1.2f,pen);
        }
    }
    private void drawAwakening(Canvas c){
        float stage=4.2f-awake;
        if(stage<.2f)return;
        float radius=Math.min(800,stage*280f);
        float fade=Math.max(0f,1f-stage/4.2f);
        pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(2f+2f*fade);
        pen.setColor(Color.argb((int)(90*(1-fade)),250,51,45));
        c.drawCircle(435,1090,radius,pen);
        pen.setStyle(Paint.Style.FILL);
    }
    private void reset(Particle p,boolean initial){
        p.x=random.nextInt(W);p.y=initial?random.nextInt(H):H-110+random.nextInt(65);
        p.vx=-15+random.nextFloat()*30;p.vy=-20-random.nextFloat()*38;
        p.phase=random.nextFloat()*6.28f;
    }
    private static final class Particle{float x,y,vx,vy,phase;}
}
