# LUDO-T Work 03 Local Validation Guide

## Prerequisites

- Windows PowerShell
- JDK 26 selected by `JAVA_HOME`
- The complete Work 03 project extracted to a normal writable folder

## 1. Format and run all automated tests

```powershell
java -version
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
```

Confirm that all modules succeed and the reactor reports no failures or errors. The source tree
contains 117 `@Test` methods.

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

## 4. Manual Work 03 checks

1. Connect both clients.
2. In Client A, create `Game-1`.
3. In Client B, refresh the lobby, select `Game-1` and join it.
4. Start the game from A.
5. Pause it, step once, change speed, then resume.
6. Leave from A. Verify B still has the game and can pause/get the latest control response.
7. From A, create `Game-2`.
8. Verify the lobby shows two different session UUIDs and independent state.
9. Stop the sessions before closing the applications.

## 5. Evidence to retain

- complete `clean test` output;
- lobby screenshot showing two session UUIDs;
- game screen screenshots for running, paused and stepped states;
- console lines showing different `session-<UUID>` workers if printed during debugging;
- Git log containing the two Work 03 commits.

Do not add pushed snapshot synchronization, MySQL, rapid clients or CSV logging while validating
this checkpoint.

