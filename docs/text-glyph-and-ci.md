# Text glyph movement and test APKs

## Diagnosis and options

The reported trigger is dragging/releasing at the scroll boundary on Android 12+.
Compose's platform effect renders scroll content through a stretched RenderNode.
Isolating editable fields left static labels and lines outside the workaround.
This implicates the common rendering/compositing path rather than text input or
per-letter state. The specific driver/platform defect is not confirmed without
reproducing it on the affected device.

The application scrollbar also intentionally animates its opacity from 1 to 0;
its base colour has alpha 0.7. A colour transition in that thumb alone can be normal.
Darkening of unrelated dividers while stretching needs separate device confirmation;
it must not be attributed conclusively to the same defect from source inspection.

Options:

1. **Rasterize each viewport before platform stretch (this build).** Wrap the
   platform OverscrollFactory once in AppTheme. A layout node delegates the native
   effect and places its child in an offscreen layer. This retains native event
   handling/physics and stretch, and includes static text, lines, icons and fields.
   It avoids a buffer per field and buffers bounded viewports rather than full
   scroll content. Costs: viewport-sized GPU buffers, possible clipping of shadows
   outside the viewport, and texture sampling during stretch. Custom components
   explicitly opting out of the local factory are outside this mechanism.
2. **Replace platform stretch with a custom effect.** Greater implementation and
   gesture/nested-scroll validation cost; motion differs from the platform effect.
3. **Disable platform stretch.** Smallest workaround, but does not meet the request
   to keep stretch, so it is not used.
4. **Platform/Compose update.** Preferred once an upstream fix is identified and
   reproduced on the affected device. No version upgrade is claimed to fix this.

The field-specific layers were removed. This build is a diagnostic mitigation,
not a confirmed repair of the underlying Android GPU implementation.

References:

- [Compose Android overscroll implementation](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/androidMain/kotlin/androidx/compose/foundation/AndroidOverscroll.android.kt)
- [Offscreen compositing](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers#compositing-strategy)

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

## Validation

The release APK is built with the test certificate described above. It is a custom
build of the F-Droid distribution, not an official F-Droid signed binary.

**Not run:** device/emulator rendering and interaction checks: no device or emulator
is available. On the affected device compare top/bottom stretch in static text,
editable fields, dividers, icons and scrollbars; check return to rest, nested scroll,
light/dark, dynamic colour, one/two columns, larger font scales and IME interaction.
Inspect shadows/clipping and GPU memory/jank on long lists. Verify scrollbar fading
separately from divider colour. A successful build cannot validate these GPU effects.
