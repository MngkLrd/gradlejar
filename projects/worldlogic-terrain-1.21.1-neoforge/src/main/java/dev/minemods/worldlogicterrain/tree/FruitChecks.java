package dev.minemods.worldlogicterrain.tree;

import com.mojang.authlib.GameProfile;
import dev.minemods.worldlogicterrain.block.HangingFruitBlock;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Runs against actual configured trees in the existing isolated TreeChecks fixture. */
final class FruitChecks {
    private static final GameProfile PROFILE=new GameProfile(UUID.fromString("04babf4f-294d-4c55-96bc-0e61e6437dc3"),"[FruitQA]");
    static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    static void verifySelection(ServerLevel l){
        var selected=EnumSet.noneOf(TreeFruit.class);
        for(int i=0;i<256;i++){
            long seed=i*0x9E3779B97F4A7C15L;
            for(var zone:new String[]{"OPEN","TEMPERATE","WET","DRY","TROPICAL","COAST"}){
                double temp=zone.equals("DRY")||zone.equals("TROPICAL")||zone.equals("COAST")?25:17;
                for(var wood:new String[]{"oak","palm","date_palm","baobab"}){
                    var fruit=FruitEcology.selectSite(zone,"temperate_test",wood,temp,.1,.3,.5,RandomSource.create(seed));
                    fruit.ifPresent(p->selected.add(p.fruit()));
                    require(FruitEcology.selectSite(zone,"forest_tundra",wood,temp,.1,.3,.5,RandomSource.create(seed)).isEmpty(),"Tundra fruit "+wood);
                    require(FruitEcology.selectSite(zone,"snowy_taiga",wood,temp,.1,.3,.5,RandomSource.create(seed)).isEmpty(),"Winter fruit "+wood);
                    require(FruitEcology.selectSite(zone,"temperate_test",wood,2,.8,.3,.5,RandomSource.create(seed)).isEmpty(),"Cold fruit "+wood);
                }
            }
        }
        require(selected.equals(EnumSet.allOf(TreeFruit.class)),"Regional fruit selection missing "+selected);
        var registry=l.registryAccess().lookupOrThrow(Registries.BIOME);
        var winter=registry.getOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("snowy_taiga")));
        var plains=registry.getOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("plains")));
        require(FruitEcology.isWinter(winter)&&!FruitEcology.isWinter(plains),"Winter biome tag exclusion");
    }
    static void verifyHarvest(ServerLevel l,TreeFruit fruit,BlockPos pos){
        var block=(HangingFruitBlock)fruit.block();
        var p=FakePlayerFactory.get(l,PROFILE);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.DOWN,pos,false);
        var box=new AABB(pos).inflate(2);
        var initial=l.getEntitiesOfClass(ItemEntity.class,box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        var ripe=l.getBlockState(pos);
        require(ripe.is(block)&&ripe.getValue(HangingFruitBlock.AGE)==2&&ripe.canSurvive(l,pos),"Missing ripe supported "+fruit.id);
        var drops=Block.getDrops(ripe,l,pos,null,p,ItemStack.EMPTY);
        require(drops.size()==1&&drops.getFirst().is(fruit.food())&&drops.getFirst().getCount()==1,"Ripe break loot "+fruit.id);
        require(block.useWithoutItem(ripe,l,pos,p,hit).consumesAction(),"Fruit right-click failed "+fruit.id);
        var empty=l.getBlockState(pos);
        require(empty.is(block)&&empty.getValue(HangingFruitBlock.AGE)==0&&fruit.supports(l.getBlockState(pos.above())),"Harvest broke fruiting site "+fruit.id);
        require(Block.getDrops(empty,l,pos,null,p,ItemStack.EMPTY).isEmpty(),"Empty stem drops fruit "+fruit.id);
        require(!block.useWithoutItem(ripe,l,pos,p,hit).consumesAction()&&!block.useWithoutItem(empty,l,pos,p,hit).consumesAction(),"Duplicate harvest "+fruit.id);
        var yielded=l.getEntitiesOfClass(ItemEntity.class,box,e->!initial.contains(e.getUUID()));
        require(yielded.size()==1&&yielded.getFirst().getItem().is(fruit.food())&&yielded.getFirst().getItem().getCount()==1,"Wrong harvest yield "+fruit.id);
        yielded.forEach(ItemEntity::discard);
        for(int stage=1;stage<=2;stage++){
            for(int i=0;i<128&&l.getBlockState(pos).getValue(HangingFruitBlock.AGE)<stage;i++)
                block.randomTick(l.getBlockState(pos),l,pos,RandomSource.create((i+stage*129L)*0x9E3779B97F4A7C15L));
            var state=l.getBlockState(pos);
            require(state.is(block)&&state.getValue(HangingFruitBlock.AGE)==stage,"Fruit stage did not grow "+fruit.id);
            if(stage==1){
                require(!block.useWithoutItem(state,l,pos,p,hit).consumesAction(),"Unripe fruit harvested "+fruit.id);
                require(Block.getDrops(state,l,pos,null,p,ItemStack.EMPTY).isEmpty(),"Unripe break loot "+fruit.id);
            }
        }
        require(block.useWithoutItem(l.getBlockState(pos),l,pos,p,hit).consumesAction(),"Regrown fruit not harvestable "+fruit.id);
        var second=l.getEntitiesOfClass(ItemEntity.class,box,e->!initial.contains(e.getUUID()));
        require(second.size()==1&&second.getFirst().getItem().is(fruit.food()),"Second harvest yield "+fruit.id);
        second.forEach(ItemEntity::discard);
        // Actual neighbor update must remove the dormant site if foliage decays or is broken.
        l.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        require(l.getBlockState(pos).isAir(),"Unsupported hanging fruit remains "+fruit.id);
        require(l.getEntitiesOfClass(ItemEntity.class,box,e->!initial.contains(e.getUUID())&&e.getItem().is(fruit.food())).isEmpty(),"Empty site duplicated fruit "+fruit.id);
        var stack=new ItemStack(fruit.food(),2);
        var food=stack.get(DataComponents.FOOD);
        require(food!=null&&food.nutrition()==fruit.nutrition&&Math.abs(food.saturation()-fruit.nutrition*fruit.saturation*2)<.01,"Food properties "+fruit.id);
        if(fruit!=TreeFruit.APPLE)require(TerrainContent.allItems().stream().anyMatch(s->s.get()==fruit.food()),"Fruit absent from creative tab "+fruit.id);
        else require(fruit.food()==Items.APPLE,"Duplicate vanilla apple");
        var data=p.getFoodData();int oldFood=data.getFoodLevel();float oldSat=data.getSaturationLevel();
        try{
            data.setFoodLevel(8);data.setSaturation(0);
            var remaining=stack.finishUsingItem(l,p);
            require(remaining.getCount()==1&&data.getFoodLevel()==8+fruit.nutrition,"Fruit not consumed "+fruit.id);
        }finally{
            data.setFoodLevel(oldFood);data.setSaturation(oldSat);
        }
    }
}
