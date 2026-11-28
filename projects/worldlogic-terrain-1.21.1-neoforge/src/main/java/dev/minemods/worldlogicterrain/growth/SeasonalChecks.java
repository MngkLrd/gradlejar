package dev.minemods.worldlogicterrain.growth;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SeasonalChecks {
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2)).then(Commands.literal("season_check").executes(c->{
            var l=c.getSource().getLevel();var pos=BlockPos.containing(c.getSource().getPosition());
            String season=CorePlantClimate.season(l,pos);
            if(season.equals("none")){c.getSource().sendFailure(Component.literal("Core season unavailable"));return 0;}
            var input=CorePlantClimate.sample(l,pos);
            var p=PlantSpecies.byId("wheat");
            var actual=SeasonalGrowth.evaluate(p,input,true);
            if(!actual.season().equals(season)||season.equals("WINTER")&&actual.rate()!=0){c.getSource().sendFailure(Component.literal("Seasonal gate disagrees with Core"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("Terrarian Core season="+season+" wheat rate="+actual.rate()+" status="+actual.status()+" temp="+actual.temperature()+" moisture="+actual.moisture()),false);
            return 1;
        })));
    }
    public static void verify(ServerLevel l){
        var all=new ArrayList<PlantSpecies.Profile>(PlantSpecies.newSpecies());
        for(String id:new String[]{"pineapple","tomato","cucumber","radish","onion","cabbage","bell_pepper"})all.add(PlantSpecies.byId(id));
        require(all.size()==27,"Missing species");
        var rarity=new HashSet<Integer>();
        var pos=new BlockPos(10,246,10);var before=l.getBlockState(pos);var floor=l.getBlockState(pos.below());
        var player=FakePlayerFactory.get(l,new GameProfile(UUID.fromString("c6130ade-3414-459f-b399-2cc7277e36fa"),"[SeasonalQA]"));
        var held=player.getMainHandItem();
        try{
            for(var p:all){
                rarity.add(p.rarity());
                var key=ResourceLocation.fromNamespaceAndPath("worldlogicterrain","wild_"+p.id());
                var block=BuiltInRegistries.BLOCK.get(key);
                require(block instanceof SeasonalPlantBlock,"Missing growth block "+p.id());
                var seed=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",PlantSpecies.isNew(p.id())?"wild_"+p.id():p.id()+"_seeds"));
                require(seed instanceof SeasonalSeedItem,"Missing planting item "+p.id());
                l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);l.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(seed));
                var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
                require(seed.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Planting failed "+p.id());
                var young=l.getBlockState(pos);
                require(young.is(block)&&young.getValue(SeasonalPlantBlock.AGE)==0&&young.canSurvive(l,pos),"Planting age/support "+p.id());
                require(Block.getDrops(young,l,pos,null,player,ItemStack.EMPTY).isEmpty(),"Immature loot "+p.id());
                var good=new SeasonalGrowth.Input("SUMMER",p.optimalTemp(),p.optimalMoisture(),1,15,false);
                var perfect=SeasonalGrowth.evaluate(p,good,false);
                require(perfect.rate()>.99&&perfect.quality()>.99,"Ideal rate "+p.id());
                var cold=SeasonalGrowth.evaluate(p,new SeasonalGrowth.Input("SUMMER",p.minTemp()-1,p.optimalMoisture(),1,15,false),false);
                require(cold.rate()==0&&!SeasonalPlantBlock.advance(l,pos,cold,RandomSource.create(10)),"Cold growth "+p.id());
                var dry=SeasonalGrowth.evaluate(p,new SeasonalGrowth.Input("SUMMER",p.optimalTemp(),0,1,15,false),false);
                require(dry.rate()==0,"Dry growth "+p.id());
                var dark=SeasonalGrowth.evaluate(p,new SeasonalGrowth.Input("SUMMER",p.optimalTemp(),p.optimalMoisture(),1,0,false),false);
                require(dark.rate()==0,"Dark growth "+p.id());
                var winter=SeasonalGrowth.evaluate(p,new SeasonalGrowth.Input("WINTER",p.optimalTemp(),p.optimalMoisture(),1,15,false),false);
                require((winter.rate()>0)==p.seasonAllowed("WINTER"),"Wrong winter reaction "+p.id());
                var worn=SeasonalGrowth.evaluate(p,new SeasonalGrowth.Input("SUMMER",p.optimalTemp(),p.optimalMoisture(),1,15,true),false);
                require(Math.abs(worn.rate()-.65)<.001&&worn.quality()<perfect.quality(),"Soil factor "+p.id());
                for(int age=1;age<=3;age++){
                    require(SeasonalPlantBlock.advance(l,pos,perfect,RandomSource.create(age)),"Growth transition "+p.id());
                    require(l.getBlockState(pos).getValue(SeasonalPlantBlock.AGE)==age,"Wrong growth age "+p.id());
                }
                var ripe=l.getBlockState(pos);
                require(ripe.getValue(SeasonalPlantBlock.QUALITY)==3,"Quality not stored "+p.id());
                var drops=Block.getDrops(ripe,l,pos,null,player,ItemStack.EMPTY);
                var product=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",PlantSpecies.isNew(p.id())?"wild_"+p.id():p.id()));
                require(drops.stream().filter(s->s.is(product)).mapToInt(ItemStack::getCount).sum()==2,"High quality yield "+p.id());
                var normal=ripe.setValue(SeasonalPlantBlock.QUALITY,2);
                require(Block.getDrops(normal,l,pos,null,player,ItemStack.EMPTY).stream().filter(s->s.is(product)).mapToInt(ItemStack::getCount).sum()==1,"Standard yield "+p.id());
                require(!SeasonalPlantBlock.advance(l,pos,perfect,RandomSource.create(14)),"Mature plant grew again "+p.id());
                var view=SeasonalGrowth.view(l,pos,ripe);
                require(view!=null&&view.progress()==100&&view.speed()==0&&view.quality()==100&&view.status().equals("mature"),"HUD sample "+p.id());
                l.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
                require(l.getBlockState(pos).isAir(),"Invalid support "+p.id());
                require(p.habitatFitness(p.minTemp()-1,p.optimalMoisture(),1)==0&&p.habitatFitness(p.optimalTemp(),p.optimalMoisture(),1)>.9,"Climate suitability "+p.id());
            }
            require(rarity.size()>=6,"Plant frequencies collapsed");
            var wheat=PlantSpecies.byId("wheat");
            require(SeasonalGrowth.evaluate(wheat,new SeasonalGrowth.Input("WINTER",20,.5,1,15,false),true).rate()==0,"Native winter gate");
            require(SeasonalGrowth.evaluate(wheat,new SeasonalGrowth.Input("none",-30,0,0,0,false),true).rate()==1,"Legacy crop compatibility");
            var apple=dev.minemods.worldlogicterrain.tree.TreeFruit.APPLE.block().defaultBlockState();
            require(SeasonalGrowth.view(l,pos,apple).season().equals("all"),"Tree fruit must be perennial");
        }finally{
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
            l.setBlock(pos.below(),floor,Block.UPDATE_ALL);l.setBlock(pos,before,Block.UPDATE_ALL);
        }
    }
}
