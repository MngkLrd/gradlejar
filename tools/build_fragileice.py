#!/usr/bin/env python3
"""Generate Fragile Ice from this repository's eleven Silent Creepers Gradle templates."""
import json
import shutil
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
MODID="fragileice"
DESCRIPTION="Thin ice cracks under your weight. Sprinting is dangerous; sneaking is safer."
JAVA=r'''package dev.minemods.fragileice.mixin;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique private ServerLevel fragileice$level;
    @Unique private BlockPos fragileice$pos;
    @Unique private int fragileice$load;
    @Unique private int fragileice$limit;
    @Unique private int fragileice$stage = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void fragileice$tick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        ServerLevel level = (ServerLevel)player.level();
        int marker = -player.getId() - 1;
        if (!player.isAlive() || player.isCreative() || player.isSpectator()
                || !player.onGround() || player.isPassenger()) {
            fragileice$clear(marker);
            return;
        }
        BlockPos pos = player.blockPosition().below();
        // Only a single layer of ordinary ice over water is fragile.
        if (!level.getBlockState(pos).is(Blocks.ICE)
                || !level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            fragileice$clear(marker);
            return;
        }
        if (fragileice$level != level || !pos.equals(fragileice$pos)) {
            fragileice$clear(marker);
            fragileice$level = level;
            fragileice$pos = pos.immutable();
            fragileice$limit = ThreadLocalRandom.current().nextInt(16, 65);
        }
        // Sneaking distributes the load; standing still eventually breaks thin ice.
        fragileice$load += player.isShiftKeyDown() ? 1 : player.isSprinting() ? 12 : 4;
        int stage = Math.min(9, fragileice$load * 10 / fragileice$limit);
        if (stage != fragileice$stage) {
            level.destroyBlockProgress(marker, pos, stage);
            if (fragileice$stage < 0 || stage / 3 > fragileice$stage / 3) {
                level.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.55F, 0.8F);
            }
            fragileice$stage = stage;
        }
        if (fragileice$load >= fragileice$limit) {
            fragileice$clear(marker);
            if (level.getBlockState(pos).is(Blocks.ICE) && level.destroyBlock(pos, false)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
        }
    }

    @Unique private void fragileice$clear(int marker) {
        if (fragileice$level != null && fragileice$pos != null) {
            fragileice$level.destroyBlockProgress(marker, fragileice$pos, -1);
        }
        fragileice$level = null;
        fragileice$pos = null;
        fragileice$load = 0;
        fragileice$stage = -1;
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
        if f.exists(): f.write_text(f.read_text().replace('silentcreepers',MODID).replace('Silent Creepers','Fragile Ice'))
    java=target/'src/main/java'
    shutil.rmtree(java)
    pkg=java/'dev/minemods/fragileice'
    (pkg/'mixin').mkdir(parents=True)
    (pkg/'mixin/ServerPlayerMixin.java').write_text(JAVA)
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
    mix['mixins']=['ServerPlayerMixin']
    if mc=='1.20.1' and loader!='fabric': mix['refmap']='fragileice.refmap.json'
    (resources/'fragileice.mixins.json').write_text(json.dumps(mix,indent=2))
    if loader=='fabric':
        meta=json.loads((original/'fabric.mod.json').read_text().replace('silentcreepers',MODID).replace('SilentCreepers','FragileIce'))
        meta.update(name='Fragile Ice',description=DESCRIPTION)
        (resources/'fabric.mod.json').write_text(json.dumps(meta,indent=2))
    else:
        path='META-INF/mods.toml' if mc=='1.20.1' else 'META-INF/neoforge.mods.toml'
        meta=(original/path).read_text().replace('silentcreepers',MODID).replace('Silent Creepers','Fragile Ice')
        meta='\n'.join('description="'+DESCRIPTION+'"' if line.startswith('description=') else line for line in meta.splitlines())+'\n'
        (resources/'META-INF').mkdir()
        (resources/path).write_text(meta)
    (target/'README.md').write_text('# Fragile Ice\nServer-side thin ice pressure; ordinary ice over water only.\n')
    print(target)

if __name__=='__main__': generate(*sys.argv[1:])
