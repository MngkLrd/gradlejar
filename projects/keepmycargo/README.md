# Keep My Cargo 1.0.0
Minecraft 1.21.1 / NeoForge / Java 21. No external mods required.
Run gradle clean build (Gradle 8.14.3); output build/libs/keepmycargo-1.0.0.jar.
Vanilla chest boats (all woods including bamboo chest raft), chest and hopper minecarts.
Survival damage with entity drops enabled packs contents, preserving slot indices and custom name. Empty vehicles remain vanilla. Placement from hand and dispensers restores slots. Packed cargo has maximum stack size 1. Nesting is denied in vanilla cargo slots; if automation puts nested cargo into a vehicle, breaking falls back to vanilla drops. Creative deletion, fall destruction, and doEntityDrops=false retain vanilla behavior. 
Test each vehicle with named/enchanted items, sparse slots, save and reload, failed placement, dispenser, hopper transfer, full inventory, server and client, no duplicate item entities, and nested cargo fallback. Back up worlds before removing the mod. Runtime tests are not implied by compilation.
