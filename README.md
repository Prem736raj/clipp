# Clipp

Clipp is a private, local Android video editor focused on verified MP4 export and truthful capability boundaries.

## Current scope

The project includes local editing and export workflows for video, images, text, overlays, transitions, audio, and supported effects. Production hardening has focused on build reliability, storage safety, project recovery, media transforms, export correctness, and keeping unsupported capabilities clearly separated from working ones.

## Build and verify

Use the committed Gradle wrapper:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Additional release and device checks are tracked in the repository documentation.

## Project status

- [Hardening notes](HARDENING_NOTES.md)
- [Remaining work](CLIPP_REMAINING_WORK.txt)
- [Changelog](CHANGELOG.md)

Release signing credentials must remain outside version control. Review the remaining-work document before treating the app as production-ready.
