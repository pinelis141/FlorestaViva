#!/usr/bin/env python3
"""Idempotent source update for stable scene-compositing v0.6."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'android/app/src/main/java/br/com/florestaviva/livewallpaper'
p=J/'KnightScene.java'; s=p.read_text()
s=s.replace('''            drawAtlas(c,breathAtlas,377,687,198,251,4,16,2.85f,0f);
            drawAtlas(c,capeAtlas,542,696,338,464,6,24,1.75f,wind);''','''            // Independent region-replacement atlases: animated pixels displace original
            // paint exactly once. Both assets fade to the untouched base at their edge.
            drawAtlas(c,breathAtlas,351,612,255,369,4,16,2.85f,0f);
            drawAtlas(c,capeAtlas,510,684,402,491,4,24,1.75f,wind);''')
s=s.replace('''        drawPose(c,atlas,first,columns,fw,fh,x,y,1f-weight);
        drawPose(c,atlas,second,columns,fw,fh,x,y,weight);''','''        // The first full-opacity pose replaces the base scene. Alpha-blend only
        // the next pose: drawing both at fractional opacity leaves a static ghost.
        drawPose(c,atlas,first,columns,fw,fh,x,y,1f);
        if(weight>.003f) drawPose(c,atlas,second,columns,fw,fh,x,y,weight);''')
s=s.replace('''        for(float x:new float[]{457f,483f}){''','''        for(int eye=0;eye<2;eye++){
            float x=eye==0?457f:483f;''')
s=s.replace('''    private final float[] candleX={''','''    // 0 allocations in the renderer's per-frame paths.
    private final Path swordPath = new Path();
    private final float[] candleX={''')
s=s.replace('''            Path path=new Path();''','''            swordPath.reset();''')
s=s.replace('''if(i==0)path.moveTo(x,y);else path.lineTo(x,y);''','''if(i==0)swordPath.moveTo(x,y);else swordPath.lineTo(x,y);''')
s=s.replace('''            c.drawPath(path,pen);''','''            c.drawPath(swordPath,pen);''')
s=s.replace('import android.graphics.Path;','import android.graphics.Path;')
p.write_text(s)

p=J/'ForestWallpaperService.java';s=p.read_text()
s=s.replace('''import android.view.SurfaceHolder;''','''import android.view.SurfaceHolder;
import android.util.Log;''')
s=s.replace('''        private final Sensor rotationSensor;''','''        private final Sensor rotationSensor;
        private final float[] rotationMatrix=new float[9];
        private final float[] orientation=new float[3];
        private boolean hardwareCanvasFailed=false;
        private long lastErrorLog=0;''')
s=s.replace('''            if(visible) { stopSensor(); startSensor(); }''','''            // Slider updates should not repeatedly unregister/register the sensor.
            if(visible && ("scene".equals(key)||"knight_tilt".equals(key)||"tilt".equals(key))){
                stopSensor(); startSensor();
            }''')
s=s.replace('''                float[] r=new float[9];
                SensorManager.getRotationMatrixFromVector(r,e.values);
                float[] o=new float[3];
                SensorManager.getOrientation(r,o);
                targetX=clamp(o[2]/.55f,-1,1);
                targetY=clamp(o[1]/.85f,-1,1);''','''                SensorManager.getRotationMatrixFromVector(rotationMatrix,e.values);
                SensorManager.getOrientation(rotationMatrix,orientation);
                targetX=clamp(orientation[2]/.55f,-1,1);
                targetY=clamp(orientation[1]/.85f,-1,1);''')
s=s.replace('''                if(knightSelected && !battery){
                    try { canvas=getSurfaceHolder().lockHardwareCanvas(); }
                    catch(RuntimeException hardwareUnavailable){canvas=getSurfaceHolder().lockCanvas();}
                }else{''','''                if(knightSelected && !battery && !hardwareCanvasFailed){
                    try { canvas=getSurfaceHolder().lockHardwareCanvas(); }
                    catch(RuntimeException hardwareUnavailable){
                        hardwareCanvasFailed=true;
                        canvas=getSurfaceHolder().lockCanvas();
                    }
                }else{''')
s=s.replace('''            }catch(RuntimeException ignored){
                // Surface might disappear while the home screen is being closed.
            }finally{''','''            }catch(RuntimeException renderingError){
                // Surface may disappear during launcher transitions. Log a sampled
                // diagnostic rather than hiding recurring renderer defects entirely.
                long now=SystemClock.uptimeMillis();
                if(now-lastErrorLog>30000){
                    Log.w("FlorestaViva", "Frame rendering failed", renderingError);
                    lastErrorLog=now;
                }
            }finally{''')
p.write_text(s)

p=ROOT/'android/app/build.gradle';s=p.read_text().replace("versionCode 5","versionCode 6").replace("versionName '0.5.0-remaster'","versionName '0.6.0-quality-audit'");p.write_text(s)
print('Patched scene, service and Gradle version')
