"""Visual QA clip for the exact replacement atlas algorithm (not a device recording)."""
from pathlib import Path
import cv2, numpy as np, math
from PIL import Image
base=Path(__file__).resolve().parents[1]
res=base/'android/app/src/main/res/drawable-nodpi';out=base/'qa'
w,h=470,836; factor=w/941
source=cv2.resize(np.array(Image.open(base/'preview/reference.png').convert('RGB')),(w,h),interpolation=cv2.INTER_AREA)

def sprites(filename,cols,rows):
 a=np.array(Image.open(res/filename).convert('RGBA'))
 ph,pw=a.shape[0]//rows,a.shape[1]//cols
 return [cv2.resize(a[j*ph:(j+1)*ph,i*pw:(i+1)*pw],(round(pw*factor),round(ph*factor)),interpolation=cv2.INTER_AREA) for j in range(rows) for i in range(cols)]
cs=sprites('cape_quality.webp',4,6);bs=sprites('chest_quality.webp',4,4)

def place(dst,frame,point,opacity):
 x0,y0=[round(v*factor) for v in point]
 hh,ww=frame.shape[:2];x1,y1=min(w,x0+ww),min(h,y0+hh)
 if x1<=x0 or y1<=y0:return
 q=frame[:y1-y0,:x1-x0]
 alpha=q[:,:,3:4].astype('float32')/255*opacity
 patch=dst[y0:y1,x0:x1]
 patch[:]=patch*(1-alpha)+q[:,:,:3]*alpha

def motion(dst,frames,box,n,speed,t):
 v=(t*speed/(2*math.pi)*n)%n
 first=int(v);second=(first+1)%n;blend=v-first
 place(dst,frames[first],box,1.)
 place(dst,frames[second],box,blend)

file=out/'DarkFantasy-Quality-v06-preview.mp4'
fps=24;video=cv2.VideoWriter(str(file),cv2.VideoWriter_fourcc(*'mp4v'),fps,(w,h))
if not video.isOpened():raise RuntimeError('VideoWriter failed')
R=np.random.default_rng(23);ash=R.uniform([0,0,0],[w,h,1],size=(48,3))
for i in range(120):
 t=i/fps
 frame=source.astype('float32')
 motion(frame,bs,(351,612),16,2.85,t)
 motion(frame,cs,(510,684),24,1.75,t)
 # This is a look-dev aid; Android uses its own sword + atmosphere rendering.
 power=.77+.23*math.sin(t*5.)
 for k in range(7):
  y=(970+(t*250+k*57)%395)*factor
  x=(434+math.sin(t*5+k)*2)*factor
  cv2.line(frame,(round(x),round(y)),(round(x),round(y+10*factor)),(250,84,56),1,cv2.LINE_AA)
 for p in ash:
  xx=int((p[0]+t*(p[2]*14-7))%w)
  yy=int((p[1]-t*(12+p[2]*10))%h)
  if 0<=xx<w and 0<=yy<h:cv2.circle(frame,(xx,yy),1,(239,67,46),-1,cv2.LINE_AA)
 vid=np.uint8(np.clip(frame,0,255))
 video.write(cv2.cvtColor(vid,cv2.COLOR_RGB2BGR))
video.release()
print('QA video',file,file.stat().st_size)
