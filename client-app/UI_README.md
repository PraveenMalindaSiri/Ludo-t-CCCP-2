# LUDO-T Swing client

The `client-app` module provides the graphical thin client for LUDO-T. It uses standard Java
Swing components and communicates with the standalone server through the shared NDJSON protocol.

## Interface

- The connection screen accepts the server host and port.
- The lobby lists available sessions and allows a user to create or join a session.
- The game screen displays the board, session status, player information, controls and live events.
- The Copy Session ID action copies the complete session UUID for another client or a rapid-client
  command.
- The board displays the four coloured bases, starting X cells, approach circles, home paths,
  mystery locations, pieces, blocks and active-player indicators.

## Architectural boundary

- The client contains presentation and network-transport code only.
- Game rules and mutable game state remain in the server-side `game-core` module.
- Database access remains in `server-app`.
- Protocol records are supplied by the shared `protocol` module.
- Network callbacks are transferred to the Swing Event Dispatch Thread before UI state changes.

## Run

From the project root, build and test the project:

```powershell
.\mvnw.cmd clean test
```

Start `server.ServerMain`, and then start `client.ClientMain`. The default server address is
`127.0.0.1:5050`.
