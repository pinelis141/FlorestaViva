#!/usr/bin/env python3
"""Deterministic 24-frame cape replacement atlas builder.

Inputs: approved, untouched knight art from drawable-nodpi/cavaleiro.webp.
Motion is locally warped and smoothly decays to zero at the crop boundary.
Opaque replacement tiles prevent the doubled silhouettes of previous builds.
"""
from pathlib import Path
import numpy as np, cv2, math
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/"android/app/src/main/res/drawable-nodpi"
ART=np.array(Image.open(RES/"cavaleiro.webp").convert("RGB"))
assert ART.shape == (1672,941,3)
H,W=ART.shape[:2]

def atlas(filename,box,n,cols,mask,motion,lighting):
    x0,y0,x1,y1=box
    original=ART[y0:y1,x0:x1]
    h,w=original.shape[:2]
    xx,yy=np.meshgrid(np.arange(w,dtype=np.float32),np.arange(h,dtype=np.float32))
    d=np.minimum.reduce((xx,yy,w-1-xx,h-1-yy))
    edge=np.clip(d/24,0,1)
    edge=edge*edge*(3-2*edge)
    influence=np.clip(mask,0,1)*edge
    sheet=np.empty((math.ceil(n/cols)*h,cols*w,3),dtype=np.uint8)
    for k in range(n):
        phase=math.tau*k/n
        vx,vy=motion(xx,yy,phase)
        mx=(xx-vx*influence).astype("float32")
        my=(yy-vy*influence).astype("float32")
        warped=cv2.remap(original,mx,my,cv2.INTER_CUBIC,
                         borderMode=cv2.BORDER_REFLECT101)
        change=lighting(xx,yy,phase)*influence
        tile=np.uint8(np.clip(warped.astype("float32")*(1+change[...,None]),0,255))
        tile[:2]=original[:2]
        tile[-2:]=original[-2:]
        tile[:,:2]=original[:,:2]
        tile[:,-2:]=original[:,-2:]
        sy,sx=(k//cols)*h,(k%cols)*w
        sheet[sy:sy+h,sx:sx+w]=tile
    Image.fromarray(sheet,"RGB").save(RES/filename,"WEBP",quality=95,method=6)
    print("Built:",filename,"frames:",n,"pixels:",sheet.shape)

cape_box=(510,684,912,1175)
polygon=np.array([(547,703),(563,722),(572,755),(592,786),(642,827),
    (714,863),(787,910),(830,941),(875,969),(858,1001),
    (885,1028),(864,1061),(882,1105),(864,1144),(832,1145),
    (783,1128),(752,1141),(711,1116),(668,1100),(614,1056),
    (587,1005),(567,962),(549,913),(537,854),(538,790)],dtype=np.int32)
region=np.zeros((H,W),dtype=np.uint8)
cv2.fillPoly(region,[polygon],255)
cape_mask=cv2.GaussianBlur(region,(0,0),11)[684:1175,510:912].astype("float32")/255
colors=ART[684:1175,510:912].astype("float32")
confidence=np.clip((colors[:,:,0]-colors[:,:,1]+7)/27,0,1)*np.clip(
    (colors[:,:,0]-colors[:,:,2]+13)/30,0,1)
cape_mask*=.32+.68*confidence

def cape_move(x,y,t):
    depth=np.clip((y+10)/450,0,1)**1.45
    dx=depth*(33*np.sin(t-y/83)+13*np.sin(2*t+x/64-y/42))
    dy=depth*(6.6*np.cos(t*1.13-y/97)+2.6*np.sin(2*t+x/71))
    return dx,dy

def cape_light(x,y,t):
    return (0.085*np.sin(2*t+x/48-y/110)+.025*np.sin(3*t+x/29))*np.clip((y+10)/460,0,1)

# O peitoral permanece inteiramente estático na arte original.
# Apenas a capa gera quadros de substituição.
atlas("cape_quality.webp",cape_box,24,4,cape_mask,cape_move,cape_light)
