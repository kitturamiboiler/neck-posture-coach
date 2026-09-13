# Neck Posture Coach

Android posture-measurement portfolio project based on the Google ML Kit vision quickstart.

The goal is to capture a side-view posture frame, detect ear and shoulder landmarks on-device, estimate the neck angle, show a self-reference posture result, suggest corrective exercises, and track changes over time.

## Current Status

- Base project: Google ML Kit samples
- Working app: `android/vision-quickstart`
- Target platform: Android
- Processing: on-device only
- Network/backend: none
- Image storage: none; only derived measurement values should be stored
- Verified so far: Gradle debug build succeeds with JDK 17

## Project Rules

See [`AGENTS.md`](AGENTS.md) before changing code.

Important boundaries:

- Do not add a server, cloud sync, analytics, ads, or account system.
- Do not save or upload photos/videos.
- Do not use medical diagnosis or treatment wording.
- Do not add new dependencies without review.
- Keep changes small and test posture math separately from ML Kit/camera code.

## Development Flow

Branch strategy:

```text
master or main  stable branch
dev             integration branch
feat/*          feature branches from dev
```

Commit format:

```text
type(scope): summary
```

Examples:

```text
docs: describe neck posture coach project
fix(android): remove unsupported Gradle JVM option
feat(M-03): add posture angle calculator
```

## Build

Use JDK 17 for this sample project.

PowerShell:

```powershell
$env:JAVA_HOME='C:\Users\mjm32\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd C:\dev\mlkit\android\vision-quickstart
.\gradlew.bat assembleDebug
```

Android Studio:

1. Open `C:\dev\mlkit\android\vision-quickstart`.
2. Set Gradle JDK to `C:\Users\mjm32\.jdks\jbr-17.0.14`.
3. Sync Gradle.
4. Run on a real Android device.

## Next Milestones

1. Run the ML Kit sample on a Galaxy Tab or Android device.
2. Confirm pose detection shows ear and shoulder landmarks.
3. Add a minimal posture-angle prototype.
4. Add frame quality gates.
5. Add risk classification and local measurement storage.

## Upstream

This project starts from the Google ML Kit samples repository:

https://github.com/googlesamples/mlkit

