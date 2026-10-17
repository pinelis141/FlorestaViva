# Wallpaper Quality Standard 1.0

**Scope:** applies to all future live wallpapers in this application, regardless of theme or subject (forest, medieval fantasy, vehicles, space scenes, planets, abstract objects, etc.). This is a quality contract, not a character template.

## Visual integrity (hard gate)
- Preserve the approved illustration and composition unless a new art direction is approved.
- Never draw a full-opacity unmodified animated object over the same object in the base image.
- Moving regions must be a genuine replacement of the painted base or an isolated layer with a clean background plate.
- No detached glows, floating energy lines, opaque rectangular patches, visible sprite-atlas seams, stretched textures or stale duplicate edges.
- Validate representative frames at 0%, 25%, 50%, 75% of every cycle and at loop wraparound.

## Motion (hard gate)
- Motion must be perceivable on a phone screen at normal brightness and viewing distance, without exaggerating anatomy, mechanics or physics.
- Independent elements must have distinct rhythms; do not animate every element with one global sine wave.
- Loop endpoints must be continuous; frame interpolation must not reveal static ghost images.
- At 30 fps, frame-to-frame motion should be visibly continuous. Target up to 60 fps when the device can sustain it without overheating; never claim achieved fps without a device measurement.

## Interaction and state
- Touch events must map correctly after image crop, scale and sensor movement.
- Alternative gestures are required when the launcher reserves long presses.
- State transitions are bounded and must settle back to idle; interactions must not accumulate particles or leaked resources.

## Battery and reliability (hard gate)
- No ongoing render loop or sensor listener while invisible.
- User-selectable battery-saving profile (lower fps and particles); no network or microphone needed.
- No per-frame image loading, large object allocations, or tight busy-loop waiting.
- Measure frame times and battery cost on the target Android device, with both normal and economical settings.
- APK compilation success does **not** prove runtime stability, high fps or visual quality.

## Reusable process for any subject
1. Approve static artwork and focal composition.
2. Identify real animated subjects and local motion/lighting boundaries.
3. Create source-controlled layers, masks, sprites or parametric deformations as appropriate.
4. Render offline QA previews and inspect full-size source + crops for seams and ghosts.
5. Run static checks, compile the exact Android code and inspect the APK assets.
6. Validate on-device interaction, fluidity, thermal behaviour and visibility lifecycle.
7. Accept the wallpaper only after device testing, then use it as a visual-quality reference.

## Current Cavalier stage
v0.6 tests *local artwork-region replacement* instead of transparent cutouts over an unchanged base. This eliminates the primary static-ghost mechanism without pretending that an unobserved background was faithfully reconstructed. The motion should be evaluated on a real phone before adopting it as the gold standard.
