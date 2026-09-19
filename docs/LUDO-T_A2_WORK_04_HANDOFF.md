# LUDO-T Assignment 2 - Work 04 Handoff

**Module:** COMP63038 Clean Coding and Concurrent Programming  
**Student:** CB008920  
**Work packet:** Work 04 - Phases 7-8  
**Status:** Implementation complete and verified on Java 26

## 1. Input checkpoint

- Input ZIP: `CB008920_CCCP2_WORK_03_FINAL(2).zip`
- Work 03 session queues, lifecycle automation, persistent TCP connections and Swing thin-client
  boundaries were preserved.
- The original 79 game-core tests remain green.

## 2. Scope completed

### Complete immutable snapshots

- Expanded the game-core `GameSnapshot` with presentation-safe current-player, final-placement,
  player-count and structured piece information.
- Every protocol snapshot contains the complete state required to rebuild the GUI without domain
  access.
- All four players and all 16 pieces are included.
- Each piece carries a stable ID, name, colour, area, standard-path position or home-straight
  index, direction, state label/duration and block membership.
- Snapshots also include lifecycle status, round, turn, speed, mystery state, most recent dice,
  result/action, placements and session queue depth.
- Snapshot lists are defensively copied and immutable.

### Mapping and event collection

- Added `server.session.SnapshotMapper`.
- Added `server.session.GameEventCollector`, implementing the existing
  `IGameEventListener`.
- Observer callbacks only create presentation-safe event data. They do not write to sockets,
  update Swing, access a database or mutate the engine.
- Mapping remains in the server adapter layer; neither `protocol` nor `client-app` depends on
  `game-core`.

### Versioning

- Version ownership remains inside each `GameSession`.
- Initial version is `0`.
- Each accepted start, pause, resume, step, stop, speed change or automatic turn increments the
  session version exactly once.
- Join, leave and `GET_SNAPSHOT` do not increment game-state version.
- Versions are independent per session.
- The client accepts a different session's version sequence, but rejects duplicate or older
  snapshots for the currently displayed session.

### Server push and slow-client handling

- A successful state mutation first queues its correlated response.
- The session worker then offers any `GAME_EVENT_BATCH` followed by the complete versioned
  snapshot event to every current subscriber.
- Every socket still has exactly one bounded outbound queue and one writer task.
- No observer or session worker performs socket writes.
- If a subscriber rejects a pushed message because its outbound path is unavailable/full, the
  subscriber is removed. The session worker does not wait indefinitely.

### Join, leave and synchronization

- Create/join responses contain the current full snapshot, so a late client reconstructs the
  latest state without replaying missed animations.
- Leaving removes only that subscription and returns the GUI to the lobby; the server-owned game
  continues.
- Two clients joined to one session immediately receive the same pushed version after either
  client mutates the game.
- Different sessions retain separate engines, queues, workers, versions and subscriber sets.

### Swing

- `BoardPanel` now maps structured piece locations instead of parsing only position text.
- Block members have a visible square marker.
- The session panel displays current player, status, round, turn, version, speed, latest dice,
  latest result, placements and last action.
- A scrollable player/piece panel displays base/board/home counts, areas, state durations and
  block IDs.
- Pushed event batches are appended to the bounded event history on the EDT.

## 3. Version rules

| Operation | Version change | Push |
| --- | ---: | --- |
| Initial session snapshot | Starts at 0 | Returned on create/join |
| Join / leave | 0 | No game-state push required |
| `GET_SNAPSHOT` | 0 | Response only |
| Start / pause / resume / stop | +1 | Full snapshot |
| Set speed | +1 | Full snapshot |
| Manual step | +1 | Event batch plus full snapshot |
| Automatic turn | +1 | Event batch plus full snapshot |

## 4. Main production files added

- `server-app/src/main/java/server/session/GameEventCollector.java`
- `server-app/src/main/java/server/session/SnapshotMapper.java`

## 5. Main production files updated

- `game-core/src/main/java/event/GameSnapshot.java`
- `game-core/src/main/java/engine/GameEngine.java`
- `game-core/src/main/java/piece/state/IPieceState.java`
- timed piece-state implementations
- `protocol/src/main/java/protocol/GameSnapshotDto.java`
- `server-app/src/main/java/server/session/GameSession.java`
- `client-app/src/main/java/client/ClientController.java`
- `client-app/src/main/java/client/model/ClientViewState.java`
- `client-app/src/main/java/client/ui/BoardGeometry.java`
- `client-app/src/main/java/client/ui/BoardPanel.java`
- `client-app/src/main/java/client/ui/GamePanel.java`

## 6. Automated verification

Java 26 compilation passed for all production and test sources.

The complete suite passed:

- 130 tests found;
- 130 tests started;
- 130 tests successful;
- 0 failed, skipped or aborted.

The Work 04 test set includes:

- `GameSnapshotCompletenessTest`
- `SnapshotMapperTest`
- `SnapshotVersionTest`
- `GameEventCollectorTest`
- `TwoClientPushIT`
- `LateJoinSnapshotIT`
- `StaleSnapshotTest`
- `SessionIsolationIT`
- `DifferentSessionConcurrencyIT`
- `SlowSubscriberPolicyTest`

The existing Work 01-03 tests and all original game tests also passed.

The final ZIP was also extracted into a clean directory and compiled again with Java 26. The
complete 130-test suite passed from that extracted copy, confirming that the archive itself is
complete.

The evidence directory includes `work04-ui-preview.png`, a headless render of the updated game
screen using a complete structured protocol snapshot. It verifies the Work 04 board, metadata,
piece-state, block-marker and event-log layout. It is not a replacement for the live two-client
screenshots listed below.

## 7. Remaining limitations and manual evidence

- Advanced movement animation is not implemented. The required final board state is rendered
  correctly after every pushed snapshot.
- Late join intentionally reconstructs only the latest complete state; it does not replay missed
  animations or historical events.
- Live side-by-side screenshots of Client A and Client B must be captured on the local Windows
  GUI environment. Capture the same-session/version view, pause-through-A/resume-through-B,
  late-join view and independent Game-1/Game-2 view by following the local guide.
- MySQL persistence, rapid automatic test clients, CSV timing evidence and final assignment
  packaging remain intentionally deferred to later work packets.

## 8. Local commands

```powershell
java -version
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean test
```

Then follow `docs/LUDO-T_A2_WORK_04_LOCAL_GUIDE.md` for the two-GUI demonstration.

## 9. Scope boundary

Work 04 stops here. It does not add MySQL, JDBC repositories, rapid automatic clients, CSV
evidence logging or final submission packaging. Those remain for later work packets.
