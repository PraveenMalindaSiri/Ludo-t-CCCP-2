# LUDO-T Assignment 2 - Work 02 Local Validation Guide

This guide validates only the Swing thin-client shell and persistent TCP
connection-level operations. Run it on the `work-02-swing-tcp` branch with the
project's configured Java 26 toolchain.

## 1. Automated verification

From the repository root in PowerShell:

```powershell
git branch --show-current
java -version
javac -version
.\mvnw.cmd -version
.\mvnw.cmd clean test
```

Expected branch:

```text
work-02-swing-tcp
```

Expected tests:

| Module | Tests |
|---|---:|
| `game-core` | 79 |
| `protocol` | 14 |
| `server-app` | 9 |
| `client-app` | 7 |
| **Total** | **109** |

Expected final result:

```text
Failures: 0
Errors: 0
BUILD SUCCESS
```

The new tests cover bounded line reading, message-kind routing, server startup
on an ephemeral port, connection/ping/disconnect, two independent clients,
many requests on one socket, combined and partial TCP input, malformed input,
UUID correlation, non-interleaved concurrent output, virtual networking
threads, board geometry, all 16 DTO pieces, control enablement, bounded event
history and EDT-safe callbacks.

## 2. Dependency boundary

Run:

```powershell
.\mvnw.cmd -pl client-app dependency:tree
.\mvnw.cmd -pl server-app dependency:tree
```

Confirm:

- `client-app` depends on `protocol` and test-only JUnit.
- `client-app` does not depend on `game-core`, JDBC or MySQL.
- `server-app` depends on `protocol` and the already-approved `game-core`
  boundary, but Work 02 server code does not create or mutate a game.

Optional source check:

```powershell
Get-ChildItem .\client-app\src\main -Recurse -Filter *.java |
  Select-String -Pattern 'import (engine|board|piece|player|rules|factory)\.'
```

Expected: no matches.

## 3. Start the standalone applications

First compile and install the reactor modules once:

```powershell
.\mvnw.cmd clean install
```

Terminal 1 - server:

```powershell
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain"
```

Expected:

```text
LUDO-T server listening on 127.0.0.1:5050
```

Terminal 2 - Client A:

```powershell
.\mvnw.cmd -pl client-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=client.ClientMain"
```

Terminal 3 - Client B: run the same client command again.

IntelliJ alternative: run `server.ServerMain`, then run `client.ClientMain`
twice with parallel run enabled.

## 4. Manual two-client checklist

1. In Client A, keep `127.0.0.1` and `5050`, then select **Connect**.
2. Repeat in Client B.
3. Confirm both move to the lobby and display different client UUIDs.
4. Confirm the server logs two different connection UUIDs.
5. Select **Ping server** in both clients and confirm `PONG` appears.
6. Select **Refresh sessions** and confirm `No sessions available` appears.
7. Resize both windows. Confirm the connection and lobby layouts remain usable.
8. Disconnect Client A. Confirm Client B can still ping and the server remains
   running.
9. Reconnect Client A to prove a new persistent connection can be established.
10. Stop the server and confirm each client returns to the connection screen
    without freezing.

The create/join controls are intentionally disabled because sessions begin in
Work 03.

## 5. DTO-only board preview

The board preview is test-source-only and does not create a local game engine.

```powershell
.\mvnw.cmd -pl client-app -am test-compile
.\mvnw.cmd -pl client-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=client.ui.BoardPreviewMain" `
  "-Dexec.classpathScope=test"
```

Confirm the preview shows:

- all 52 standard path cells;
- four coloured bases;
- four home straights and homes;
- all 16 pieces;
- a mystery marker;
- current player, status, round, turn, version and last action;
- disabled/enabled controls based on the paused fixture;
- wrapped event history.

## 6. Configuration overrides

Defaults are stored in:

- `server-app/src/main/resources/server.properties`
- `client-app/src/main/resources/client.properties`

Each property can be overridden with a JVM system property. Example:

```powershell
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain" `
  "-Dserver.port=5051"
```

Use the same port in the client connection screen.

## 7. Evidence to capture for the checker

- Full `mvnw.cmd clean test` output showing 109 passing tests.
- `client-app` dependency tree showing no `game-core` dependency.
- Server terminal with two distinct connection UUIDs.
- Client A and Client B lobby screenshots.
- Board preview screenshot.
- A short note confirming Client B continued after Client A disconnected.

