# Text glyph movement and test APKs

## Diagnosis and minimal change

The reported trigger is scrolling from the bottom edge, rather than a floating
label transition. On Android 12 (API 31) and newer, Compose's default overscroll
uses the platform stretch effect on a RenderNode. This deforms the rendered
content at the scroll boundary and is the leading explanation for letters moving
independently near that boundary. No per-letter animation was found in the form
components. The specific device/GPU defect has not been reproduced here.

`FormTextField` and `FormDropdownField` now use
`graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }`. Each bounded
field is rasterized into an intermediate texture before its parent applies the
stretch effect. The compositor then transforms the field as an image rather than
replaying its glyph display list through the stretch path. Overscroll/stretch
remains enabled. Typography, text sizes, editing state and normal scrolling are
unchanged. The tradeoff is an additional offscreen buffer for each visible field;
this is deliberately scoped to the two shared form components rather than whole
screens or potentially unbounded editor content.

References:

- [Compose Android overscroll implementation](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/androidMain/kotlin/androidx/compose/foundation/AndroidOverscroll.android.kt)
- [Compose graphics modifiers and offscreen compositing](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#compositing-strategy)

This is a targeted mitigation for the reported edge-scroll trigger. Device testing
is still required to confirm that it eliminates the user's glyph artifact while
preserving stretch. No device-specific rendering defect was reproduced here.

## CI

`.github/workflows/ci.yml` runs on pushes, pull requests and manual dispatch.
It initializes native submodules, builds HEV for the declared ABIs, downloads the
Xray AAR from the exact checked-out submodule tag, runs Play Store debug unit tests,
compiles Kotlin, builds APKs, verifies their signatures and uploads APKs/test
reports. No release secrets are needed. Each run generates a new test key, so APKs
from different runs cannot update one another without uninstalling the previous
installation. The existing release workflow remains available for stable signing.

## Reusable local test certificate

Generate a persistent test key and export its public certificate outside Git:

```sh
keytool -genkeypair -noprompt -storetype JKS \
  -keystore /absolute/path/test-signing.jks \
  -storepass android -keypass android -alias v2rayng-test \
  -keyalg RSA -keysize 2048 -validity 3650 \
  -dname 'CN=v2rayNG Test,OU=Testing,O=v2rayNG,C=ZZ'
keytool -exportcert -rfc -keystore /absolute/path/test-signing.jks \
  -storepass android -alias v2rayng-test -file /absolute/path/test-signing.pem
```

This key and the public example passwords are for testing only. Keep private keys
out of Git. To reuse the key for local APKs, run from `V2rayNG/`:

```sh
./gradlew :app:assemblePlaystoreDebug \
  -Pandroid.injected.signing.store.file=/absolute/path/test-signing.jks \
  -Pandroid.injected.signing.store.password=android \
  -Pandroid.injected.signing.key.alias=v2rayng-test \
  -Pandroid.injected.signing.key.password=android
```

## Device validation still required

Local validation passed after the offscreen form-field change:
`FormFieldsTest`, all 148 tests in `:app:testPlaystoreDebugUnitTest`,
`:app:compilePlaystoreDebugKotlin`, `:app:assemblePlaystoreDebug`,
`actionlint .github/workflows/ci.yml`, and `git diff --check`.
All five APKs passed `apksigner verify` and use the generated test certificate.
No JVM test can verify this GPU rendering change.

**Not run:** `:app:lintPlaystoreDebug` on the final patch. An earlier lint run was
cancelled when the implementation changed; its results do not validate this patch.
APKs are in `V2rayNG/app/build/outputs/apk/playstore/debug/`.

**Not run:** emulator/physical-device rendering and interaction checks: no emulator
or system image is installed and `adb devices` lists no devices. On API 31+ scroll to the bottom, continue dragging past the edge and release,
with and without the IME; confirm stretch remains visible and text glyphs stay
aligned. Also check API 24–30 retains its edge glow, labels/borders are not clipped,
selection/cursor/dropdown interactions still work, and large font scales fit. Check focus/blur and typing with the IME visible, first/last form controls and submission; light/dark
themes, dynamic colour on/off (API 31+) and single/double-column layouts. Record
whether the symptom concerns the floating label or editable text, including at
larger font scales. Unit tests and an assembled APK do not verify visual behaviour.

GitHub Actions run status and downloadable APK artifacts are available on the PR.
