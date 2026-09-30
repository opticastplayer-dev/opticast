# TMDB Proxy Setup — Secure Networking for OptiCast

Your proxy: `https://tmdb-proxy-xstu.onrender.com/` — hides API key, handles TMDB requests server-side.

## 1. What you built (client-side)

File: `project/app/src/main/java/com/opticast/player/data/remote/TmdbProxyClient.kt`

### Base Client

```kotlin
object TmdbProxyConfig {
    const val BASE_URL = "https://tmdb-proxy-xstu.onrender.com/"
    const val CONNECT_TIMEOUT_SEC = 35L // Render cold start 30-50s
    const val READ_TIMEOUT_SEC = 60L
    const val MAX_RETRIES = 3
}
```

OkHttpClient tuned for Render free tier:
- connectTimeout 35s, readTimeout 60s, writeTimeout 30s
- retryOnConnectionFailure true
- No api_key param anywhere in APK

### Error Handling for Render Waking

Render free tier sleeps after 15 min idle. First request after sleep returns:
- `502 Bad Gateway` / `503` / `504` 
- Or `SocketTimeoutException`

Handled via:

```kotlin
sealed class ApiResult<T> {
  Success(data)
  Error(message, code, isNetworkError, isServerWaking)
}

- 502/503/504 => isServerWaking = true, retry with exponential backoff 2s, 4s, 8s
- SocketTimeoutException => isServerWaking = true
- IOException => isNetworkError = true
- 429 => rate limited, respect Retry-After
```

UI can show:
```kotlin
if (result.isServerWaking) {
  Text("Server waking up, please wait 30s — happens once after idle")
  CircularProgressIndicator()
}
```

### Data Models

Matches TMDB JSON exactly with `@Serializable` + `ignoreUnknownKeys = true`:

- `TmdbSearchResponse` + `TmdbSearchResultDto` (id, title/name, poster_path, vote_average, release_date etc)
- `TmdbMovieDetailsDto` (runtime, genres, imdb_id)
- `TmdbTvDetailsDto` (episode_run_time, number_of_seasons)
- `TmdbSeasonDto` + `TmdbEpisodeDto` (still_path)
- `TmdbCreditsResponse` + `TmdbCastMemberDto` (profile_path)
- `TmdbExternalIdsDto` (imdb_id for OMDb)

### Service

```kotlin
class TmdbProxyService {
  suspend fun searchMovies(query, year): ApiResult<List<Dto>>
  suspend fun searchTv(query, year): ApiResult<List<Dto>>
  suspend fun getMovieDetails(id): ApiResult<Details>
  suspend fun getTvDetails(id): ApiResult<TvDetails>
  suspend fun getSeasonDetails(id, season)
  suspend fun getMovieCredits(id)
  suspend fun getImdbId(id, isTv) // for OMDb
}
```

## 2. Express Proxy (server-side) — Reference

Your Render service should look like this:

```js
// server.js
import express from 'express';
import fetch from 'node-fetch';
import cors from 'cors';

const app = express();
const TMDB_KEY = process.env.TMDB_API_KEY; // set in Render dashboard, never in code
const TMDB_BASE = 'https://api.themoviedb.org';

app.use(cors());
app.use(express.json());

// Health check to keep alive (optional UptimeRobot ping /health)
app.get('/health', (req, res) => res.json({ status: 'ok' }));

// Proxy all /3/* to TMDB
app.get('/3/*', async (req, res) => {
  try {
    const tmdbPath = req.originalUrl; // /3/search/movie?query=...
    const url = `${TMDB_BASE}${tmdbPath}${tmdbPath.includes('?') ? '&' : '?'}api_key=${TMDB_KEY}`;
    
    const response = await fetch(url, {
      headers: { 'Accept': 'application/json' }
    });
    
    const data = await response.text();
    res.status(response.status).set('Content-Type', 'application/json').send(data);
  } catch (e) {
    console.error(e);
    res.status(502).json({ error: 'TMDB proxy failed', details: e.message });
  }
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => console.log(`Proxy running on ${PORT}`));
```

`.env` on Render:
```
TMDB_API_KEY=your_real_tmdb_key_here
```

`package.json`:
```json
{
  "type": "module",
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "node-fetch": "^3.3.2"
  }
}
```

Deploy to Render, set env var, get URL `https://tmdb-proxy-xstu.onrender.com/`.

**Security:**
- API key never in APK, only server env
- You can add simple rate limiting or API key header check from app if you want

## 3. Migration from old TmdbApi.kt

Old:
```kotlin
val key = apiKey()
val url = "https://api.themoviedb.org/3/search/movie?api_key=$key&query=..."
```

New:
```kotlin
// In AppContainer.kt
val tmdb: TmdbProxyService by lazy { TmdbProxyService() }

// In ViewModel
when (val result = tmdb.searchMovies("Inception")) {
  is ApiResult.Success -> // use result.data
  is ApiResult.Error -> if (result.isServerWaking) showWaking() else showError()
}
```

Or keep same interface: create wrapper `TmdbApi` that internally uses `TmdbProxyClient` but returns same `Metadata` types (see TmdbProxyClient.kt comments).

## 4. Testing cold start

1. Wait 15 min (Render sleeps)
2. Open app, search — should show "Server waking up..." then succeed after 30s
3. Check logs: retries 3 times with backoff

To avoid sleep: set UptimeRobot to ping `https://tmdb-proxy-xstu.onrender.com/health` every 5 min.

## 5. Next steps

- Replace `TmdbApi` usage with `TmdbProxyService` in `AppContainer`
- Remove `tmdbApiKey` from `SettingsRepository` (no longer needed)
- Keep image URLs direct `https://image.tmdb.org/t/p/...` (no need to proxy images)
- Add unit tests for timeout handling
