# Tablet layout verification — Issue #16

Requires #15 (landscape layouts) to be merged first. This branch currently builds on that branch.

Tablet resources use smallest width >= 600dp, in both orientations. Phone resources are unchanged. Portrait uses the phone layout in a single centered column, capped at 600dp. Landscape uses two independently scrollable columns capped at 1000dp, with larger artwork. Existing view IDs and Activity state handling are retained.

| Device/configuration | Home | Result | Status |
| --- | --- | --- | --- |
| 600dp tablet, portrait | Filters, artwork and draw accessible | Stick, fortune, accept/redraw accessible | Pending emulator |
| 600dp tablet, landscape | Balanced columns, no excessive stretching | Balanced columns, no excessive stretching | Pending emulator |
| 800dp+ tablet, both orientations | Content remains centered within 1000dp | Content remains centered within 1000dp | Pending emulator |
| Largest system font, both orientations | Scroll to all controls; long menu names wrap | Scroll to all text and actions | Pending emulator |
| Day/night and Thai/English | Contrast and labels readable | Contrast and labels readable | Pending emulator |

Rotate with filters selected and with a pending result. Verify the same selection, dish, stick number and redraw quota remain. Accept once and rotate; history must not gain a duplicate record. Return to a phone emulator to verify phone layout selection is unchanged.

Resource compilation/linking is a build check, not evidence of visual acceptance. Do not mark emulator rows complete until tested.
