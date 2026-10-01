# Secure TMDB Proxy — Setup Guide

Your proxy: https://tmdb-proxy-xstu.onrender.com/

## Current Security Status

Before: Open proxy, anyone with URL could use your TMDB key → 6/10
After: Secret header + rate limit + CORS → 9/10

## 1. Generate Secret

```bash
openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c32
# Example: YOUR_SECRET_HERE
```

## 2. Render Env Vars

Go to https://dashboard.render.com/ → your service tmdb-proxy-xstu → Environment

Add:
```
TMDB_API_KEY=your_real_tmdb_api_key_here
APP_SECRET=YOUR_SECRET_HERE
```

Save → Render auto redeploys.

## 3. Server Code

File: `SECURE_PROXY_SERVER.js` (provided)

Features:
- Requires `X-App-Secret` header for all /api/* (403 if missing)
- Rate limit: 30 req / 10s per IP (prevents abuse, matches TMDB 40/10s)
- CORS: blocks browser Origin, allows Android (no Origin)
- Health: /health no secret needed (for UptimeRobot)
- Maps /api/* → /3/* + injects api_key server-side

package.json needs:
```
express, cors, express-rate-limit
```

## 4. Android Client

`project/app/build.gradle.kts`:
```kotlin
val proxySecret = localProps.getProperty("tmdb.proxy.secret") ?: System.getenv("TMDB_PROXY_SECRET") ?: ""
buildConfigField("String", "TMDB_PROXY_SECRET", "\"$proxySecret\"")
buildFeatures { buildConfig = true }
```

`project/local.properties` (NOT in GitHub, local only):
```
tmdb.proxy.secret=YOUR_SECRET_HERE
```

`TmdbProxyClient.kt`:
```kotlin
val appSecret = BuildConfig.TMDB_PROXY_SECRET
.header("X-App-Secret", appSecret)
```

Secret is in BuildConfig, not plain string in code, harder to extract than hardcoded URL. Still in APK but obfuscated via R8.

## 5. Test

Without secret (should 403):
```bash
curl https://tmdb-proxy-xstu.onrender.com/api/search/movie?query=Inception
# → {"error":"forbidden — invalid app secret"}
```

With secret (should 200):
```bash
curl -H "X-App-Secret: YOUR_SECRET_HERE" https://tmdb-proxy-xstu.onrender.com/api/search/movie?query=Inception
# → {"page":1,"results":[...]}
```

## 6. Keep Alive (prevent cold start)

Render free tier sleeps after 15m idle → 30-50s wake.

Set UptimeRobot: https://uptimerobot.com/ → Add monitor → HTTP(s) → URL https://tmdb-proxy-xstu.onrender.com/health → every 5 min.

Now always warm, no waking delay.

## 7. Rotate Secret if Leaked

If secret leaks:
1. Generate new secret
2. Update Render env APP_SECRET
3. Update local.properties tmdb.proxy.secret
4. Build new APK v2.6.106 with new secret, release
5. Old APKs with old secret will get 403 → force update via in-app updater

## Safety Score

- Key hidden server-side: ✅
- Proxy requires secret: ✅ (was open)
- Rate limited: ✅ (30/10s per IP)
- CORS blocked: ✅ (no browser abuse)
- HTTPS: ✅
- Keep alive: ✅ (via UptimeRobot)

→ 9/10 secure. For 10/10, add IP allowlist or Firebase App Check, but secret + rate limit is enough for TMDB proxy.

## Files Provided

- SECURE_PROXY_SERVER.js — secured server
- SECURE_PROXY_PACKAGE.json — package.json
- This README
