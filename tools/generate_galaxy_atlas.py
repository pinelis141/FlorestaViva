#!/usr/bin/env python3
"""Reproducible region-replacement flow atlas from the user's unaltered Galaxy art.

A cycle consists of 16 exact, RGB replacement tiles with tapered motion at all
four boundaries. No invented structures, detached glows or drifting rectangle.
"""
from pathlib import Path
import math
import cv2
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / 'android/app/src/main/res/drawable-nodpi'
ART = np.asarray(Image.open(DRAWABLE / 'galaxy_space.jpg').convert('RGB'))
assert ART.shape == (1536, 728, 3), ART.shape
X0,Y0,X1,Y1 = 170, 175, 728, 970
COLUMNS, FRAMES = 4, 16
W,H = X1-X0,Y1-Y0
base = ART[Y0:Y1, X0:X1]
xx,yy=np.meshgrid(np.arange(W,dtype=np.float32),np.arange(H,dtype=np.float32))
x,y=xx+X0, yy+Y0
cx,cy=586.,520.
dx=x-cx; dy=y-cy
r=np.sqrt(dx*dx+dy*dy)+1.e-5
angle=np.arctan2(dy,dx)
# Window in the galaxy's upper right region; must fade to original at seam.
edge=np.minimum.reduce((xx,yy,W-1-xx,H-1-yy))
edge=np.clip(edge/52.,0,1)
edge=edge*edge*(3-2*edge)
shape=np.exp(-.5*((dx/285.)**2+(dy/325.)**2))
# Keep far-left app icons and the lower planet almost entirely static.
mask=shape*edge
mask*=np.clip((990.-y)/180,0,1)
# Spiral and radial flow are cyclic, not monotonically rotating the entire photo.
frames=[]
for i in range(FRAMES):
    t=2.*math.pi*i/FRAMES
    # Travelling spiral phase, varying with distance so that arms have flowing texture.
    shift=(.077*np.sin(t-r*.018 + angle*.55) + .017*np.sin(2*t + r*.014))
    # Slightly different motion at each radius; never warp the center wildly.
    displacement=mask*np.clip(r/180.,.10,1.0)
    rotation=shift*displacement
    radial=3.6*np.sin(t+r*.016-angle*1.4)*mask
    new_r=r+radial
    new_ang=angle+rotation
    sx=(cx + new_r*np.cos(new_ang)-X0).astype(np.float32)
    sy=(cy + new_r*np.sin(new_ang)-Y0).astype(np.float32)
    tile=cv2.remap(base,sx,sy,cv2.INTER_CUBIC,borderMode=cv2.BORDER_REFLECT101)
    # Preserve the entire perimeter exactly, no visible rectangular seam.
    tile[:4,:,:]=base[:4,:,:];tile[-4:,:,:]=base[-4:,:,:]
    tile[:,:4,:]=base[:,:4,:];tile[:,-4:,:]=base[:,-4:,:]
    frames.append(tile)
atlas=np.empty((4*H,4*W,3),dtype=np.uint8)
for i,f in enumerate(frames):
    top=i//4*H;left=i%4*W
    atlas[top:top+H,left:left+W,:]=f
Image.fromarray(atlas,'RGB').save(DRAWABLE/'galaxy_flow.webp','WEBP',quality=92,method=5)
# Record measurable image checks for reproducibility.
seam=np.mean(np.abs(frames[0][0:4].astype('float32')-base[0:4]))
movement=np.mean(np.abs(frames[2].astype('float32')-frames[10].astype('float32')))
print('Galaxy flow atlas',atlas.shape,'seam error',round(float(seam),3),'movement',round(float(movement),3),'bytes',(DRAWABLE/'galaxy_flow.webp').stat().st_size)
assert seam==0 and movement>1.0,('Atlas failed visual test',seam,movement)
