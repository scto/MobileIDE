# MobileIDE – Icon Assets

Complete icon set for the MobileIDE Android app.

---

## File Structure

```
MobileIDE_Icons/
│
├── svg/                              ← Source SVG files (edit these)
│   ├── ic_launcher_full.svg          Master icon — 512 × 512 px
│   ├── ic_launcher_round.svg         Round variant — 512 × 512 px
│   ├── ic_launcher_foreground.svg    Foreground layer — 108dp canvas
│   ├── ic_launcher_background.svg    Background layer — 108dp canvas
│   ├── ic_launcher_monochrome.svg    Monochrome layer — 108dp canvas
│   └── ic_launcher_preview_1024.png  High-res preview PNG
│
├── drawable/                         ← Copy to app/src/main/res/drawable/
│   ├── ic_launcher_background.xml    Gradle/XML: radial gradient background
│   ├── ic_launcher_foreground.xml    Gradle/XML: bitmap foreground reference
│   ├── ic_launcher_monochrome.xml    Gradle/XML: monochrome bitmap reference
│   ├── ic_launcher_adaptive.xml      Standalone adaptive icon reference
│   ├── ic_launcher_fg_asset.png      Foreground bitmap (432 × 432 px)
│   ├── ic_launcher_bg_asset.png      Background bitmap (432 × 432 px)
│   └── ic_launcher_mono_asset.png    Monochrome bitmap (432 × 432 px)
│
├── mipmap-anydpi-v26/                ← Copy to app/src/main/res/mipmap-anydpi-v26/
│   ├── ic_launcher.xml               Adaptive icon — all shapes (API 26+)
│   └── ic_launcher_round.xml         Round adaptive icon (API 26+)
│
├── mipmap-mdpi/                      ← Legacy PNG fallbacks
│   ├── ic_launcher.png               48 × 48 px
│   └── ic_launcher_round.png         48 × 48 px
├── mipmap-hdpi/                      72 × 72 px
├── mipmap-xhdpi/                     96 × 96 px
├── mipmap-xxhdpi/                    144 × 144 px
└── mipmap-xxxhdpi/                   192 × 192 px
```

---

## Design

```
┌─────────────────────────────────────────────────────┐
│         Dark navy circle background                  │
│                                                     │
│    ╔═══╗ Neon Teal  ╔═══╗ Electric Blue ╔═══╗      │
│    ║   ║  #00D2C8   ║   ║  #0082FF      ║   ║      │
│    ╚═══╝            ╚═══╝               ╚═══╝      │
│                                                     │
│              ┌─────────────┐                        │
│    ╔═══╗     │   < / >     │    ╔═══╗               │
│    ║   ║     │   Mobile    │    ║   ║               │
│    ║   ║     │    IDE      │    ║   ║               │
│    ╚═══╝     └─────────────┘    ╚═══╝               │
│  Deep Violet                    Vivid Green          │
│  #8232FF                        #32D25A              │
└─────────────────────────────────────────────────────┘
```

**Centre square:** `#16162A` (dark navy, matches Catppuccin Mocha IDE theme)

---

## Usage in AndroidManifest.xml

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ... >
```

## Adaptive Icon Layer System

```
API 26+:  mipmap-anydpi-v26/ic_launcher.xml
          → background: @drawable/ic_launcher_background  (gradient XML)
          → foreground: @drawable/ic_launcher_foreground  (bitmap PNG)
          → monochrome: @drawable/ic_launcher_monochrome  (white PNG, API 33+)

API < 26: mipmap-{density}/ic_launcher.png  (legacy PNG fallbacks)
```

## SVG Editing

Open any SVG in **Inkscape**, **Figma** (import), **Adobe Illustrator**,
or any browser. Edit colours, shapes or text, then export back to PNG
at the required densities.

Recommended export sizes for PNGs:
| Density   | px  |
|-----------|-----|
| mdpi      | 48  |
| hdpi      | 72  |
| xhdpi     | 96  |
| xxhdpi    | 144 |
| xxxhdpi   | 192 |
| Adaptive foreground/background/mono | 432 |
