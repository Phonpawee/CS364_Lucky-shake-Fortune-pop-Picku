# Home and result layout verification (#15)

Home and Result use two columns in landscape. Each column scrolls independently. Narrow controls and large text reflow into vertical rows. Selected menu-set and menu-type names wrap onto multiple lines.

## Device matrix

Repeat in Thai and English, with light and dark themes.

| Viewport | Orientation | System font | Required outcome | Status |
| --- | --- | --- | --- | --- |
| 320 dp wide phone | Portrait | Default and maximum | Every field and button reachable by scrolling; no clipped text | Pending device verification |
| 568 × 320 dp phone | Landscape | Default and maximum | Both columns scroll; Draw, Accept and Redraw remain reachable | Pending device verification |
| Pixel 6a | Both | Default and maximum | Labels wrap and controls remain tappable | Pending device verification |
| 960 × 600 dp tablet | Landscape | Default and maximum | Two balanced columns, full labels and reachable buttons | Pending device verification |

## Rotation and state

- Select a menu set, a type and both switches. Rotate twice and confirm the same selections.
- Rotate during the shake animation. The same pending result must open once.
- Draw a dish, use one redraw and note the dish, stick number and remaining quota. Rotate twice and confirm all three values.
- Accept the dish, rotate and confirm the accepted state remains and is recorded once.
- Choose a long menu-set name. Verify its full selected value is readable in the picker.

## Checks completed in this environment

- Android resource compilation and linking.
- Compilation of the changed Java classes.

A full Gradle build and emulator verification remain pending because the environment blocks the Gradle daemon's local socket.
