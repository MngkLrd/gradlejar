# Combat animations (optional integration)

Norse Gear 0.3.0 ships with **seven native Better Combat JSON weapon profiles** in `src/main/resources/data/norsegear/weapon_attributes/`.

Compatible baseline: Minecraft 1.21.1, NeoForge 21.1.x, Better Combat NeoForge **2.4.0+1.21.1**. Follow Better Combat's installation instructions for its player-animation dependency. No Better Combat classes are linked into the Norse Gear JAR, so the base mod runs when it is absent.

| Norse Gear item | Better Combat animation preset | Vanilla fallback |
| --- | --- | --- |
| Seax | `bettercombat:dagger` | sword attack |
| Bearded axe | `bettercombat:axe` | axe attack |
| Dane axe | `bettercombat:heavy_axe` | axe attack |
| Viking sword | `bettercombat:sword` | sword attack |
| Spear | `bettercombat:spear` | sword melee + hold/release RMB for throw |
| Javelin | `bettercombat:spear` | hold/release RMB for throw |
| Throwing axe | `bettercombat:axe` | hold/release RMB for throw |

Better Combat supplies attack-combo animations if installed. Norse Gear remains responsible for projectile charge, release, damage and recovery. It does not add new first-person custom attack animations or modify Better Combat's timing globally. Projectile spin uses the existing client renderer and does not require Better Combat.

**Compatibility limitations:** This integration uses Better Combat's public JSON resource extension, not its Java combat API. The combined setup still requires an in-game client test, because a Gradle/server smoke test with only Norse Gear cannot certify third-party animations. Weapons with a right-click throw might need player-animation pose refinements in some modpacks.

Sources: https://www.curseforge.com/minecraft/mc-mods/better-combat-by-daedelus/files/8430183 and https://github.com/ZsoltMolnarrr/BetterCombat/tree/1.21.1/common/src/main/resources/data/bettercombat/weapon_attributes
