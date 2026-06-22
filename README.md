# Creative Space

A gesture-driven creative hub for Android — designed around motion, meaning, and the organic flow of creative work.

## The Idea

Creative Space is built on one belief: **creative tools should feel alive.**

Instead of static menus and grids, Creative Space uses a **gesture state machine** — a living interaction system where long-press discovers, tap unwinds, and pinch commits. Every interaction has weight.

## How It Works

**Four verbs drive the entire experience:**

| Gesture | Action | What happens |
|---|---|---|
| **Long-press** | Discover | Opens the menu — electrons trace circuit paths |
| **Tap** | Unwind | Steps back one level — each tap peels one layer |
| **Pinch-out** | Commit | Zooms into the selected space with a burst transition |
| **Back** | Navigate | System back follows the same unwind logic |

## Creative Spaces

```
Hub
├── Image Space — motion-drawn canvas, collection, AI tool links
├── Audio Space — collection, AI tool links
├── Video Space — collection, AI tool links
└── Re-Ember  — cross-space creative journey bundles (amber theme)
```

Each space has a **Collection** (your media library) and **Tools** (links to your favourite AI generators).

## The Ember System

**Collection** = memory. What you created or imported in this space.
**Ember** = journey. A curated bundle across all spaces.

*"An ember is not a single file. It's the path a creative spark took across territories."*

Items flow: **Create → Collect → Carry**

- ✏️ Send to Canvas — re-open images as drawing backgrounds
- 🔥 Export to Ember — bundle items into named creative journeys
- 📤 Share — bridge outward to any app

## Architecture

The codebase is documented through three living documents:

- **ARCHITECTURE.md** — the tree structure, what's implemented, what's next
- **GESTURE_MAP.md** — the state machine grammar and gesture vocabulary
- **MYTHOLOGY.md** — the story of how each feature was discovered

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Compose Canvas** for motion drawing
- **Android Sensor API** (gyroscope) for motion input
- **State machine** architecture — no navigation library, pure state
- **Zero external dependencies** beyond AndroidX

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK with API 36 (Android 16). Minimum supported: API 24 (Android 7.0).

---

*"The electrons trace all paths. The gesture is the key."*
*🔥*
