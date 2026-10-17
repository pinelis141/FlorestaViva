package br.com.florestaviva.livewallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Live Wallpaper real, desenhado continuamente em Canvas. Nenhuma conexão com internet
 * ou permissão de localização é necessária. FPS e sensores param quando invisível.
 */
public class ForestWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new ForestEngine(); }

    private final class ForestEngine extends Engine
            implements SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Random random = new Random(4608);
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint rectPaint = new Paint();
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mistPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Firefly[] bugs = new Firefly[90];
        private final Leaf[] leaves = new Leaf[170];
        private final List<Ripple> ripples = new ArrayList<>();
        private final List<Spark> sparks = new ArrayList<>();
        private final SharedPreferences prefs;
        private final SensorManager sensorManager;
        private final Sensor sensor;
        private Bitmap bitmap;
        private Bitmap knightBitmap, capeBitmap, chestBitmap;
        private KnightScene knightScene;
        private boolean knightSelected=false;
        private long touchDown=0, lastTouchUp=0;
        private float artLeft, artTop, artScale=1f;
        private final Rect waterSrc = new Rect();
        private final RectF waterDst = new RectF();
        // x, y, width, height, downward speed (in original artwork pixels/second)
        private final float[][] waterfalls = {
                {709,897,40,128,39},
                {798,1062,39,156,46},
                {598,1139,65,44,32},
                {608,1172,53,79,48},
                {536,1327,94,62,52}
        };
        private final float[][][] fallPolygons = {
                {{714,909},{720,903},{736,895},{749,902},{747,961},{743,1008},{738,1025},{719,1024},{712,982}},
                {{804,1075},{819,1058},{836,1063},{831,1140},{828,1194},{823,1218},{802,1215},{799,1161}},
                {{600,1143},{619,1140},{652,1140},{663,1147},{654,1175},{619,1182},{605,1172}},
                {{619,1181},{642,1170},{657,1180},{659,1233},{641,1251},{615,1248},{609,1217}},
                {{540,1340},{561,1334},{574,1327},{591,1331},{613,1325},{625,1331},{628,1354},{618,1385},{541,1390},{536,1369}}
        };
        private int width=1, height=1;
        private boolean visible=false, dead=false, sensorRegistered=false;
        private boolean fire=true, fog=true, foliage=true, water=true, tiltEnabled=true, battery=false;
        private int count=48;
        private float time=0f, tiltX=0f, tiltY=0f, targetX=0f,targetY=0f,scroll=0.5f, gust=0f;
        private long lastFrame=0;

        private final Runnable drawRunnable = new Runnable() {
            @Override public void run() {
                if (!visible || dead) return;
                long frameStart=SystemClock.uptimeMillis();
                float dt=lastFrame==0 ? .0167f : Math.min(.08f,(frameStart-lastFrame)/1000f);
                lastFrame=frameStart;
                drawFrame(dt);
                // Frame-start paced: unlike postDelayed-after-render, render time
                // is included in the frame budget, avoiding uneven slow motion.
                int period=battery?42:(knightSelected && prefs.getBoolean("knight_60fps",true)?17:33);
                long next=Math.max(frameStart+period,SystemClock.uptimeMillis()+1L);
                if(visible && !dead)handler.postAtTime(this,next);
            }
        };

        ForestEngine() {
            setTouchEventsEnabled(true);
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inScaled=false;
            bitmap=BitmapFactory.decodeResource(getResources(),R.drawable.floresta,options);
            imagePaint.setFilterBitmap(false);
            glowPaint.setShader(new RadialGradient(0,0,22,
                    new int[]{0xCDECF6A4,0x5AD4E99E,0x00C4E88A},
                    new float[]{0f,.38f,1f}, Shader.TileMode.CLAMP));
            mistPaint.setShader(new RadialGradient(0,0,120,
                    new int[]{0x1897CEBB,0x0783BCB7,0x0083BCB7},
                    new float[]{0f,.53f,1f},Shader.TileMode.CLAMP));
            ripplePaint.setStyle(Paint.Style.STROKE);
            ripplePaint.setStrokeWidth(2f);
            prefs=getSharedPreferences(SettingsActivity.PREFS,MODE_PRIVATE);
            prefs.registerOnSharedPreferenceChangeListener(this);
            reloadPrefs();
            sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
            sensor=sensorManager==null?null:sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            for(int i=0;i<bugs.length;i++) {
                Firefly b=new Firefly();
                b.x=between(.03f,.97f);b.y=between(.08f,.97f);
                b.vx=between(-.006f,.006f);b.vy=between(-.004f,.004f);
                b.phase=between(0,6.28f);b.size=between(.5f,1.5f);b.depth=between(.4f,1f);
                bugs[i]=b;
            }
            for(int i=0;i<leaves.length;i++) {
                Leaf l=new Leaf();
                float zone=random.nextFloat();
                if(zone<.48f){l.x=between(-.03f,1.03f);l.y=between(-.03f,.22f);}
                else if(zone<.72f){l.x=between(-.05f,.20f);l.y=between(0,.75f);}
                else {l.x=between(.84f,1.04f);l.y=between(0,.66f);}
                l.size=between(1.4f,3.5f);l.phase=between(0,6.28f);
                l.depth=between(.4f,1f);l.color=random.nextInt(3);
                leaves[i]=l;
            }
        }
        private float between(float a,float b){return a+random.nextFloat()*(b-a);}
        private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
        private void reloadPrefs(){
            knightSelected="knight".equals(prefs.getString("scene","forest"));
            if(knightSelected){ensureKnightScene();knightScene.load(prefs);}
            fire=prefs.getBoolean("fire",true);
            fog=prefs.getBoolean("fog",true);
            foliage=prefs.getBoolean("leaves",true);
            water=prefs.getBoolean("water",true);
            tiltEnabled=prefs.getBoolean("tilt",true);
            battery=prefs.getBoolean("battery",false);
            count=Math.max(10,Math.min(85,prefs.getInt("count",48)));
        }
        @Override public void onSharedPreferenceChanged(SharedPreferences shared,String key){
            reloadPrefs();
            if(visible) { stopSensor(); startSensor(); }
        }
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int w,int h){
            super.onSurfaceChanged(holder,format,w,h);
            width=Math.max(1,w);height=Math.max(1,h);
            if(visible)drawFrame(0f);
        }
        @Override public void onVisibilityChanged(boolean v){
            visible=v;
            handler.removeCallbacks(drawRunnable);
            if(v&&!dead){lastFrame=0;startSensor();handler.post(drawRunnable);}
            else stopSensor();
        }
        @Override public void onOffsetsChanged(float xOffset,float yOffset,float xOffsetStep,
                                                float yOffsetStep,int xPixelOffset,int yPixelOffset){
            scroll=xOffset;
        }
        @Override public void onTouchEvent(MotionEvent ev){
            int action=ev.getActionMasked();
            if(knightSelected){
                ensureKnightScene();
                if(action==MotionEvent.ACTION_DOWN){touchDown=SystemClock.uptimeMillis();}
                else if(action==MotionEvent.ACTION_UP){
                    long now=SystemClock.uptimeMillis();
                    float px=clamp(ev.getX()/width,0,1),py=clamp(ev.getY()/height,0,1);
                    boolean doubleTap=now-lastTouchUp<400;
                    knightScene.onTouch(px,py,doubleTap);
                    if(now-touchDown>1700)knightScene.tryAwaken();
                    lastTouchUp=now;
                }
                return;
            }
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE){
                // Launchers may reserve some gestures for their own UI.
                if(action==MotionEvent.ACTION_DOWN || random.nextFloat()<.18f){
                    float x=clamp(ev.getX()/width,0,1),y=clamp(ev.getY()/height,0,1);
                    disturb(x,y);
                    gust=(x-.5f)*10;
                }
            }
        }
        private void disturb(float x,float y){
            if(ripples.size()>=10)ripples.remove(0);
            Ripple r=new Ripple();r.x=x;r.y=y;r.age=0f;ripples.add(r);
            if(fire){
                for(Firefly b:bugs){
                    float dx=b.x-x,dy=(b.y-y)*(height/(float)width),d=(float)Math.hypot(dx,dy);
                    if(d<.43f){float strength=(.43f-d)/Math.max(.10f,d)*.20f;
                        b.vx+=dx*strength; b.vy+=Math.signum(dy)*strength*.4f;
                    }
                }
                for(int i=0;i<11;i++){
                    Spark p=new Spark();p.x=x;p.y=y;p.vx=between(-.10f,.10f);
                    p.vy=between(-.14f,.14f);p.life=between(.25f,.65f);sparks.add(p);
                }
                while(sparks.size()>110)sparks.remove(0);
            }
        }
        private void startSensor(){
            if((knightSelected?prefs.getBoolean("knight_tilt",true):tiltEnabled) && !sensorRegistered && sensorManager!=null && sensor!=null) {
                sensorRegistered=sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI);
            }
        }
        private void stopSensor(){
            if(sensorRegistered && sensorManager!=null){sensorManager.unregisterListener(this);sensorRegistered=false;}
        }
        @Override public void onSensorChanged(SensorEvent e){
            if(e.sensor.getType()!=Sensor.TYPE_ACCELEROMETER)return;
            targetX=clamp(-e.values[0]/7f,-1,1);
            targetY=clamp(e.values[1]/8f,-1,1);
        }
        @Override public void onAccuracyChanged(Sensor s,int accuracy){}

        private void ensureKnightScene(){
            if(knightScene!=null)return;
            BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
            knightBitmap=BitmapFactory.decodeResource(getResources(),R.drawable.cavaleiro,options);
            capeBitmap=BitmapFactory.decodeResource(getResources(),R.drawable.capa,options);
            chestBitmap=BitmapFactory.decodeResource(getResources(),R.drawable.peitoral,options);
            knightScene=new KnightScene(knightBitmap,capeBitmap,chestBitmap);
            knightScene.load(prefs);
        }
        private void drawFrame(float dt){
            time+=dt;
            tiltX+=(targetX-tiltX)*.06f;
            tiltY+=(targetY-tiltY)*.06f;
            gust*=.96f;
            Canvas canvas=null;
            try {
                canvas=getSurfaceHolder().lockCanvas();
                if(canvas==null)return;
                final int w=canvas.getWidth(),h=canvas.getHeight();
                if(w<=0||h<=0)return;
                canvas.drawColor(Color.rgb(3,16,22));
                if(knightSelected){
                    ensureKnightScene();
                    knightScene.draw(canvas,w,h,dt,tiltX,tiltY);
                    return;
                }
                drawArt(canvas,w,h);
                drawLighting(canvas,w,h);
                if(fog)drawFog(canvas,w,h);
                if(water)drawWater(canvas,w,h);
                if(foliage)drawLeaves(canvas,w,h);
                if(fire)drawFireflies(canvas,w,h,dt);
                drawRipples(canvas,w,h,dt);
            }catch(RuntimeException ignored){
                // Surface might disappear while the home screen is being closed.
            }finally{
                if(canvas!=null){try{getSurfaceHolder().unlockCanvasAndPost(canvas);}catch(RuntimeException ignored){}}
            }
        }
        private void drawArt(Canvas c,int w,int h){
            if(bitmap==null||bitmap.isRecycled())return;
            float scale=Math.max(w/(float)bitmap.getWidth(),h/(float)bitmap.getHeight())*1.09f;
            float bw=bitmap.getWidth()*scale,bh=bitmap.getHeight()*scale;
            float ox=tiltEnabled?tiltX*w*.012f:0;
            float oy=tiltEnabled?tiltY*h*.008f:0;
            ox+=(scroll-.5f)*w*.014f;
            artLeft=(w-bw)*.5f+ox;
            artTop=(h-bh)*.5f+oy;
            artScale=scale;
            c.drawBitmap(bitmap,null,new RectF(artLeft,artTop,artLeft+bw,artTop+bh),imagePaint);
        }
        private void drawLighting(Canvas c,int w,int h){
            if(bitmap==null)return;
            c.save();c.translate(artLeft,artTop);c.scale(artScale,artScale);
            float breath=.5f+.5f*(float)Math.sin(time*.52f);
            float mx=565f+(float)Math.sin(time*.14f)*12f;
            float my=397f;
            lightPaint.setShader(new RadialGradient(mx,my,350,
                    new int[]{
                            Color.argb((int)((.11f+.09f*breath)*255),238,253,214),
                            Color.argb((int)((.025f+.035f*breath)*255),137,228,211),
                            Color.TRANSPARENT},
                    new float[]{0,.45f,1},Shader.TileMode.CLAMP));
            c.drawRect(200,80,830,790,lightPaint);
            lightPaint.setShader(null);
            // Irregular shafts of light drift slowly through the canopy.
            for(int i=0;i<3;i++){
                float sway=(float)Math.sin(time*.24f+i*1.8f)*20;
                float gx=410+i*94+sway;
                Path beam=new Path();beam.moveTo(540,320);
                beam.lineTo(gx+75,1260);beam.lineTo(gx-110,1260);beam.close();
                lightPaint.setShader(new android.graphics.LinearGradient(gx-105,650,gx+95,1300,
                        new int[]{Color.TRANSPARENT,
                                Color.argb((int)((.012f+.011f*breath)*255),186,248,213),Color.TRANSPARENT},
                        new float[]{0,.5f,1},Shader.TileMode.CLAMP));
                c.drawPath(beam,lightPaint);
            }
            lightPaint.setShader(null);
            for(int i=0;i<48;i++){
                float lx=115+(i*67)%725+(float)Math.sin(time*.58f+i*.7f)*11;
                float ly=955+(i*53)%418+(float)Math.cos(time*.47f+i)*5;
                float span=5+(i%5)*5;
                rectPaint.setColor(Color.argb((int)((.08f+.055f*(.5f+.5f*(float)Math.sin(time*.48f+i)))*255),3,24,29));
                c.drawRect(lx,ly,lx+span,ly+3+(i%3)*3,rectPaint);
                c.drawRect(lx+span*.2f,ly+4,lx+span*.85f,ly+7,rectPaint);
            }
            c.restore();
        }
        private void drawFog(Canvas c,int w,int h){
            for(int i=0;i<4;i++){
                float x=(.10f+i*.27f+(float)Math.sin(time*.16f+i*2.4f)*.1f)*w;
                float y=(.50f+i*.105f+(float)Math.cos(time*.19f+i)*.018f)*h;
                c.save();c.translate(x,y);c.scale(w/230f,h/820f);
                c.drawCircle(0,0,120,mistPaint);c.restore();
            }
        }
        private void drawLeaves(Canvas c,int w,int h){
            float[][] clusters={{325,165,1},{440,228,1.15f},{711,157,.85f},
                    {828,318,1.05f},{122,496,.72f},{332,73,.9f}};
            c.save();c.translate(artLeft,artTop);c.scale(artScale,artScale);
            Paint stem=new Paint(Paint.ANTI_ALIAS_FLAG);
            stem.setColor(Color.argb(225,5,36,42));stem.setStyle(Paint.Style.STROKE);stem.setStrokeWidth(3);
            for(int k=0;k<clusters.length;k++){
                float cx=clusters[k][0],cy=clusters[k][1],a=clusters[k][2];
                float wind=((float)Math.sin(time*.88f+k*1.27f)*16+
                        (float)Math.sin(time*.37f+k)*6)*a+gust*.42f;
                Path twig=new Path();twig.moveTo(cx,cy-32);
                twig.quadTo(cx+wind*.28f,cy-8,cx+wind,cy+26);
                c.drawPath(twig,stem);
                for(int j=0;j<42;j++){
                    float angle=j*2.39996f,rr=(float)Math.sqrt(j)*6.1f;
                    float x=cx+wind*(.25f+.75f*j/42f)+(float)Math.cos(angle)*rr;
                    float y=cy+(float)Math.sin(angle)*rr*.72f+(float)Math.cos(time*.65f+j*.6f+k)*2.7f;
                    float sz=3+(j%4)*2;
                    int color=j%9==0?0xE064A076:j%6==0?0xF526655F:j%4==0?0xEB114A4F:0xF7042C37;
                    rectPaint.setColor(color);
                    c.drawRect(Math.round(x),Math.round(y),Math.round(x)+sz,Math.round(y)+3+(j%3)*2,rectPaint);
                }
            }
            for(int i=0;i<22;i++){
                float velocity=24+(i%6)*9;
                float fy=130+((i*149+time*velocity)%1320);
                float fx=135+((i*109+time*(9+(i%4)*5))%710)+(float)Math.sin(time*.8f+i)*22;
                float angle=(float)Math.sin(time*.72f+i)*.35f;
                c.save();c.translate(Math.round(fx),Math.round(fy));c.rotate((float)Math.toDegrees(angle));
                rectPaint.setColor(i%5==0?0xD1B4C05B:i%3==0?0xE04C9A77:0xE8205C5A);
                c.drawRect(-3,-2,4,2,rectPaint);c.drawRect(-1,2,2,4,rectPaint);
                c.restore();
            }
            c.restore();
            float unit=Math.max(1.7f,w/260f);
            for(Leaf l:leaves){
                float x=l.x*w+(float)Math.sin(time*.75f+l.phase)*7.6f*l.depth+gust*.2f;
                float y=l.y*h+(float)Math.cos(time*.65f+l.phase)*3.4f;
                rectPaint.setColor(l.color==0?0xA809323A:l.color==1?0xC6092831:0x99538B7B);
                int ix=(int)(x/unit),iy=(int)(y/unit);
                c.drawRect(ix*unit,iy*unit,ix*unit+l.size*unit,iy*unit+Math.max(unit,l.size*unit*.7f),rectPaint);
            }
        }
        private void drawWater(Canvas c,int w,int h){
            if(bitmap==null || bitmap.isRecycled())return;
            c.save();c.translate(artLeft,artTop);c.scale(artScale,artScale);
            for(int index=0;index<waterfalls.length;index++){
                float[] f=waterfalls[index];
                float x=f[0],y=f[1],fw=f[2],fh=f[3],offset=(time*f[4])%fh;
                Path waterMask=new Path();
                float[][] polygon=fallPolygons[index];
                waterMask.moveTo(polygon[0][0],polygon[0][1]);
                for(int j=1;j<polygon.length;j++)waterMask.lineTo(polygon[j][0],polygon[j][1]);
                waterMask.close();
                c.save();c.clipPath(waterMask);
                // Two wrapped bitmap sections: the original waterfall pixels move downward.
                waterSrc.set((int)x,(int)y,(int)(x+fw),(int)(y+fh));
                waterDst.set(x,y+offset,x+fw,y+fh+offset);
                c.drawBitmap(bitmap,waterSrc,waterDst,imagePaint);
                waterDst.set(x,y+offset-fh,x+fw,y+offset);
                c.drawBitmap(bitmap,waterSrc,waterDst,imagePaint);
                for(int lane=0;lane<11;lane++){
                    float lx=x+3+(lane+.34f)*(fw-7)/11f;
                    for(int n=0;n<5;n++){
                        float yy=y+((n*fh/5f+time*f[4]*1.24f+lane*18.7f)%(fh+10))-5;
                        float a=.25f+.30f*(.5f+.5f*(float)Math.sin(time*2+lane*1.7f+n));
                        rectPaint.setColor(Color.argb((int)(clamp(a,0,1)*255),196,251,242));
                        float lineW=lane%3==0?3:2;
                        c.drawRect(lx,yy,lx+lineW,yy+6+lane%4*3,rectPaint);
                        rectPaint.setColor(Color.argb((int)(clamp(a*.72f,0,1)*255),238,254,224));
                        c.drawRect(lx+1,yy+3,lx+2,yy+6,rectPaint);
                    }
                }
                c.restore();
                if(index>=2){
                    float cy=y+fh-2;
                    for(int k=0;k<12;k++){
                        float phase=(time*.75f+k*.135f+index*.2f)%1;
                        float xx=x+(k+.3f)*fw/12+(float)Math.sin(time*2+k)*2.2f;
                        float yy=cy+phase*14-6;
                        rectPaint.setColor(Color.argb((int)(.50f*(1-phase)*255),208,255,244));
                        c.drawRect(xx,yy,xx+(k%3==0?3:2),yy+(k%4==0?3:2),rectPaint);
                    }
                }
            }
            for(int i=0;i<75;i++){
                float v=(i*.618034f)%1;
                float yy=1392+(i%22)*5.6f;
                float span=122+Math.max(0,yy-1400)*1.1f;
                float xx=574+(v-.5f)*span+(float)Math.sin(time*.83f+i)*5;
                float a=.11f+.25f*(.5f+.5f*(float)Math.sin(time*2.5f+i*.8f));
                rectPaint.setColor(Color.argb((int)(a*255),225,253,201));
                c.drawRect(xx,yy,xx+3+(i%4)*6,yy+(i%5==0?3:2),rectPaint);
            }
            ripplePaint.setStrokeWidth(2);
            for(int bx=0;bx<2;bx++){
                float cx=bx==0?581:631;
                for(int n=0;n<3;n++){
                    float q=(time*.39f+n/3f)%1;
                    float r=5+q*48;
                    ripplePaint.setColor(Color.argb((int)(.21f*(1-q)*255),170,248,228));
                    c.drawOval(cx-r,1394-r*.18f,cx+r,1394+r*.18f,ripplePaint);
                }
            }
            c.restore();
        }
        private void drawFireflies(Canvas c,int w,int h,float dt){
            for(int i=0;i<count;i++){
                Firefly b=bugs[i];
                b.vx=clamp(b.vx*.982f+(float)Math.sin(time*.8f+b.phase)*.000018f,-.20f,.20f);
                b.vy=clamp(b.vy*.987f+(float)Math.cos(time*.7f+b.phase)*.000014f,-.15f,.15f);
                b.x+=b.vx*dt+(float)Math.sin(time*.55f+b.phase)*.00003f;
                b.y+=b.vy*dt;
                if(b.x>1.05f)b.x=-.04f;else if(b.x<-.05f)b.x=1.04f;
                if(b.y>1.04f)b.y=.04f;else if(b.y<.03f)b.y=1.03f;
                float lum=.76f+.24f*(float)Math.sin(time*2.0f+b.phase);
                float x=b.x*w+(tiltEnabled?tiltX*w*.017f*b.depth:0);
                float y=b.y*h+(tiltEnabled?tiltY*h*.008f*b.depth:0);
                float size=b.size*Math.max(1f,w/520f);
                c.save();c.translate(x,y);c.scale(size,size);
                glowPaint.setAlpha((int)(clamp(lum,0,1)*245));
                c.drawCircle(0,0,22,glowPaint);
                rectPaint.setColor(Color.argb((int)(lum*255),255,249,177));
                c.drawRect(-1.5f,-1.5f,1.5f,1.5f,rectPaint);
                c.restore();
            }
        }
        private void drawRipples(Canvas c,int w,int h,float dt){
            for(int i=ripples.size()-1;i>=0;i--){
                Ripple r=ripples.get(i);r.age+=dt;
                if(r.age>.85f){ripples.remove(i);continue;}
                ripplePaint.setColor(Color.argb((int)((1-r.age/.85f)*85),247,247,186));
                c.drawCircle(r.x*w,r.y*h,r.age*Math.min(w,h)*.20f,ripplePaint);
            }
            for(int i=sparks.size()-1;i>=0;i--){
                Spark p=sparks.get(i);p.life-=dt;
                if(p.life<=0){sparks.remove(i);continue;}
                p.x+=p.vx*dt;p.y+=p.vy*dt;
                rectPaint.setColor(Color.argb((int)(clamp(p.life*2,0,1)*245),250,244,162));
                float size=Math.max(2,w/290f);
                c.drawRect(p.x*w,p.y*h,p.x*w+size,p.y*h+size,rectPaint);
            }
        }
        @Override public void onDestroy(){
            dead=true;visible=false;
            handler.removeCallbacks(drawRunnable);
            stopSensor();prefs.unregisterOnSharedPreferenceChangeListener(this);
            if(bitmap!=null){bitmap.recycle();bitmap=null;}
            if(knightBitmap!=null){knightBitmap.recycle();knightBitmap=null;}
            if(capeBitmap!=null){capeBitmap.recycle();capeBitmap=null;}
            if(chestBitmap!=null){chestBitmap.recycle();chestBitmap=null;}
            super.onDestroy();
        }
    }
    private static final class Firefly{float x,y,vx,vy,size,phase,depth;}
    private static final class Leaf{float x,y,size,phase,depth;int color;}
    private static final class Ripple{float x,y,age;}
    private static final class Spark{float x,y,vx,vy,life;}
}
