# Bowling Tracker — App Plan

*Single Android phone on a tripod. You set up at the non-striker end, mark the batting-end stumps, bowl, and the app measures ball speed, swing, line/length and pitching point.*

Planning document only — no code yet.

---

## 1. Reality check on accuracy

You asked for **broadcast-grade (sub-1 km/h, Hawk-Eye level)** accuracy, but for a **personal practice tool** running on **one Android phone**.

These two goals conflict, and it's worth being honest about it before any code gets written:

- Real Hawk-Eye uses **6+ synchronised high-speed cameras** placed around the ground, calibrated to the pitch in 3D, plus a dedicated processing rig. That's how it gets sub-centimetre tracking and sub-1 km/h speed.
- A **single camera** can only see the world in 2D. It cannot, on its own, recover true 3D depth. It estimates depth using assumptions (the ball travels roughly along the known pitch line between two sets of stumps). Those assumptions introduce error.

**What one phone can realistically achieve:**

| Metric | Realistic accuracy (single phone, well calibrated) | What would improve it |
|---|---|---|
| Ball speed | ±2–4 km/h | Higher fps, second camera, better calibration |
| Line (off/leg position) | Good — directly visible in 2D | — |
| Length (where it pitches down the wicket) | ±15–40 cm | Depth is the hard part; 2nd camera helps a lot |
| Swing (lateral deviation) | Detectable for noticeable swing; small swing is marginal | High fps + clean ball detection |
| Full 3D trajectory | Approximate | Multi-camera |

**Recommendation:** Build for **"reasonably accurate" (±2–4 km/h)** as the target, design the system so accuracy can be *improved later* by adding a second phone, and clearly label every number in the UI as an *estimate*. Chasing literal sub-1 km/h on one phone will frustrate you — it isn't physically reachable.

---

## 2. Where the computation runs (recommendation)

You asked me to recommend. **Record-then-process, on the phone.**

Three options were on the table:

1. **Live on-device, real-time** — process every frame as it's captured. Hardest. High-fps frames arrive faster than a phone can analyse them; you'd drop frames and lose accuracy. Not worth it for a practice tool.
2. **Record then process, on the phone (recommended)** — capture a short high-fps clip of the delivery, then analyse the saved frames a second or two later and show insights. Keeps everything offline and private, no server bills, and frame-accurate because you process every frame without real-time pressure.
3. **Record then process, in the cloud/PC** — most compute, but needs internet, costs money, and adds upload latency for no real benefit at this scale.

**Pick option 2.** It's the sweet spot for a single-user practice tool: fast enough (a few seconds after each ball), fully offline, private, and accurate because nothing is rushed. Architecture below leaves a clean seam to add a cloud/PC processing option later if you ever want it.

---

## 3. How the measurement actually works (the physics)

This is the heart of the app. Everything else is plumbing.

### 3.1 Calibration — the one step that makes numbers trustworthy

The app needs to know how pixels map to real-world distances. Cricket gives us a gift: **the pitch is a fixed, known size.**

- Distance between the two bowling creases (stump to stump): **20.12 m**
- Stump height above ground: **0.711 m**, width of the three stumps: **0.2286 m**

Calibration flow:

1. Phone is on the tripod at the non-striker end, behind/above the bowling crease.
2. You **tap the batting-end stumps** in the camera view (you already planned to "mark" them). You also tap the near-end crease/stumps.
3. From those known real distances + the tapped pixel positions, the app computes a **homography** (a mapping from the camera's 2D image to the flat plane of the pitch). This is standard, well-understood computer-vision math.
4. Once calibrated, any point the ball passes through on/near the pitch plane can be converted to a real-world position in metres.

Calibration is per-setup. Re-mark whenever you move the tripod.

### 3.2 Ball detection & tracking

For each frame of the recorded delivery:

- **Detect the ball** — a small fast-moving object. Two complementary methods, used together:
  - *Motion/background subtraction*: the ball is the thing that moves between frames. Cheap and effective against a static net background.
  - *A trained ball detector* (small ML model) for robustness when motion alone is ambiguous (ball near the bowler's hand, occlusions).
- **Link detections into a track** across frames, rejecting outliers (birds, hand, shadows) by requiring the path to be physically plausible (a ball follows a smooth parabola-ish curve, not random jumps).

### 3.3 Deriving the insights

- **Speed**: distance the ball travels between two frames ÷ time between those frames. Time per frame comes from the capture frame rate (e.g. at 240 fps each frame is 1/240 s). Distance comes from the homography. Measure speed near release (most meaningful "bowling speed") and report it. Higher fps = more frames over the flight = more samples = less error — **this is the single biggest accuracy lever.**
- **Pitching point**: the frame where the ball's vertical motion reverses (it stops descending and starts rising) is the bounce. Map that image point through the homography → position on the pitch → gives you **line and length**.
- **Swing / direction**: fit the ball's pre-bounce path. Lateral deviation from a straight line between release and bounce = swing amount and direction (in/out). Also report the **release-to-pitch angle** (the "bowling path").
- **Bounce/deviation off pitch**: compare path angle before vs. after the bounce.

### 3.4 The accuracy levers (so you can tune later)

1. **Frame rate** — shoot at the highest fps your phone supports (120/240 fps). Biggest single win.
2. **Calibration care** — tap the stumps precisely; this directly bounds line/length error.
3. **Camera placement** — stable tripod, ball travelling across a clean background, full pitch in frame.
4. **(Future) Second phone** at the side — turns 2D guesses into real 3D, the single biggest accuracy upgrade beyond fps.

---

## 4. Tech stack (recommended)

Native Android, on-device, offline.

| Layer | Choice | Why |
|---|---|---|
| **Language** | **Kotlin** | Standard for modern Android; best library & docs support. |
| **UI** | **Jetpack Compose** | Modern declarative UI; fast to build the capture/calibration/results screens. |
| **Camera & high-fps capture** | **CameraX 1.5+** (high-speed capture API) | As of late 2025 CameraX added first-class **high-speed / slow-motion (120/240 fps) capture**, which previously needed raw Camera2. Falls back to Camera2's `CameraConstrainedHighSpeedCaptureSession` if you need finer control. |
| **Computer vision** | **OpenCV (Android SDK)** | Background subtraction, contour/blob detection, homography, trajectory fitting — all built in and battle-tested. The workhorse. |
| **ML ball detector** | **LiteRT** (formerly TensorFlow Lite) + **MediaPipe Object Detector** | On-device, GPU-accelerated. Start with a generic small detector (EfficientDet-Lite) and later fine-tune on your own ball images for robustness. |
| **Math / trajectory** | Kotlin + a light linear-algebra helper | Homography apply, curve fitting, speed calc. |
| **Local storage** | **Room (SQLite)** for session/delivery records; app-private file storage for clips/frames | Keeps a history of every ball you bowl, offline. |
| **Charts** | **Vico** or **MPAndroidChart** | Speed-over-session, swing trends, pitch maps. |
| **Build** | Android Studio, Gradle | Standard. |

**Why native (not React Native / Flutter):** high-fps camera capture and frame-by-frame CV are heavy, hardware-close, performance-sensitive tasks. Cross-platform wrappers fight you here. Since the requirement is Android-only, native Kotlin is the path of least resistance and best performance. (If you ever want iOS too, the CV core can be reused, but plan that as a separate effort.)

---

## 5. Architecture

Clean separation so the hard CV part is testable independently of the UI.

```
┌─────────────────────────────────────────────────────────┐
│                      UI (Jetpack Compose)                │
│   Capture screen · Calibration screen · Results screen   │
│              Session history · Trend charts              │
└───────────────┬─────────────────────────┬───────────────┘
                │                          │
        ┌───────▼────────┐        ┌────────▼─────────┐
        │ Capture module │        │  Results / Stats │
        │  (CameraX hi-  │        │  (Room queries,  │
        │   fps → clip)  │        │   charts)        │
        └───────┬────────┘        └────────▲─────────┘
                │ saved frames             │ insights
                ▼                          │
        ┌───────────────────────────────────────────┐
        │         Analysis Engine (pure Kotlin)      │
        │  1. Calibration (homography from taps)     │
        │  2. Ball detection (OpenCV + LiteRT)       │
        │  3. Tracking (link + outlier reject)       │
        │  4. Physics (speed, bounce, swing, line/   │
        │     length) via homography + frame timing  │
        └───────────────────┬───────────────────────┘
                            │ persists
                    ┌───────▼────────┐
                    │  Room (SQLite) │
                    │ sessions/balls │
                    └────────────────┘
```

Key principle: **the Analysis Engine takes "frames + calibration" in and gives "insights" out, with no UI or camera dependencies.** That makes it unit-testable (feed it a known recorded clip, check the speed it computes) and reusable.

### Per-delivery data flow
1. You tap **Record** → CameraX captures a short high-fps clip of the delivery.
2. App extracts frames to app-private storage.
3. Analysis Engine runs (a couple of seconds): detect ball per frame → build track → apply calibration → compute insights.
4. Results screen shows speed, line/length on a pitch map, swing direction, trajectory overlay on the video.
5. Saved to Room → feeds session history and trends.

---

## 6. Phased build plan

Build the risky CV part first — if ball tracking doesn't work, nothing else matters.

**Phase 0 — Spike / feasibility (prove it works)**
- Record one delivery at high fps on your phone. Pull the clip to a PC.
- Hand-prototype the pipeline in Python + OpenCV: calibrate from stump taps, detect the ball, compute a speed.
- Compare against a known reference (e.g. bowl past a speed-gun, or a friend's known pace).
- **Goal: confirm you can get a believable speed and pitch point from your actual phone footage before writing any Android code.** This de-risks everything.

**Phase 1 — Capture + calibration (Android)**
- CameraX high-fps recording; save clips.
- Calibration screen: tap stumps, compute & store homography.
- Verify frame timestamps / fps are reliable on your device.

**Phase 2 — Analysis engine (Android, ported from the spike)**
- Ball detection (OpenCV motion + blob first; add LiteRT detector for robustness).
- Trajectory linking + outlier rejection.
- Speed calculation. Validate against Phase 0 reference.

**Phase 3 — Insights**
- Bounce detection → line & length → pitch map.
- Swing direction & amount; trajectory overlay on the saved video.

**Phase 4 — History & trends**
- Room storage; session view; charts (speed over a session, swing consistency, pitch-map heatmap).

**Phase 5 — Polish & accuracy tuning**
- Calibration UX, error handling, confidence indicators on each number.
- Tune detection; consider fine-tuning the ball model on your own footage.

**Phase 6 (optional, later) — Accuracy upgrade**
- Second phone at side-on for true 3D / better depth, sync, and triangulation.

---

## 7. Main risks & how the plan handles them

| Risk | Mitigation |
|---|---|
| **Single-camera depth is inherently approximate** | Constrain to the known pitch plane via homography; label numbers as estimates; design for an optional 2nd camera. |
| **Ball too small / blurry at high speed** | High fps shortens motion blur; combine motion + ML detection; ensure good lighting and clean background. |
| **Phone fps not actually constant** | Use per-frame timestamps where available, not assumed fps; validate on your specific device in Phase 1. |
| **Detection picks up hand/shadow/birds** | Physical-plausibility filter on the trajectory; ML detector as a tiebreaker. |
| **Scope creep toward "Hawk-Eye"** | Explicit accuracy targets (Section 1); phased plan; ship a working speed+pitch tool before adding 3D. |
| **CV doesn't work on real footage** | Phase 0 spike on a PC *before* committing to Android build. |

---

## 8. What I'd want to confirm before building

- Your phone model and the **max fps it can record at** (decides the accuracy ceiling).
- Whether a **side-on second phone** is acceptable later (big accuracy unlock).
- Whether you want **video saved/replayable** with the trajectory drawn on it (nice but adds work).
- Any **reference speed source** (speed gun, club data) to validate against in Phase 0.

---

### TL;DR
Native **Kotlin + CameraX (high-fps) + OpenCV + LiteRT**, **record-then-process on the phone**, calibrated off the known 20.12 m pitch via a homography. Target **±2–4 km/h** (not sub-1 — impossible on one camera), design so a second phone can later push accuracy up. Build the **CV pipeline as a PC spike first** to de-risk, then port to Android in phases.
