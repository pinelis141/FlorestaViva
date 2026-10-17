#!/usr/bin/env python3
"""Construct duplicate-free animated region patches from source artwork.

Each frame contains a *replacement render of the original image region*, not a
cutout laid over the same character. A full-alpha core and zero-displacement
feathered edge avoid duplicate silhouettes and hard seams. This is a reversible,
non-generative treatment of the approved reference; it does not hallucinate a
missing painted background. Call this a compositing fix, not true cape removal.
"""
from pathlib import Path
from PIL import Image
import cv2, numpy as np, math
BASE=Path(__file__).resolve().parents[1]
RES=BASE/'android/app/src/main/res/drawable-nodpi'
QA=BASE/'qa'; QA.mkdir(exist_ok=True)
src=np.array(Image.open(BASE/'preview/reference.png').convert('RGB'))
H,W=src.shape[:2]

def soft_region_mask(box, pts=None, ell=None, blur=17):
 x0,y0,x1,y1=box
 region=np.zeros((H,W),np.uint8)
 if pts is not None:cv2.fillPoly(region,[np.array(pts,np.int32)],255)
 if ell is not None:cv2.ellipse(region,ell[0],ell[1],ell[2],0,360,255,-1)
 # The stencil is only to restrict motion and compositing to a local artwork region.
 soft=cv2.GaussianBlur(region,(0,0),blur)/255.0
 return soft[y0:y1,x0:x1].astype('float32')

def atlas_file(name,box,n,cols,mask,move,quality=94):
 x0,y0,x1,y1=box
 crop=src[y0:y1,x0:x1]
 ht,wd=crop.shape[:2]
 ax,ay=np.meshgrid(np.arange(wd,dtype='float32'),np.arange(ht,dtype='float32'))
 rows=math.ceil(n/cols)
 atlas=np.zeros((rows*ht,cols*wd,4),np.uint8)
 # Core alpha fully replaces artwork; in the feather perimeter, motion approaches zero.
 alpha=np.clip(mask*3.0,0,1)
 # No alpha at atlas borders, so interpolation never exposes rectangular crop edges.
 safe=np.clip(np.minimum.reduce([ax,ay,wd-1-ax,ht-1-ay])/18,0,1)
 alpha=alpha*safe
 assert np.max(alpha)>.99, (name,alpha.max())
 for i in range(n):
  ph=2*math.pi*i/n
  dx,dy=move(ax,ay,ph)
  dx=dx*mask*safe
  dy=dy*mask*safe
  mx=(ax-dx).astype('float32');my=(ay-dy).astype('float32')
  warped=cv2.remap(crop,mx,my,cv2.INTER_CUBIC,borderMode=cv2.BORDER_REFLECT101)
  # Use full-alpha core and feathered edge where image is nearly identical to base.
  f=np.dstack([warped,np.uint8(np.clip(alpha*255,0,255))]);a=(i//cols)*ht;b=(i%cols)*wd
  atlas[a:a+ht,b:b+wd]=f
 Image.fromarray(atlas,'RGBA').save(RES/name,'WEBP',quality=quality,method=5)
 Image.fromarray((mask*255).astype('uint8')).save(QA/(name+'.mask.png'))
 print(name,'size',atlas.shape,'file KiB',round((RES/name).stat().st_size/1024,1),'alpha max',round(float(np.max(alpha)),3))
 return atlas

# Animated zone specifically traces the torn red cloth, with controlled padding.
cape_box=(510,684,912,1175)
cape_polygon=[(547,703),(563,722),(572,755),(592,786),(642,827),(714,863),(787,910),
 (830,941),(875,969),(858,1001),(885,1028),(864,1061),(882,1105),(864,1144),
 (832,1145),(783,1128),(752,1141),(711,1116),(668,1100),(614,1056),
 (587,1005),(567,962),(549,913),(537,854),(538,790)]
cm=soft_region_mask(cape_box,pts=cape_polygon,blur=13)

def cape_move(x,y,t):
 depth=np.clip((y+15)/475,0,1)**1.65
 # Two independently travelling waves; positive displacement is rightward.
 dx=(23*np.sin(t-y/85)+9*np.sin(2*t+x/77-y/38))*depth
 dy=(4.5*np.sin(t*1.1+y/80)+2*np.sin(2*t+x/50))*depth
 return dx.astype('float32'),dy.astype('float32')

# The torso patch replaces painted pixels within the armor, not a glowing overlay.
chest_box=(351,612,606,981)
cmask=soft_region_mask(chest_box,ell=((475,815),(99,155),0),blur=15)
def breath_move(x,y,t):
 dx=2.2*np.sin(t)*np.sin(np.clip(y/368,0,1)*math.pi)
 dy=-5.2*np.sin(t)*np.exp(-((y-160)/170)**2)
 return np.asarray(dx,dtype='float32'),np.asarray(dy,dtype='float32')

c=atlas_file('cape_quality.webp',cape_box,24,4,cm,cape_move)
b=atlas_file('chest_quality.webp',chest_box,16,4,cmask,breath_move)
# Comparison preview (basic exact-alpha compositing, without lights or Android drawing).
def frame_region(canvas,atlas,box,n,cols,f):
 x0,y0,x1,y1=box; h=y1-y0;w=x1-x0
 a=atlas[(f//cols)*h:((f//cols)+1)*h,(f%cols)*w:((f%cols)+1)*w]
 alpha=a[:,:,3:4].astype('float32')/255
 z=canvas[y0:y1,x0:x1].astype('float32')
 canvas[y0:y1,x0:x1]=np.uint8(np.clip(z*(1-alpha)+a[:,:,:3]*alpha,0,255))
for t in [0,6,12,18]:
 q=src.copy();frame_region(q,b,chest_box,16,4,(t*16//24)%16);frame_region(q,c,cape_box,24,4,t)
 Image.fromarray(q).save(QA/f'quality_pose_{t:02}.jpg',quality=92)
print('Done')
