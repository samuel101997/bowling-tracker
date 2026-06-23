# Module design: engine:vision

## Purpose
Detect the ball in each frame, returning candidate `BallDetection`s (image px).
Implements the `BallDetector` domain port (ADR-0004).

## Why it's the only Android engine module
Ball detection needs OpenCV (motion/contours) and optionally LiteRT (a learned
detector). Those are Android-bound, so this module is an `android.library`.
**All** OpenCV/LiteRT usage is confined here, behind the `BallDetector` port, so
no other module ever sees a `Mat`/`Bitmap`/tensor (ARCHITECTURE.md P8).

## Verification (IMPORTANT)
This module **cannot be compiled or unit-tested in the planning sandbox** (no
Android SDK / OpenCV). It must be verified in Android Studio / on-device:
- Enable the OpenCV (and later LiteRT) dependency in `build.gradle.kts`.
- Implement `detect()` per the algorithm below.
- Add a **golden-frame fixture test** (instrumented): a few bundled frames with a
  hand-labelled ball position; assert detection is within N px.

## Algorithm (ARCHITECTURE.md §3.2)
1. absdiff / `BackgroundSubtractorMOG2` between consecutive frames (ball = motion).
2. threshold + morphological open to remove speckle.
3. `findContours`; keep blobs by area (minRadius..maxRadius) and circularity.
4. Emit best candidate per frame; false positives are fine (tracking rejects them).

## Status
Scaffold committed: interface (in domain) + `OpenCvMotionBallDetector` returning a
typed error until the OpenCV dependency is wired. No silent empty results (P9).
