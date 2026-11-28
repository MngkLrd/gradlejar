package dev.minemods.worldlogicterrain.food;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.Map;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import dev.minemods.worldlogicterrain.block.BerryBushBlock;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ForageChecks {
    private record Expect(int hunger,float saturation,double recovery) {}
    private static final Map<String,Expect> EXPECTED=Map.ofEntries(
        Map.entry("strawberry",new Expect(2,1.20f,2)),
        Map.entry("blueberry",new Expect(2,1.00f,2)),
        Map.entry("raspberry",new Expect(2,1.40f,2)),
        Map.entry("blackberry",new Expect(2,1.60f,0)),
        Map.entry("grape",new Expect(3,1.80f,5)),
        Map.entry("pineapple",new Expect(5,4.00f,6)),
        Map.entry("tomato",new Expect(3,2.10f,0)),
        Map.entry("cucumber",new Expect(2,0.80f,0)),
        Map.entry("radish",new Expect(2,1.80f,0)),
        Map.entry("onion",new Expect(2,1.60f,0)),
        Map.entry("cabbage",new Expect(4,4.00f,0)),
        Map.entry("bell_pepper",new Expect(3,2.70f,0))
    );
    private static final GameProfile PROFILE=new GameProfile(UUID.fromString("631a76d8-526b-47be-8e90-83cf5f0447fa"),"[TerrainQA]");
    private static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("food_owner_check").then(Commands.argument("expected",StringArgumentType.word()).executes(c->{
            String expected=StringArgumentType.getString(c,"expected");
            boolean peer=MetabolismBridge.automaticFood();
            if(!expected.equals(peer?"metabolism":"terrarian")){c.getSource().sendFailure(Component.literal("Food owner mismatch"));return 0;}
            try{
                for(var entry:EXPECTED.entrySet())check(c.getSource().getLevel(),entry.getKey(),entry.getValue());
                c.getSource().sendSuccess(()->Component.literal("Food owner verified "+expected+": all 12 foods, version boundary, legacy stamina and tooltip policy"),false);return 1;
            }catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          }))));
        event.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("food_check").then(Commands.argument("food",StringArgumentType.word()).executes(c->{
            String id=StringArgumentType.getString(c,"food");
            if(!EXPECTED.containsKey(id))return 0;
            try {
                require(!MetabolismBridge.automaticFood(),"Standalone food fixture requires Terrarian to own recovery");
                check(c.getSource().getLevel(),id,EXPECTED.get(id));
                c.getSource().sendSuccess(()->Component.literal("Forage verified "+id+": loot, food, saturation, consumption, core stamina; standalone food owner verified"),false);
                return 1;
            } catch (IllegalStateException ex) {c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          }))));
    }
    private static void check(ServerLevel level,String id,Expect expect) {
        var key=ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id);
        var item=BuiltInRegistries.ITEM.get(key);
        var plant=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","wild_"+id));
        require(item instanceof ForageFoodItem,"missing food "+id);
        require(!MetabolismBridge.supportsAutomaticFood(1,3)&&MetabolismBridge.supportsAutomaticFood(1,4)
            &&MetabolismBridge.supportsAutomaticFood(1,5)&&!MetabolismBridge.supportsAutomaticFood(2,4),"Metabolism version boundary");
        var forage=(ForageFoodItem)item;
        double legacy=MetabolismBridge.automaticFood()?0:expect.recovery();
        require(forage.legacyRecovery()==legacy,"legacy food/tooltip recovery ownership "+id);
        var p=FakePlayerFactory.get(level,PROFILE);
        var pos=level.getSharedSpawnPos();
        var drops=Block.getDrops(plant.defaultBlockState(),level,pos,null,p,ItemStack.EMPTY);
        var seed=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id+"_seeds"));
        require(seed!=net.minecraft.world.item.Items.AIR,"Missing seed "+id);
        require(drops.size()==2&&drops.stream().filter(s->s.is(item)&&s.getCount()==1).count()==1&&drops.stream().filter(s->s.is(seed)&&s.getCount()==1).count()==1,"wrong harvest or seed "+id);
        require(dev.minemods.worldlogicterrain.registry.TerrainContent.allItems().stream().anyMatch(s->s.get()==seed),"seed absent from own tab inventory "+id);
        require(dev.minemods.worldlogicterrain.registry.TerrainContent.allItems().stream().anyMatch(s->s.get()==item),"food absent from own tab inventory "+id);
        require(Math.abs(ForageFoodItem.energyIcons(expect.recovery())-expect.recovery()/10.0)<.00001,"HUD recovery conversion "+id);
        var tab=dev.minemods.worldlogicterrain.registry.TerrainContent.TERRARIAN_TAB.get();
        require(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).equals(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","terrarian")),"Dedicated creative tab missing");
        var listed=dev.minemods.worldlogicterrain.registry.TerrainContent.allItems().stream().map(s->BuiltInRegistries.ITEM.getKey(s.get())).collect(java.util.stream.Collectors.toSet());
        var registered=BuiltInRegistries.ITEM.keySet().stream().filter(k->k.getNamespace().equals("worldlogicterrain")).collect(java.util.stream.Collectors.toSet());
        require(listed.equals(registered)&&listed.size()==dev.minemods.worldlogicterrain.registry.TerrainContent.allItems().size(),"Creative tab inventory incomplete or duplicate");
        if(plant instanceof BerryBushBlock berry)checkBerry(level,p,berry,item,id);
        var stack=new ItemStack(item,2);
        var food=stack.get(DataComponents.FOOD);
        require(food!=null&&food.nutrition()==expect.hunger()&&Math.abs(food.saturation()-expect.saturation())<.001&&!food.canAlwaysEat(),"food properties "+id);
        var data=p.getFoodData();
        int oldFood=data.getFoodLevel();float oldSat=data.getSaturationLevel();
        require(WorldLogicApi.fatigue(p).enabled(),"core exertion must be enabled for food QA");
        try{
            data.setFoodLevel(8);data.setSaturation(0);
            WorldLogicApi.recoverExertion(p,100);WorldLogicApi.addExertion(p,40);
            double before=WorldLogicApi.fatigue(p).fatiguePercent();
            ItemStack left=stack.finishUsingItem(level,p);
            require(left.getCount()==1,"not consumed "+id);
            require(data.getFoodLevel()==8+expect.hunger(),"hunger not restored "+id+" actual="+data.getFoodLevel());
            require(Math.abs(data.getSaturationLevel()-expect.saturation())<.01,"saturation not restored "+id);
            double after=WorldLogicApi.fatigue(p).fatiguePercent();
            require(Math.abs(after-Math.max(0,before-legacy))<.001,"wrong core recovery "+id+" before="+before+" after="+after);
        }finally{
            data.setFoodLevel(oldFood);data.setSaturation(oldSat);
            WorldLogicApi.recoverExertion(p,100);
        }
    }
    private static void checkBerry(ServerLevel level,net.minecraft.server.level.ServerPlayer p,BerryBushBlock berry,net.minecraft.world.item.Item food,String id){
        BlockPos pos=new BlockPos(12,240,12);
        var old=level.getBlockState(pos);var floor=level.getBlockState(pos.below());
        var box=new AABB(pos).inflate(2);
        try{
            level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
            level.setBlock(pos,berry.defaultBlockState(),Block.UPDATE_ALL);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            var existing=level.getEntitiesOfClass(ItemEntity.class,box);
            var initialIds=existing.stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
            var ripe=level.getBlockState(pos);
            require(ripe.getValue(BerryBushBlock.RIPE),"initial berry not ripe "+id);
            require(berry.useWithoutItem(ripe,level,pos,p,hit).consumesAction(),"right-click failed "+id);
            var empty=level.getBlockState(pos);
            require(empty.is(berry)&&!empty.getValue(BerryBushBlock.RIPE),"bush not preserved "+id);
            var drops=level.getEntitiesOfClass(ItemEntity.class,box,e->!initialIds.contains(e.getUUID()));
            var seed=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id+"_seeds"));
            require(drops.size()==2&&drops.stream().filter(e->e.getItem().is(food)&&e.getItem().getCount()==1).count()==1&&drops.stream().filter(e->e.getItem().is(seed)&&e.getItem().getCount()==1).count()==1,"right-click fruit and seed yield "+id);
            require(!berry.useWithoutItem(empty,level,pos,p,hit).consumesAction(),"repeat harvest accepted "+id);
            require(level.getEntitiesOfClass(ItemEntity.class,box,e->!initialIds.contains(e.getUUID())).size()==2,"duplicate berry drop "+id);
            require(Block.getDrops(empty,level,pos,null,p,ItemStack.EMPTY).isEmpty(),"empty bush drops berries "+id);
            drops.forEach(ItemEntity::discard);
            // Seed item performs real placement, with no free ripe fruit.
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            var held=p.getMainHandItem();
            try{
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(seed));
                var floorHit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
                var use=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,floorHit);
                require(seed.useOn(use).consumesAction()&&level.getBlockState(pos).is(berry)&&!level.getBlockState(pos).getValue(BerryBushBlock.RIPE),"Seed failed to place unripe bush "+id);
            }finally{p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);}
            require(Block.getDrops(level.getBlockState(pos),level,pos,null,p,ItemStack.EMPTY).isEmpty(),"Planted unripe bush yields free produce "+id);
            // deterministic random-tick path; no forced state reset.
            var random=RandomSource.create(84);
            for(int i=0;i<100&&!level.getBlockState(pos).getValue(BerryBushBlock.RIPE);i++)
                berry.randomTick(level.getBlockState(pos),level,pos,random);
            require(level.getBlockState(pos).getValue(BerryBushBlock.RIPE),"berry regrowth failed "+id);
        }finally{
            level.setBlock(pos,old,Block.UPDATE_ALL);level.setBlock(pos.below(),floor,Block.UPDATE_ALL);
        }
    }
}
