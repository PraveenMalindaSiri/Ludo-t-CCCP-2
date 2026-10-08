# LUDO-T — Clean Coding and Concurrent Programming Assignment 2

LUDO-T is a Java client-server application that runs an automatic four-player Ludo simulation. Several users can connect through Swing clients, create or join sessions, control the simulation and view live board updates. Separate rapid test clients send asynchronous request bursts to check the server under concurrent activity.

The server owns the game rules and live game state. The clients display information and send requests. MySQL stores session metadata and completed results.

## Project structure

| Folder | Purpose |
| --- | --- |
| `game-core/` | Game rules, board, pieces, blocks and simulation logic, based on Assignment 1. |
| `protocol/` | Shared request, response and event DTOs, NDJSON encoding, bounded line reading and CSV encoding. |
| `server-app/` | TCP server, session coordination, scheduling, JDBC persistence and server CSV recording. |
| `client-app/` | Swing thin client, board display, session lobby and network transport. |
| `test-client/` | Automatic rapid client, asynchronous request bursts and response reconciliation. |
| `database/` | Database creation, table definitions, demo reset and inspection SQL. |
| `scripts/` | PowerShell launcher for two independent rapid-client processes. |
| `test-results/` | Runtime output and selected preserved test evidence. |

The Swing and rapid clients depend on `protocol`; they do not depend on `game-core` and do not access MySQL.

## Requirements

- JDK 26, with `JAVA_HOME` pointing to the JDK and Java available on `PATH`.
- Apache Maven 3.9.x, or the complete Maven wrapper supplied with the project.
- MySQL 8.0 for the running server and the optional MySQL integration tests.
- Windows PowerShell for `scripts/run-two-test-clients.ps1`.
- A graphical desktop for the Swing client.

Run the commands below from the project root, where the parent `pom.xml` is located. Check the installed tools:

```powershell
java -version
mvn -version
mysql --version
```

The commands in this README use installed Maven. When the wrapper is complete, `mvn` can be replaced with `.\mvnw.cmd` on Windows or `./mvnw` on Linux/macOS.

**Wrapper requirement:** `mvnw` and `mvnw.cmd` need `.mvn/wrapper/maven-wrapper.properties`. Keep that file when downloading or creating a submission ZIP. If it is missing, restore the original `.mvn` directory from the repository. The rapid-client PowerShell script calls `.\mvnw.cmd` directly, so installed Maven alone does not satisfy that script's wrapper requirement.

## MySQL setup

### Create the databases and tables

Start MySQL, then run these commands using an administrator account:

```powershell
Get-Content database\create-databases.sql | mysql -u root -p
Get-Content database\schema.sql | mysql -u root -p ludot
Get-Content database\schema.sql | mysql -u root -p ludot_test
```

`ludot` is the application database. `ludot_test` is a separate database for the integration tests. Each uses exactly these two tables:

| Table | Stored information |
| --- | --- |
| `game_sessions` | Session UUID, name, seed, status and lifecycle timestamps. |
| `game_results` | Winner, placements, final round and completion time, linked to its session. |

Open MySQL as an administrator:

```powershell
mysql -u root -p
```

Create the application account, replacing the example password with your own local password:

```sql
CREATE USER IF NOT EXISTS 'ludot_app'@'localhost'
    IDENTIFIED BY 'YOUR_LOCAL_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON ludot.*
    TO 'ludot_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON ludot_test.*
    TO 'ludot_app'@'localhost';
```

If that account already exists, this command does not change its password. Use its existing password or change it separately as an administrator. Table creation and the reset script use the administrator account; the application account only needs the four permissions above.

### Configure the server connection

Set these variables in the PowerShell window that will launch the server:

```powershell
$env:LUDOT_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_DB_USER = "ludot_app"
$env:LUDOT_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"
```

Keep the real password out of tracked files. The checked-in `server.properties` has an empty password. Database settings are resolved in this order: Java system properties, environment variables, then `server.properties`.

These environment variables belong to the current PowerShell process and its child processes. If starting the server through an IDE, enter them in the `server.ServerMain` run configuration instead. The application does not automatically load `.env` or `*.local.properties` files.

## Build and run

### Build the complete project

```powershell
mvn clean install
mvn -pl server-app,client-app dependency:copy-dependencies "-DincludeScope=runtime"
```

The first command builds all five modules and runs the normal tests. The second copies the runtime dependencies needed by the terminal launch commands. No executable or bundled application JAR is required.

### Start the server

In the PowerShell window containing the database environment variables:

```powershell
java -cp "server-app\target\classes;server-app\target\dependency\*" server.ServerMain
```

Successful startup prints `MySQL ready:` followed by:

```text
LUDO-T server listening on 127.0.0.1:5050
```

The server validates MySQL before opening its listening socket. Keep this process running while using either client.

### Start two Swing clients

Open two additional PowerShell windows at the project root and run this command once in each:

```powershell
java -cp "client-app\target\classes;client-app\target\dependency\*" client.ClientMain
```

Alternatively, import the parent `pom.xml` into an IDE, select JDK 26, run `server.ServerMain`, then launch `client.ClientMain` twice as separate processes. An IDE may require its setting for multiple instances to be enabled.

### Join the same session

1. Connect Client A to `127.0.0.1`, port `5050`.
2. In its lobby, select **Create session** and enter a session name.
3. Connect Client B to the same server and select **Refresh** in its lobby.
4. Select the session created by Client A, then select **Join selected**.
5. Check that both windows show the same session UUID and corresponding board state.

There is no need to copy and paste a UUID to join through the Swing lobby. **Copy ID** copies the full session UUID for the rapid-test command.

Use **Start**, **Pause**, **Resume**, **Step once**, the speed selector and **Stop** to control the automatic simulation. **Step once** requires a paused session. **Stop** ends that session permanently; **Leave session** returns that client to the lobby.

## Automated tests

### Normal tests

```powershell
mvn clean test
```

This runs the game, protocol, server, Swing and rapid-client tests. The six MySQL integration tests are skipped unless the `mysql-it` profile is enabled. The default test run does not need a running MySQL server.

### MySQL integration tests

Create the `ludot_test` tables as described above, then set the test credentials in the window running Maven:

```powershell
$env:LUDOT_TEST_DB_URL = "jdbc:mysql://127.0.0.1:3306/ludot_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LUDOT_TEST_DB_USER = "ludot_app"
$env:LUDOT_TEST_DB_PASSWORD = "YOUR_LOCAL_PASSWORD"
mvn -Pmysql-it clean test
```

This runs the full suite with the six `JdbcGameRepositoryIT` tests enabled. The integration test class checks that its connection URL targets `ludot_test`.

Surefire writes results to each module's `target/surefire-reports/`. Copy any results you want to preserve before another `clean` command deletes them. Store selected reports under `test-results/preserved/automated/`, using separate folders for normal and MySQL-enabled runs. Use actual results from the run; a successful compilation or package build alone is not rapid-test evidence.

## Two rapid test clients

### Prepare a fresh run

For a run with matching client and server CSVs, use a new output directory for every run. The example below uses `test-results\rapid\run-03`; change it if that directory already contains evidence.

Stop the server with **Ctrl+C**, then restart it with the same database environment variables and a dedicated evidence directory:

```powershell
java "-Dserver.evidenceDirectory=test-results\rapid\run-03" -cp "server-app\target\classes;server-app\target\dependency\*" server.ServerMain
```

Restarting the server removes its in-memory sessions. Connect a Swing client, create a fresh session, select **Start**, then **Pause**. Wait until it shows `PAUSED`, then select **Copy ID**. Keep the session and server running.

### Run the burst

In another PowerShell window at the project root, replace `SESSION_UUID` with the copied UUID:

```powershell
.\scripts\run-two-test-clients.ps1 `
    -Session "SESSION_UUID" `
    -Requests 200 `
    -Scenario step `
    -Output "test-results\rapid\run-03"
```

The script builds the rapid client and launches Client A and Client B as separate Java processes. Each joins the session and submits its request burst before waiting for all responses. It checks both process exit codes and both summaries before producing `summary.csv`.

| Scenario | Request sent | Preparation |
| --- | --- | --- |
| `step` | `STEP_GAME` | Use a started, paused session. |
| `snapshot` | `GET_SNAPSHOT` | Use an existing session. |
| `queue-full` | `STEP_GAME` | Use a paused session and a smaller server session queue to exercise overload handling. |

`queue-full` sends the same operation as `step`; it does not automatically change the server queue capacity. For an overload demonstration, restart the server with a smaller `-Dserver.sessionQueueCapacity` value, create a fresh session and repeat the preparation. Queue rejection depends on load and timing.

### Read and preserve the results

| File | Meaning |
| --- | --- |
| `client-A.csv`, `client-B.csv` | Per-request client timings, request UUIDs and outcomes. |
| `server.csv` | Server session-request timings, queue information and execution-thread details. It can also contain setup requests such as join, start and pause. |
| `summary-A.csv`, `summary-B.csv` | Counts, maximum sampled queue depth and duration for each client. |
| `summary.csv` | Combined counts; the greater client queue-depth sample and greater client duration. |

For a fully successful 200-request burst from each client, the combined summary is expected to show 400 sent, 400 responses, 400 successes, zero rejections, zero lost responses and zero duplicates. A finished or otherwise invalid game state can cause explicit rejections; inspect their error codes rather than assuming every rejection means overload.

For an overload run, `QUEUE_FULL` is an explicit rejection, not a lost response. Successful reconciliation requires `sent = responses`, `success + rejected = responses`, and zero lost and duplicate responses. Verify the per-request CSVs when checking why a request was rejected. Queue depth and duration vary between runs.

After both clients finish, stop the server with **Ctrl+C** so its evidence writer can drain and close. Preserve all six CSVs together. The client script overwrites its five output files in the selected directory, and server startup overwrites `server.csv` in its evidence directory. Do not reuse a preserved output directory.

The `.gitignore` allows the six CSVs in `test-results/fixed-run-01/` and all selected evidence in `test-results/preserved/`. Other runtime output is ignored. To retain a fresh run in Git, copy its complete directory into `test-results/preserved/rapid/` after recording finishes.

## Configuration and runtime behaviour

Default server settings are in `server-app/src/main/resources/server.properties`; client defaults are in `client-app/src/main/resources/client.properties`. Java system properties override these settings. The default address is `127.0.0.1:5050`, intended for a local demonstration.

- **Transport:** persistent TCP sockets with one JSON message per line (NDJSON). UUID request identifiers correlate responses with pending requests. Live events are separate from responses.
- **Consistency:** each session has its own game object graph and one sequential session worker. Requests and automatic turns for that session are processed by that worker. Different sessions can execute concurrently.
- **Threads and backpressure:** virtual threads handle networking and session work. Bounded outbound, session and CSV queues limit buffered work. Session queue saturation produces structured `QUEUE_FULL` responses; slow connections can be disconnected to isolate them.
- **Thin clients:** game rules and JDBC remain on the server. Swing displays snapshots and applies UI changes on the Event Dispatch Thread. Snapshot versions help avoid applying older state.
- **Evidence:** dedicated CSV writers keep file writing separate from request processing. Rapid-client summaries wait for recording callbacks as well as response completion.
- **Shutdown:** the server closes connections, stops session work and attempts bounded queue draining before interruption fallback and resource cleanup.
- **Persistence:** MySQL records metadata and completed results. Live board state is held in memory and is not restored after a server restart. Unfinished persisted sessions are marked `INTERRUPTED` on startup and orderly shutdown.

## Inspect or reset database history

To inspect stored metadata and results:

```powershell
Get-Content database\demo-queries.sql | mysql -u ludot_app -p ludot
```

To remove all application history, stop the server first and run the reset using an administrator account:

```powershell
Get-Content database\reset-demo.sql | mysql -u root -p ludot
```

This truncates both tables and permanently removes their rows. The reset needs permissions beyond the application's CRUD grants. It does not create tables or reset a running in-memory session.
