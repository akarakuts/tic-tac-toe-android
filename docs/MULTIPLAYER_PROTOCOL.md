# Multiplayer wire protocol

JSON frames, one per line (UTF-8, newline-delimited). Implemented in `MultiplayerMessage.kt`.

## Message types

| `type` | Direction | Purpose |
|--------|-----------|---------|
| `config` | Host → Guest | Board size, win length, host player (X), mode |
| `ack` | Guest → Host | Handshake complete |
| `move` | Either | `row`, `col`, `player` |
| `new_game` | Either | Reset board with given size / win length |

## Handshake

1. Transport reaches `Connected`.
2. **Host** sends `config` (host plays X, uses current Play-tab board settings).
3. **Guest** validates, creates local game, sends `ack`, enters play.
4. **Host** on `ack` creates game and enters play.

## Move validation

Receiver checks: cell empty, in range, `player` is peer (not local), `player == game.currentPlayer`. Invalid frames are dropped.

## Example frames

```json
{"type":"config","boardSize":3,"winLength":3,"localPlayer":"X","opponentMode":"HumanHuman"}
{"type":"ack"}
{"type":"move","row":1,"col":1,"player":"X"}
{"type":"new_game","boardSize":3,"winLength":3}
```

## Transport

`BluetoothTransport` — classic RFCOMM with fixed service UUID. Permissions: `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (with `neverForLocation` on Android 12+).
