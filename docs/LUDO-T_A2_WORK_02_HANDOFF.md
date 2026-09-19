# LUDO-T Assignment 2 - Work 02 Handoff

**Module:** COMP63038 Clean Coding and Concurrent Programming  
**Student:** CB008920  
**Work packet:** Work 02 - Phases 3-4  
**Status:** Implementation complete; requires final Java 26 local/checker execution  

## 1. Input checkpoint

- Input ZIP: `CB008920_CCCP2_WORK_01_APPROVED.zip`
- ZIP SHA-256:
  `a799e22e7e47c0511634a4c3d889bf0c36857014d811bc4225898a18f64de0bc`
- ZIP comment/source commit:
  `1f9156b15b0bf81ac04ca94cb40edeb10e1b617a`
- Intended working branch: `work-02-swing-tcp`
- Preserved short shared package: `protocol`

## 2. Scope completed

### Phase 3 - Swing thin client

- Added a standalone Swing entry point created on the EDT.
- Added connection, lobby and game screens using `CardLayout`.
- Added an immutable protocol-only client view state.
- Added an EDT-enforcing controller that never blocks on socket reads or
  futures.
- Added a complete top-down board renderer with 52 path cells, four bases,
  four home straights/homes, 16 piece support and mystery-cell rendering.
- Added session metadata for status, current player, round, turn, version and
  last action.
- Added status-based control enablement and bounded visible event history.
- Added a test-source-only DTO board preview. No production local-engine mode
  was added.

### Phase 4 - persistent TCP

- Added standalone server startup and configurable binding.
- Added persistent raw `ServerSocket`/`Socket` communication.
- Added one virtual reader task, one bounded outbound queue and one virtual
  writer task per connection on both client and server.
- Added a virtual server accept task and a virtual client connection task.
- Added UUID request correlation with
  `ConcurrentHashMap<UUID, CompletableFuture<ResponseMessage>>`.
- Added separate routing for correlated responses and pushed events.
- Added clean EOF, socket-reset, malformed-message and disconnect handling.
- Added `CONNECT`, `PING`, `LIST_SESSIONS` and `DISCONNECT` only.
- `LIST_SESSIONS` returns an immutable empty list until Work 03.
- Unsupported later operations return `INVALID_REQUEST` without implementing
  session behavior.

### Required bounded receive behavior

`protocol.BoundedLineReader` checks the accumulated character count while the
line is being received. It throws before a line can grow past the configured
limit, rather than calling an unbounded `BufferedReader.readLine()` and checking
only after the whole line has been allocated. Both client and server use this
reader directly on their socket input.

## 3. Files added

### Protocol

- `protocol/src/main/java/protocol/BoundedLineReader.java`
- `protocol/src/test/java/protocol/BoundedLineReaderTest.java`

### Client production

- `client-app/src/main/java/client/ClientMain.java`
- `client-app/src/main/java/client/ClientConfig.java`
- `client-app/src/main/java/client/ClientController.java`
- `client-app/src/main/java/client/network/ClientTransport.java`
- `client-app/src/main/java/client/network/ServerConnection.java`
- `client-app/src/main/java/client/model/ClientViewState.java`
- `client-app/src/main/java/client/ui/MainFrame.java`
- `client-app/src/main/java/client/ui/ConnectionPanel.java`
- `client-app/src/main/java/client/ui/LobbyPanel.java`
- `client-app/src/main/java/client/ui/GamePanel.java`
- `client-app/src/main/java/client/ui/BoardPanel.java`
- `client-app/src/main/java/client/ui/BoardGeometry.java`
- `client-app/src/main/java/client/ui/ControlPanel.java`
- `client-app/src/main/java/client/ui/EventLogPanel.java`
- `client-app/src/main/resources/client.properties`

### Client tests and preview

- `client-app/src/test/java/client/ClientControllerTest.java`
- `client-app/src/test/java/client/network/ServerConnectionTest.java`
- `client-app/src/test/java/client/model/ClientViewStateTest.java`
- `client-app/src/test/java/client/ui/BoardGeometryTest.java`
- `client-app/src/test/java/client/ui/ControlPanelTest.java`
- `client-app/src/test/java/client/ui/BoardPreviewMain.java`

### Server production

- `server-app/src/main/java/server/ServerMain.java`
- `server-app/src/main/java/server/ServerConfig.java`
- `server-app/src/main/java/server/network/GameServer.java`
- `server-app/src/main/java/server/network/ClientConnection.java`
- `server-app/src/main/java/server/network/RequestDispatcher.java`
- `server-app/src/main/resources/server.properties`

### Server tests

- `server-app/src/test/java/server/network/RequestDispatcherTest.java`
- `server-app/src/test/java/server/network/GameServerIntegrationTest.java`
- `server-app/src/test/java/server/network/ClientConnectionWriterTest.java`

### Documentation

- `docs/LUDO-T_A2_WORK_02_LOCAL_GUIDE.md`
- `docs/LUDO-T_A2_WORK_02_HANDOFF.md`
- `docs/evidence/work02-board-preview.png`

## 4. Files changed

- `client-app/pom.xml` - test dependency and standard build plugins.
- `server-app/pom.xml` - test dependency and standard build plugins.
- `protocol/src/main/java/protocol/JsonLineCodec.java` - safe top-level message
  kind inspection before typed decoding.
- `protocol/src/main/java/protocol/GameSnapshotDto.java` - current-player,
  turn and last-action presentation metadata, with a compatibility constructor.
- `protocol/src/test/java/protocol/JsonLineCodecTest.java` - message-kind tests.

No files were moved or deleted. The `game-core` source and its 79 tests were
not edited.

## 5. Architectural decisions implemented

- `client-app` imports only JDK/Swing and `protocol`; it has no `game-core`
  dependency or domain imports.
- Swing renders DTO values and sends transport requests. It contains no game
  rules, engine, pieces, players or board-domain objects.
- All Swing state mutations and rendering happen on the EDT.
- Network reader/writer/connect/accept work uses virtual threads. Swing's EDT
  is correctly not claimed as virtual.
- Socket output is never written by dispatchers, GUI callbacks or arbitrary
  producers. Every message enters the connection's bounded queue and the sole
  writer emits and flushes one complete NDJSON line.
- Client responses complete only the future with the matching request UUID.
  Unknown response UUIDs are logged and ignored.
- Pushed events use a separate callback and never masquerade as responses.
- The last valid DTO snapshot is retained if a connection is lost.
- Malformed or oversized incoming data safely closes only the affected
  connection.

## 6. Dependencies

No new production library was added. Work 02 reuses:

- JDK Swing/AWT;
- JDK socket and concurrency APIs;
- the Work 01 `protocol` module and its Jackson dependencies.

JUnit Jupiter was added as a test-scoped dependency to `client-app` and
`server-app`, using the version already managed by the parent POM.

## 7. Tests added and expected totals

| Module | Existing | Added | Expected total |
|---|---:|---:|---:|
| `game-core` | 79 | 0 | 79 |
| `protocol` | 10 | 4 | 14 |
| `server-app` | 0 | 9 | 9 |
| `client-app` | 0 | 7 | 7 |
| **Reactor** | **89** | **20** | **109** |

Coverage includes:

- LF/CRLF, partial input and early oversized-line rejection;
- typed routing by message kind;
- one client connect/ping/list/disconnect;
- two independent clients;
- 40 sequential pings through one socket;
- two messages delivered in one write;
- one message split across writes;
- malformed JSON closing only the bad connection;
- request UUID correlation with out-of-order responses;
- unknown response UUID handling;
- separate pushed-event delivery;
- concurrent output producers with no line interleaving;
- virtual-thread evidence;
- 52-cell geometry and all 16 DTO pieces;
- status-based controls;
- bounded model history with long text;
- EDT callback/render enforcement.

## 8. Verification performed in the implementation environment

Completed source-level checks:

- all Work 02 production sources except the unchanged Jackson implementation
  were type-checked with the available JDK compiler using compatibility stubs;
- all Work 02 tests were also type-checked with temporary JUnit/virtual-thread
  compatibility stubs;
- the only direct compile diagnostics against the unmodified Work 02 sources
  under the available Java 17 runtime were the expected missing Java 21+
  virtual-thread methods;
- all POM files remain well-formed;
- source scanning confirmed no game-domain imports in `client-app`;
- source scanning confirmed no `GameSession`, scheduler, JDBC/MySQL, CSV or
  rapid-client implementation was added;
- source test inventory is exactly 109 `@Test` methods: 79 core, 14 protocol,
  9 server and 7 client.
- the DTO-only `GamePanel` was rendered headlessly and visually inspected; the
  resulting board/metadata/control preview is stored in
  `docs/evidence/work02-board-preview.png`.

The container provides only a Java 17 runtime without `javac`, while the project
is configured for Java 26. Its restricted network also prevented Maven from
retrieving missing build plugins. Therefore a genuine `mvn clean test` and live
Swing/socket run could not be executed here. This is an environment limitation,
not a claimed passing result. Run the Java 26 procedure in
`LUDO-T_A2_WORK_02_LOCAL_GUIDE.md` and preserve the resulting output for the
checker.

## 9. Exact local commands

Automated suite:

```powershell
.\mvnw.cmd clean test
```

Server:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain"
```

Client A and Client B, in separate terminals:

```powershell
.\mvnw.cmd -pl client-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=client.ClientMain"
```

See `LUDO-T_A2_WORK_02_LOCAL_GUIDE.md` for the complete manual checklist and
DTO-only board preview command.

## 10. Suggested Git commits

Use these two commits on `work-02-swing-tcp` after the Java 26 checks pass:

```text
feat: add Swing thin-client interface
feat: add persistent NDJSON TCP communication
```

Suggested split:

1. First commit: Swing configuration, controller, model, UI, DTO metadata and
   Swing/model tests.
2. Second commit: bounded reader, codec routing, client transport, server,
   network tests and Work 02 documentation.

Then push the branch and return the complete ZIP, this handoff and the local
test output to the checker chat.

## 11. Known limitations by design

- Lobby session creation/joining is visible but disabled.
- The server does not create `GameSession` objects or touch `GameEngine`.
- No automatic simulation scheduling or per-session queue exists.
- The server accepts the event-capable protocol, but no game events are emitted
  until later session/push work.
- No JDBC/MySQL, CSV logging, rapid test client or deployment packaging exists.
- Runnable shaded JARs and start scripts remain Work 07 scope.

## 12. Next packet readiness

The source stops at the Work 02 boundary. Begin Work 03 only after the checker
confirms the Java 26 reactor result, dependency direction, two-client manual
behavior and DTO board preview.
