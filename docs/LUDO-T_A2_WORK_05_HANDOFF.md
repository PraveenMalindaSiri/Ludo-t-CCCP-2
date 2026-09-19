# LUDO-T Assignment 2 - Work 05 Handoff (Two-Table Version)

**Module:** COMP63038 Clean Coding and Concurrent Programming  
**Student:** CB008920  
**Work packet:** Work 05 - Phase 9, local MySQL persistence  
**Input checkpoint:** `CB008920_CCCP2_WORK_04_Final(1).zip`  
**Design:** simplified persistence using only `game_sessions` and `game_results`

## 1. Scope completed

- Added MySQL as a separate application tier used only by `server-app`.
- Added an application-layer `GameRepository` port and plain JDBC implementation.
- Kept the live `GameSession` and `GameEngine` authoritative in memory.
- Persisted session identity, seed, lifecycle status, timestamps, and completed results.
- Added repeatable schema, reset, demonstration, and test scripts.
- Preserved the Work 04 TCP, queue, session, snapshot, push, and Swing behaviour.

The optional `game_events` and `request_audit` features have deliberately been removed from the
schema, Java interfaces/implementations, runtime hooks, tests, and demonstration queries. This is
a complete two-table build, not a schema-only edit.

## 2. Database objects

| Table | Purpose | Key design |
| --- | --- | --- |
| `game_sessions` | Stores each session's UUID, name, random seed, lifecycle state, and timestamps | UUID primary key; indexes for status/update time and creation time |
| `game_results` | Stores the winner, ordered final placements, final round, and completion time | One result per session; foreign key to `game_sessions`; completion-time index |

`game_sessions` is required for creating and tracking persistent sessions. `game_results` is
required to demonstrate completed-game database output. A stopped or interrupted game has a
session row but no invented result row.

Scripts are under `database/`:

- `create-databases.sql`
- `schema.sql`
- `reset-demo.sql`
- `demo-queries.sql`
- `README.md`

## 3. Persistence boundary and transactions

`server.application.port.GameRepository` exposes application records only; JDBC types do not
cross the port. Its operations validate access, create/update session rows, atomically save a
completed session and result, mark unfinished rows interrupted, and close the adapter.

`JdbcGameRepository` uses prepared statements and try-with-resources. Saving a completed game
updates `game_sessions` and inserts `game_results` in one transaction. If result insertion fails,
the status update rolls back. Each repository call opens and closes a short-lived connection; no
connection pool was added for this assignment phase.

## 4. Runtime behaviour

### Startup

1. `ServerMain` loads database configuration.
2. Connector/J and the MySQL connection are validated before TCP startup.
3. Old `CREATED`, `RUNNING`, and `PAUSED` rows become `INTERRUPTED`.
4. No unfinished engine is reconstructed from the database.

### During play

- A session row must be created before the create request succeeds.
- Start, pause, resume, speed, stop, and turn changes update session lifecycle metadata.
- Natural completion stores final session status and the result atomically.
- Explicit stop stores `STOPPED` without creating a false winner.
- Live game events remain in the normal in-memory/client event flow and are not stored in MySQL.
- Individual requests are not audited in MySQL; Work 06 timing evidence remains a separate task.

### Shutdown

- Session workers are interrupted and joined before repository closure.
- Any still-created/running/paused session becomes `INTERRUPTED` and is updated in MySQL.
- Completed and explicitly stopped sessions retain their final states.

## 5. Configuration

| Purpose | Environment variable | Java system property |
| --- | --- | --- |
| JDBC URL | `LUDOT_DB_URL` | `database.url` |
| Username | `LUDOT_DB_USER` | `database.user` |
| Password | `LUDOT_DB_PASSWORD` | `database.password` |

Priority is Java system property, environment variable, then the redacted default in
`server.properties`. `DatabaseConfig.toString()` never exposes the password.

## 6. Local setup and verification

Run from PowerShell in the project root:

```powershell
mysql --version
Get-Content database\create-databases.sql | mysql -u root -p
Get-Content database\schema.sql | mysql -u root -p ludot
Get-Content database\schema.sql | mysql -u root -p ludot_test
```

Create/grant `ludot_app` with the commented template in `create-databases.sql`, using your own
local password. Then set the runtime and test environment variables:

```powershell
$env:LUDOT_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_DB_USER = "ludot_app"
$env:LUDOT_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"

$env:LUDOT_TEST_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_TEST_DB_USER = "ludot_app"
$env:LUDOT_TEST_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"

.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
.\mvnw.cmd -Pmysql-it clean test
```

The opt-in integration class refuses a URL that does not clearly target `ludot_test`. Its six
cases cover duplicate identifiers, lifecycle timestamps, atomic result saving, rollback,
unfinished-session interruption, and concurrent writes to different sessions.

## 7. Manual demonstration

1. Reset with `database/reset-demo.sql`.
2. Start the server and two Swing clients.
3. Create a session, note its displayed UUID, and exercise start/pause/resume/step/stop or allow
   the game to finish.
4. Run `database/demo-queries.sql`.
5. Match the GUI UUID to `game_sessions`; for a naturally completed game, confirm its matching
   `game_results` row.
6. Leave another game running, stop/restart the server, and confirm the old row is
   `INTERRUPTED` and is not restored to the live lobby.
7. Confirm `client-app` contains no database URL, password, or Connector/J dependency.

## 8. Known limitations and scope

- Persistence provides session history and final results, not live-session recovery.
- A process/OS crash may lose the latest in-memory change before its JDBC call completes.
- There is no distributed transaction between Java memory and MySQL.
- Events and per-request audits are intentionally outside this simplified version.
- Local MySQL integration execution and screenshots must still be captured on the student's
  Windows/MySQL installation.
- Rapid automatic clients and CSV timing evidence remain reserved for Work 06.

Suggested commits:

```text
feat: add two-table MySQL persistence for sessions and results
test: verify session lifecycle and result transactions
docs: add local MySQL setup and demonstration guide
```
