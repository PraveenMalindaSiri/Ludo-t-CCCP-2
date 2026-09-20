# LUDO-T Assignment 2 - Work 06 Handoff

**Module:** COMP63038 Clean Coding and Concurrent Programming

**Student:** CB008920

**Work packet:** Work 06 - phases 10-12, rapid clients, CSV evidence and resilience

**Input checkpoint:** `CB008920_CCCP2_WORK_05_final.zip`

**Input SHA-256:** `802c2b8ae41915526e057523ef64222b3cf3c38f85f96e04dc0a7356ab25155f`

**Persistence design preserved:** only `game_sessions` and `game_results`

## 1. Scope completed

- Added a standalone `test-client` implementation with no Swing, game-core or JDBC dependency.
- Added one parameterized Java entry point that can run as Client A or Client B in independent JVMs.
- Added a PowerShell launcher that starts both client processes nearly simultaneously.
- Each client connects and joins first, sends its entire request burst without waiting after each
  send, and only then awaits the outstanding correlated futures.
- Added separate Client A, Client B and server CSV evidence plus per-client and combined summaries.
- Added bounded evidence queues with one virtual writer per CSV file.
- Added visible queue-full, slow-client, evidence-full, disconnect and malformed-input policies.
- Added shutdown notification, bounded accepted-work drain, forced-interruption fallback and
  deterministic resource closure.
- Preserved GUI, networking, sessions, synchronization, game-core behaviour and the approved
  two-table MySQL design.

Work 07 final runnable-JAR packaging, report evidence indexing and the final demonstration bundle
remain intentionally out of scope.

## 2. Rapid load scenario

The controlled scenario is:

1. Start the server and connect a Swing client.
2. Create a game session.
3. Start it and then pause it.
4. Copy the session UUID from the GUI.
5. Start Client A and Client B as separate processes.
6. Each process joins the paused session, submits 200 asynchronous `STEP_GAME` requests, and then
   waits for completion.

Run from the project root in PowerShell:

```powershell
.\scripts\run-two-test-clients.ps1 `
  -Session "PASTE-THE-SESSION-UUID" `
  -Requests 200 `
  -Scenario step
```

The script packages the required modules, launches two separate Java processes, waits for both,
and writes a combined `test-results/summary.csv`. To demonstrate the explicit queue-full response,
start the server with `-Dserver.sessionQueueCapacity=8` and run:

```powershell
.\scripts\run-two-test-clients.ps1 `
  -Session "PASTE-THE-SESSION-UUID" `
  -Requests 200 `
  -Scenario queue-full
```

`QUEUE_FULL` is a rejected response, not a lost request.

## 3. Proof that sending is asynchronous

`RequestBurstRunner` loops over all requests and stores every `PendingRequest` before calling
`awaitAll`. It never waits inside the send loop. `RapidClientConnection` maintains a concurrent map
of pending UUIDs, and `maximumOutstanding()` proves that more than one request was in flight.

`RapidClientConnectionTest` uses a fake TCP server that withholds every response until all 25
requests have arrived. The test confirms that all 25 futures are simultaneously outstanding and
that every later response is matched to its original UUID.

## 4. Queue capacities and policies

| Queue | Default capacity | Policy |
| --- | ---: | --- |
| Server session request queue | 512 | `offer`; one structured `QUEUE_FULL` response on rejection; never silently drops or blocks the socket reader indefinitely |
| Server connection outbound queue | 2048 | one writer; when full, close and unsubscribe the persistently slow connection so it cannot block the session worker |
| Rapid-client outbound queue | 512 | failed future with `Client outbound queue is full`; visible as lost in the client summary |
| Server evidence queue | 4096 | waits up to 100 ms, increments a visible dropped counter, prints failure on close |
| Client evidence queue | 1024 | waits up to 100 ms, increments dropped count; the process exits unsuccessfully if any evidence is lost |

All application-created server/client networking, evidence-writing, session-worker and shutdown
worker threads are virtual. The required JVM shutdown-hook carrier is a normal unstarted platform
thread because Java does not permit a virtual thread to be registered directly as a shutdown hook;
it immediately delegates the actual application shutdown to `server-shutdown-worker`, which is
virtual.

## 5. CSV evidence

Client A and B files:

```csv
client_id,request_id,session_id,request_type,sent_at,response_at,elapsed_ms,status
```

Server file:

```csv
request_id,client_id,session_id,request_type,received_at,queued_at,started_at,completed_at,queue_wait_ms,processing_ms,queue_depth,result,thread_name,is_virtual
```

Summary file:

```csv
total_sent,total_responses,total_success,total_rejected,total_lost,total_duplicate,max_queue_depth,test_duration_ms
```

Each logger accepts immutable records through a bounded queue. Exactly one virtual consumer owns
each `BufferedWriter`. The header is written once, the writer flushes every 50 records, and close
drains accepted records for up to five seconds. `CsvEncoder` correctly quotes commas, quotes, CR
and LF without an external CSV dependency.

The curated sample under `docs/evidence/work06-sample/` is a genuine 20 + 20 run:

- 40 sent = 40 responses
- 40 successes
- 0 lost
- 0 duplicate
- maximum queue depth 28
- maximum queue wait 16 ms
- overlapping Client A/B send windows
- all server worker rows marked `is_virtual=true`

## 6. Disconnect and backpressure behaviour

- Normal EOF, reset/read failure, write failure, malformed JSON and interruption close only the
  affected connection.
- All client pending futures complete exceptionally when the connection closes.
- `ClientController` already converts connection-close callbacks to EDT updates; no network thread
  directly modifies Swing.
- Closing any GUI removes only that subscriber. It does not stop or delete its game.
- A slow server subscriber cannot block a session worker; a failed outbound offer removes it.
- Completed and persisted history is not deleted on disconnect.

## 7. Graceful shutdown sequence

1. Mark the dispatcher/server as stopping so no new game work is accepted.
2. Close the listening socket.
3. Queue `SERVER_SHUTTING_DOWN` to each connected client and close each connection after its sole
   writer sends the notification.
4. Stop every session from accepting new requests.
5. Drain already accepted session requests within `server.shutdownDrainMillis` (default 5000 ms).
6. Interrupt any worker that exceeds the bound and fail remaining envelopes exactly once.
7. Mark non-completed/non-stopped live sessions `INTERRUPTED` and persist that lifecycle state.
8. Drain and close the server evidence logger.
9. Close the repository/JDBC adapter.
10. Complete the server termination latch.

Every caught `InterruptedException` either continues the intentional shutdown drain or restores the
thread interruption status before returning.

## 8. Files added

- `protocol/src/main/java/protocol/CsvEncoder.java`
- `server-app/src/main/java/server/evidence/ServerCsvLogger.java`
- `server-app/src/main/java/server/lifecycle/ShutdownCoordinator.java`
- `test-client/src/main/java/testclient/RapidTestClientMain.java`
- `test-client/src/main/java/testclient/RapidClientConnection.java`
- `test-client/src/main/java/testclient/RequestBurstRunner.java`
- `test-client/src/main/java/testclient/TestResultRecorder.java`
- `test-client/src/main/java/testclient/ClientCsvLogger.java`
- `test-client/src/main/java/testclient/TestSummaryWriter.java`
- `scripts/run-two-test-clients.ps1`
- Work 06 automated tests and curated evidence files

Existing server/session/configuration files were changed only where required to inject evidence,
expose queue timings and coordinate shutdown. No database table, SQL script or database contract was
changed.

## 9. Verification results

Java 26 direct compilation succeeded for all five production modules.

| Suite | Passed | Failed |
| --- | ---: | ---: |
| Existing game-core tests | 81 | 0 |
| Protocol, server, Swing client and rapid-client tests | 64 | 0 |
| Total | 145 | 0 |

Covered behaviours include CSV escaping/header/drain, concurrent producers, many outstanding
futures, correlation, duplicate detection, server-disconnect completion, queue bounds, malformed
JSON isolation, slow subscribers, creator disconnect, EDT callbacks, session isolation, MySQL
repository unit behaviour and graceful server shutdown.

Maven Central was unreachable in the review container, so the source was compiled directly with
Temurin Java 26 and the cached project dependency versions. Run the authoritative Maven commands on
the normal Windows development machine:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
.\mvnw.cmd -Pmysql-it clean test
```

The last command requires the existing `ludot_test` MySQL schema and the same environment variables
documented in the Work 05 handoff. It still runs the six opt-in `JdbcGameRepositoryIT` cases. No
MySQL integration result is claimed from the review container.

## 10. Manual verification checklist

1. Set `LUDOT_DB_URL`, `LUDOT_DB_USER`, `LUDOT_DB_PASSWORD` and the three test equivalents.
2. Start MySQL and reset `ludot` if a clean demonstration is required.
3. Start `ServerMain`, then two Swing clients.
4. Create, start and pause one shared session.
5. Run the two-client PowerShell script with that UUID.
6. Confirm `summary.csv`: sent equals responses, lost is zero, duplicate is zero.
7. Compare the two client send windows and inspect server queue wait/depth.
8. Disconnect one GUI and verify the other GUI/game continues.
9. Press Ctrl+C in the server and observe the shutdown notification/clean termination.
10. Query `game_sessions` and confirm a live session became `INTERRUPTED`; completed/stopped rows
    keep their final state.

## 11. Known limitations

- The curated evidence run is deliberately small (20 + 20); generate the rubric run (200 + 200) on
  the demonstration laptop.
- Runtime CSV files are overwritten at the configured names on each run. Preserve a chosen full run
  by copying it into the final Work 07 evidence folder.
- Persistence still provides history/results rather than live object-graph recovery, unchanged from
  Work 05.
- Final shaded/runnable JARs and complete submission packaging remain Work 07.

## 12. Suggested commits

```text
feat: add asynchronous rapid-load clients
feat: record client and server concurrency evidence as CSV
feat: handle backpressure disconnects and graceful shutdown
test: verify Work 06 load evidence and resilience
```

## 13. Next packet readiness

The project is ready for checker review of Work 06. After approval, proceed to Work 07 only:
complete Maven verification on the local MySQL machine, create final runnable JARs/start scripts,
assemble the rubric evidence index, and prepare the demonstration/report handoff.
