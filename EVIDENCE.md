# CapCut Evidence Dossier (for Chad-VidCraft)

Date: 2026-11-07
Target: CapCut Android app (package `com.lemon.lvoverseas`, international build)
Purpose: Document how the shipped CapCut app works so Chad-VidCraft can be built as a
separate, original, improved app. Every finding cites its source artifact.

Constraint (hard rule): CapCut was only READ. No CapCut file, code, asset, icon, endpoint,
hostname, model ID, prompt, template, or brand was modified or copied. CapCut remains
read-only. Chad-VidCraft uses ORIGINAL names, IDs, icons, and endpoints throughout.

## 0. Evidence definitions

- **VERIFIED** — directly present in the shipped CapCut APK(s) (manifest, resources, decompiled
  code, strings).
- **INFERRED** — behavior implied by class/method/field/string names and wiring; not observed
  at runtime.
- **NOT FOUND** — specifically searched the shipped artifacts and did not find it. Bounded
  negative; not proof it does not exist server-side.

## 1. Artifacts and integrity

CapCut APKs were copied read-only from the installed device:

| Artifact | Path on device | SHA-256 |
|---|---|---|
| Base APK | `/data/app/~~FI3gP3Xc_8JGNf3Ffzji-g==/com.lemon.lvoverseas-HHIFiUW4CQ4JLLIi3NFuaw==/base.apk` | `1b94e8737355c93289f70c855aecfce07b69f9e04ca0ffcf7571a04a238b7603` |
| arm64_v8a split | `.../split_config.arm64_v8a.apk` | `2be0c55ecc7bb07bfbf65dde9829c2a5361cf758265bc9a38ff65c2dcee53160` |
| en split | `.../split_config.en.apk` | `8411a93c7f1ecb7f34f848ff7b905eae1f9c50d0cfd61b10168dd0dd559cfe14` |
| xxhdpi split | `.../split_config.xxhdpi.apk` | `a744cc846957b2defe79f37df976eee0a4a35cd0395ce378f45fce330d5f02c7` |

Working copies (read-only): `/tmp/opencode/capcut/apk/`

Tooling note: REA MCP server was not connected in this session (only fal, openrouter,
replicate MCP servers registered). Analysis used `aapt2` (manifest/badging/xmltree), `unzip`,
and `strings` over the DEX files. Conclusions are static-analysis conclusions; no CapCut
runtime behavior was observed. apktool full-decode timed out on the 123 MB base APK.

## 2. App identity and build metadata

**VERIFIED** — `aapt2 dump badging base.apk`

- Package: `com.lemon.lvoverseas` — base.apk
- Label: `CapCut`
- Version name: `20.1.0` — badging
- Version code: `20100200` — badging
- minSdk: `23` — badging
- targetSdk: `36` — badging
- compileSdk: `36` — badging
- Launchable activity: `com.vega.main.MainActivity` — badging
- Locales: `--_--` (single); densities 160/240/320/480/640/65534/65535
- Native libs required: none marked required; OpenCL/Mali/Adreno GPU libs listed as
  `uses-native-library-not-required` (GPU-accelerated video)

Interpretation: a native-heavy ByteDance video editor supporting Android 6.0+ and targeting
Android 16, with GPU/OpenCL acceleration.

## 3. Manifest permissions and platform integration

**VERIFIED** — `aapt2 dump badging base.apk` (permission lines)

Network / media:
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_NETWORK_STATE`
- `READ_MEDIA_IMAGES`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`,
  `ACCESS_MEDIA_LOCATION`
- `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `WRITE_SYNC_SETTINGS`
- `CAMERA`, `RECORD_AUDIO`, `HIGH_SAMPLING_RATE_SENSORS`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
- `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`
- `SYSTEM_ALERT_WINDOW`
- `READ_CALENDAR`, `WRITE_CALENDAR`
- `com.android.vending.BILLING` (in-app purchases)
- `com.google.android.gms.permission.AD_ID` + adservices attribution permissions
- Package discovery queries for TikTok, Instagram, Facebook, WhatsApp, Telegram, Discord,
  Twitter, Snapchat, YouTube, LinkedIn, CapCut itself, WeChat, QQ, YouTube, etc. (sharing /
  social integration)

Components (verified via badging + xmltree):
- `com.vega.main.MainActivity` — launcher
- FileProvider authority `com.lemon.faceuassist.provider`
- App widget provider
- Billing: `InAppBillingService.BIND`, `BillingOverrideService.BIND`
- AppsFlyer referrer provider
- Custom tabs, photopicker embedded service

**INFERRED** — CapCut integrates heavily with the ByteDance/TikTok ecosystem (package queries
for `com.ss.android.ugc.aweme`, `com.zhiliaoapp.musically`, `com.ss.android.ugc.trill`) and
major social platforms for export/sharing.

## 4. Backend AI model API calls, endpoints, and providers

**VERIFIED** — strings extracted from `classes.dex`…`classes29.dex` (base.apk)

CapCut does NOT call model providers directly from the client. It routes through
ByteDance-owned backend services under `capcutapi.com` and `byteintl.net`.

### 4.1 CapCut backend hostnames

| Hostname | Role (inferred from path strings) | Evidence |
|---|---|---|
| `editor-api.capcutapi.com` | Editor API — settings, feedback, image upload | `editor-api.capcutapi.com/service/settings/v3/`, `/feedback/3/list/`, `/feedback/image/v1/upload/` |
| `feed-api.capcutapi.com` | Feed / template / export / config | `/lv/v1/edit/get_config`, `/tools_export/`, `/template_export/`, `/autocut_export/`, `/replicate/get_collections` |
| `commerce-api.capcutapi.com` | Commerce / billing / IAP | `/purchase/iap/make_order`, commerce-api host string |
| `aivideo-normal.capcutapi.com` | AI video generation (storyboard agent / AI lab) | `/storyboard_agent/v1/ai_lab/send_msg`, `/storyboard_agent/v1/ai_lab/poll_by_event_id` |
| `cloudspace-normal.capcutapi.com` | Cloud space / project storage | host string present |
| `mon-v2-boot.capcutapi.com` | Monitoring / boot config | host string present |
| `rc-verification.capcutapi.us/` | RC verification (US region) | host string present |
| `general-api-us-looki.capcutapi.com` | A/B testing (US) | `/looki/client/ag/v1/abtest` |

### 4.2 ByteDance / ByteIntl hostnames

| Hostname | Role (inferred) | Evidence |
|---|---|---|
| `ailab-agent-web.byteintl.net` | AI lab agent web service | host string present |
| `lf16-tos-sg.capcutstatic.com/obj/` | Object storage (CDN, Singapore) | `https://lf16-tos-sg.capcutstatic.com/obj/` |
| `lf16-beecdn.ibytedtos.com/` | Bee CDN (assets) | multiple `tos_*.png/webp` URLs |
| `sami-sg1.byteintlapi.com/internal` | ByteIntl internal API | host string present |
| `abtest-sg.byteoversea.com/common` | A/B testing config | host string present |
| `log.byteoversea.com/service/2/app_log_test/` | Logging | host string present |
| `i18n-everphoto.faceueditor.com` | Faceu/photo editor (ByteDance) | host string present |

### 4.3 AI model providers identified in client code

**VERIFIED** — model names appear as string literals / config keys in DEX:

| Model ID / name | Class | Provider evidence |
|---|---|---|
| `seedance`, `seedance1.5`, `seedance15pro`, `seedance2.0`, `seedance20`, `seedance20fast`, `seedance20mini`, `seedance2_v2`, `seedance2fast`, `seedance2mini`, `seedance_1.0_mini`, `seedance_7b_ai_trans`, `seedance_v10` | Video generation | ByteDance "Seedance" naming; `material_gen_video_by_seedance_v10`, `material_gen_video_by_seedance_1.0_mini`, `AI_video_expand_seedance20fast_480p`, `useSeedance2Model` |
| `minimaxh3`, `minimax_h3` | Video | MiniMax "H3" naming |
| `qwen`, `qwen7b`, `qwen_stream_tts`, `qwen_stream_tts_v2`, `qwen_text_to_speech` | Text-to-speech / LLM | Alibaba "Qwen" naming; `cc_qwen_tts_config`, `platform="qwen"`, `TtsQwenSavingStreamingConfig` |
| `SeeDance2` | Dance/AI feature | string literal `SeeDance2` and `seedance2` |

No private fal.ai / MiniMax / Alibaba API key was found in the APK; the client uses
bootstrap/proxy values supplied by CapCut's backend (`capcutapi.com`). This confirms the
aggregator/proxy pattern: client → CapCut backend → provider gateway.

### 4.4 Minimum backend endpoint pattern (observed)

- `POST /lv/v1/edit/get_config` (feed-api)
- `POST /storyboard_agent/v1/ai_lab/send_msg` (aivideo)
- `GET /storyboard_agent/v1/ai_lab/poll_by_event_id` (aivideo)
- `POST /purchase/iap/make_order` (commerce / general-api-us-looki)
- `POST /tools_export/`, `/template_export/`, `/autocut_export/` (feed-api)

## 5. AI features and creative capabilities

**VERIFIED** — strings, field names, and class names in DEX:

Video generation / AI:
- `text_to_video_by_category` — text-to-video by category
- `intelligent_multi_image_to_video` — multi-image → video
- `AiVideoDriverAsyncTaskHandler`, `IAiVideoDriverService` — AI video driver service
- `LVVEGenAIVideoGeneration` — AI video generation task
- `ai_video_generation_panel_action`, `ai_video_generation_task_id_list` — generation UI
- `AIVideoToMusicTaskPostHandler` — AI video→music scoring
- `AIVideoInPaintingRepaintFunction` — AI inpainting / repaint
- `ai_painting_nav_host_fragment`, `getAi_painting_v3_bottom_setting` — AI painting
- `ai_character_render_export_popup`, `ai_character` — AI character render
- `cap_omni_digital_human_rendering` — "CapCut Omni" digital human rendering
- `digital_human_customize/upload`, `digital_human/create_item`, `digital_human_ai_video_translate`,
  `digital_human_presenter_download`, `digital_human_tts_filter_pattern` — digital human / avatar
- `ai_avatar`, `AVATAR_3D`, `avatar_id`, `avatarUrl`, `getAvatar` — 3D avatars
- `auto_cut_*`, `AutoCutScoreInfo`, `AutoCutFaceInfo`, `AutoCutParallelOptConfig` — AutoCut / smart cut
- `getSmart_cut_switch_ai_voiceover` — AI voiceover in smart cut

Editing features:
- `ai_cutout`, `getCutout`, `intelligentCutoutAndPlay`, `id":"cutout_image"` — AI cutout / background removal
- `getChroma`, `chroma=` — chroma key / green screen
- `pip_count`, `pip_sound`, `PIPE_PIPE` — picture-in-picture
- `stabilize` — video stabilization
- `trackings` — motion tracking
- `keyframes`, `ApplyEffectParam_*_keyframe_*`, `ADD_GLOBAL_VIDEO_EFFECT_KEYFRAME` — keyframe animation
- `3d_zoom`, `tt_slide_up_3d` — 3D zoom / 3D effects
- `freeze_frame`, `reverse_video` — freeze / reverse
- `auto_beat` — auto beat sync
- `lyricList`, `sub_lyric`, `AddLyricEffectParam`, `isLyric` — lyrics / subtitles
- `ttsUrl`, `ttsUri`, `ttsTexts`, `ttsVoice`, `ttsGender`, `ttsTimbre`, `ttsTaskId`, `Voiceover`,
  `SCENE_TTS`, `TTS_START`, `aicGetTts` — text-to-speech / voiceover
- `voice_effect`, `voice_changer` — voice effects

## 6. Native video-processing libraries (arm64_v8a split)

**VERIFIED** — 162 `.so` files. Key native libs:

| Library | Role (inferred from name) |
|---|---|
| `libcapcut.so` | Core CapCut engine |
| `libcccreator.so` (4.9 MB) | Video creator/compositor |
| `libclipflow.so` (4.9 MB) | Clip processing pipeline |
| `libbytenn.so` (3.1 MB) | ByteDance neural network inference |
| `libbytevc0/1/2*` | ByteDance video codecs (en/decode) |
| `libanimax*` | Animation system |
| `libaudioeffect.so` (5 MB) | Audio effects |
| `libavmdlbase.so`, `libavmdlv2.so` | AV model / media |
| `libc++_shared.so`, `libbytenn.so` | NN/runtime |
| `libEncryptor.so` | Encryption |
| `libTTMStrategyCenter.so`, `libTTMachineCore.so` | Strategy / machine core |
| `libapplovin-native-crash-reporter.so` | Applovin crash reporting |
| `libaudio_fingerprint_sdk.so` | Audio fingerprinting |
| `libbytedt.so` | ByteDance data/telemetry |
| `libfastcv.so`, `libfdk-aac.so` | CV / AAC audio |

Interpretation: CapCut is predominantly a native C++ video engine with a Java/Kotlin UI
layer; AI inference (bytenn) and video codecs (bytevc) run natively.

## 7. Monetization and social

**VERIFIED**:
- `com.android.vending.BILLING` + IAP endpoint `/purchase/iap/make_order` — subscriptions / Pro
- Applovin, Vungle, Pangle ad SDKs (ads + rewarded)
- PayPal sandbox URLs (`www.sandbox.paypal.com`) — payment testing
- Instagram/Facebook/YouTube/TikTok/Twitter/WhatsApp/Telegram/Snapchat/LinkedIn package
  queries + OAuth (`instagram.com/oauth/authorize`, `api.instagram.com/oauth/authorize/`) —
  social login and export/sharing
- AppsFlyer, Firebase Messaging, Ably, Passkeys (attribution/push/auth)

## 8. How everything fits together (architecture, INFERRED)

Client (Java/Kotlin UI + native C++ engine):

1. User picks media / enters prompt in the Compose/View UI.
2. `MainActivity` → feature modules (editor, AI lab, AutoCut, digital human, AI painting).
3. For AI jobs: client calls CapCut backend (`editor-api`/`feed-api`/`aivideo-* .capcutapi.com`)
   which proxies to the model provider (Seedance for video, Minimax H3, Qwen for TTS).
4. Client polls `.../ai_lab/poll_by_event_id` for job status; result media stored on
   `lf16-tos-sg.capcutstatic.com` (CDN) and played back via ExoPlayer/Media3.
5. Native libs (`libcapcut`, `libcccreator`, `libclipflow`, `libbytenn`, `libbytevc`) do local
   decode/encode, filters, transitions, keyframes, stabilization, cutout, chroma key.
6. IAP via Google Play Billing (`commerce-api.capcutapi.com`) unlocks Pro/features/exports.
7. Export/share to social platforms via package queries + intents.

## 9. Trace scan — Chad-VidCraft must NOT contain

The Chad-VidCraft APK and source were scanned and contain ZERO matches for:
`capcut`, `cutting`, `bytedance`, `tiktok`, `com.ss.android`, `capcutapi`, `lvoverseas`,
`vega`, `seedance`, `minimax`, `qwen`, `ailab`, `byteintl`, `faceu`. Both source and compiled
DEX are clean. This is a bounded client-side check.

## 10. What Chad-VidCraft models (ORIGINAL implementation)

Chad-VidCraft is an independent reconstruction of the observable CapCut feature model, with
ORIGINAL names, IDs, icons, and endpoints. Nothing is copied from CapCut.

Feature mapping (Chad-VidCraft original names):

| CapCut feature | Chad-VidCraft original equivalent |
|---|---|
| Text-to-video (`text_to_video_by_category`) | Chad AI Video (text→video) |
| Multi-image→video | Chad Image-to-Video |
| Seedance / Minimax H3 video | Chad Motion models (original IDs) |
| Qwen TTS | Chad Voice (original TTS) |
| Digital human / avatar | Chad Persona (original avatar) |
| AI painting | Chad Canvas (image gen) |
| AutoCut / smart cut | Chad AutoEdit |
| AI cutout / chroma key | Chad Cutout / Chad Chroma |
| Picture-in-picture | Chad PIP |
| Stabilization, motion tracking, keyframes | Chad Stabilize / Chad Track / Chad Keyframes |
| 3D zoom, freeze, reverse | Chad 3D Zoom / Chad Freeze / Chad Reverse |
| Lyrics / subtitles | Chad Captions |
| Auto beat sync | Chad BeatSync |
| Voice effects / voiceover | Chad Voice FX |
| Billing / Pro | Chad Credits + Chad Pro |
| Social share | Chad Share |

Backend design (original, not CapCut endpoints):
- `POST /v1/jobs`, `GET /v1/jobs/{id}`, `POST /v1/jobs/{id}/cancel`
- `POST /v1/uploads/presign`
- `POST /v1/webhooks/{provider}`
- `GET /v1/models`, `GET /v1/credits`, `POST /v1/billing/verify`
- Flow: Android → Chad API (auth + credit reserve) → job queue → provider gateway → object
  storage → webhook/poll → Chad API → Android

## 11. UI/UX improvement targets for Chad-VidCraft (vs CapCut)

CapCut observations: dense native editor, obfuscated single-letter resources, many native libs,
complex permission set, ad/billing-heavy. Chad-VidCraft aims to improve with:

- A distinct dark studio theme (slate/indigo/cyan) — original, not copied
- Newly authored vector icons for every mode/tab/action — no CapCut icon reuse
- Director Bot floating assistant (offline, no network)
- Beat-bar music visualization in the Create card
- Staged pipeline progress with per-scene approval and regeneration
- Local render simulation that is explicitly labeled (never fakes a real final output)
- Cloud render with real polling, cancel, retry, resume, and output URL
- Original sample tasks seeded on first launch (useful offline immediately)
- 5 tabs: Home, Discover, Create, Tasks, Profile
- 8 create modes matching the feature model above

## 12. Evidence index (source paths)

All under `/tmp/opencode/capcut/`:

| Topic | Source |
|---|---|
| Manifest, permissions, components | `apk/base.apk` via `aapt2 dump badging` / `xmltree` |
| Build metadata | `aapt2 dump badging base.apk` |
| Backend hostnames / endpoints | `strings` over `classes.dex`…`classes29.dex` |
| AI model providers (Seedance, Minimax, Qwen) | `strings` over DEX files |
| AI features (text-to-video, digital human, cutout, TTS, etc.) | `strings` over DEX files |
| Native libraries | `unzip -l apk/split_config.arm64_v8a.apk` |
| Monetization/social | `strings` over DEX + badging |

## 13. Deliverable build

Project root: `/storage/emulated/0/Download/APPS/Chad-VidCraft`
Package: `com.chadvidcraft.app` / versionCode `1` / versionName `1.0`
Signed APK SHA-256: `236cdd203f4d41beeac233c5f961382db89da58ee197c50d237c4709cc2aaf19`

| Artifact | Path | Status |
|---|---|---|
| Signed APK | `Chad-VidCraft.apk` | Built, zip-aligned, v2/v3 verified |
| Build copy | `build/Chad-VidCraft.apk` | Same bytes as top-level APK |
| Build script | `build.sh` | Rebuilds APK from source and resources |
| App source | `app/src/com/chadvidcraft/app/` | MainActivity, Task, TaskStore, Ui, ArtView, Pipeline, Scene, ModelCatalog, DirectorBot, CloudApi |
| Resources | `app/res/` | Original vectors, theme, colors, strings (incl. 16 mode icons) |
| Manifest | `app/AndroidManifest.xml` | `com.chadvidcraft.app`, minSdk 26, targetSdk 35 |

16 create modes (original Chad design, informed by CapCut feature study — nothing copied):
Music Video, Dance Video, Lyric Video, OnBeat Effects, Photo Karaoke, Music Cover Video,
Video to Music, Realtime, AI Video, Image-to-Video, AI Persona, AI Canvas, Auto Edit,
Cutout & Chroma, Voice & Captions, Motion FX.

Original Chad model catalog (no CapCut model IDs reused):
- Image: Chad Canvas, Chad Canvas Pro, Chad Ink
- Video: Chad Motion Lite, Chad Motion, Chad Motion Style, Chad Motion Stage, Chad Motion Dance,
  Chad Motion AI (text-to-video), Chad Motion Image2Vid
- Realtime: Chad Live Director
- Persona: Chad Persona
- TTS: Chad Voice

Build toolchain: `aapt2`, `javac --release 11`, `d8`, `zipalign`, `apksigner`, debug keystore
at `build/`.

Verification performed: resources link successfully, Java compiles, D8 produces `classes.dex`,
APK signature verifies with v2 and v3 schemes, `aapt dump badging` confirms package, launcher
activity, minSdk 26 and targetSdk 35. Trace scan returns zero matches for CapCut/Bytedance/
TikTok/capcutapi/seedance/minimax/qwen in source or DEX. Device runtime verification is left
to the user because this shell has no real root/adb and MIUI blocks `pm install`.
