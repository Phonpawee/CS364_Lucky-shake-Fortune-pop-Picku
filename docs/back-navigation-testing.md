# Back navigation verification (#14)

Use Android system Back or the Back gesture. If the keyboard is open, dismiss it first. Repeat the UI cases in portrait and landscape.

| Screen and state | Back | Stay | Discard |
| --- | --- | --- | --- |
| Home, no pending draw | Exit app | — | — |
| Home, draw animation in progress | Show confirmation | Continue the same draw | Exit app |
| Result, unaccepted | Show confirmation | Keep the dish and redraw quota | Return Home and clear the pending draw |
| Result, accepted | Return Home | — | — |
| Dishes | Return Home | — | — |
| Add or edit dish, unchanged | Return Home | — | — |
| Add or edit dish, changed name, type or tags | Show confirmation | Keep edits | Return Home without saving |
| Group setup | Return Home | — | — |
| Group proposal, collecting votes | Show confirmation | Keep votes, turn and current input | Return Home |
| Group tally, before drawing a winner | Show confirmation | Keep the tally | Return Home |
| Group winner displayed | Return Home | — | — |
| Journal, including statistics | Return Home | — | — |
| Settings | Return Home | — | — |

After discarding a solo result, draw again and verify the quota is restored and no acceptance history was written for the discarded result. After discarding a group round, start a new round and verify it starts at the first turn. The explicit Cancel button in the dish form returns to the dish list after the same confirmation if edits are pending.
