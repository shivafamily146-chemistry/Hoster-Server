# Hoster Server

A responsive Minecraft hosting dashboard starter that connects to a **Pterodactyl Panel** using its Client API.

## Features
- Password-protected private dashboard
- Lists servers available to the configured Pterodactyl account
- Reads live CPU, memory, disk and server state
- Sends start, stop and restart commands
- Keeps the panel API key on the server, never in browser JavaScript

## Requirements
- Node.js 20+
- A running Pterodactyl Panel and a Client API key with access to the servers
- A hosting service/VPS to run this web application

## Run locally
1. Copy `.env.example` to `.env`.
2. Set a strong `SESSION_SECRET` and `DASHBOARD_PASSWORD`.
3. Set `PANEL_URL` to your panel base URL (for example `https://panel.example.com`).
4. Set `PANEL_API_KEY` to a Pterodactyl **Client API** key (starts with `ptlc_`). Do not use an Application API key.
5. Run `npm install` and `npm start`.
6. Open `http://localhost:3000`.

## Deploy
Deploy this Node.js project to a VPS or Node.js host. Configure the same environment variables in the host's secret/environment settings. Use HTTPS in production.

## Important
GitHub stores the source code; it does not run Minecraft servers or host this Node.js app. This dashboard does not provision new game servers or create panel accounts. Server creation, billing, multi-user isolation, file management and console streaming require additional backend work and a real hosting infrastructure. Do not expose your panel API key or commit your `.env` file.
