## 2026-01-31 - Keyboard Interaction in Brutalist Forms
**Learning:** The custom `BrutalistTextField` component lacked keyboard configuration (IME actions, Input types), forcing users to manually dismiss keyboards or use wrong layouts (text instead of number).
**Action:** Always expose `KeyboardOptions` and `KeyboardActions` in custom input wrappers to ensure native Android keyboard features (Next, Done, Number pad) work correctly.

## 2026-02-01 - Shared Component Fragmentation
**Learning:** Core UI components like `BrutalistTextField` were defined inside specific screens (`AddHabitScreen`) but used globally, causing inconsistent behavior (missing keyboard actions) across the app.
**Action:** When modifying shared components found in specific screens, treat them as library components—enhance them with default parameters (`enabled`, `minLines`) to support new use cases without breaking existing ones.

## 2026-02-02 - Custom Progress Bar Semantics
**Learning:** Custom progress indicators built with `Box`/`Row` (like `BrutalistProgressBar`) have no native accessibility role, making them invisible to screen readers.
**Action:** Apply `Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(...) }` to the parent container of custom progress components to announce values correctly.
