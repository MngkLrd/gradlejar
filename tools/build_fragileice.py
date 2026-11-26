#!/usr/bin/env python3
"""Generate Fragile Ice from this repository's eleven Silent Creepers Gradle templates."""
import json
import shutil
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MODID="fragileice"
VERSION="1.0.1"
DESCRIPTION="Ordinary ice cracks under your weight and jumping can break it."
JAVA=r'''package dev.minemods.fragileice.mixin;
import dev.minemods.fragileice.IceStress;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Unique private final IceStress fragileice$stress = new IceStress();
    @Inject(method = "tick", at = @At("TAIL"))
    private void fragileice$tick(BooleanSupplier time, CallbackInfo ci) {
        fragileice$stress.tick((ServerLevel)(Object)this);
    }
}
'''
CORE=r'''package dev.minemods.fragileice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class IceStress {
    private static final int LIMIT = 40;
    private static final int RETAIN = 60;
    private static final DustParticleOptions CRACK = @DUST@;
    private final Map<BlockPos, Cell> cells = new HashMap<>();
    private Map<Integer, Motion> previous = new HashMap<>();
    private int nextMarker = 1500000000;
    private long clock;
    private record Motion(Vec3 pos, double peak, boolean airborne) {}
    private static final class Cell {
        final int marker;
        int load, stage = -1;
        long lastContact, fullSince = -1;
        Cell(int marker, long tick) { this.marker = marker; lastContact = tick; }
    }

    public void tick(ServerLevel level) {
        ++clock;
        Map<Integer, Motion> current = new HashMap<>();
        Map<BlockPos, Integer> pressure = new HashMap<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()
                    || player.isPassenger() || player.getAbilities().flying) continue;
            Vec3 now = player.position();
            Motion before = previous.get(player.getId());
            double feet = player.getBoundingBox().minY;
            BlockPos pos = BlockPos.containing(now.x, feet - 0.07, now.z);
            double gap = feet - (pos.getY() + 1.0);
            boolean iceContact = gap >= -0.025 && gap <= 0.07 && level.getBlockState(pos).is(Blocks.ICE);
            double dx = before == null ? 0 : now.x - before.pos().x;
            double dy = before == null ? 0 : now.y - before.pos().y;
            double dz = before == null ? 0 : now.z - before.pos().z;
            boolean teleported = dx*dx + dy*dy + dz*dz > 64;
            if (teleported) before = null;
            boolean airborne = !player.onGround() && !iceContact;
            double peak = before != null && before.airborne() ? Math.max(before.peak(), now.y) : now.y;
            if (airborne && before != null) peak = Math.max(peak, before.pos().y);
            current.put(player.getId(), new Motion(now, airborne ? peak : now.y, airborne));
            if (!iceContact) continue;
            boolean landed = before != null && before.airborne() && dy < -0.015
                    && before.peak() - now.y >= 0.28;
            boolean moving = dx*dx + dz*dz > 0.0009;
            int weight = landed ? LIMIT : player.isShiftKeyDown() ? 1 :
                    player.isSprinting() ? 16 : moving ? 8 : 2;
            pressure.merge(pos.immutable(), weight, (a,b) -> Math.min(LIMIT, a+b));
        }
        previous = current;
        for (Map.Entry<BlockPos,Integer> entry : pressure.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.getBlockState(pos).is(Blocks.ICE)) continue;
            Cell cell = cells.computeIfAbsent(pos, p -> new Cell(nextMarker++, clock));
            cell.lastContact = clock;
            // Leave a visible warning, and only collapse while actually occupied.
            if (cell.fullSince >= 0 && clock - cell.fullSince >= 3) {
                level.destroyBlockProgress(cell.marker, pos, -1);
                boolean water = level.getFluidState(pos.below()).is(FluidTags.WATER);
                if (level.destroyBlock(pos, false) && water)
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                cells.remove(pos);
                continue;
            }
            cell.load = Math.min(LIMIT, cell.load + entry.getValue());
            if (cell.load == LIMIT && cell.fullSince < 0) cell.fullSince = clock;
            int stage = Math.min(9, cell.load * 10 / LIMIT);
            if (stage != cell.stage) {
                if (cell.stage < 0 || stage / 3 > cell.stage / 3)
                    level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8F, 0.65F + stage * 0.045F);
                cell.stage = stage;
                level.destroyBlockProgress(cell.marker, pos, stage);
                showCracks(level, pos, stage);
            } else if (clock % 4 == 0) {
                level.destroyBlockProgress(cell.marker, pos, stage);
                showCracks(level, pos, stage);
            }
        }
        Iterator<Map.Entry<BlockPos,Cell>> it = cells.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos,Cell> e = it.next();
            if (pressure.containsKey(e.getKey())) continue;
            Cell cell = e.getValue();
            if (clock - cell.lastContact > RETAIN) {
                level.destroyBlockProgress(cell.marker, e.getKey(), -1);
                it.remove();
            } else if (clock % 8 == 0 && level.getBlockState(e.getKey()).is(Blocks.ICE)) {
                level.destroyBlockProgress(cell.marker, e.getKey(), cell.stage);
                showCracks(level, e.getKey(), cell.stage);
            }
        }
    }

    // Particle fallback for transparent ice; stationary dark blue branching trace.
    private static void showCracks(ServerLevel level, BlockPos pos, int stage) {
        double[][] lines = {
            {0.09,0.19,0.33,0.37},{0.33,0.37,0.48,0.49},
            {0.48,0.49,0.68,0.62},{0.68,0.62,0.92,0.83},
            {0.48,0.49,0.54,0.20},{0.54,0.20,0.72,0.09},
            {0.68,0.62,0.52,0.82},{0.52,0.82,0.30,0.94},
            {0.33,0.37,0.17,0.56},{0.68,0.62,0.88,0.48}
        };
        int segments = Math.min(lines.length, 2 + stage);
        for (int i=0; i<segments; i++) {
            double[] l = lines[i];
            for (int j=0; j<5; j++) {
                double t=j/4.0;
                double x=pos.getX()+l[0]+(l[2]-l[0])*t;
                double z=pos.getZ()+l[1]+(l[3]-l[1])*t;
                level.sendParticles(CRACK, x, pos.getY()+1.025, z, 1, 0, 0, 0, 0);
            }
        }
    }
}
'''

def generate(mc,loader):
    source=ROOT/'projects'/f'silentcreepers-{loader}-{mc}'
    target=ROOT/'build'/f'fragileice-{loader}-{mc}'
    if target.exists(): shutil.rmtree(target)
    shutil.copytree(source,target,ignore=shutil.ignore_patterns('build','.gradle','.idea'))
    for name in ('settings.gradle','build.gradle','gradle.properties','build-profile.json'):
        f=target/name
        if f.exists(): f.write_text(f.read_text().replace('silentcreepers',MODID).replace('Silent Creepers','Fragile Ice').replace('1.0.0',VERSION))
    java=target/'src/main/java'
    shutil.rmtree(java)
    pkg=java/'dev/minemods/fragileice'
    (pkg/'mixin').mkdir(parents=True)
    (pkg/'mixin/ServerLevelMixin.java').write_text(JAVA)
    dust='new DustParticleOptions(0x173D55, 0.65F)' if mc.startswith('26.') else 'new DustParticleOptions(new org.joml.Vector3f(0.09F, 0.24F, 0.33F), 0.65F)'
    (pkg/'IceStress.java').write_text(CORE.replace('@DUST@',dust))
    if loader=='fabric':
        entry='package dev.minemods.fragileice;\nimport net.fabricmc.api.ModInitializer;\npublic final class FragileIce implements ModInitializer { public void onInitialize() {} }\n'
    else:
        api='net.minecraftforge' if mc=='1.20.1' else 'net.neoforged'
        entry=f'package dev.minemods.fragileice;\nimport {api}.fml.common.Mod;\n@Mod("fragileice") public final class FragileIce {{ public FragileIce() {{}} }}\n'
    (pkg/'FragileIce.java').write_text(entry)
    resources=target/'src/main/resources'
    shutil.rmtree(resources)
    resources.mkdir(parents=True)
    original=source/'src/main/resources'
    pack=original/'pack.mcmeta'
    data=json.loads(pack.read_text())
    data['pack']['description']='Fragile Ice resources'
    (resources/'pack.mcmeta').write_text(json.dumps(data,indent=2))
    mix=json.loads((original/'silentcreepers.mixins.json').read_text().replace('silentcreepers',MODID))
    mix['mixins']=['ServerLevelMixin']
    if mc=='1.20.1' and loader!='fabric': mix['refmap']='fragileice.refmap.json'
    (resources/'fragileice.mixins.json').write_text(json.dumps(mix,indent=2))
    if loader=='fabric':
        meta=json.loads((original/'fabric.mod.json').read_text().replace('silentcreepers',MODID).replace('SilentCreepers','FragileIce'))
        meta.update(name='Fragile Ice',description=DESCRIPTION,version=VERSION)
        (resources/'fabric.mod.json').write_text(json.dumps(meta,indent=2))
    else:
        path='META-INF/mods.toml' if mc=='1.20.1' else 'META-INF/neoforge.mods.toml'
        meta=(original/path).read_text().replace('silentcreepers',MODID).replace('Silent Creepers','Fragile Ice')
        meta=meta.replace('1.0.0',VERSION)
        meta='\n'.join('description="'+DESCRIPTION+'"' if line.startswith('description=') else line for line in meta.splitlines())+'\n'
        (resources/'META-INF').mkdir()
        (resources/path).write_text(meta)
    (target/'README.md').write_text('# Fragile Ice\nServer-side ordinary ice pressure and jump impact; visible crack particle fallback.\n')
    print(target)

if __name__=='__main__': generate(*sys.argv[1:])
