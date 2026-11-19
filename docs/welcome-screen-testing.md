# Welcome screen

Launcher now opens WelcomeActivity. It shows the supplied day/night artwork with live Lucky Shake text and a Start button. The button opens MainActivity and finishes the welcome screen, so Back from Home retains its existing exit behavior. Opening a reminder uses the existing explicit destination. No timer or automatic draw is added.

Phone/tablet portrait uses a centered brand column (maximum 600dp); landscape provides a scrolling brand panel and a full illustration beside it. Portrait and landscape inset containers have separate IDs to avoid restoring ScrollView state to a LinearLayout on rotation. Theme and language use application preferences.

Validation completed: resource compile/link, Java compilation of WelcomeActivity, XML parsing of the manifest, and git diff checks. Full APK build/device verification is still pending (Gradle daemon socket restrictions in this environment).

Manual checks:
- Cold launch from app icon opens the cover; Start enters the draw screen.
- Tap Start rapidly; only one draw screen opens. Back from Home does not return to the cover.
- Switch light/dark mode in Settings, close and reopen app; the matching artwork appears.
- Rotate cover on phone and tablet; button is reachable, large text scrolls, and no crash occurs.
- Test Thai/English and largest system font. Open reminder notification and verify its intended destination.
