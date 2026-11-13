# Script2Cartoon (fresh Android starter)

This is a **basic animated caption video generator**, not AI cartoon character animation.
Paste Hindi or English text, tap Generate MP4, and the app writes a silent portrait H.264 MP4 to `Movies/Script2Cartoon`.

## Build with GitHub (no PC required)
1. Create a **new** GitHub repository (for example `Script2Cartoon-New`) with no README.
2. Upload the **contents** of this package to the repository root, including `.github/workflows/android-apk.yml`.
   **Important:** GitHub's web upload may skip hidden folders when selecting a directory; use GitHub's Create new file option for `.github/workflows/android-apk.yml` if necessary.
3. Go to Actions > Build Android APK > Run workflow, or push to main.
4. Open the successful run > Artifacts > Script2Cartoon-debug-apk; download and unzip it.
5. Install `app-debug.apk` on Android (allow installation from the browser/file manager if prompted).

## Notes
- Needs Android 8+; saving via MediaStore RELATIVE_PATH is best supported on Android 10+.
- No voiceover, AI scene illustrations, or lip-sync in this starter.
- Rendering depends on device H.264 hardware encoder availability and can take time.
- This source has not been verified with a completed GitHub Actions run in this environment.
