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
import java.util.Random;

/**
 * Animated space scene: locally warped looping galaxy imagery, stars and orbital
 * dust. The user illustration remains the source of truth; its dark left side
 * and lower planet are not globally rotated or scrolled.
 */
final class GalaxyScene {
    private static final int X0=170,Y0=175, FRAME_W=558,FRAME_H=795;
    private static final int FRAMES=16,COLUMNS=4;
    private static final int DUST_COUNT=88,STARS_COUNT=94;
    private final Bitmap base,flowAtlas;
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint effects=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coreGlow=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source=new Rect();
    private final RectF destination=new RectF(X0,Y0,X0+FRAME_W,Y0+FRAME_H);
    private final Dust[] dust=new Dust[DUST_COUNT];
    private final Star[] stars=new Star[STARS_COUNT];
    private final Ripple[] ripples=new Ripple[5];
    private int pulses=0,screenWidth=1,screenHeight=1;
    private float left=0f,top=0f,scale=1f,elapsed=0f;
    private boolean flow=true,particles=true,starsEnabled=true,tilt=true,coreEnabled=true,battery=false;
    private float intensity=1f;
    private final Random rng=new Random(1001787);

    private static final class Dust {float theta,radius,speed,size,phase,depth; int tint;}
    private static final class Star {float x,y,phase,speed,size,depth;}
    private static final class Ripple {float x,y,age;}

    GalaxyScene(Bitmap illustration,Bitmap atlas){
        base=illustration;flowAtlas=atlas;
        coreGlow.setShader(new RadialGradient(0f,0f,110f,
                new int[]{0x42FFE6DA,0x28BFC4FF,0x005C78D1},
                new float[]{0f,.3f,1f},Shader.TileMode.CLAMP));
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2f);
        for(int i=0;i<DUST_COUNT;i++){
            Dust d=new Dust();
            d.theta=rng.nextFloat()*6.283185f;
            d.radius=48f+rng.nextFloat()*345f;
            d.speed=(.15f+rng.nextFloat()*.20f)*(d.radius<135f?1.4f:1f);
            d.size=.7f+rng.nextFloat()*1.6f;
            d.phase=rng.nextFloat()*6.283185f;
            d.depth=.45f+rng.nextFloat()*.55f;
            d.tint=i%4;
            dust[i]=d;
        }
        for(int i=0;i<STARS_COUNT;i++){
            Star s=new Star();
            s.x=rng.nextFloat()*base.getWidth();
            s.y=rng.nextFloat()*base.getHeight();
            s.phase=rng.nextFloat()*6.283185f;
            s.speed=.5f+rng.nextFloat()*2f;
            s.size=.35f+rng.nextFloat()*1.15f;
            s.depth=.4f+rng.nextFloat()*.6f;
            stars[i]=s;
        }
        for(int i=0;i<ripples.length;i++)ripples[i]=new Ripple();
    }

    void release(){ if(flowAtlas!=null&&!flowAtlas.isRecycled())flowAtlas.recycle(); }

    void load(SharedPreferences p){
        flow=p.getBoolean("galaxy_flow",true);
        particles=p.getBoolean("galaxy_particles",true);
        starsEnabled=p.getBoolean("galaxy_stars",true);
        coreEnabled=p.getBoolean("galaxy_loop",true);
        tilt=p.getBoolean("galaxy_tilt",true);
        battery=p.getBoolean("battery",false);
        intensity=.65f+Math.max(0,Math.min(100,p.getInt("galaxy_intensity",68)))*.01f;
    }

    void onTouch(float normalizedX,float normalizedY){
        if(pulses==ripples.length){
            // Shift oldest entries; memory is bounded and no allocations occur here.
            for(int i=1;i<pulses;i++){
                Ripple a=ripples[i-1],b=ripples[i];
                a.x=b.x;a.y=b.y;a.age=b.age;
            }
            pulses--;
        }
        Ripple r=ripples[pulses++];
        r.x=(normalizedX*screenWidth-left)/Math.max(.1f,scale);
        r.y=(normalizedY*screenHeight-top)/Math.max(.1f,scale);
        r.age=0;
    }

    void draw(Canvas c,int w,int h,float dt,float tx,float ty){
        if(base==null||base.isRecycled())return;
        dt=Math.max(0f,Math.min(dt,.05f));
        elapsed+=dt;
        screenWidth=w;screenHeight=h;
        scale=Math.max(w/(float)base.getWidth(),h/(float)base.getHeight())*1.032f;
        final float wide=base.getWidth()*scale, high=base.getHeight()*scale;
        float bleedX=Math.max(0,(wide-w)*.5f),bleedY=Math.max(0,(high-h)*.5f);
        float parallaxX=tilt?Math.max(-bleedX*.9f,Math.min(bleedX*.9f,tx*w*.008f)):0;
        float parallaxY=tilt?Math.max(-bleedY*.9f,Math.min(bleedY*.9f,ty*h*.007f)):0;
        left=(w-wide)*.5f+parallaxX;
        top=(h-high)*.5f+parallaxY;
        c.save(); c.translate(left,top); c.scale(scale,scale);
        imagePaint.setAlpha(255);
        c.drawBitmap(base,0f,0f,imagePaint);
        if(flow&&flowAtlas!=null&&!flowAtlas.isRecycled())drawFlowAtlas(c);
        if(coreEnabled)drawCore(c);
        if(starsEnabled)drawStars(c);
        if(particles)drawDust(c);
        drawPulses(c,dt);
        c.restore();
    }

    private void drawFlowAtlas(Canvas c){
        // 16-frame loop at a fixed 6.5-second period; first frame is opaque,
        // next frame fades over it. 16->0 is the same cyclic displacement.
        float frame=((elapsed*16f/6.5f)*(.75f+.25f*intensity))%FRAMES;
        int a=(int)frame,b=(a+1)%FRAMES;
        float weight=frame-a;
        drawFrame(c,a,255);
        if(weight>.002f)drawFrame(c,b,(int)(255*weight));
        imagePaint.setAlpha(255);
    }
    private void drawFrame(Canvas c,int i,int alpha){
        source.set((i%COLUMNS)*FRAME_W,(i/COLUMNS)*FRAME_H,
                (i%COLUMNS+1)*FRAME_W,(i/COLUMNS+1)*FRAME_H);
        imagePaint.setAlpha(alpha);
        c.drawBitmap(flowAtlas,source,destination,imagePaint);
    }
    private void drawCore(Canvas c){
        float light=.84f+.16f*(float)Math.sin(elapsed*2.2f);
        c.save();c.translate(586f,520f);c.scale(1f,1f);
        coreGlow.setAlpha((int)(115*light));
        c.drawCircle(0,0,110f,coreGlow);
        c.restore();
    }
    private void drawStars(Canvas c){
        for(int i=0;i<STARS_COUNT;i++){
            Star s=stars[i];
            float wave=.55f+.45f*(float)Math.sin(elapsed*s.speed+s.phase);
            // Keep Niagara's left-side icons readable.
            float leftSide=s.x<base.getWidth()*.46f?.26f:1f;
            int alpha=Math.min(170,(int)(130f*s.depth*wave*leftSide));
            effects.setColor(Color.argb(alpha,210,226,255));
            c.drawCircle(s.x,s.y,s.size*(.8f+.28f*wave),effects);
        }
    }
    private void drawDust(Canvas c){
        final int active=battery?32:DUST_COUNT;
        for(int i=0;i<active;i++){
            Dust d=dust[i];
            float theta=d.theta+elapsed*d.speed*intensity;
            float spiral=theta-d.radius*.0085f;
            float rr=d.radius+7f*(float)Math.sin(theta*2f+d.phase);
            float x=586f+rr*(float)Math.cos(spiral);
            float y=520f+rr*(float)Math.sin(spiral)*.78f;
            if(x<210||x>735||y<160||y>970)continue;
            float tw=.68f+.32f*(float)Math.sin(elapsed*3f+d.phase);
            int alpha=Math.min(225,(int)(146*tw*d.depth));
            if(d.tint==0) effects.setColor(Color.argb(alpha,255,208,230));
            else if(d.tint==1)effects.setColor(Color.argb(alpha,156,185,255));
            else if(d.tint==2)effects.setColor(Color.argb(alpha,210,124,255));
            else effects.setColor(Color.argb(alpha,224,241,255));
            c.drawCircle(x,y,d.size*tw,effects);
        }
    }
    private void drawPulses(Canvas c,float dt){
        for(int i=pulses-1;i>=0;i--){
            Ripple p=ripples[i];p.age+=dt;
            if(p.age>1.1f){
                for(int j=i+1;j<pulses;j++){
                    Ripple a=ripples[j-1],b=ripples[j];a.x=b.x;a.y=b.y;a.age=b.age;
                }
                pulses--;
                continue;
            }
            float fade=1f-p.age/1.1f;
            pulsePaint.setColor(Color.argb((int)(95*fade),142,184,255));
            c.drawCircle(p.x,p.y,13f+p.age*105f,pulsePaint);
        }
    }
}
