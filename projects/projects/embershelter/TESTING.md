# Ember Shelter 1.0.1 test plan

1. Check Camp Bundle + Camp Kit recipes in JEI and crafting table, and confirm both icons in inventory.
2. On flat dirt, grass, under tree cover and on flat stone in cave, activate Camp Kit. Confirm tree logs/leaves/grass are cleared within 7x7x4; all 7x7 floor stays intact.
3. On waterlogged block, underwater and above water: placement rejected with no kit loss.
4. With solid stone, chest, bed or machine occupying airspace: reject placement without destroying any block.
5. In survival have NO extra resources; after crafting Kit, deployment succeeds and only Kit is used.
6. Zombie, Skeleton, Spider, Creeper stop targeting player inside finished lit camp at night; daytime and extinguished camp do not protect.
7. Player hits zombie/skeleton/creeper: struck creature retaliates. Other creatures remain calm.
8. Hit a cow, pig or sheep: it briefly chases and attacks you; otherwise passive.
9. Wither, Wither Skeleton, Ender Dragon, Warden, Witch, raid mobs stay hostile.
10. Restart server after completed camp and test persistence; check dedicated server classloading.
