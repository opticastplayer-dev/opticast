# Official Distribution - OptiCast Video Player

**Package:** `com.opticast.player`  
**License:** GPL-3.0-only  
**Website:** https://opticastplayer-dev.github.io/opticast/

## Official Sources

OptiCast is officially distributed only from sources maintained by the OptiCast Project:

| Source | URL | Status |
|--------|-----|--------|
| **GitHub Releases** | https://github.com/opticastplayer-dev/opticast/releases | Official - direct APK and checksums |
| **Official Website** | https://opticastplayer-dev.github.io/opticast/ | Official - direct download and documentation |
| **F-Droid** | https://f-droid.org/packages/com.opticast.player | MR !50919 awaiting merge |

All official builds are signed with the same release key, allowing smooth updates while preserving user data. If Android reports a conflicting signature during installation, the APK originates from an unofficial source.

## Distribution Policy

OptiCast is open source under GPL-3.0-only. Redistribution is permitted under the terms of the license, which requires preservation of the license file, provision of source code, and clear attribution.

For user safety and to avoid confusion, we recommend obtaining OptiCast only from the official sources listed above. Builds obtained from other locations are not verified by the OptiCast Project and may be outdated or modified.

We kindly ask redistributors to:

- Clearly indicate that the build is not an official release if modified
- Preserve the GPL-3.0 license and provide a link to the source repository
- Avoid using the OptiCast name and branding in a way that suggests official status

## Trademark

OptiCast and associated branding are trademarks of the OptiCast Project. Please avoid using the name or logo in a manner that could imply official endorsement when distributing unofficial builds or forks. Forks are welcome but should use a distinct name.

## Verification

Official releases include SHA256 checksums and are signed with the same key for smooth updates:

```bash
# Verify SHA256 checksum
sha256sum OptiCast-v2.6.120.apk
# Compare with SHA256SUMS file in the GitHub release

# Verify signing certificate (same key allows install over old version, data preserved)
apksigner verify --print-certs OptiCast-v2.6.120.apk
# Expected: certificate SHA256 should match across all official releases
# If Android reports conflicting signature during install, APK is from unofficial source
```

The official website and GitHub Releases are the primary sources for verified builds. F-Droid builds will be signed with F-Droid's own key (different from GitHub releases, requires uninstall to switch).

## Reporting Issues

If you encounter OptiCast distributed outside the official channels in a way that violates the GPL-3.0 license or misrepresents itself as official, please contact us at opticastproject@gmail.com with details.

For takedown requests, please include the URL and a brief description of the concern. We review reports and take appropriate action under applicable license and trademark policies.

## In-App Protection

Starting from v2.6.120, the app displays installation source information in Settings → About, helping users confirm they are using an official build. If the app was installed from an unofficial source, a notice is shown recommending installation from official channels.

## Contact

- Email: opticastproject@gmail.com
- GitHub Issues: https://github.com/opticastplayer-dev/opticast/issues
- Website: https://opticastplayer-dev.github.io/opticast/

**Last updated:** 2026-10-09 - v2.6.120 (169)
