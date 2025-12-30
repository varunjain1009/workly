# workly-Admin-Portal

The **Admin Portal** is a modern React application for workly administrators. It provides a UI to manage runtime configurations, view audit logs, and analytics.

## Features
*   **Config Management**: Create, Edit, and View configurations by Scope.
*   **Audit History**: Visual timeline of changes.
*   **Rollback UI**: One-click rollback for safety.
*   **Dashboard**: Overview of system status.

## Tech Stack
*   **React 19**, **Vite**
*   **TailwindCSS 3**: Styling.
*   **Lucide React**: Icons.

## Setup
```bash
npm install
npm run dev
```
Port: `5173`

## Config
`vite.config.ts` proxies `/api` calls to `http://localhost:8084` (Config Service).
