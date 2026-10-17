#!/usr/bin/env python3
"""Visual and source gates for the new Galaxy wallpaper."""
from pathlib import Path
from PIL import Image
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'android/app/src/main/res/drawable-nodpi'
J=ROOT/'android/app/src/main/java/br/com/florestaviva/livewallpaper'
art=np.asarray(Image.open(R/'galaxy_space.jpg').convert('RGB'),dtype=np.int16)
a=np.asarray(Image.open(R/'galaxy_flow.webp').convert('RGB'),dtype=np.int16)
assert art.shape==(1536,728,3),art.shape
assert a.shape==(3180,2232,3),a.shape
w,h=558,795
src=art[175:970,170:728]
frames=[a[(i//4)*h:(i//4+1)*h,(i%4)*w:(i%4+1)*w] for i in range(16)]
for f in frames:
    # Lossy WebP boundary should remain imperceptibly close to the background.
    for current,original in [(f[:5],src[:5]),(f[-5:],src[-5:]),(f[:,:5],src[:,:5]),(f[:,-5:],src[:,-5:])]:
        assert np.abs(current-original).mean()<4.5, 'Visible atlas edge'
movement=float(np.abs(frames[2]-frames[10]).mean())
assert movement>5.0, ('Flow too weak',movement)
# An exactly periodic source function means the motion is looped at phase 2π.
srcs=(J/'GalaxyScene.java').read_text()
ui=(J/'MainActivity.java').read_text()
settings=(J/'SettingsActivity.java').read_text()
service=(J/'ForestWallpaperService.java').read_text()
assert 'frame=((elapsed*16f/6.5f)' in srcs
assert 'frame-a' in srcs and 'int a=(int)frame,b=(a+1)%FRAMES' in srcs
assert 'GalaxyScene' in service and 'ensureGalaxyScene' in service
assert '"galaxy".equals' in service
assert 'GALÁXIA' in ui and 'galleryScroll' in ui
assert '"galaxy_flow"' in settings and '"galaxy_particles"' in settings
assert 'Capa ao vento' in settings
assert 'breathAtlas' not in (J/'KnightScene.java').read_text()
assert 'chestBitmap' not in service
assert 'versionCode 10' in (ROOT/'android/app/build.gradle').read_text()
print('PASS: Galaxy art untouched, atlas boundaries, measurable flow, cyclic frames, settings, and Knight regression gates. movement',round(movement,2))
