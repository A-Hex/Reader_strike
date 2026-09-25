# On-device model (LiteRT-LM)

The reading assistant can run on a **local language model** instead of a cloud API. Everything —
summary, deep analysis, character map, plot breakdown, RSVP prep, vocabulary and Ask — then runs on
the phone, with no key, no network, and no passage leaving the device.

The app ships **no model**. A `.litertlm` bundle is 0.5-2 GB, so it is always something the user
brings onto the device.

## 1. Get a bundle

Download a LiteRT-LM model (`.litertlm`), for example **Gemma 3 1B IT** from the
[LiteRT community on Hugging Face](https://huggingface.co/litert-community). Accept the model
licence on the model page first — gated repos answer direct downloads with HTTP 401 until you do.

Use the 4-bit quantised build. A ~1 GB bundle is the practical ceiling for a phone.

## 2. Install it

Pick whichever fits your workflow:

| Path | How | Notes |
| --- | --- | --- |
| In app | Settings → **Reading AI** → **Install model** | Copies the file into private storage. Shows progress; keep the app open. |
| Development | `adb push model.litertlm /data/local/tmp/llm/` | Fastest loop; the file survives reinstalls. |
| External | Place it in `Android/data/com.aistudio.ahexstreak.rxmpb/files/models/` | Picked up automatically. |

The app resolves the model in that order (explicit import, then managed directories, then
`/data/local/tmp/llm/`) and only ever accepts non-empty `.litertlm` files.

## 3. Control it

Settings → **Reading AI**:

- **Prefer the on-device model** (on by default): when a model is installed it is used instead of
  the Gemini key. Turn it off to send requests to Gemini again.
- **Compute backend**: `CPU` works everywhere; `GPU` is faster where the vendor exposes OpenCL.

The engine order is: on-device model → Gemini (if a key is set) → deterministic extraction from the
passage. Every answer is labelled with the engine that produced it; nothing is fabricated when an
engine fails.

## Requirements

- `arm64-v8a` or `x86_64` device. Other ABIs have no LiteRT-LM native library and the app reports
  that instead of crashing.
- Roughly 1-1.5 GB of free RAM for a 1B 4-bit model (`android:largeHeap` is enabled for this).
- The first request after install pays the model load (tens of seconds); it stays resident after
  that.

## Dependency pin

`app/build.gradle.kts` uses `com.google.ai.edge.litertlm:litertlm-android:0.8.0`, pinned in
`gradle/libs.versions.toml`.

This pin is **not** laziness. Every LiteRT-LM release from 0.9.0 on is compiled with Kotlin 2.3 or
newer, and a Kotlin compiler refuses to read metadata newer than itself (`BinaryVersion.isCompatibleTo`
requires the library's minor version to be `<=` the compiler's). This project compiles with Kotlin
2.2.10, and 0.8.0 is the newest release whose metadata is `2.2.0`.

To move to the current LiteRT-LM, bump `kotlin` in `gradle/libs.versions.toml` first (which also
bumps the Compose compiler plugin, so re-check KSP and the Compose BOM), then raise `litertlm` to
the version you want and re-run a build.

## Backups

Model files are excluded from Android auto-backup and device transfer
(`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`) and from the app's own library
backup and cloud sync — they are large, device-specific and always re-installable.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| "This device's CPU architecture is not supported" | 32-bit-only device, or an ABI without a LiteRT-LM native library. |
| "Not enough free memory to load the model" | Model too large for the device; use a smaller bundle. |
| "Not enough free space" | The copy needs the bundle size plus headroom in private storage. |
| Results still show the Gemini badge | **Prefer the on-device model** is off, or no bundle is installed. |
| The assistant reports extraction only | Neither a model nor a Gemini key is configured. |
