# Catch the Blast 0.2.2-beta
Minecraft 1.21.1 / NeoForge 21.1.1+ / Java 21.
Build: gradle --no-daemon clean build (Gradle 8.14.3).
A custom shield stores explosion damage actually blocked. Charge persists in item data, and four model states use shared geometry. Sneak-right-click releases a cone attack, no terrain damage. Charge above 100% must be released within 70 ticks. Vanilla shaped crafting is visible in JEI if installed. No dependencies.
Test: obtain shield from Combat tab or craft; face creeper while blocking; inspect tooltip; release at mobs; test rear attacks, overload and offhand, relog, dedicated server.

0.2.0: native shield renderer, fiery travelling wave, protected terrain carving (configurable), HUD and ghast fireball capture. Terrain damage respects normal block break permissions; block entities, fluids and high-resistance blocks are excluded.

0.2.1: charge-sensitive pixelated metal rim and emissive accents; HUD visible only during overload. Operator commands: /catchtheblast terrain [true|false] (omit value to show setting). Setting is saved to the world server config.

0.2.2: thin XP-style charge bar under the crosshair, visible only for a held shield with nonzero charge. Overload adds a matching countdown bar and state-bound warning; no lingering overload actionbar message after the shield breaks.
