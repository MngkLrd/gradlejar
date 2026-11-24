# Campfire Batch 1.0.0
Minecraft 1.21.1 / NeoForge / Java 21. Install on client and server.
Normal right click is untouched. Sneak-right-click a lit regular or soul campfire
to place min(free slots, held count) items. Creative does not consume the stack.
No custom recipes, timers, data, assets, networking or mixins.
NeoForge's block-use phase respects earlier item hooks; vanilla placeFood handles
individual items and synchronizes the block entity. No manual inventory decrement.
The interaction result is sent once; the normal campfire game event still occurs for
each vanilla insertion. A successful action prevents the standard offhand fallback.
Build: gradle --no-daemon clean build (Java 21, Gradle 9.2.1).
Output: build/libs/campfirebatch-1.0.0.jar.

Manual acceptance tests: normal click, empty and partial campfires, mixed foods,
one/two/three-item stacks, creative with one or four items, offhand, invalid recipe,
unlit/waterlogged/full fire, datapack nonfood recipe with custom cook time, component
preservation, save/reload, two players racing and breaking the campfire.
Test both dedicated server and integrated singleplayer. No runtime test claimed.
