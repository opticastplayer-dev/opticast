# 2.6.51 host compatibility audit

- All baseline `project/app/src/` files match the locked 2.6.50 source ZIP byte-for-byte. No PiP, UI, buffer or resume behavior code changed.
- All 10 controlled ARM64 native libraries retain baseline hashes. The new ARM32 runtime contains all 10 corresponding libraries; JNI exports, ELF32/ARM identity, dependency closure and actual FFmpeg GPL/version3 configuration checked.
- All 22,787 bundled native source-file hashes passed after the ARM32 build. Existing source bundles are unchanged.
- AndroidX ARM32 payloads came from the existing hash-locked official AARs. ELF32 ARM, 16KB LOAD alignment, dependencies only libc/libm/libdl; evidence retained alongside this file.
- mpv ARM32 Ninja compile arguments contain `-D_FILE_OFFSET_BITS=64`; FFmpeg ARM32 CPPFLAGS contain `-D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE`. This is a build configuration check, not a >2GB device playback test.
- ARM32 standard NDK libc++ requires only the normal 4KB minimum LOAD alignment; ARM64 retains the stricter 16KB gate. No 16KB ARM32 device support claim.
- Library permission selection uses READ_EXTERNAL_STORAGE below API33; MediaStore projection avoids relative_path below API29. Android8/API26 minimum unchanged.
- Device model/ABI unknown. ARMv7/NEON and ARM64 are supported targets; no x86 libraries. Physical installation, playback and PiP acceptance remain outstanding.
