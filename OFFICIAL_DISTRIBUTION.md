# Official Distribution — OptiCast Video Player

**Package:** `com.opticast.player`  
**Signing Key SHA256:** `f7e5ba2643181fbe2327f29ad814c0de8c937fab9ae276c2957f7ddc1d3f0240`  
**Certificate:** CN=Luma App Release, RSA/3072, Valid 2026-2034  
**License:** GPL-3.0-only

## ✅ Official Sources Only

OptiCast is officially distributed **only** from these sources controlled by the OptiCast Project (same signing key):

| Source | URL | Status |
|--------|-----|--------|
| **GitHub Releases** | https://github.com/opticastplayer-dev/opticast/releases | ✅ Official, direct APK + SHA256SUMS |
| **Website** | https://opticast.app/ (and https://opticastplayer-dev.github.io/opticast/) | ✅ Official, direct download page |
| **F-Droid** | https://f-droid.org/packages/com.opticast.player | ⏳ MR !50919 open, mergeable — will be official when merged |
| **IzzyOnDroid** | https://apt.izzysoft.de/fdroid/index/apk/com.opticast.player | ⏳ Issue #659 open — will be official when approved |

**All official APKs are signed with the same key** `f7e5ba26...3f0240`. You can install new version over old, data preserved. If signature differs, Android will refuse install ("App not installed — conflicting package") — that means it's **unofficial/fake**.

## ❌ Unofficial / Unauthorized Stores

The following stores are **NOT authorized** and **NOT official**:

APKPure, APKMirror, Aptoide, Uptodown, APKMirror, Malavida, APKFab, APKCombo, any Play Store clone, any site offering "OptiCast Pro / Mod / Premium".

- They are **not controlled** by OptiCast Project
- They **may contain malware, ads, or modified code**
- They **may not provide source** → GPL-3.0 violation
- They **may use different signing key** → cannot update to official, data loss risk
- They **impersonate** official app using our name/logo → trademark violation

> **Download only from official sources to avoid malware. If you got APK from unofficial store, uninstall and install from https://opticast.app/**

## Trademark

**OptiCast™** and OptiCast logo are trademarks of OptiCast Project.

- Unofficial builds **must not** use name "OptiCast" or logo to imply official, per trademark law.
- Forks must rename: e.g., "OptiCast Fork" is not allowed, use different name like "MyPlayer (based on OptiCast)".
- If you see app named "OptiCast" on Play Store or other store not listed above, it's **impersonation**.

## GPL-3.0 Compliance

OptiCast is GPL-3.0-only. Anyone redistributing APK **must**:

1. Keep GPL-3.0 license file
2. Provide link to source: https://github.com/opticastplayer-dev/opticast
3. State changes if modified
4. Provide source of modifications under GPL-3.0
5. Not add proprietary tracking

If a store distributes APK without source link or license, it's **GPL violation**.

## How to Report Unofficial / Fake Upload

If you find OptiCast on unofficial store:

1. **Check signing key**: Settings → About → Signing key should be `f7e5ba26...3f0240`. If different → fake.
2. **Email us**: opticastproject@gmail.com with URL, screenshot, store name
3. **We will DMCA**: Template:

> Subject: DMCA Takedown — Unauthorized distribution of OptiCast (GPL-3.0 + Trademark)
> I am copyright holder of OptiCast Video Player (com.opticast.player, GPL-3.0-only, source https://github.com/opticastplayer-dev/opticast). Your site [URL] distributes APK without GPL compliance (no source link) and/or uses trademark OptiCast to impersonate official. Official sources only: https://opticast.app/ and https://github.com/opticastplayer-dev/opticast/releases. Please remove or add GPL source link + rename to avoid trademark confusion. Signing key of official: f7e5ba2643181fbe2327f29ad814c0de8c937fab9ae276c2957f7ddc1d3f0240.

Most stores remove within 24-48h after DMCA.

## In-App Protection

From v2.6.120+, app detects installer:

- `org.fdroid.fdroid` → F-Droid (official when merged)
- `org.fdroid.fdroid.privileged` → F-Droid privileged
- `org.izzyondroid.izzyondroid` → IzzyOnDroid (official when approved)
- `com.android.packageinstaller` / `com.google.android.packageinstaller` → Sideload from GitHub/website (official)
- `com.apkpure.aegon`, `com.aptoide.aptoide`, `com.uptodown.android`, etc. → **Unofficial** → shows warning in Settings → About: "⚠️ Unofficial store detected — download only from opticast.app to avoid malware"

## Verification

Verify APK authenticity:

```bash
# Check SHA256 of APK file vs SHA256SUMS in release
sha256sum OptiCast-v2.6.119.apk
cat SHA256SUMS

# Check signing key via apksigner
apksigner verify --print-certs OptiCast-v2.6.119.apk
# Should show: SHA-256: f7e5ba2643181fbe2327f29ad814c0de8c937fab9ae276c2957f7ddc1d3f0240
# CN: Luma App Release
```

## Contact

- Email: opticastproject@gmail.com
- GitHub Issues: https://github.com/opticastplayer-dev/opticast/issues
- Website: https://opticast.app/

**Last updated:** 2026-10-02 — v2.6.119 (168)
