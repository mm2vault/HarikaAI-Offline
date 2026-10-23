# Aura AI — Clean Rebuild Audit

## Safety rules
- This branch starts from `main`; existing `main` and the previous draft PR are left untouched.
- Keep the existing Android `applicationId` (`ai.harika.jarvis`) until the installed APK's package identity and signing certificate are confirmed. Display branding can be Aura AI without changing this ID.
- Never ask the user to uninstall the installed app until chat history has an export/restore path and the signature situation is understood.
- No paid APIs or services. Offline model remains the default; web search is explicitly user-triggered.
- A successful CI build is not proof of on-device model inference or in-place APK update.

## Initial audit findings (2026-10-23)
1. The default `main` branch is a small initial Android app and still uses JARVIS branding in the manifest, Gradle root project, source package, and prompt.
2. The experimental branch accumulated UI, updater, web search, package moves, and signing changes in a single `MainActivity.kt`-centric implementation.
3. The experimental branch has two APK workflows: one debug workflow publishes a `aura-latest` APK, while another publishes numbered releases. The debug workflow can produce a signature that does not match a previously installed APK.
4. The latest experimental workflow now requires four signing secrets and correctly fails if they are absent. Run 33 failed at that preflight because the secrets were missing; no APK was produced. This is safer than silently publishing a debug APK, but does not solve an already-installed APK signed by another key.
5. The model download currently uses a direct Hugging Face URL and only a rough file-size check. It needs stronger failure recovery and a verifiable integrity check if the upstream provides a stable checksum.
6. Chat history is saved in app-private SharedPreferences. Package ID/signing mistakes or uninstalling can put local history at risk; export/import should be designed before any migration.
7. Dependency version `latest.release` is not reproducible. Pin a tested LiteRT-LM version.
8. The system prompt file is documented but the current activity appears to handle inference directly; verify prompt wiring and separate inference from UI.
9. README and `brain/README.md` contain legacy JARVIS wording and point to the old source path.

## Rebuild phases
1. **Inventory and requirements:** map every source/resource/workflow and preserve current behavior intentionally.
2. **Identity and update safety:** document package ID, versionCode strategy, signing setup, single canonical release URL, and chat backup/export.
3. **Architecture:** split UI, local model runtime, model download, chat persistence, voice I/O, updater, and explicit web search into separate modules/classes.
4. **Reliability:** pinned dependencies, bounded network operations, partial-download cleanup/resume where supported, explicit errors, lifecycle-safe threading, and safe APK installation.
5. **Branding/docs:** Aura AI consistently in all user-visible names, README, prompts, workflow/release names; keep package ID stable for upgrade compatibility.
6. **Validation:** inspect diffs, run GitHub Actions, check APK signature metadata, verify version/package values, and report what still requires a real Android device.

## Acceptance criteria
- No user-visible Jarvis/JARVIS/Jarvis branding remains.
- Existing installed app is not replaced by a second package.
- APK release is signed by a stable key, and no debug fallback is published as a production update.
- Chat history can be backed up and restored before any destructive migration.
- Local model mode, lightweight fallback, voice input/output, web search, and updater failures are handled honestly.
- CI passes before calling the rebuild build-verified; real-device checks remain explicitly marked until performed.
