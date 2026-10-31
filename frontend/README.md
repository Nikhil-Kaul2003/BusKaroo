# BusKaroo frontend

React and TypeScript single-page application built with Vite. It provides customer trip search and booking, reservations and profile pages, plus an admin interface for bus and category management.

For full-stack setup, database profiles, API details, and environment variables, see the [root README](../README.md).

## Run locally

```powershell
npm install
$env:VITE_API_PROXY_TARGET = "http://localhost:8081"
npm run dev
```

Use `http://localhost:8082` as the proxy target when the backend runs with its `existing-db` profile. The frontend dev server is available at `http://localhost:5173`.

## Build

```powershell
npm run build
```
