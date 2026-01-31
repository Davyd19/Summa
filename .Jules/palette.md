## 2026-01-31 - Keyboard Interaction in Brutalist Forms
**Learning:** The custom `BrutalistTextField` component lacked keyboard configuration (IME actions, Input types), forcing users to manually dismiss keyboards or use wrong layouts (text instead of number).
**Action:** Always expose `KeyboardOptions` and `KeyboardActions` in custom input wrappers to ensure native Android keyboard features (Next, Done, Number pad) work correctly.
