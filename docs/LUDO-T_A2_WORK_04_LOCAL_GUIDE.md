# LUDO-T Work 04 Local Validation Guide

## Prerequisites

- Windows PowerShell
- JDK 26 selected by `JAVA_HOME`
- The complete Work 04 project extracted to a normal writable folder

## 1. Format and run all automated tests

```powershell
java -version
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
```

Expected result: all modules succeed and all 130 tests pass.

## 2. Start the standalone server

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain"
```

Expected message:

```text
LUDO-T server listening on 127.0.0.1:5050
```

## 3. Start two Swing clients

Run this command in two separate PowerShell terminals:

```powershell
.\mvnw.cmd -pl client-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=client.ClientMain"
```

## 4. Same-session push demonstration

1. Connect both clients.
2. Client A creates `Game-1`.
3. Client B refreshes the lobby and joins `Game-1`.
4. Confirm both show the same session UUID and version `0`.
5. Client A starts and then pauses the game.
6. Without refreshing Client B, confirm both show `PAUSED` and the same version.
7. Client B resumes the game.
8. Confirm Client A immediately changes to `RUNNING` and both versions continue together.
9. Check that turns, pieces and event text continue updating without polling.

Capture a side-by-side screenshot showing the same session ID, status and version.

## 5. Late-join demonstration

1. Leave `Game-1` from Client B.
2. Let Client A run or step the game so its version increases.
3. Rejoin `Game-1` from Client B.
4. Confirm Client B immediately reconstructs the latest version, all 16 pieces and current
   session metadata.
5. Confirm no manual refresh is required after joining.

## 6. Independent-session demonstration

1. Client A leaves `Game-1` and creates `Game-2`.
2. Keep Client B joined to `Game-1`.
3. Start/resume both games.
4. Confirm each GUI shows only its joined session ID and version.
5. Pause or change speed in one session and confirm the other session is unaffected.

## 7. GUI checks

- Current player, status, round, turn and version are visible.
- Speed, latest dice, latest result and action are visible.
- Base, standard path, home straight and home pieces appear in the correct areas.
- State names/durations and block IDs appear in the player/piece panel.
- Blocked groups have visible square piece markers.
- Pushed events appear in the event log.
- Rapid automatic turns do not freeze the Swing EDT.

## 8. Evidence to retain

- full `clean test` output showing 130 successful tests;
- side-by-side same-session/version screenshot;
- pause-through-A and resume-through-B screenshot or short recording;
- late-join latest-version screenshot;
- Game-1/Game-2 screenshots showing independent IDs and versions;
- Git log for the Work 04 commits.

Do not add MySQL, rapid automatic clients, CSV logging or final packaging while validating this
checkpoint.
