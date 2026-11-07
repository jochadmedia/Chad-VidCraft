# Agent Guide — Chad-VidCraft

Read `MEMORY.md` first, then `memory/STATE.json`. `EVIDENCE.md` holds the durable research
dossier.

## Non-negotiable product rules

- Chad-VidCraft is original work. Do not copy CapCut or any third-party app's code, assets,
  icons, fonts, prompts, templates, hostnames, endpoints, model IDs, or branding.
- The shipped APK must contain no name, URL, or identifier from CapCut or any reference app.
- Run the CapCut/Bytedance trace scan after every build (commands in `MEMORY.md` §4).
- Do not claim the APK was installed or runtime-tested unless it actually was.

## Working directory

Use `/storage/emulated/0/Download/APPS/Chad-VidCraft` as the project root.

## Build and test

```bash
bash build.sh
```

Server (optional):

```bash
cd server && npm test && npm run typecheck
```

## Code conventions

- Android: Java, framework Views only. No AndroidX, Kotlin, Compose, Room, or new runtime
  libraries unless the user explicitly approves.
- UI helpers live in `Ui.java`; keep colors/spacing consistent with the dark studio theme.
- Server: Node 24 TypeScript with `.ts` imports and strict mode.
- Never fake render progress or output. If the provider is not configured, fail closed with a
  clear error.
- Never put provider API keys, tokens, or secrets in the APK or repository.

## Persistence of context

At the end of every working session:

1. Update `MEMORY.md` session log with date, version, hash, changes, and next steps.
2. Update `memory/STATE.json` with the same facts.
3. Recompute and record the APK SHA-256.
4. Leave the tree buildable.

If context is lost, resume from `MEMORY.md`; do not redo completed work.

## Safety

- CapCut remains read-only. Development is static study only unless the user explicitly
  requests runtime observation.
- Do not ship provider credentials in the APK.
