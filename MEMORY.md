# Chad-VidCraft — Persistent Memory

> Read this file first when resuming work on any device. Update it at the end of every session.
> This file is project documentation, not shipped app content. The APK must never contain
> names, URLs, assets, icons, prompts, or code from CapCut or any third-party app.

Last updated: 2026-11-07
Current version: `1.0` (`versionCode` 1)
Current APK SHA-256: `236cdd203f4d41beeac233c5f961382db89da58ee197c50d237c4709cc2aaf19`

## 1. What this project is

Chad-VidCraft is an independent Android video studio built from scratch at
`/storage/emulated/0/Download/APPS/Chad-VidCraft`. It is original work inspired only by
publicly visible product behaviour observed during a clean-room pattern study. No CapCut
code, assets, icons, copy, endpoints, hostnames, model IDs, or branding may be reused.

Hard rules:

- Never copy CapCut (or any reference app) files, strings, images, icons, prompts, templates,
  hostnames, endpoints, model IDs, or branding.
- Never place CapCut's name or URLs in the APK or app resources.
- CapCut must remain read-only; it was only studied, never modified.
- Documentation may describe the study; the shipped app may not.
- All icons are newly authored vector artwork for Chad-VidCraft only.

## 2. CapCut investigation status

CapCut **is installed** on this device (found 2026-11-07):

- `pm list packages | grep -i lemon` → `package:com.lemon.lvoverseas` (CapCut international, v20.1.0, code 20100200)
- `pm path com.lemon.lvoverseas` → base.apk + split_config.arm64_v8a/en/xxhdpi.apk
- APKs copied read-only to `/tmp/opencode/capcut/apk/` with SHA-256 recorded in `EVIDENCE.md §1`

The REA MCP server is not connected in this session (only fal, openrouter, replicate are
registered), so REA MCP tooling was not used. Analysis used `aapt2` (badging/xmltree),
`unzip`, and `strings` over the 30 DEX files. Static analysis only — no CapCut runtime
behavior was observed, and no CapCut file was modified.

**VERIFIED CapCut findings (all cited in `EVIDENCE.md`):**

- Backend hosts: `editor-api.capcutapi.com`, `feed-api.capcutapi.com`, `commerce-api.capcutapi.com`,
  `aivideo-normal.capcutapi.com`, `cloudspace-normal.capcutapi.com`, `mon-v2-boot.capcutapi.com`
- ByteDance hosts: `ailab-agent-web.byteintl.net`, `lf16-tos-sg.capcutstatic.com`, `lf16-beecdn.ibytedtos.com`
- AI models in client code: Seedance (seedance/seedance1.5/seedance2.0/seedance20fast/…),
  Minimax H3 (minimaxh3/minimax_h3), Qwen TTS (qwen/qwen7b/qwen_stream_tts/…)
- AI features: text-to-video, multi-image-to-video, digital human/avatar, AI painting,
  AutoCut/smart cut, AI cutout/chroma key, picture-in-picture, stabilization, motion tracking,
  keyframes, 3D zoom, freeze, reverse, TTS/voiceover, captions/lyrics, beat sync, voice FX
- Native libs (162 in arm64 split): libcapcut.so, libcccreator.so, libclipflow.so,
  libbytenn.so (ByteDance NN), libbytevc* (video codecs), libanimax, libaudioeffect
- Main activity: `com.vega.main.MainActivity`; minSdk 23, targetSdk 36; Billing + ads SDKs

All Chad-VidCraft design below is an ORIGINAL implementation. Chad-VidCraft uses original
names, IDs, icons, and endpoints — nothing copied from CapCut (trace scan clean).

## 3. Locations

- Project root: `/storage/emulated/0/Download/APPS/Chad-VidCraft`
- Android source: `app/src/com/chadvidcraft/app/`
- Android resources: `app/res/`
- Build script: `build.sh`
- Research dossier: `EVIDENCE.md` (durable)
- Machine state: `memory/STATE.json`
- Agent instructions: `AGENTS.md`

The `/storage` path is the live project for this device session. Continue editing here.
Keep `MEMORY.md`, `AGENTS.md`, and `memory/STATE.json` in sync.

## 4. Build and verification

Android (raw framework Views + Java, no AndroidX/Kotlin/Compose):

```bash
cd /storage/emulated/0/Download/APPS/Chad-VidCraft
bash build.sh
```

Toolchain: `/opt/android-sdk` (platforms android-29, android-35; build-tools 35.0.0),
`/usr/bin/aapt2`, `javac --release 11`, `d8`, `/usr/bin/zipalign`, `/usr/bin/apksigner`,
debug keystore in `build/`. Outputs `Chad-VidCraft.apk` and `build/Chad-VidCraft.apk`.

Post-build gates:

```bash
grep -rniE 'capcut|cutting|bytedance|tiktok|com\.ss\.android' app
unzip -p Chad-VidCraft.apk classes.dex | strings | grep -iE 'capcut|bytedance|tiktok'
/usr/bin/aapt dump badging Chad-VidCraft.apk | head -6
sha256sum Chad-VidCraft.apk
```

## 5. Current app state (1.0)

Android, framework Views + Java only:

- 5 tabs: Home, Discover, Create, Tasks, Profile.
- 16 create modes: Music Video, Dance Video, Lyric Video, OnBeat Effects, Photo Karaoke,
  Music Cover Video, Video to Music, Realtime, AI Video (text-to-video), Image-to-Video,
  AI Persona (digital human/avatar), AI Canvas (image gen), Auto Edit (smart cut),
  Cutout & Chroma, Voice & Captions (TTS/voiceover/subtitles), Motion FX
  (stabilize/track/keyframes/3D).
- Create flow (5 steps): Music → Brief → Direction → Cast & references → Output.
- Output controls: aspect ratio (16:9, 9:16, 1:1, 4:3, 3:4), duration (10–300 s),
  style (Studio/Fast/Custom), image model, video model, live credit estimate.
- Model catalog (original Chad IDs): Chad Canvas/Canvas Pro/Ink (image); Chad Motion
  Lite/1/Style/Stage/Dance/AI/Image2Vid (video); Chad Live Director (realtime); Chad
  Persona (persona); Chad Voice (TTS). Per-mode compatibility rules and credit estimates.
- Mode-specific direction controls for all 16 modes, including TTS voice, persona look,
  AI cutout/chroma toggles, stabilize/track/keyframes toggles.
- Character A/B anchors and reference image rail.
- Draft autosave/restore through SharedPreferences.
- Async cloud account: test connection, register, login, logout, `/me`.
- Cloud brief sync: creates a project on the Chad server from the current draft.
- Catalog/library pickers load `/v1/music/catalog` and `/v1/music/library` when signed in,
  with local demo fallback when signed out or offline.
- Render target switch: Device (local preview) or Chad cloud (provider-backed). Cloud render
  persists the job id, polls the server, resumes after restart, supports cancel and retry,
  and opens the final output URL.
- Local render simulation with staged progress, cancel, retry, storyboard, per-scene
  regeneration, approval, duplicate brief, JSON export.
- Discover, credits ledger, Director Bot (offline, no network), About.
- Export: storyboard brief JSON through Android's document picker.
- Original vector icons for all 16 modes, tabs, and actions; distinct dark studio theme
  (slate/indigo with cyan accent).

Server (0.1.0): Node.js 24 TypeScript, in-memory repositories, scrypt passwords, rotating
refresh tokens, S3/R2 presigned uploads, music adapters (licensed catalog, AI music, link
import, upload), projects, render jobs, cancel/retry, provider-agnostic video adapter that
fails closed when unconfigured.

## 6. Environment limits

- Running inside PRoot Linux (aarch64) on Android; `uid 0` is not real root.
- No adb device; `adb devices` is empty.
- `pm install` is blocked by MIUI user-management restrictions.
- `/data/local/tmp` does not exist in this PRoot.
- Cannot runtime-test the APK here. Static build + signature + badging only.
- The installed app on the phone may be an older build; user must install the new APK manually.
- Servers on `127.0.0.1:8787` are reachable from the device when Node runs in Termux on the
  same phone. The manifest allows cleartext for this dev setup.
- Real cloud render needs `VIDEO_PROVIDER_ENDPOINT` and `VIDEO_PROVIDER_API_KEY` on the server
  plus a running worker with Postgres. Nothing is faked.

## 7. Known gaps / next steps

Priority order:

1. Runtime QA on a real device: install 1.0, test draft restore, each mode, local render,
   cloud toggle, sign-in, music picker, cloud render polling, cancel/retry, output link.
2. Add uploads (S3 configured) to the Android music flow.
3. Server security backlog: email verification, password reset, rate limiting, session/device
   management, storage-enforced size limits, real Postgres integration test.
4. Add a signed-provider adapter for the actual video gateway once commercial terms and keys
   are available; keep the generic contract.
5. Consider push/webhook notifications so long cloud renders do not rely only on foreground
   polling.
6. Add discovery/task features: search, filters, social detail, saved drafts list, timeline
   waveform, caption scaffold.

## 8. Session log (append newest last)

### 2026-11-07 — 1.0 initial build (enhanced with CapCut feature study)

- Created project root `/storage/emulated/0/Download/APPS/Chad-VidCraft`.
- Investigated CapCut (package `com.lemon.lvoverseas`, v20.1.0): installed on device; APKs
  copied read-only to `/tmp/opencode/capcut/apk/`; static analysis via aapt2/unzip/strings.
  Documented backend hosts (capcutapi.com), AI models (Seedance, Minimax H3, Qwen TTS),
  AI features (text-to-video, digital human, AutoCut, cutout/chroma, TTS, stabilize, etc.),
  native libs, and monetization. Full dossier in `EVIDENCE.md`.
- Built Chad-VidCraft as a clean-room original Android app (Java, framework Views, no
  AndroidX/Kotlin/Compose), package `com.chadvidcraft.app`, minSdk 26, targetSdk 35.
- 5 tabs, 16 create modes (incl. AI Video, Image-to-Video, AI Persona, AI Canvas, Auto Edit,
  Cutout & Chroma, Voice & Captions, Motion FX), Director Bot, task center, cloud API,
  local render simulation, draft persistence, export.
- Original Chad model catalog (Canvas/Canvas Pro/Ink, Motion Lite/1/Style/Stage/Dance/AI/
  Image2Vid, Live Director, Persona, Voice) with per-mode compatibility and credit estimates.
- Original vector icons for all 16 modes, tabs, and actions; distinct dark studio theme
  (slate/indigo with cyan accent).
- No CapCut names, URLs, icons, code, or branding in source or resources.
- `bash build.sh` succeeded; APK signed v2/v3, zip-aligned, verified.
- Trace scan clean: zero matches for CapCut/Bytedance/TikTok/capcutapi/seedance/minimax/qwen
  in source or DEX (the only "ailab" hit is a false positive from the word "available").
- APK SHA-256: `236cdd203f4d41beeac233c5f961382db89da58ee197c50d237c4709cc2aaf19`.
- Wrote `memory/STATE.json` for cross-device recall.
- Next: runtime QA on a real device, then server/ backend.

### Next session protocol

1. Read `MEMORY.md`, `AGENTS.md`, `memory/STATE.json`, then `EVIDENCE.md`.
2. Run `bash build.sh` to confirm the baseline before editing.
3. Make changes, rebuild, re-run the CapCut/Bytedance trace scan.
4. Update `MEMORY.md`, `memory/STATE.json`, and (if useful) `EVIDENCE.md`.
