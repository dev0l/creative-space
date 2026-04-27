# Index

**1. Product Brief**
**2. Functional Specification v0.1**
**3. Prompt / handoff note to orchestration**
**4. Additive v0.2 — Creative Modes & Entry System**

---

# Creative-SensO-r / Motion Draw / Sensor Canvas — Product Brief v0.1

## Working title

**Motion Draw**
Alternative internal framing: **Sensor Canvas**
Alternative playful framing: **Creative SensO-r** (O = "ring" -r = "ing". So "O-r" = "ing" — "Creative Sensing")

## Product type

Interactive mobile creative app

## Product vision

A mobile app that turns physical phone movement into visual output on screen, allowing the user to draw by moving the device through space rather than only touching the screen.

## Problem / opportunity

Most mobile creative apps rely on direct finger or stylus input on a flat screen. Modern smartphones already contain motion and orientation sensors, but those sensors are rarely used as the primary creative input method.

This creates an opportunity to explore a new interaction model:

**move the phone → create marks, lines, and shapes**

The app should investigate whether sensor data such as acceleration, rotation, and orientation can be translated into a playful, understandable, and visually engaging drawing experience.

## Goal

Build an early interactive prototype that demonstrates:

* live sensor input
* mapping motion to drawing behavior
* visible on-screen output
* a clear user interaction loop

## User value

The user gets:

* a playful and unusual way to draw
* a more embodied interaction with the phone
* a creative “sensor playground” experience
* a demoable app with visible feedback and interaction

## Primary project goal

Create a publishable or near-publishable school project with:

* multiple screens
* meaningful interaction
* visible results
* room for later expansion

## Non-goal for v0.1

This version is not intended to be:

* a full art platform
* a complete AR system
* a perfect gesture recognizer
* a background service utility

---

# Creative-SensO-r / Motion Draw / Sensor Canvas — Functional Specification v0.1

## 1. Core concept

The app reads motion-related sensor data from the device and maps that input to visual marks drawn on a canvas.

The first version should focus on a direct interaction loop:

1. user opens drawing mode
2. app begins reading sensor data
3. phone movement changes the position of a virtual drawing point
4. the app renders the resulting line on screen

## 2. Primary MVP feature

### Motion Drawing

The user can create lines or shapes on a 2D canvas by moving the phone physically.

### MVP objective

Demonstrate that phone motion can drive drawing in a way that is:

* responsive
* understandable
* visually noticeable

## 3. Supported input sources

For the early prototype, use one or more of:

* accelerometer
* gyroscope
* rotation/orientation sensors

The system may begin with a simplified mapping if that makes the prototype more stable.

## 4. Primary screens

### 4.1 Home Screen

Purpose:

* introduce the concept
* provide navigation to the drawing mode (canvas)

Suggested elements:

* title
* short description
* start button
* optional list of future modes

### 4.2 Motion Draw Screen

Purpose:

* main interaction surface

Suggested elements:

* drawing canvas
* current sensor status
* simple indicator for whether tracking is active
* clear canvas button
* pause/resume button
* optional sensitivity control

### 4.3 Gallery / Saved Results Screen

Purpose:

* show previously saved drawings or captures

Can be minimal in v0.1.

### 4.4 Settings Screen

Purpose:

* let the user adjust:

  * sensitivity
  * smoothing
  * line thickness
  * color
  * tracking mode if multiple exist later

## 5. Core interaction flow

### Main flow

1. User opens app
2. User starts Motion Draw mode
3. App activates sensor reading
4. Sensor values are translated into screen movement
5. The drawing path updates continuously
6. User can pause, clear, and optionally save

### Secondary flow

1. User creates a drawing
2. User taps save
3. Drawing is stored locally
4. User can review it later in Gallery

## 6. Functional requirements

### FR-1 Sensor activation

The app shall activate relevant motion sensors when the Motion Draw screen is active.

### FR-2 Real-time mapping

The app shall convert incoming sensor data into changes in the position of a drawing point or path.

### FR-3 Live rendering

The app shall render a visible line or path on screen in real time as the device is moved.

### FR-4 Pause / resume

The app shall allow the user to pause and resume motion tracking.

### FR-5 Clear canvas

The app shall allow the user to clear the current drawing.

### FR-6 Save output

The app should allow the user to save the current drawing locally.

### FR-7 Adjustable sensitivity

The app should allow the user to adjust sensitivity or motion scaling.

### FR-8 Stable session behavior

The app shall stop or suspend sensor reading when the drawing screen is no longer active, to avoid unnecessary battery use.

## 7. Non-functional requirements

### NFR-1 Responsiveness

Visual feedback should feel near real-time.

### NFR-2 Clarity

The user should be able to understand that phone motion is controlling the drawing.

### NFR-3 Simplicity

The MVP should prioritize a clear and working interaction loop over advanced features.

### NFR-4 Battery awareness

Sensors should only be active when necessary.

### NFR-5 Maintainability

The app architecture should support later expansion into additional sensor-based features.

## 8. Technical direction

### Platform

* Android
* Kotlin
* Jetpack Compose

### Architecture

* Compose UI
* ViewModel-based state handling
* sensor manager / sensor listeners
* optional Room for saved drawings or sessions

### Suggested modules

* `ui`
* `viewmodel`
* `sensors`
* `domain`
* `data`

## 9. Data model ideas

### DrawingSession

Fields:

* id
* createdAt
* duration
* trackingMode
* sensitivity
* wasSaved

### DrawPoint

Fields:

* x
* y
* timestamp

### SavedArtwork

Fields:

* id
* title
* createdAt
* previewPath or serialized points

For MVP, you can start simpler and only store:

* session metadata
* optional bitmap/snapshot
* or serialized list of points

## 10. Mapping logic, MVP-safe version

The first implementation does not need to reconstruct true 3D spatial motion.

It is acceptable to:

* interpret selected sensor axes as 2D movement
* scale those values into screen deltas
* smooth noisy input
* clamp movement within canvas bounds

This should be treated as an interaction prototype rather than a precise physics system.

## 11. UX principles

* visible feedback immediately
* low-friction controls
* playful but understandable experience
* clear distinction between active tracking and paused state
* simple controls before advanced settings

## 12. Stretch goals

Not required for MVP, but possible later:

* circle / loop detection
* shape snapping
* camera overlay mode
* audio-reactive drawing
* multi-sensor creative modes
* motion replay
* export/share
* widget support
* alternative brush behaviors

## 13. Constraints / reality notes

* sensor data is noisy
* movement is three-dimensional while the screen is two-dimensional
* some calibration and smoothing will likely be necessary
* advanced gesture recognition should not block the MVP

## 14. Acceptance criteria for v0.1

The prototype is successful if:

* the app launches and navigates correctly
* the Motion Draw screen activates sensors
* moving the phone changes the drawing on screen
* the user can clearly see cause and effect
* the user can pause and clear the drawing
* battery-aware behavior is respected by stopping tracking outside the active drawing screen

---

# Handoff Note to Anti-Gravity / Hearth Orchestration

Use the existing Android/Kotlin/Compose scaffold as the base.

The product direction has pivoted from background-style pocket-dial detection toward an interactive sensor-driven drawing experience.

The goal of this cycle is not to build a full creative platform.
The goal is to produce a clean MVP that proves the core interaction:

**physical phone movement → visible line drawing on screen**

Prioritize:

* active-screen interaction
* sensor integration
* real-time visual feedback
* simple controls
* low architectural friction

Do not over-expand into AR, audio, exports, or advanced recognition in the first implementation unless they are explicitly scoped as later extensions.

Use the existing sensor foundation where relevant.

Recommended first build sequence:

1. verify sensor stream on active screen
2. map sensor values to 2D point movement
3. render line path on Compose canvas
4. add pause / clear
5. optionally add save and settings

---

## Creative-SensO-r / Motion Draw / Sensor Canvas - Additive v0.2 — Creative Modes & Entry System

---

## 1. Purpose of this additive

This additive extends the original specification by introducing:

* a **central interaction hub (the circle)**
* a **progressive entry system**
* a **multi-mode creative architecture**

The goal is to evolve the app from a single-feature interaction into a scalable creative system while preserving a simple and focused MVP.

---

## 2. Core concept update

The center element (previously a simple entry trigger) is redefined as:

> **A dynamic interaction portal that adapts to different creative modes**

This portal:

* invites interaction
* provides feedback before entering a mode
* transitions the user into a full creative surface

---

## 3. Entry system (progressive interaction)

The app introduces a **multi-stage entry flow**:

### 3.1 Idle state

* calm background
* soft central circle
* subtle pulse indicating interactivity

---

### 3.2 Engagement state

* user touches or interacts with the circle
* visual feedback begins:

  * glow
  * deformation
  * trace lines (optional)

This stage:

* teaches the interaction
* builds anticipation

---

### 3.3 Activation threshold

* interaction reaches a threshold (movement, expansion, trace distance)
* circle reacts strongly (scale / brightness / deformation)

---

### 3.4 Transition state

* circle expands into full screen
* background shifts (dark → light)
* user enters creative canvas

---

## 4. Entry interaction variants

The system supports multiple entry methods:

### Primary (MVP)

* tap → immediate entry

### Enhanced

* expand / pinch-out gesture → entry
* trace-out gesture → entry (preferred expressive variant)

### Experimental (future)

* motion-based gesture → entry trigger

---

## 5. Creative modes (conceptual extension)

The app evolves into a **multi-mode creative system**, where each mode uses the same entry logic but different input-output mappings.

### 5.1 Motion Draw (MVP)

* input: device movement
* output: drawn lines on canvas

---

### 5.2 Sound Mode (future)

* input: audio (microphone / recorded)
* output: waveform-based visual interaction

#### Entry behavior:

* portal morphs into waveform
* user holds or taps microphone trigger
* waveform reacts before transition

---

### 5.3 Surface modes (future)

The canvas can adapt to different backgrounds:

* blank surface (default)
* map snapshot
* imported image
* (future) camera overlay

---

## 6. Interaction principle

All modes follow a shared interaction philosophy:

> **Same entry language — different creative outputs**

This ensures:

* consistency
* learnability
* scalability

---

## 7. Visual identity update

The central portal is the key visual element:

### Idle

* soft circle
* blurred edge
* subtle glow

### Engaged

* responsive deformation
* trace or energy feedback

### Transition

* expansion into canvas
* light/dark inversion

---

## 8. UX principles (extended)

* entry should feel like creation, not navigation
* interaction should begin before mode activation
* feedback should be immediate and intuitive
* complexity should be layered, not exposed at once
* the system should invite exploration without overwhelming the user

---

## 9. MVP boundary (important)

For implementation v0.1:

Focus only on:

* Motion Draw mode
* basic entry (tap + optional simple expansion)
* single canvas surface (blank)

Do not implement yet:

* sound mode
* multiple surfaces
* advanced gesture recognition
* complex animations beyond basic transitions

---

## 10. Future expansion hooks

The architecture should allow:

* mode switching via portal state
* different input pipelines (motion, audio, etc.)
* background surface injection (map, image, camera)
* additional creative tools

---

## 11. Summary

This additive introduces:

* a **central interaction portal**
* a **progressive entry experience**
* a **multi-mode creative system**

while maintaining a focused MVP:

> **move the phone → create visible drawing**
