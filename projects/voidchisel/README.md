# Void Chisel 1.0.0

NeoForge 1.21.1 | Java 21 | Gradle 9.2.1

Craft with a Nether Star at top center, two netherite ingots on middle-left and middle-right, and two sticks down the center. The chisel removes exactly five bedrock blocks. Bedrock never drops.

Recipe:
```
 . W .
 N S N
 . S .
```
W = Nether Star, N = Netherite Ingot, S = Stick.

### Build

`gradle --no-daemon clean build`
Output: `build/libs/voidchisel-1.21.1-1.0.0.jar`

### Test

In Survival, hold the chisel and left-click vanilla bedrock. One successful removal consumes one of five charges; no drops. Attempting non-bedrock blocks or protected blocks must not consume charges. Test dedicated server, multiplayer, and claims separately. Successful CI compilation does not replace an in-game test.

NeoForge 21.1.197 is used to compile; metadata requires NeoForge 21.1.0 or later. No JEI hard dependency; recipe is standard vanilla crafting.
