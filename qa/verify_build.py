#!/usr/bin/env python3
"""Fail builds on invisible galleries, old transparent atlas bugs or weak motion."""
from pathlib import Path
from PIL import Image
import numpy as np, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/"android/app/src/main/res/drawable-nodpi"
J=ROOT/"android/app/src/main/java/br/com/florestaviva/livewallpaper"
source=np.asarray(Image.open(R/"cavaleiro.webp").convert("RGB"))
assert source.shape==(1672,941,3)
for name,w,h,frames,cols,x,y in [
    ("cape_quality.webp",402,491,24,4,510,684),
    ("chest_quality.webp",255,369,16,4,351,612)]:
    img=np.asarray(Image.open(R/name))
    assert img.shape==(frames//cols*h,cols*w,3),(name,img.shape)
    a=np.stack([img[i//cols*h:(i//cols+1)*h,i%cols*w:(i%cols+1)*w] for i in range(frames)])
    original=source[y:y+h,x:x+w]
    border=np.concatenate([
        np.abs(a[:,:3].astype("int16")-original[:3]).reshape(-1),
        np.abs(a[:,-3:].astype("int16")-original[-3:]).reshape(-1),
        np.abs(a[:,:,:3].astype("int16")-original[:,:3]).reshape(-1),
        np.abs(a[:,:,-3:,:].astype("int16")-original[:,-3:]).reshape(-1)])
    err=border.mean()
    movement=np.abs(a[frames//4].astype("float32")-a[3*frames//4].astype("float32")).mean()
    assert err<3.9,(name,"seam",err)
    assert movement>4,(name,"motion",movement)
    print(name,"border error",round(err,3),"movement",round(movement,3))

ui=(J/"MainActivity.java").read_text()
scene=(J/"KnightScene.java").read_text()
service=(J/"ForestWallpaperService.java").read_text()
assert any("root.addView(gallery,new LinearLayout.LayoutParams(-1,dp(320)))" in line
   and not line.lstrip().startswith("//") for line in ui.splitlines())
assert "\\n        root.addView(gallery" not in ui
assert "FLORESTA VIVA" in ui and "DARK FANTASY" in ui
assert "drawPose(c,atlas,first,columns,fw,fh,x,y,1f)" in scene
assert "drawPose(c,atlas,second,columns,fw,fh,x,y,weight)" in scene
assert "boolean hitKnight=knightScene.onTouch" in service
assert "hitKnight && now-touchDown>1250" in service
assert "private final float[] rotationMatrix=new float[9]" in service
assert "onVisibilityChanged(boolean v)" in service
assert "versionCode 8" in (ROOT/"android/app/build.gradle").read_text()
for p in ["AndroidManifest.xml","res/xml/wallpaper.xml","res/values/strings.xml"]:
    path=ROOT/"android/app/src/main"/p if p.startswith("res/") else ROOT/"android/app/src/main"/p
    ET.parse(path)
print("PASS: original art, motion and seam thresholds, UI, input, lifecycle, XML")
