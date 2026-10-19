package dev.minemods.redstonevision.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CircuitScanner {
    private static final int LIMIT=480;
    private static final TagKey<Block> COMPONENT_TAG=TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("redstonevision","components"));
    private static final List<Hit> CACHE=new ArrayList<>();
    private static int ticks=0;
    private CircuitScanner() {}
    public record Hit(BlockPos pos, BlockState state) {}
    public static List<Hit> hits() { return CACHE; }
    public static void tick(Minecraft mc) {
        if (!VisionState.active()) { CACHE.clear();ticks=0;return; }
        if (++ticks%10!=0) return;
        CACHE.clear();
        var level=mc.level;
        var origin=mc.player.blockPosition();
        int range=VisionState.RANGE;
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        outer: for (int dy=-range;dy<=range;dy++)
            for (int dx=-range;dx<=range;dx++)
                for (int dz=-range;dz<=range;dz++) {
                    if (dx*dx+dy*dy+dz*dz>range*range) continue;
                    pos.set(origin.getX()+dx,origin.getY()+dy,origin.getZ()+dz);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    BlockState state=level.getBlockState(pos);
                    if (isCircuit(state)) {
                        CACHE.add(new Hit(pos.immutable(),state));
                        if (CACHE.size()>=LIMIT) break outer;
                    }
                }
    }
    public static boolean isCircuit(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(COMPONENT_TAG)) return true;
        String path=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("redstone_wire")||path.contains("redstone_cable")
            ||path.contains("repeater")||path.contains("comparator")
            ||path.contains("logic_gate")||path.contains("redstone_torch")
            ||path.contains("redstone_lamp")||path.contains("redstone_relay");
    }
    public static int signal(BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWER)) return state.getValue(BlockStateProperties.POWER);
        if (state.hasProperty(BlockStateProperties.POWERED)) return state.getValue(BlockStateProperties.POWERED)?15:0;
        if (state.hasProperty(BlockStateProperties.LIT)) return state.getValue(BlockStateProperties.LIT)?15:0;
        return -1;
    }
}
