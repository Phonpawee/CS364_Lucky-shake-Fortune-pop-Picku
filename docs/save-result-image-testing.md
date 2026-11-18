# Save result image — Issue #17

The user requested gallery saving instead of SAF. The result card is captured as PNG using Bitmap/Canvas. A local preview provides Save to gallery and Cancel. Android 29+ saves through MediaStore with RELATIVE_PATH=Pictures/LuckyShake and IS_PENDING until the stream closes successfully; failure deletes the pending entry. Android 26–28 requests WRITE_EXTERNAL_STORAGE (manifest maxSdkVersion=28), saves to public Pictures/LuckyShake and triggers media scanning. No READ_MEDIA_IMAGES permission is requested.

The preview snapshot and pending legacy permission state survive Activity recreation. Back/outside-tap/Cancel dismisses preview, deletes the snapshot and displays cancellation. Save uses a worker thread and reports success or error with application context.

## Completed checks
- Android resource compilation/link passed.
- All result-package Java sources compiled successfully.
- Full Gradle build/device tests remain blocked by daemon socket permission restrictions.

## Manual checks (pending)
- Pixel 6a: draw, open preview, Save; open the PNG in Photos or Files/Pictures/LuckyShake. Verify stick number, menu, stars and full fortune.
- Open preview and press Cancel, Back and outside-tap separately. No new gallery image; result/quota stay unchanged.
- Rotate with preview open; preview restores and Save/Cancel work. Test portrait/landscape/tablet, largest font, light/dark, Thai/English.
- Android 26–28: deny storage permission (no crash, explanation); retry and allow (PNG saved); rotate during permission prompt.
- Save after accepting the result; retry after a storage failure. Confirm incomplete gallery entries are removed.

The Bitmap/PNG instrumentation test remains applicable; it has not been executed here. Update the issue/PR to document the user-approved replacement of SAF with gallery saving.
