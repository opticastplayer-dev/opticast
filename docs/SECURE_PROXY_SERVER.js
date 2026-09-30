// SECURE TMDB Proxy — Render Express — https://tmdb-proxy-xstu.onrender.com/
// Security: API key hidden server-side, secret header, rate limit, CORS blocked
// Env vars on Render: TMDB_API_KEY, APP_SECRET

import express from 'express';
import cors from 'cors';
import rateLimit from 'express-rate-limit';

const app = express();
const TMDB_KEY = process.env.TMDB_API_KEY;
const APP_SECRET = process.env.APP_SECRET;
const TMDB_BASE = 'https://api.themoviedb.org';

if (!TMDB_KEY) {
  console.error('FATAL: TMDB_API_KEY not set in env');
  process.exit(1);
}
if (!APP_SECRET) {
  console.error('FATAL: APP_SECRET not set in env');
  process.exit(1);
}

// --- Middleware ---
app.use(express.json());

// CORS: block browser usage, allow only non-browser (Android OkHttp has no Origin header)
// If Origin present (browser), reject. Android requests have no Origin.
app.use(cors({
  origin: (origin, callback) => {
    // No origin = Android app, curl, mobile — allow
    if (!origin) return callback(null, true);
    // Allow your GitHub Pages for testing if needed
    const allowed = ['https://opticastplayer-dev.github.io', 'https://opticast-project.github.io'];
    if (allowed.some(a => origin.startsWith(a))) return callback(null, true);
    console.warn(`Blocked CORS origin: ${origin}`);
    return callback(new Error('CORS blocked'), false);
  }
}));

// Rate limit: 30 req per IP per 10s — matches TMDB 40/10s, leaves buffer, prevents abuse
const limiter = rateLimit({
  windowMs: 10 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'rate limited, try later' },
  keyGenerator: (req) => req.ip || req.headers['x-forwarded-for'] || 'unknown'
});
app.use('/api/', limiter);

// Secret check: require X-App-Secret header for all /api/* except /health
app.use((req, res, next) => {
  if (req.path === '/health' || req.path === '/') return next();
  const secret = req.header('X-App-Secret') || req.header('x-app-secret');
  if (!secret || secret !== APP_SECRET) {
    console.warn(`Forbidden: IP ${req.ip} missing/invalid secret for ${req.originalUrl}`);
    return res.status(403).json({ error: 'forbidden — invalid app secret' });
  }
  next();
});

// Health check for UptimeRobot (no secret needed, keeps Render awake)
app.get('/health', (req, res) => {
  res.json({ status: 'ok', time: new Date().toISOString(), version: 'secure-v1' });
});

app.get('/', (req, res) => {
  res.json({ status: 'ok', message: 'OptiCast TMDB Proxy — use /api/* with X-App-Secret', health: '/health' });
});

// Proxy all /api/* to TMDB — inject api_key server-side
app.get('/api/*', async (req, res) => {
  try {
    // req.originalUrl = /api/search/movie?query=Inception
    // Convert to TMDB path: /3/search/movie?query=Inception
    // Our client uses /api/search/movie, TMDB uses /3/search/movie — map it
    let tmdbPath = req.originalUrl.replace(/^\/api\//, '/3/');
    // Ensure api_key param added server-side, never from client
    const separator = tmdbPath.includes('?') ? '&' : '?';
    const url = `${TMDB_BASE}${tmdbPath}${separator}api_key=${TMDB_KEY}`;

    // Optional: log without key for debugging (remove key from log)
    console.log(`Proxy ${req.ip} -> ${tmdbPath.split('?')[0]}`);

    const response = await fetch(url, {
      headers: { 'Accept': 'application/json' }
    });

    const data = await response.text();
    // Forward status and content-type
    res.status(response.status).set('Content-Type', 'application/json').send(data);
  } catch (e) {
    console.error('Proxy error:', e.message);
    res.status(502).json({ error: 'TMDB proxy failed', details: e.message });
  }
});

// Handle all methods (in case TMDB needs POST later, but we only use GET)
app.all('/api/*', (req, res) => {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'method not allowed, use GET' });
  }
  // GET already handled above, this catches if somehow missed
  res.status(404).json({ error: 'not found' });
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => {
  console.log(`Secure proxy running on ${PORT}`);
  console.log(`TMDB key set: ${!!TMDB_KEY}, APP_SECRET set: ${!!APP_SECRET}`);
});
