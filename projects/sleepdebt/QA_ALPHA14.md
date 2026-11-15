# Alpha 14 | Targeting + Texture QA

## Reproducible hunt test
1. /gamemode survival
2. /time set midnight
3. /sleepdebt debug preset critical
4. /sleepdebt debug spawn_sleepless
5. Move away and make eye contact. Sleepless must resume movement after
   a short observation pause; no infinite freeze.
6. Watch from 20 to 3 blocks. It should stalk, flank, rush, occasionally
   relocate to the rear on a loaded, collision-safe position, and attack.
7. Run the same in Creative: the debug mob should visibly pursue and
   fog should apply, but not damage the invulnerable player.
8. Run a NATURAL encounter. A newly respawned player with grace should not
   be targeted. No hunter remains by day or after a successful bed entry.
9. In caves with only 2 blocks of clearance, no head-through-ceiling
   natural spawn or teleport.
10. Use /sleepdebt debug why_no_sleepless to inspect threshold, Creative
    mode, grace and day/night conditions.

## Visual QA
11. HUD texture is 158x68. Look for walnut frame with stepped brass inlays,
    three separate status wells, neat corners and no distracting 1px noise.
    Text (native font) at x+12,y+8 and rows 26,39,52 must not collide.
12. Coffee screen is 176x166: slots align with x=28,55,82,136 and
    inventory x=8+18*n, y=84,102,120,142. Progress is x+109..131.
13. Sleep Journal 228x188: all 3 data bars and final forecast text
    must stay on quiet parchment, readable in RU/EN at GUI scale 2,3,4.
14. Creative mode: Sleep Debt tab is before Tools & Utilities, contains
    all 29 items once with no injection into vanilla tabs.
15. View all 15 block material textures on 3D furniture models, 8 growth
    stages and 22 icons at native resolution. Transparent lantern/flame/net
    gaps stay open. Compare item variants for visual uniqueness.
16. Validate dedicated server startup, optional JEI/EMI/Jade and an old save.

## Known limits
CI checks compilation, PNG validity, item coverage and art constraints.
It cannot assert emotional horror impact, pathfinding in all modded terrain,
shader fog compatibility, or readability across all resource packs.
