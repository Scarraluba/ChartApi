# Architecture Notes

## Rule 1
chart-core must remain UI-toolkit neutral.

## Rule 2
Swing, Android, and JavaFX modules should only adapt:
- render target
- input system
- popup/property hosting
- platform-specific utilities

## Rule 3
Indicators, tools, replay, pane layout, transforms, and axis logic should live in chart-core.

## Rule 4
Any new feature should be implemented core-first whenever possible.
