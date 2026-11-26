from pathlib import Path
import json
import sys

root=Path(__file__).resolve().parents[1]
matrix=[('forge','1.20.1')]+[(loader,mc) for loader in ('neoforge','fabric') for mc in ('1.20.1','1.21.1','26.1.2','26.2','26.3')]
java_code="""package dev.minemods.burningarrows.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import ARROW_PACKAGE.AbstractArrow;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class ArrowMixin {
    GROUND_SHADOW
    @Unique private Vec3 burningarrows$start;

    @Inject(method = "tick", at = @At("HEAD"))
    private void burningarrows$begin(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;
        burningarrows$start = null;
        if (!(arrow.level() instanceof ServerLevel) || arrow.getType() == EntityType.TRIDENT
                || GROUND_CHECK || arrow.isNoPhysics()) return;
        burningarrows$start = arrow.position();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void burningarrows$end(CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;
        if (burningarrows$start != null) burningarrows$trace(arrow, arrow.position());
        burningarrows$start = null;
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void burningarrows$entityHit(EntityHitResult hit, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;
        if (burningarrows$start != null) burningarrows$trace(arrow, hit.getLocation());
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"))
    private void burningarrows$blockHit(BlockHitResult hit, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;
        if (burningarrows$start == null || !(arrow.level() instanceof ServerLevel level)) return;
        burningarrows$trace(arrow, hit.getLocation());
        if (arrow.getRemainingFireTicks() <= 0 || arrow.isInWater()) return;
        BlockPos struck = hit.getBlockPos();
        BlockPos target = struck.relative(hit.getDirection());
        if (!level.hasChunkAt(struck) || !level.hasChunkAt(target)) return;
        if (arrow.getOwner() instanceof Player p && (!level.mayInteract(p, struck) || !level.mayInteract(p, target))) return;
        BlockState state = level.getBlockState(struck);
        if (!(FLAMMABLE)) return;
        if (!level.getBlockState(target).isAir() || !level.getFluidState(target).isEmpty()) return;
        BlockState fire = BaseFireBlock.getState(level, target);
        if (fire.canSurvive(level, target)) level.setBlockAndUpdate(target, fire);
    }

    @Unique
    private void burningarrows$trace(AbstractArrow arrow, Vec3 end) {
        if (!(arrow.level() instanceof ServerLevel level) || burningarrows$start == null) return;
        Vec3 start = burningarrows$start;
        burningarrows$start = end;
        double length = start.distanceTo(end);
        if (!Double.isFinite(length) || length > 64.0) return;
        int steps = Math.max(1, (int)Math.ceil(length * 8.0));
        BlockPos last = null;
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = start.lerp(end, (double)i / steps);
            BlockPos pos = BlockPos.containing(sample.x, sample.y, sample.z);
            if (pos.equals(last)) continue;
            last = pos;
            if (!level.hasChunkAt(pos)) break;
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                arrow.clearFire();
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(state)
                    || level.getFluidState(pos).is(FluidTags.LAVA)) {
                arrow.setRemainingFireTicks(Math.max(arrow.getRemainingFireTicks(), 100));
            }
        }
    }
}
"""
invoker="""package dev.minemods.burningarrows.mixin;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(FireBlock.class)
public interface FireBlockAccessor {
    @Invoker("canBurn")
    boolean burningarrows$canBurn(BlockState state);
}
"""
def write(p,s):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(s,encoding='utf-8')
def dump(p,o):
    write(p,json.dumps(o,indent=2)+'\n')
def generate(loader,mc):
    old=mc=='1.20.1'
    modern=mc.startswith('26.')
    fabric=loader=='fabric'
    version=25 if modern else 17 if old else 21
    source=root/'projects'/f'realisticdrops-{loader}-{mc}'
    dest=root/'projects'/f'burningarrows-{loader}-{mc}'
    resources=dest/'src/main/resources'
    code=dest/'src/main/java/dev/minemods/burningarrows'
    def transform(s):
        return s.replace('realisticdrops','burningarrows').replace('Realistic Drops','Burning Arrows').replace('Thrown items inherit player momentum.','Arrows catch fire in flames and can ignite flammable blocks.')
    for f in ['settings.gradle','build.gradle','build-profile.json','src/main/resources/pack.mcmeta']:
        write(dest/f,transform((source/f).read_text()))
    metadata='fabric.mod.json' if fabric else ('META-INF/mods.toml' if old else 'META-INF/neoforge.mods.toml')
    meta=transform((source/'src/main/resources'/metadata).read_text())
    if not fabric and not old:
        meta+='\n[[mixins]]\nconfig="burningarrows.mixins.json"\n'
    write(resources/metadata,meta)
    if old and not fabric:
        with (dest/'build.gradle').open('a') as f:
            f.write("\ndependencies { annotationProcessor 'org.spongepowered:mixin:0.8.5:processor' }\nmixin { add sourceSets.main, 'burningarrows.refmap.json'; config 'burningarrows.mixins.json' }\njar { manifest.attributes(['MixinConfigs': 'burningarrows.mixins.json']) }\n")
    mixins={"required":True,"minVersion":"0.8","package":"dev.minemods.burningarrows.mixin","compatibilityLevel":"JAVA_"+str(21 if modern else version),"mixins":["ArrowMixin"],"injectors":{"defaultRequire":1}}
    if old and not fabric: mixins['refmap']='burningarrows.refmap.json'
    if fabric:
        mixins['mixins'].append('FireBlockAccessor')
        write(code/'mixin/FireBlockAccessor.java',invoker)
    dump(resources/'burningarrows.mixins.json',mixins)
    arrow=java_code.replace('ARROW_PACKAGE','net.minecraft.world.entity.projectile.arrow' if modern else 'net.minecraft.world.entity.projectile')
    if mc in ('26.2','26.3'):
        arrow=arrow.replace('import net.minecraft.world.entity.EntityType;', 'import net.minecraft.core.registries.BuiltInRegistries;')
        arrow=arrow.replace('arrow.getType() == EntityType.TRIDENT', '"trident".equals(BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType()).getPath())')
    arrow=arrow.replace('GROUND_SHADOW','@Shadow protected abstract boolean isInGround();' if modern else '@Shadow protected boolean inGround;').replace('GROUND_CHECK','isInGround()' if modern else 'inGround')
    arrow=arrow.replace('FLAMMABLE','((FireBlockAccessor)(Object)Blocks.FIRE).burningarrows$canBurn(state)' if fabric else 'state.isFlammable(level, struck, hit.getDirection())')
    write(code/'mixin/ArrowMixin.java',arrow)
    if not fabric:
        pkg='net.minecraftforge.fml.common.Mod' if old else 'net.neoforged.fml.common.Mod'
        write(code/'BurningArrows.java',f'package dev.minemods.burningarrows;\nimport {pkg};\n@Mod("burningarrows")\npublic final class BurningArrows {{}}\n')
    write(dest/'README.md','# Burning Arrows 1.0.0\nServer-side arrow ignition and flammable-block impact.\nBuild: gradle clean build\n')
    return str(dest.relative_to(root))
if __name__=='__main__':
    if len(sys.argv)==3:
        pair=tuple(sys.argv[1:])
        assert pair in matrix,pair
        print(generate(*pair))
    else:
        for pair in matrix: print(generate(*pair))
