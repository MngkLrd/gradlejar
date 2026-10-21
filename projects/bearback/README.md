# Bearback 0.1.1 (NeoForge 1.21.1)

Adds black bears and allows taming, saddling and riding polar bears and black bears. No dependency on Rideable Polar Bears. Java 21.

## Mechanics
- Feed an adult wild bear cod/salmon (25% chance per fish). Hearts mean it is tamed.
- Use a vanilla saddle on an owned bear. Right click with empty hand to ride.
- WASD to ride, Space to jump, R to perform a powerful paw swipe while mounted (1.5s cooldown).
- Owned bears defend against attackers of their owner and follow when the owner moves away.
- Heal by feeding fish. Sneak + shears remove saddle.
- The black bear reuses vanilla polar bear model/animations; charcoal fur and a beige chest marking are generated from the installed game's vanilla skin, with no bundled Mojang textures.
- Black bears spawn in forest/taiga biomes, or use the creative spawn egg.

## Build
Java 21, Gradle 9.2.1: `gradle clean build`.
JAR: `build/libs/bearback-1.21.1-0.1.1.jar`

## Testing
- New world spawn of black bear; `/summon bearback:black_bear`.
- Tame both bear species; saddle, mount, dismount, jump, fight.
- Test visibility of saddle, wolf and hostile mob defense, multiplayer ownership and game reload.
- Dedicated server launch (client code isolated).

## Known limitations
- A dedicated leather saddle is made of fitted 3D cubes and inherits the vanilla torso animation. Final fit must be tested in the game.
- Bear movement uses authoritative server ride packets and may need smoothing in high-latency multiplayer.
- Attack while mounted is keyed to R; vanilla paw raising is used for the animation.
- Existing RideablePolarBears is not required or integrated.

### CI
This module has its own build-profile.json and is selectable via project_dir=projects/bearback.
