# F-Droid test release

Source commit: `c60e6c36`. Distribution: F-Droid release, universal ABI.
Native platform stretch is retained; viewports are rasterized before stretch.
Signed with the testing certificate (SHA-256):
`E9:A3:93:EC:A5:79:8F:23:7D:AA:39:0B:AF:37:CE:8E:5E:B5:87:FB:6F:48:B2:C5:C9:F9:F5:1F:7C:C8:92:29`.
This is not an official F-Droid signed package.

Validation: 150 F-Droid debug unit tests passed, including two event-delegation
regressions; F-Droid debug Kotlin compilation, release assembly and release vital
lint passed. APK signature, application ID and checksum checked.

Not run: physical-device/emulator rendering, gesture/IME interaction and GPU memory
checks. The viewport rendering workaround requires validation on the affected device.
