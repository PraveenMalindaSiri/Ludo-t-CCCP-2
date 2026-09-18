# LUDO-T Assignment 2 - Work 01 Handoff

**Module:** COMP63038 Clean Coding and Concurrent Programming  
**Student:** CB008920  
**Work packet:** Work 01 - Phases 0-2  
**Status:** Implemented, locally verified and committed on `foundation`  

## 1. Input

- Input project: `CCCP2(1).zip`
- Baseline branch used by the student: `foundation`
- Baseline local environment reported by the student:
  - Oracle Java/Javac 26.0.1
  - Apache Maven 3.9.16
  - Windows 11
- Baseline Maven result reported by the student:

```text
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 2. Completed scope

### Phase 0 - Baseline

- Confirmed the original project contained 63 production Java files.
- Confirmed 16 test classes plus `support/TestSupport.java`.
- Confirmed exactly 79 existing `@Test` methods.
- Preserved the original automatic console entry point.
- Preserved the original package names and GoF participants.

### Phase 1 - Maven multi-module preparation

- Replaced the single-module root POM with a Maven reactor POM.
- Moved the original `src` tree byte-for-byte into `game-core/src`.
- Added `game-core/pom.xml` with the original test dependencies and test setup.
- Added valid future-module skeletons:
  - `protocol`
  - `server-app`
  - `client-app`
  - `test-client`
- Added Apache Maven Wrapper 3.3.4 using the `only-script` distribution.
- Configured the wrapper to use Apache Maven 3.9.16.
- Kept Java source and target version 26 in the parent POM.

### Phase 2 - Shared protocol

- Added immutable request, response, event, session and game snapshot DTOs.
- Added explicit request, event, error, status and message-kind enums.
- Added UUID request/client/session identifiers and monotonic version fields.
- Added compact Jackson JSON serialization with Java Time support.
- Added NDJSON framing through `encodeLine`, which appends exactly one LF.
- Added one-megabyte maximum line-length protection.
- Added controlled `ProtocolException` failures for malformed/invalid input.
- Added 10 protocol tests covering the required round trips and edge cases.

No Swing, socket, session worker, queue, MySQL, JDBC or automatic test-client
implementation was started.

## 3. Final module tree

```text
CB008920_CCCP2/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── .gitignore
├── CB008920_CCCP2.iml
├── pom.xml
├── mvnw
├── mvnw.cmd
├── docs/
│   ├── LUDO-T_A2_WORK_01_HANDOFF.md
│   └── LUDO-T_A2_WORK_01_LOCAL_GUIDE.md
├── game-core/
│   ├── pom.xml
│   └── src/
│       ├── main/java/        63 unchanged production files
│       ├── main/resources/
│       └── test/java/        16 test classes + TestSupport; 79 tests
├── protocol/
│   ├── pom.xml
│   └── src/
│       ├── main/java/protocol/
│       └── test/java/protocol/
├── server-app/
│   └── pom.xml
├── client-app/
│   └── pom.xml
└── test-client/
    └── pom.xml
```

Empty application modules intentionally contain no fake application classes.

## 4. Dependency direction

```text
client-app  -> protocol
test-client -> protocol
server-app  -> protocol
server-app  -> game-core
game-core   -> JDK only in production
protocol    -> Jackson only in production
```

Verified absent:

- `client-app -> game-core`
- `test-client -> game-core`
- `protocol -> game-core`
- `game-core -> Jackson`
- `game-core -> Swing`
- `game-core -> socket APIs`
- `game-core -> JDBC`

## 5. Exact versions

| Component | Version | Scope |
|---|---:|---|
| Java source/target | 26 | All modules |
| Apache Maven Wrapper | 3.3.4 | Build bootstrap |
| Wrapper Maven distribution | 3.9.16 | Reactor build |
| Jackson Databind | 2.18.3 | Protocol runtime |
| Jackson Java Time | 2.18.3 | Protocol runtime |
| JUnit 4 | 4.13.1 | Preserved game-core test dependency |
| JUnit Jupiter | 5.8.1 | Tests |
| Mockito Core | 5.23.0 | Preserved game-core test dependency |
| Mockito JUnit Jupiter | 5.23.0 | Preserved game-core test dependency |
| Maven Compiler Plugin | 3.14.1 | Managed build plugin |
| Maven Dependency Plugin | 3.8.1 | Existing Mockito-agent property setup |
| Maven Surefire Plugin | 3.5.2 | Tests |
| Spotless Maven Plugin | 3.10.2 | Java formatting |

Only `jackson-databind` and `jackson-datatype-jsr310` were intentionally added
as direct production dependencies. Jackson's normal transitive core components
are resolved by Maven.

## 6. Protocol classes and responsibilities

| Class | Responsibility |
|---|---|
| `MessageKind` | Distinguishes request, response and pushed event messages. |
| `RequestType` | Lists all supported client operations. |
| `EventType` | Lists all server-pushed event categories. |
| `ErrorCode` | Provides stable machine-readable failure categories. |
| `SessionStatus` | Provides the planned server session lifecycle states. |
| `RequestMessage` | Correlated client operation with UUIDs, optional session and immutable string parameters. |
| `ResponseMessage` | Exactly one correlated result, error information and optional session/snapshot data. |
| `EventMessage` | Unsolicited server event with its own event UUID and no reused request UUID. |
| `SessionSummaryDto` | Lightweight lobby/session state. |
| `GameSnapshotDto` | Versioned full game view with nested immutable player, piece and mystery records. |
| `JsonLineCodec` | Compact JSON encoding/decoding and one-line NDJSON framing. |
| `ProtocolException` | Controlled protocol-boundary failure. |

## 7. Required operations included

Requests:

```text
CONNECT, PING, LIST_SESSIONS, CREATE_SESSION, JOIN_SESSION,
LEAVE_SESSION, START_GAME, PAUSE_GAME, RESUME_GAME, STEP_GAME,
STOP_GAME, SET_SPEED, GET_SNAPSHOT, DISCONNECT
```

Events:

```text
SESSION_UPDATED, GAME_EVENT_BATCH, SESSION_COMPLETED,
SESSION_STOPPED, SERVER_SHUTTING_DOWN
```

Errors:

```text
MALFORMED_MESSAGE, INVALID_REQUEST, INVALID_STATE,
UNKNOWN_SESSION, QUEUE_FULL, UNSUPPORTED_PROTOCOL, SERVER_ERROR
```

## 8. Verification performed

The implementation was compiled with Temurin OpenJDK/Javac 26.0.2.1.

### Game core

```text
79 tests found
79 tests started
79 tests successful
0 tests failed
```

### Protocol

```text
10 tests found
10 tests started
10 tests successful
0 tests failed
```

### Other verification

- All 63 original production files compiled successfully under Java 26.
- All 12 protocol production files compiled successfully under Java 26.
- The original and moved `src` directories were compared recursively and were
  byte-for-byte identical.
- The automatic `Main` simulation completed normally with exit code 0 and
  printed final placements.
- All six POM files were parsed successfully as XML.
- The Maven wrapper launched Maven 3.9.16 successfully.
- Root-only Maven validation succeeded.
- Maven discovered the correct six-project reactor order.
- All selected dependency and plugin coordinates were confirmed to exist.
- No forbidden imports or dependencies were found in `game-core` or `protocol`.

Combined verified test count: **89 passing tests**.

## 9. Local verification result

The complete Maven reactor was verified locally on Windows 11 with Oracle JDK
26.0.1 and Apache Maven 3.9.16. All six reactor projects completed successfully:

```text
game-core: 79 tests passed
protocol: 10 tests passed
total: 89 tests passed
failures: 0
errors: 0
BUILD SUCCESS
```

## 10. Exact local verification

From the repository root on Windows PowerShell:

```powershell
java -version
javac -version
.\mvnw.cmd -version
.\mvnw.cmd clean test
```

Expected total after the reactor migration:

```text
game-core: 79 tests
protocol: 10 tests
total: 89 tests
failures: 0
errors: 0
```

Run the console regression simulation either from IntelliJ or with:

```powershell
java -cp .\game-core\target\classes Main
```

Dependency evidence:

```powershell
.\mvnw.cmd -pl game-core dependency:tree
.\mvnw.cmd -pl protocol dependency:tree
```

## 11. Sample protocol lines

Request:

```json
{"protocolVersion":1,"kind":"REQUEST","requestId":"11111111-1111-1111-1111-111111111111","clientId":"22222222-2222-2222-2222-222222222222","requestType":"SET_SPEED","sessionId":"33333333-3333-3333-3333-333333333333","parameters":{"turnDelayMillis":"500"},"sentAt":"2026-09-18T12:30:45Z"}
```

Response and event messages use the same compact single-line rule. Their tests
include nested session snapshots, null/optional fields, UUIDs, enums and ISO
timestamps. `encodeLine` appends one LF after the JSON object.

## 12. Deviations

- The protocol package was simplified from
  `com.cb008920.ludot.protocol` to `protocol`. This is consistent with the
  existing short package names such as `engine`, `event`, `factory` and
  `rules`. Module boundaries and dependency direction remain unchanged.
- `ProtocolException` was added as one small supporting class so malformed
  input is reported through a controlled protocol-specific exception.
- The Maven wrapper uses the official `only-script` form, so no wrapper JAR is
  committed. This is an official Maven Wrapper distribution type.
- The complete reactor was successfully confirmed in the local environment.

## 13. Git checkpoint

The completed Work 01 state is available on the pushed `foundation` branch.

```text
e0a631e refactor: move existing LUDO-T implementation into game-core
e9f0968 add shared Jackson NDJSON protocol contracts
01704ff fix: track moved game-core and Maven module files
```

The final correction commit tracks the moved game-core sources, tests, Maven
wrapper files and all module POMs. The final working tree was clean and matched
`origin/foundation`.

## 14. Stop point

Work Chat 02 has not begun. There is no Swing UI, TCP connection, session
registry, worker queue, MySQL integration or automatic load client in this
deliverable.
