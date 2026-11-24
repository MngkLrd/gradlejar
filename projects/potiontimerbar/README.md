# Potion Timer Bar 0.4.0

Minecraft 1.21.1 NeoForge, Java 21. Client only.

- One 81x5 GUI-pixel timer per active status effect, stacked upwards above food with 1px gaps.
- Actual vanilla XP background sprite; recolored pixel fill with subtle divisions and a stronger gradient.
- Longest remaining duration at the bottom, stable registry-key tiebreak. Infinite effects remain full.
- Potion-specific colors; harmful red, beneficial green and neutral olive fallbacks.
- No effects: no HUD. Air-bubble avoidance and vertical offset retained.
- Remaining duration is relative to first client observation.

Build: `gradle --no-daemon clean build` (Java 21, Gradle 9.2.1).
Output: `build/libs/potiontimerbar-0.4.0.jar`.

QA: multiple effects, expiration, infinite effects, refresh, GUI scales, underwater, F1, multiplayer. In-game tests required.
