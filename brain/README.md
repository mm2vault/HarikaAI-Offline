# JARVIS Brain

This directory documents the AI runtime boundary for JARVIS AI.

## Design goals
- Offline-first: the Android app runs the Qwen LiteRT-LM model on the user's device.
- No paid API key is required for local inference.
- A missing or broken model must be reported clearly; the lightweight fallback is not presented as a full AI.
- Keep UI code separate from inference, prompt policy, local memory, and tools.
- Any future remote brain must be optional, separately deployable, and never require paid services by default.

## Current runtime
The current Android inference path is implemented in `app/src/main/java/ai/harika/jarvis/MainActivity.kt` using LiteRT-LM. The app downloads the model file and initializes an `Engine` and `Conversation`.

## Next implementation milestones
1. Move model initialization and generation out of MainActivity into a dedicated Kotlin brain class.
2. Add verified model download (expected size, checksum if upstream provides one, temporary file, resume/retry).
3. Add structured conversation memory and safe tool dispatch (date/time, arithmetic, local app actions).
4. Add clear model health diagnostics and recoverable initialization errors.
5. Test on a real Android device with the actual model; a successful APK build alone does not prove inference works.

## Important limitation
Without a loaded language model or a remote inference service, ordinary code cannot provide open-ended, general-purpose AI answers. The no-model mode is only a small rule-based fallback.
