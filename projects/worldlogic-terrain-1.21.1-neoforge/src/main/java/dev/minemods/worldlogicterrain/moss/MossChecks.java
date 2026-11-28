package dev.minemods.worldlogicterrain.moss;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MossChecks {
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException("Moss: "+msg);}
    private static CoreHabitat.Sample sample(double t,double m,double d,double water,String zone){
        return new CoreHabitat.Sample(t,m,.6,d,water,.1,1,zone,"NORMAL",2,.2,1,0,.2);
    }
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
            .then(Commands.literal("moss_check").executes(c->{
                try{verify(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Moss verified: terrestrial species, climate, support, placement, loot and edible lichens"),false);return 1;}
                catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
            })));
    }
    public static void verify(ServerLevel l){
        var pos=new BlockPos(4,242,4);var old=l.getBlockState(pos);var floor=l.getBlockState(pos.below());
        var p=FakePlayerFactory.get(l,new GameProfile(UUID.fromString("6d5f3c6b-0c1a-4737-91f7-f8b9b909e933"),"[MossQA]"));
        var held=p.getMainHandItem();var data=p.getFoodData();int food=data.getFoodLevel();float sat=data.getSaturationLevel();
        try{
            for(var s:MossSpecies.values()){
                var item=s.block().asItem();
                require(s.block() instanceof MossBlock&&TerrainContent.allItems().stream().anyMatch(x->x.get()==item),"registration "+s.id);
                var good=sample((s.minTemp+s.maxTemp)/2,Math.max(.80,s.minMoisture),.12,.2,"OPEN_PLAINS");
                var dry=sample(10,.02,.99,.1,"OPEN_PLAINS");
                require(s.suitable(good)&&!s.suitable(dry)&&!s.suitable(sample(100,.9,.1,.3,"OPEN_PLAINS"))
                    &&!s.suitable(sample(10,.9,.1,.3,"MARINE")),"climate "+s.id);
                l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                l.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
                require(!MossFeature.tryPlace(l,pos,s,dry),"dry site accepted "+s.id);
                require(MossFeature.tryPlace(l,pos,s,good),"suitable site rejected "+s.id);
                require(!MossFeature.tryPlace(l,pos,s,good),"replaced existing plant "+s.id);
                var drops=Block.getDrops(l.getBlockState(pos),l,pos,null,p,ItemStack.EMPTY);
                require(drops.size()==1&&drops.getFirst().is(item),"loot "+s.id);
                l.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                require(l.getBlockState(pos).isAir()&&!MossFeature.tryPlace(l,pos,s,good),"support "+s.id);
                l.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,2));
                var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
                require(item.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()&&l.getBlockState(pos).is(s.block()),"manual placement "+s.id);
                require(new ItemStack(item).has(DataComponents.FOOD)==s.edible,"edibility "+s.id);
                if(s.edible)eat(l,p,new ItemStack(item,2));
            }
            var marine=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","irish_moss"));
            require(TerrainContent.allItems().stream().anyMatch(x->x.get()==marine),"Irish moss creative tab");
            eat(l,p,new ItemStack(marine,2));
        }finally{
            p.setItemInHand(InteractionHand.MAIN_HAND,held);data.setFoodLevel(food);data.setSaturation(sat);
            l.setBlock(pos,old,Block.UPDATE_ALL);l.setBlock(pos.below(),floor,Block.UPDATE_ALL);
        }
    }
    private static void eat(ServerLevel l,net.minecraft.server.level.ServerPlayer p,ItemStack stack){
        var f=stack.get(DataComponents.FOOD);
        require(f!=null&&f.nutrition()==1,"food component");
        var d=p.getFoodData();d.setFoodLevel(8);d.setSaturation(0);
        require(stack.finishUsingItem(l,p).getCount()==1&&d.getFoodLevel()==9,"food consumption");
        require(Math.abs(d.getSaturationLevel()-f.saturation())<.01,"food saturation");
    }
}
