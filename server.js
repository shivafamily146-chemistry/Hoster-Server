require("dotenv").config();
const express = require("express");
const session = require("express-session");
const path = require("path");

const app = express();
const PORT = process.env.PORT || 3000;
const required = ["SESSION_SECRET", "DASHBOARD_PASSWORD", "PANEL_URL", "PANEL_API_KEY"];
const missing = required.filter((key) => !process.env[key]);
if (missing.length) console.warn("Missing environment variables:", missing.join(", "));

app.set("trust proxy", 1);
app.use(express.json());
app.use(session({
  name: "hoster.sid",
  secret: process.env.SESSION_SECRET || "development-only-change-me",
  resave: false,
  saveUninitialized: false,
  cookie: { httpOnly: true, sameSite: "lax", secure: process.env.NODE_ENV === "production", maxAge: 8 * 60 * 60 * 1000 }
}));

function requireLogin(req, res, next) {
  if (req.session && req.session.loggedIn) return next();
  return res.status(401).json({ error: "Please sign in first." });
}
function panelRequest(pathname, options = {}) {
  if (!process.env.PANEL_URL || !process.env.PANEL_API_KEY) throw new Error("Configure PANEL_URL and PANEL_API_KEY.");
  const base = process.env.PANEL_URL.replace(/\/+$/, "");
  return fetch(base + "/api/client" + pathname, {
    ...options,
    headers: {
      Authorization: "Bearer " + process.env.PANEL_API_KEY,
      Accept: "Application/vnd.pterodactyl.v1+json",
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  }).then(async (response) => {
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.errors?.[0]?.detail || body.message || "Panel API request failed (" + response.status + ").");
    return body;
  });
}

app.post("/api/login", (req, res) => {
  if (!process.env.DASHBOARD_PASSWORD) return res.status(503).json({ error: "Dashboard password is not configured." });
  if (!req.body || req.body.password !== process.env.DASHBOARD_PASSWORD) return res.status(401).json({ error: "Incorrect password." });
  req.session.regenerate((err) => {
    if (err) return res.status(500).json({ error: "Could not create session." });
    req.session.loggedIn = true;
    res.json({ ok: true });
  });
});
app.post("/api/logout", requireLogin, (req, res) => req.session.destroy(() => res.json({ ok: true })));
app.get("/api/me", (req, res) => res.json({ loggedIn: !!req.session?.loggedIn }));

app.get("/api/servers", requireLogin, async (req, res) => {
  try {
    const data = await panelRequest("/servers");
    res.json(data.data || []);
  } catch (error) { res.status(502).json({ error: error.message }); }
});
app.get("/api/servers/:id/resources", requireLogin, async (req, res) => {
  try { res.json(await panelRequest("/servers/" + encodeURIComponent(req.params.id) + "/resources")); }
  catch (error) { res.status(502).json({ error: error.message }); }
});
app.post("/api/servers/:id/power", requireLogin, async (req, res) => {
  const signal = req.body?.signal;
  if (!["start", "stop", "restart", "kill"].includes(signal)) return res.status(400).json({ error: "Invalid power signal." });
  try {
    await panelRequest("/servers/" + encodeURIComponent(req.params.id) + "/power", { method: "POST", body: JSON.stringify({ signal }) });
    res.json({ ok: true });
  } catch (error) { res.status(502).json({ error: error.message }); }
});
app.get("/api/servers/:id/websocket", requireLogin, async (req, res) => {
  try { res.json(await panelRequest("/servers/" + encodeURIComponent(req.params.id) + "/websocket")); }
  catch (error) { res.status(502).json({ error: error.message }); }
});

app.use(express.static(path.join(__dirname, "public")));
app.get("*", (req, res) => res.sendFile(path.join(__dirname, "public", "index.html")));
app.listen(PORT, () => console.log("Hoster Server dashboard listening on port " + PORT));
