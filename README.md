# Wild Drives V14 — Android / GitHub build

Offline landscape driving game using the V14 showroom and WebGL 2 engine inside a native Android WebView. Seven reference-built vehicle meshes and the showroom images are already embedded in `app/src/main/assets/index.html`. No external model download, Node package install, API key, NDK or Unreal Engine is needed. The original Free3D/Sketchfab/Poly Haven files are not included; the supplied 3D meshes and environment are reconstructions/procedural assets.

## Build on GitHub (recommended)

1. Create an empty **private** GitHub repository, e.g. `Wild-Drives`. Do not initialize with a README, license or .gitignore if pushing this project.
2. Extract the ZIP. In Terminal, enter the extracted `Wild_Drives_Android` directory (the directory containing `settings.gradle` and `.github`).
3. Run the following, replacing `YOUR_USERNAME`:

```sh
git init -b master
git add .
git commit -m "Initial Wild Drives Android build"
git remote add origin https://github.com/YOUR_USERNAME/Wild-Drives.git
git push -u origin master
```

If Git asks for authentication, use GitHub's normal authenticated Git flow (e.g. GitHub CLI or a credential manager). Do not paste tokens into source files. The workflow is already present at `.github/workflows/build-apk.yml`. It runs on pushes to master or main, or manually from **Actions > Build Wild Drives APK > Run workflow**.

4. Open the successful workflow run and download the artifact `WildDrives-v14-APK`. Extract it and transfer `WildDrives-v14-debug.apk` to your phone.
5. Open the APK and permit installation from the specific file manager or browser if prompted. No game network or storage permission is required for normal gameplay. Keep Android System WebView current and run in landscape. If you later build again, debug signing keys may differ between CI runs, so Android might require uninstalling the previous debug APK before installing a new one (this deletes app data). Use a persistent private signing key for reliable in-place updates; no private signing key is included.

## Build locally (optional)

Install JDK 17, Gradle 8.11.1, Android SDK platform 35, build-tools 35.0.0, Python 3 and Node.js. Set `ANDROID_HOME` to the SDK directory or set `sdk.dir` in an untracked `local.properties`. From the project root:

```sh
python3 scripts/verify_assets.py
gradle --no-daemon :app:assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions installs Gradle directly, so no `gradlew` wrapper is required for CI.

## Galaxy S26

- Enable the phone's adaptive/high refresh-rate display option.
- The game requests up to 120 Hz and starts in adaptive rendering mode. Sustained FPS depends on WebView, device GPU, power policy, resolution and temperature.
- Use **60 FPS balanced** or **40 FPS cool** in settings for long sessions if the device warms up. Native Android thermal status is passed to the renderer, which reduces workload under thermal pressure. The game also lowers its rendering rate while stationary, paused, or hidden. Zero heat cannot be guaranteed.
- Tap Garage to select a bundled vehicle, or use Settings for weather, camera and profile options. Optional local GLB import uses the Android system file picker. Imported models are rigid visual meshes; their wheel rigs may not be animated.

## Verification and limitations

`verify_assets.py` checks the seven embedded model hashes, finite triangulated vertex arrays, expected triangle counts and JavaScript syntax. The CI workflow additionally checks APK signing and that the exact game HTML is present inside the signed APK. The current environment has no Android SDK or Gradle installation; the APK has not been compiled or installed here. Full-game GPU rendering, touchscreen behavior on a physical S26, actual temperature and sustained 120 FPS are not verified. `docs/VALIDATION.txt` is the earlier V14 test record, not new Android test results.
