package dev.minemods.treechopper;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only server-thread tree cutting contract. No forced chunk loading. */
public final class TreeChopperApi {
    public static final int API_VERSION=1, API_REVISION=1;
    private TreeChopperApi(){}
    public record CutSnapshot(int north,int south,int west,int east,int totalPermille,boolean severed){}
    public enum LooseKind { TWIG, PEBBLE, FLINT }
    /** Source substrate or tree, collectible item ID, and nominal material volume in cubic metres.
     * Pebbles use the source's exact Core composition with scaled mass, not a local hardness table.
     * NaN volume means the provider has not assigned a nominal volume. */
    public record LooseFind(LooseKind kind,ResourceLocation sourceBlock,ResourceLocation yieldItem,double volumeM3){}
    public static Optional<LooseFind> looseFind(BlockState state){
        if(state.getBlock() instanceof TwigBlock twig){
            var original=TreeChopper.original(twig.species);
            if(original.isAir())return Optional.empty();
            return Optional.of(new LooseFind(LooseKind.TWIG,BuiltInRegistries.BLOCK.getKey(original.getBlock()),BuiltInRegistries.ITEM.getKey(Items.STICK),Double.NaN));
        }
        if(state.getBlock() instanceof PebbleBlock rock)return Optional.of(new LooseFind(rock.flint?LooseKind.FLINT:LooseKind.PEBBLE,rock.source,
            BuiltInRegistries.ITEM.getKey(rock.collectedStack().getItem()),rock.flint?LooseRocks.FLINT_VOLUME:LooseRocks.PEBBLE_VOLUME));
        return Optional.empty();
    }
    public static Optional<LooseFind> looseFind(ServerLevel l,BlockPos pos){
        if(!l.getServer().isSameThread()||!l.hasChunkAt(pos)||l.isOutsideBuildHeight(pos))return Optional.empty();
        return looseFind(l.getBlockState(pos));
    }
    /** Supported substrate -> exact matching loose block. Empty for dirt, sand, worked masonry and unsupported materials. Grass generation uses nearby underlying raw stone identity, not this exact-substrate mapping. */
    public static Optional<BlockState> rockForSurface(BlockState state){return Optional.ofNullable(LooseRocks.forSurface(state));}
    public static boolean requiresAxe(BlockState state){return TreeChopper.requiresAxe(state);}
    public static Optional<CutSnapshot> cut(ServerLevel l,BlockPos pos){
        if(!l.getServer().isSameThread()||!l.hasChunkAt(pos)||l.isOutsideBuildHeight(pos))return Optional.empty();
        var state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof CutBlock))return Optional.empty();
        int[] w=CutProgress.read(l,pos,state);
        return Optional.of(new CutSnapshot(w[0],w[1],w[2],w[3],ChopEffort.progress(w),ChopEffort.severed(w)));
    }
}
