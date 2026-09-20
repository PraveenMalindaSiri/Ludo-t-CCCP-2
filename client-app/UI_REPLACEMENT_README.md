# LUDO-T Modern Swing Client Replacement

This folder is a drop-in replacement for the existing `client-app` module.

## What changed

- Modern dark Swing theme implemented with standard JDK/Swing APIs only.
- Simplified LUDO-T connection screen with clear server and client status.
- Detailed session lobby with status, client count, version and creation time.
- Live-game dashboard with status pills, metrics, player cards and tabs.
- One-click copying of the complete session UUID for rapid-client commands.
- Assignment 1-aligned board painting: Green/Yellow/Red/Blue bases, clockwise numbering from Yellow X, coloured approach circles, Alpha/Beta/Gamma teleport destinations, active-player outlines and temporary mystery-cell styling.
- Restyled simulation controls and live activity view.

## What did not change

- The application is still entirely Java Swing.
- No JavaFX or browser UI was introduced.
- No game rules were moved into the client.
- No database access was added to the client.
- The TCP/NDJSON transport, protocol DTOs and EDT-safe controller remain unchanged.
- No external UI dependency is required.

## Install

1. Keep a backup or make sure the current work is committed.
2. Replace the project-root `client-app` folder with this folder.
3. Reload the Maven project in IntelliJ.
4. From the project root run:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
```

5. Start `ServerMain`, then run `ClientMain`.

The server and database setup are unchanged.
