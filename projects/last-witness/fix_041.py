from pathlib import Path
p=Path(__file__).resolve().parent
f=p/'src/main/java/dev/minemods/lastwitness/guard/GuardPatrols.java'
s=f.read_text(encoding='utf8')
def rep(old,new):
    global s
    if old not in s: raise RuntimeError('Missing expected GuardPatrols pattern: '+old[:90])
    s=s.replace(old,new,1)
rep('if (tick % 400 != 0) return;', 'if (tick % 100 != 0) return;')
rep('player.getBoundingBox().inflate(32)', 'player.getBoundingBox().inflate(54, 28, 54)')
rep('if (nearby.size() < 2) continue;\n            BlockPos bell = BellFinder.find(level, nearby.get(0));\n            if (bell == null || !level.hasChunkAt(bell)) continue;\n            String key = level.dimension().location() + ":" + bell.asLong();', '''if (nearby.isEmpty()) continue;
            BlockPos bell = null;
            for (int i = 0; i < Math.min(12, nearby.size()); i++) {
                bell = BellFinder.find(level, nearby.get(i));
                if (bell != null) break;
            }
            if (nearby.size() < 2 && bell == null) continue;
            long x = 0, y = 0, z = 0;
            for (Villager villager : nearby) {
                BlockPos pos = villager.blockPosition();
                x += pos.getX(); y += pos.getY(); z += pos.getZ();
            }
            BlockPos center = new BlockPos((int) Math.floorDiv(x, nearby.size()),
                    (int) Math.floorDiv(y, nearby.size()),
                    (int) Math.floorDiv(z, nearby.size()));
            BlockPos home = bell != null ? bell : center;
            if (!level.hasChunkAt(home)) home = nearby.get(0).blockPosition();
            String key = level.dimension().location() + ":" +
                    Math.floorDiv(home.getX(), 24) + ":" + Math.floorDiv(home.getZ(), 24);''')
rep('NEXT_CHECK.put(key, tick + 600L); // 30s scan; no per-tick expensive search.',
    'NEXT_CHECK.put(key, tick + 100L); // 5s per area, no forced chunks.')
rep('new AABB(bell).inflate(44)', 'new AABB(home).inflate(48, 20, 48)')
rep('Math.max(2, 1 + nearby.size() / 5)', 'Math.max(2, 1 + nearby.size() / 4)')
rep('BlockPos space = safePosition(level, bell);\n            if (space == null) continue;', '''BlockPos space = safePosition(level, home);
            if (space == null) {
                for (Villager villager : nearby) {
                    space = safePosition(level, villager.blockPosition());
                    if (space != null) break;
                }
            }
            if (space == null) continue;''')
rep('guard.stationAt(bell);', 'guard.stationAt(home);')
rep('for (int radius = 2; radius <= 8; radius++)', 'for (int radius = 1; radius <= 10; radius++)')
rep('for (int i = 0; i < 12; i++)', 'for (int i = 0; i < 16; i++)')
rep('i * Math.PI / 6.0 + radius', 'i * Math.PI / 8.0 + radius')
rep('for (int dy = -2; dy <= 2; dy++)', 'for (int dy = -7; dy <= 5; dy++)')
rep('if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below())',
    'if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below()) || !level.getBlockState(pos.below())')
rep('    public static void protectNearbyVillagers(ServerPlayer attacker, BlockPos scene) {',
'''    public static String diagnostics(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(54, 28, 54),
                v -> v.isAlive() && !v.isBaby());
        BlockPos bell = null;
        for (int i = 0; i < Math.min(12, nearby.size()); i++) {
            bell = BellFinder.find(level, nearby.get(i));
            if (bell != null) break;
        }
        int guards = level.getEntitiesOfClass(GuardEntity.class,
                player.getBoundingBox().inflate(70), GuardEntity::isAlive).size();
        return "Guards enabled=" + WitnessConfig.ENABLE_GUARDS.get()
                + ", nearby adult villagers=" + nearby.size()
                + ", bell=" + (bell == null ? "not found (fallback active)" : bell.toShortString())
                + ", guards=" + guards + ", cap=" + WitnessConfig.PATROL_MAX.get()
                + ". Scan every 5 seconds.";
    }

    public static void protectNearbyVillagers(ServerPlayer attacker, BlockPos scene) {''')
f.write_text(s,encoding='utf8')
mod=p/'src/main/java/dev/minemods/lastwitness/LastWitnessMod.java'
s=mod.read_text(encoding='utf8')
needle='NeoForge.EVENT_BUS.register(new CrimeEvents());'
if needle not in s: raise RuntimeError('Missing event bus registration')
mod.write_text(s.replace(needle,needle+'\n        NeoForge.EVENT_BUS.addListener(dev.minemods.lastwitness.guard.GuardCommands::register);'),encoding='utf8')
for path in (p/'README.md',p/'src/main/resources/META-INF/neoforge.mods.toml',p/'gradle.properties',p/'build-profile.json'):
    path.write_text(path.read_text(encoding='utf8').replace('0.4.0','0.4.1'),encoding='utf8')
with (p/'README.md').open('a',encoding='utf8') as out:
    out.write('\n## 0.4.1 fixed village guards\n\nScans every five seconds. Multiple villagers checked for bells; inhabited CTOV and vanilla settlements can spawn guards without a bell memory. Existing worlds supported. Operators can check /lastwitness guards.\n')
