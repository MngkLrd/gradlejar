package dev.minemods.worldlogicterrain.botany;
import dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class BotanyChecks {
    private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain",s);}
    private static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException("Botany: "+msg);}
    private static ItemStack stack(String s){require(BuiltInRegistries.ITEM.containsKey(id(s)),"missing "+s);return new ItemStack(BuiltInRegistries.ITEM.get(id(s)));}
    public static void verify(ServerLevel l){
        var p=new BlockPos(10,250,10);var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(var x:List.of(p,p.below(),p.north(),p.above(),p.above().north()))saved.put(x,l.getBlockState(x));
        try{
            for(String s:List.of("wild_yarrow","wild_plantain","wild_wild_mint","wild_calendula","marshmallow_root","flax_stalk","flax_fiber","willow_bark","pine_resin"))stack(s);
            l.setBlock(p.below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
            for(String[] pair:new String[][]{{"wild_marshmallow","marshmallow_root"},{"wild_flax","flax_stalk"}}){
                var b=BuiltInRegistries.BLOCK.get(id(pair[0]));var item=stack(pair[1]).getItem();
                var ripe=b.defaultBlockState().setValue(SeasonalPlantBlock.AGE,3);
                require(Block.getDrops(ripe,l,p,null,null,ItemStack.EMPTY).stream().anyMatch(x->x.is(item)),"mature part "+pair[0]);
                require(Block.getDrops(ripe.setValue(SeasonalPlantBlock.AGE,2),l,p,null,null,ItemStack.EMPTY).stream().noneMatch(x->x.is(item)),"immature part "+pair[0]);
            }
            var input=CraftingInput.of(1,1,List.of(stack("flax_stalk")));
            var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l).orElseThrow();
            require(recipe.id().equals(id("flax_fiber"))&&recipe.value().assemble(input,l.registryAccess()).is(stack("flax_fiber").getItem()),"native flax recipe");
            for(var res:TreeResourceBlock.Resource.values()){
                var host=p.north();var log=BuiltInRegistries.BLOCK.get(id(res.host)).defaultBlockState();
                l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                l.setBlock(host,log,Block.UPDATE_ALL);
                var b=(TreeResourceBlock)res.block();
                var ripe=b.defaultBlockState().setValue(TreeResourceBlock.FACING,Direction.NORTH);
                require(ripe.canSurvive(l,p),"host "+res.node);
                l.setBlock(p,ripe,Block.UPDATE_ALL);
                require(b.harvest(l,p).is(stack(res.output).getItem())&&l.getBlockState(host).equals(log),"harvest "+res.node);
                require(b.harvest(l,p).isEmpty()&&!l.getBlockState(p).getValue(TreeResourceBlock.RIPE),"duplicate "+res.node);
                require(b.regrow(l,p,l.getBlockState(p))&&!b.harvest(l,p).isEmpty(),"renewal "+res.node);
                l.setBlock(host,Blocks.OAK_PLANKS.defaultBlockState(),Block.UPDATE_ALL);
                require(l.getBlockState(p).isAir()&&!ripe.canSurvive(l,p),"host loss "+res.node);
            }
        }finally{saved.entrySet().stream().sorted(Comparator.comparingInt(x->x.getKey().getY())).forEach(x->l.setBlock(x.getKey(),x.getValue(),Block.UPDATE_ALL));}
    }
}
