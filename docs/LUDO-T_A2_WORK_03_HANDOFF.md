# LUDO-T Assignment 2 - Work 03 Handoff

**Module:** COMP63038 Clean Coding and Concurrent Programming  
**Student:** CB008920  
**Work packet:** Work 03 - Phases 5-6  
**Status:** Implementation complete; final Java 26 Maven run required locally

## 1. Input checkpoint

- Input ZIP: `CB008920_CCCP2_WORK_02_FINAL(1).zip`
- Input commit recorded in the ZIP comment:
  `f36f8f7ecef02d253f3a0e745d67cbe9be61e085`
- Expected branch: `work-03-sessions-automation`
- Work 02 game core, shared protocol contracts and thin-client boundaries were preserved.

## 2. Scope completed

### Phase 5 - isolated sessions and queues

- Added a thread-safe `SessionRegistry` backed by `ConcurrentHashMap<UUID, GameSession>`.
- Every session receives a unique server-generated UUID and a new isolated engine from
  `GameFactory.createGame(seed)`.
- Every session owns one bounded `ArrayBlockingQueue<RequestEnvelope>` and exactly one virtual
  consumer thread.
- Only the session worker initializes or mutates `GameEngine`.
- Implemented create, list, join, leave and snapshot requests.
- A client must join a session before using its game operations.
- Leaving or disconnecting removes only that connection's subscription; it does not stop the
  game.
- Accepted envelopes have an atomic exactly-once completion path. A full queue returns one
  structured `QUEUE_FULL` response.

### Phase 6 - automatic lifecycle controls

- Implemented `START_GAME`, `PAUSE_GAME`, `RESUME_GAME`, `STEP_GAME`, `STOP_GAME` and
  `SET_SPEED`.
- The same session worker performs timed queue polling and automatic turns. No scheduler or
  secondary simulation loop was added.
- Unrelated queued requests do not reset the automatic deadline.
- A due turn is performed after the current envelope, preventing continuous requests from
  starving simulation progress.
- `STEP_GAME` advances exactly one complete simulated player turn while leaving the session
  paused unless the game completes.
- Duplicate start and all other illegal transitions return `INVALID_STATE`.
- Game completion automatically changes the status to `COMPLETED`.

### Swing wiring

- Lobby refresh, create and join now call real server operations.
- The game screen now sends start, pause, resume, step, stop, speed and leave requests.
- Controls are enabled from server-confirmed session status.
- Control responses carry the latest snapshot, so Work 03 does not add regular polling.
- Full pushed snapshot synchronization remains deliberately deferred to Work 04.

## 3. Session model and ownership

Each `GameSession` owns:

- UUID and display name;
- isolated `GameEngine` object graph;
- bounded request queue;
- one named virtual worker;
- lifecycle status and turn delay;
- immutable latest protocol snapshot;
- version and completed-turn counter;
- subscriber set;
- created, started and completed timestamps.

Mutable domain objects are not shared between sessions. The client still depends only on the
protocol module and contains no game-core imports.

## 4. Queue implementation

- Type: `ArrayBlockingQueue<RequestEnvelope>`
- Default capacity: `64`
- Configuration key: `server.sessionQueueCapacity`
- Submission: non-blocking `offer`
- Full queue result: `ErrorCode.QUEUE_FULL`
- Consumer count: exactly one per session
- Worker name: `session-<UUID>`
- Worker type: virtual thread

`RequestEnvelope` records received/enqueued timing, the requesting subscriber and an atomic
completion guard. Session exceptions are converted to a response for the affected envelope so
the worker can continue where safe.

## 5. Worker timing design

When a session is not running, the worker blocks on `take()`. When it is running, the worker uses
`poll(remainingDelay, NANOSECONDS)` so either a client request or the next turn deadline wakes it.
After processing at most one available envelope, it checks the preserved deadline and advances a
turn if due. Only one future deadline exists; automatic ticks are never queued in advance.

The default delay is configured by `server.defaultTurnDelayMillis` and is `500 ms`. Accepted speed
values range from `10 ms` to `60,000 ms`.

## 6. Lifecycle implemented

| Request | Required state | Resulting state |
| --- | --- | --- |
| Start | `CREATED` | `RUNNING` |
| Pause | `RUNNING` | `PAUSED` |
| Resume | `PAUSED` | `RUNNING` |
| Step | `PAUSED` | `PAUSED`, or `COMPLETED` |
| Stop | `CREATED`, `RUNNING`, or `PAUSED` | `STOPPED` |
| Automatic turn | `RUNNING` | `RUNNING`, or `COMPLETED` |
| Server shutdown | Active nonterminal state | `INTERRUPTED` |

## 7. Request types completed

- `CREATE_SESSION`
- `LIST_SESSIONS`
- `JOIN_SESSION`
- `LEAVE_SESSION`
- `GET_SNAPSHOT`
- `START_GAME`
- `PAUSE_GAME`
- `RESUME_GAME`
- `STEP_GAME`
- `STOP_GAME`
- `SET_SPEED`

Existing `CONNECT`, `PING` and `DISCONNECT` behavior remains.

## 8. Main files added

- `server/application/GameService.java`
- `server/application/port/SessionSubscriber.java`
- `server/session/RequestEnvelope.java`
- `server/session/GameSession.java`
- `server/session/SessionRegistry.java`
- `server/session/GameSessionTest.java`

## 9. Main files updated

- `server/ServerConfig.java`
- `server/network/RequestDispatcher.java`
- `server/network/GameServer.java`
- `server/network/ClientConnection.java`
- `server.properties`
- `client/network/ClientTransport.java`
- `client/ClientController.java`
- `client/model/ClientViewState.java`
- `client/ui/LobbyPanel.java`
- `client/ui/ControlPanel.java`
- `client/ui/GamePanel.java`
- `client/ui/MainFrame.java`
- related server integration, dispatcher and client-controller tests

No game-core or protocol source file was changed.

## 10. Tests and verification

The source tree now contains `117` JUnit `@Test` methods: the earlier `109` plus eight Work 03
tests.

Work 03 test coverage includes:

- unique UUIDs and isolated snapshots;
- registry list, lookup and removal;
- deterministic bounded-queue rejection;
- lifecycle transitions and duplicate-start rejection;
- paused single-step behavior;
- concurrent producers and one response per accepted request;
- separate-session progress on different workers;
- multiple subscribers and creator disconnect behavior;
- two-client TCP create/list/join/control/leave flow;
- Swing controller routing for session/control operations.

Verification completed in the implementation environment:

- Java compatibility compilation passed for every production source after substituting Java 26
  virtual-thread/List convenience calls only in a temporary verification copy.
- Six `GameSessionTest` tests passed.
- Eleven dispatcher, TCP integration and client-controller tests passed.
- A real Jackson NDJSON two-client network harness passed the complete create/list/join,
  start/pause/step, leave and snapshot flow.
- A session harness passed lifecycle, automatic timing, isolation and creator-leave checks.
- Static checks confirmed no client game-domain imports, no scheduler/executor pool, no database,
  no CSV implementation and no changes to game-core/protocol.

The container runtime is Java 17 and does not contain the Java 26 Maven toolchain. Therefore a
genuine `mvn clean test` against Java 26 virtual threads must be run locally before checkpoint
approval. The implementation itself retains the real Java 26 virtual-thread calls.

## 11. Exact local commands

Run from the project root in PowerShell with JDK 26 active:

```powershell
java -version
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
```

Expected source test count: `117` tests with no failures or errors.

Start the server:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd -pl server-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=server.ServerMain"
```

Start Client A and Client B in separate terminals:

```powershell
.\mvnw.cmd -pl client-app org.codehaus.mojo:exec-maven-plugin:3.5.0:java `
  "-Dexec.mainClass=client.ClientMain"
```

## 12. Manual demonstration

1. Connect Client A and Client B.
2. Client A creates `Game-1`; Client B refreshes and joins it.
3. Start, pause, step once and resume.
4. Leave from Client A and confirm Client B can still retrieve/control the session.
5. Create `Game-2` and show different UUIDs and independent turns.
6. Stop both sessions before closing the applications.

## 13. Deliberately deferred

- pushed snapshot/event synchronization and immediate two-GUI equality;
- MySQL/database tier;
- rapid automatic test-client module;
- CSV timing evidence;
- final resilience, packaging and demonstration scripts.

These remain Work 04 and later scope.

## 14. Suggested commits

```text
feat: add isolated queued game sessions
feat: control automatic simulations through session workers
```

