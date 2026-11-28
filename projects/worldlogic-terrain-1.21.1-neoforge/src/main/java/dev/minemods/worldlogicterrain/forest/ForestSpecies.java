package dev.minemods.worldlogicterrain.forest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** World representation and shared habitat/physical traits. Climate remains owned by Core. */
public enum ForestSpecies {
    BARK_MOSS("bark_moss",Kind.EPIPHYTE,3,0,1),
    HANGING_LICHEN("hanging_lichen",Kind.EPIPHYTE,5,0,1),
    IVY("climbing_ivy",Kind.VINE,3,0,1),
    VINE("woodland_vine",Kind.VINE,3,0,1),
    ROOT("exposed_root",Kind.ROOT,5,0,.92),
    CONIFER_ROOT("conifer_root",Kind.ROOT,5,0,.92),
    PALE_ROOT("pale_root",Kind.ROOT,5,0,.92),
    NETTLE("stinging_nettle",Kind.THORN,15,.5f,.91),
    THISTLE("wild_thistle",Kind.THORN,15,.5f,.94),
    THORN("thorn_scrub",Kind.THORN,15,1f,.77),
    NEEDLES("needle_litter",Kind.LITTER,1,0,.98),
    BARK("bark_litter",Kind.LITTER,1,0,.98),
    DAMP_LEAVES("damp_leaf_litter",Kind.LITTER,1,0,.97);
    public enum Kind { EPIPHYTE,VINE,ROOT,THORN,LITTER }
    public final String id;
    public final Kind kind;
    public final int extent;
    public final float contactDamage;
    public final double movement;
    ForestSpecies(String id,Kind kind,int extent,float contactDamage,double movement){
        this.id=id;this.kind=kind;this.extent=extent;this.contactDamage=contactDamage;this.movement=movement;
    }
    public Block block(){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id));}
    public boolean wall(){return kind==Kind.EPIPHYTE||kind==Kind.VINE;}
    public static boolean naturalLog(BlockState s){
        String path=BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return s.is(BlockTags.LOGS)&&!path.startsWith("stripped_")&&(path.endsWith("_log")||path.endsWith("_wood")||path.endsWith("_stem")||path.endsWith("_hyphae"));
    }
    public static boolean soil(BlockState s){
        if(s.is(BlockTags.DIRT)||s.is(Blocks.MUD)||s.is(Blocks.CLAY))return true;
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        return id.getNamespace().equals("worldlogic")&&java.util.Set.of("loam","silt","sandy_loam","peat","clay_soil","silty_loam","sandy_soil").contains(id.getPath());
    }
    public boolean supports(BlockGetter l,BlockPos p,Direction towardHost){
        var host=wall()?p.relative(towardHost):p.below();
        var s=l.getBlockState(host);
        if(!s.getFluidState().isEmpty())return false;
        if(wall())return (naturalLog(s)||s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(Blocks.COBBLESTONE))
            &&s.isFaceSturdy(l,host,towardHost.getOpposite());
        return soil(s)&&s.isFaceSturdy(l,host,Direction.UP);
    }
    public static boolean conifer(String wood){return java.util.Set.of("spruce","fir","larch","pine","cedar").contains(wood);}
    public static ForestSpecies root(String wood){return conifer(wood)?CONIFER_ROOT:wood.equals("birch")||wood.equals("beech")?PALE_ROOT:ROOT;}
}
