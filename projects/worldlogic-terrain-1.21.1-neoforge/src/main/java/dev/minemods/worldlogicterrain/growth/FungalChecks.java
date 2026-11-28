package dev.minemods.worldlogicterrain.growth;

import com.mojang.authlib.GameProfile;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import dev.minemods.worldlogicterrain.worldgen.FungalFeature;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Executed by the existing production-server patch_check scenario. */
public final class FungalChecks {
    private FungalChecks(){}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static void reset(ServerLevel l,BlockPos center,BlockState floor){
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            var q=center.offset(dx,0,dz);
            l.setBlock(q,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(q.below(),floor,Block.UPDATE_ALL);
        }
    }
    public static void verify(ServerLevel l){
        var center=new BlockPos(8,249,8);
        require(l.hasChunkAt(center),"Fungal fixture chunk missing");
        var saved=new HashMap<BlockPos,BlockState>();
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=0;dy++){
            var q=center.offset(dx,dy,dz);saved.put(q,l.getBlockState(q));
        }
        var player=FakePlayerFactory.get(l,new GameProfile(UUID.fromString("f2bbef19-26c2-434a-9bea-a839650ecc3b"),"[FungalQA]"));
        var held=player.getMainHandItem();
        var box=new AABB(center).inflate(4);
        var initial=new HashSet<UUID>();
        for(var e:l.getEntitiesOfClass(ItemEntity.class,box))initial.add(e.getUUID());
        try{
            require(FungalEcology.Species.values().length==4,"Wrong fungus inventory");
            for(var s:FungalEcology.Species.values()){
                require(s.block() instanceof FungalColonyBlock&&s.item() instanceof BlockItem i&&i.getBlock()==s.block(),"Fungal registration "+s.id);
                require(TerrainContent.allItems().stream().anyMatch(x->x.get()==s.item()),"Fungus missing from creative "+s.id);
                var cfgKey=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","fungal_"+s.id));
                var cfg=l.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(cfgKey).value().config();
                require(cfg instanceof FungalFeature.Config c&&c.species().equals(s.id),"Missing configured fungus "+s.id);
                var floor=s.wood?Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X):Blocks.GRASS_BLOCK.defaultBlockState();
                var bad=s.wood?Blocks.OAK_LOG.defaultBlockState():Blocks.STONE.defaultBlockState();
                var young=s.block().defaultBlockState();
                require(young.getValue(FungalColonyBlock.AGE)==0&&s.supports(floor)&&!s.supports(bad),"Substrate/default state "+s.id);
                if(s.wood){
                    var terrarian=TerrainContent.BLOCKS.getEntries().stream().filter(x->x.getId().getPath().equals("willow_log")).findFirst().orElseThrow().get();
                    require(s.supports(terrarian.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z)),"Addon log support");
                }
                var ideal=new SeasonalGrowth.Input("WINTER",s.optimalTemp,.75,1,8,false);
                var dry=new SeasonalGrowth.Input("SUMMER",s.optimalTemp,0,1,8,false);
                var cold=new SeasonalGrowth.Input("SUMMER",s.minTemp-2,.75,1,8,false);
                require(FungalEcology.rate(s,s.optimalTemp,.75,8)>.99&&FungalEcology.rate(s,s.optimalTemp,0,8)==0
                    &&FungalEcology.rate(s,s.maxTemp+1,.75,8)==0,"Fungal climate response "+s.id);
                var ages=new BitSet();var counts=new BitSet();
                for(int seed=1;seed<=16;seed++){
                    reset(l,center,floor);
                    l.setBlock(center.east(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
                    int n=FungalFeature.populate(l,RandomSource.create(seed),center,0,0,s,(x,z)->new BlockPos(x,center.getY(),z));
                    int actual=0;
                    for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                        var q=center.offset(dx,0,dz);var state=l.getBlockState(q);
                        if(state.is(s.block())){
                            actual++;ages.set(state.getValue(FungalColonyBlock.AGE));counts.set(state.getValue(FungalColonyBlock.COUNT));
                            require(Math.abs(dx)<=2&&Math.abs(dz)<=2&&state.canSurvive(l,q),"Fungal placement escaped habitat "+s.id);
                        }
                    }
                    require(n==actual&&n>=2&&n<=4&&l.getBlockState(center.east()).is(Blocks.STONE),"Natural colony cluster "+s.id+" count="+n);
                }
                require(ages.cardinality()==3&&counts.cardinality()==4,"Missing generated maturity/count "+s.id);
                reset(l,center,bad);
                require(FungalFeature.populate(l,RandomSource.create(12),center,0,0,s,(x,z)->new BlockPos(x,center.getY(),z))==0,"Invalid generated substrate "+s.id);
                reset(l,center,floor);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(s.item(),2));
                var hit=new BlockHitResult(Vec3.atCenterOf(center.below()).add(0,.5,0),Direction.UP,center.below(),false);
                require(s.item().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Fungus placement failed "+s.id);
                require(l.getBlockState(center).equals(young),"Planting did not create mycelium "+s.id);
                require(player.getMainHandItem().getCount()==(player.getAbilities().instabuild?2:1),"Inoculum consumption "+s.id);
                require(Block.getDrops(young,l,center,null,player,ItemStack.EMPTY).isEmpty(),"Mycelium loot "+s.id);
                require(!FungalEcology.advance(l,center,dry,RandomSource.create(1))&&!FungalEcology.advance(l,center,cold,RandomSource.create(2)),"Unsuitable growth "+s.id);
                for(int age=1;age<=2;age++){
                    require(FungalEcology.advance(l,center,ideal,RandomSource.create(age)),"Fungal growth transition "+s.id);
                    var current=l.getBlockState(center);
                    require(current.getValue(FungalColonyBlock.AGE)==age,"Wrong fungal age "+s.id);
                    var snap=FungalEcology.snapshot(l,center).orElseThrow();
                    var view=SeasonalGrowth.view(l,center,current);
                    require(snap.species()==s&&snap.age()==age&&snap.substrateValid()&&view.progress()==age*50&&view.season().equals("all"),"Fungal API/HUD "+s.id);
                    if(age==1)require(Block.getDrops(current,l,center,null,player,ItemStack.EMPTY).isEmpty(),"Immature loot "+s.id);
                }
                // Stack the same collected item onto the live block, including mature groups.
                var groupHit=new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(s.item(),4));
                for(int count=2;count<=4;count++){
                    require(s.item().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,groupHit)).consumesAction(),"Stack placement "+s.id);
                    var group=l.getBlockState(center);
                    require(group.getValue(FungalColonyBlock.COUNT)==count&&group.getValue(FungalColonyBlock.AGE)==2,"Stack count/age "+s.id);
                    require(FungalEcology.snapshot(l,center).orElseThrow().count()==count,"API count "+s.id);
                }
                require(player.getMainHandItem().getCount()==(player.getAbilities().instabuild?4:1),"Stack consumption "+s.id);
                require(!l.getBlockState(center).canBeReplaced(new net.minecraft.world.item.context.BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,groupHit))),"Stack exceeded cap "+s.id);
                var ripe=l.getBlockState(center);
                for(int count=1;count<=4;count++){
                    var stage=ripe.setValue(FungalColonyBlock.COUNT,count);
                    require(Block.getDrops(stage,l,center,null,player,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==count,"Counted mature loot "+s.id);
                    require(Block.getDrops(stage.setValue(FungalColonyBlock.AGE,1),l,center,null,player,ItemStack.EMPTY).isEmpty(),"Immature counted loot "+s.id);
                }
                var drops=Block.getDrops(ripe,l,center,null,player,ItemStack.EMPTY);
                require(drops.size()==1&&drops.getFirst().is(s.item())&&drops.getFirst().getCount()==4,"Mature break loot "+s.id);
                var clicked=new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false);
                var b=(FungalColonyBlock)s.block();
                require(b.useWithoutItem(ripe,l,center,player,clicked).consumesAction()&&l.getBlockState(center).equals(young.setValue(FungalColonyBlock.COUNT,4)),"Harvest removed mycelium "+s.id);
                require(!b.useWithoutItem(ripe,l,center,player,clicked).consumesAction(),"Repeated harvest "+s.id);
                var yielded=l.getEntitiesOfClass(ItemEntity.class,box,e->!initial.contains(e.getUUID())&&e.getItem().is(s.item()));
                require(yielded.size()==1&&yielded.getFirst().getItem().getCount()==4,"Harvest yield "+s.id);
                yielded.forEach(ItemEntity::discard);
                require(FungalEcology.advance(l,center,ideal,RandomSource.create(6))&&FungalEcology.advance(l,center,ideal,RandomSource.create(7)),"Regrowth after harvest "+s.id);
                require(l.getBlockState(center).getValue(FungalColonyBlock.COUNT)==4,"Regrowth lost group size "+s.id);
                var target=center.east();
                require(!FungalEcology.colonize(l,center,target,dry,RandomSource.create(8)),"Dry spread "+s.id);
                l.setBlock(target,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
                require(!FungalEcology.colonize(l,center,target,ideal,RandomSource.create(9)),"Occupied spread "+s.id);
                l.setBlock(target,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                require(FungalEcology.colonize(l,center,target,ideal,RandomSource.create(10))&&l.getBlockState(target).equals(young),"Missing daughter mycelium "+s.id);
                require(!FungalEcology.colonize(l,target,target.east(),ideal,RandomSource.create(11)),"Immature colony spread "+s.id);
                for(var q:List.of(center.south(),center.west(),center.south().east()))l.setBlock(q,young,Block.UPDATE_ALL);
                require(!FungalEcology.colonize(l,center,center.north(),ideal,RandomSource.create(12))&&l.getBlockState(center.north()).isAir(),"Local fungal cap "+s.id);
                require(!FungalEcology.colonize(l,center,center.north(3),ideal,RandomSource.create(13)),"Out of radius spread "+s.id);
                l.setBlock(center.below(),bad,Block.UPDATE_ALL);
                require(l.getBlockState(center).isAir()&&FungalEcology.snapshot(l,center).isEmpty(),"Unsupported mycelium remained "+s.id);
                var input=CraftingInput.of(2,2,List.of(new ItemStack(s.item()),new ItemStack(s.item()),new ItemStack(Items.BOWL),ItemStack.EMPTY));
                var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);
                require(recipe.isPresent()&&recipe.orElseThrow().value().assemble(input,l.registryAccess()).is(Items.MUSHROOM_STEW),"Fungal stew recipe "+s.id);
                var incomplete=CraftingInput.of(2,1,List.of(new ItemStack(s.item()),new ItemStack(Items.BOWL)));
                require(l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,incomplete,l).isEmpty(),"Stew crafted with missing ingredient "+s.id);
            }
        }finally{
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
            for(var e:l.getEntitiesOfClass(ItemEntity.class,box,e->!initial.contains(e.getUUID())))e.discard();
            saved.entrySet().stream().sorted(java.util.Comparator.comparingInt(v->v.getKey().getY())).forEach(v->l.setBlock(v.getKey(),v.getValue(),Block.UPDATE_ALL));
        }
    }
}
