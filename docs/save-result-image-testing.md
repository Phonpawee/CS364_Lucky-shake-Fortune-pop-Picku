# Save result image — Issue #17

The result screen captures the complete measured fortune card using Bitmap/Canvas, then opens CreateDocument(image/png). No storage permission is added. The temporary PNG is retained by filename across Activity recreation while the system picker is open. Document-provider I/O runs in a worker thread using application context. Cancellation and failures show localized messages, and temporary snapshots are deleted after completion/cancellation.

## Validation completed
- Android resource compile/link passed.
- ResultActivity and ResultCardImage Java compilation passed against Android/dependency classpaths.
- Full Gradle build/device test could not start: daemon socket binding denied (Operation not permitted).
- Added instrumentation test for decoding the PNG and retaining the bottom of a tall card; execution pending.

## Device checks (pending)
| Action | Expected |
| --- | --- |
| Draw, tap Save result image, choose Downloads and save | Success message; PNG opens in Files |
| Inspect saved PNG | Stick number, menu name, luck stars and fortune match the displayed card |
| Cancel file picker | Cancellation message, same result and quota; app stays usable |
| Rotate while picker is open, then save/cancel | Completes without crashing; captures original card |
| Accept result then save | Save button remains available |
| Portrait/landscape, tablet, largest font, Thai/English, light/dark | Complete readable card, all actions reachable |
| Save to a provider that fails | Error message; retry remains possible |

Manual testing: run the app on Pixel 6a, draw a result, use Save result image, save in Downloads, and open the PNG in Files. Repeat with picker cancellation and screen rotation. Do not mark acceptance complete until these have been checked.
