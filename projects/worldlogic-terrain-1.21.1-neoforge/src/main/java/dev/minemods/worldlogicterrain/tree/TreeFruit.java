package dev.minemods.worldlogicterrain.tree;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Fruit IDs and actual supporting foliage. Fruit-bearing broadleaf forms reuse native oak wood. */
public enum TreeFruit {
    APPLE("apple","minecraft","oak",4,.3f),
    PEAR("pear","minecraft","oak",3,.4f),
    PLUM("plum","minecraft","oak",2,.4f),
    FIG("fig","minecraft","oak",3,.45f),
    MANGO("mango","minecraft","jungle",5,.45f),
    COCONUT("coconut","worldlogicterrain","palm",5,.5f),
    DATE("date","worldlogicterrain","date_palm",3,.45f),
    BAOBAB("baobab_fruit","worldlogicterrain","baobab",3,.45f);
    public final String id,leafNamespace,wood;
    public final int nutrition;
    public final float saturation;
    TreeFruit(String id,String leafNamespace,String wood,int nutrition,float saturation){
        this.id=id;this.leafNamespace=leafNamespace;this.wood=wood;this.nutrition=nutrition;this.saturation=saturation;
    }
    public String blockId(){return "hanging_"+id;}
    public String itemNamespace(){return this==APPLE?"minecraft":"worldlogicterrain";}
    public Item food(){return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(itemNamespace(),id));}
    public Block block(){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",blockId()));}
    public Block leaves(){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(leafNamespace,wood+"_leaves"));}
    public boolean supports(BlockState state){return state.is(leaves());}
}
