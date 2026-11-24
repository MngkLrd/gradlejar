package com.kirill.simplesorter.integration;

import com.kirill.simplesorter.sorting.ContainerRef;
import com.kirill.simplesorter.sorting.ContainerScanner;
import com.kirill.simplesorter.sorting.StrictStackClassifier;
import com.kirill.simplesorter.sorting.WarehouseVacuumSorter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Compiled only into the disposable CI development-server source set. */
@Mod.EventBusSubscriber(modid="simplesorter",bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class CoreInventoryLiveTest {
    private static void check(boolean condition,String message) {
        if(!condition) throw new IllegalStateException("Live sorter regression: "+message);
    }
    private static ChestBlockEntity place(ServerLevel level,BlockPos pos) {
        level.getChunk(pos.getX()>>4,pos.getZ()>>4);
        check(level.getBlockState(pos).isAir(),"Expected empty test position "+pos);
        check(level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState()),"Chest placement "+pos);
        check(level.getBlockEntity(pos) instanceof ChestBlockEntity,"Missing real chest "+pos);
        return (ChestBlockEntity)level.getBlockEntity(pos);
    }
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if(!"1".equals(System.getenv("SIMPLE_SORTER_CI_STORAGE_TEST")))return;
        ServerLevel level=event.getServer().overworld();
        BlockPos spawn=level.getSharedSpawnPos();
        BlockPos center=new BlockPos(spawn.getX(),220,spawn.getZ());
        var player=FakePlayerFactory.getMinecraft(level);
        player.setPos(center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5);
        List<BlockPos> placed=new ArrayList<>();
        try {
            check("fuel".equals(StrictStackClassifier.family(new ItemStack(Items.CHARCOAL))),"real charcoal classification");
            check("ammunition".equals(StrictStackClassifier.family(new ItemStack(Items.ARROW))),"real arrow classification");
            var iron=StrictStackClassifier.profile(new ItemStack(Items.IRON_INGOT));
            var raw=StrictStackClassifier.profile(new ItemStack(Items.RAW_IRON));
            var tool=StrictStackClassifier.profile(new ItemStack(Items.IRON_PICKAXE));
            check("iron".equals(iron.material()) && "ingot".equals(iron.form()),"real iron ingot semantic profile: "+iron);
            check("iron".equals(raw.material()) && "ore".equals(raw.form()),"real raw iron semantic profile: "+raw);
            check(iron.affinity(raw)>=85 && iron.affinity(tool)==0,"real item semantic affinity");
            var log=StrictStackClassifier.profile(new ItemStack(Items.OAK_LOG));
            var plank=StrictStackClassifier.profile(new ItemStack(Items.OAK_PLANKS));
            check("oak".equals(log.material()) && log.affinity(plank)>=85,"real wood material affinity");
            for(int i=0;i<21;i++){
                // 21 independent real chests, all mixed; many beyond vanilla menu reach.
                BlockPos p=center.offset(3+(i%7)*4,0,3+(i/7)*4);
                ChestBlockEntity chest=place(level,p);placed.add(p);
                chest.setItem(0,new ItemStack(Items.CHARCOAL,3));
                chest.setItem(1,new ItemStack(Items.ARROW,5));
                chest.setChanged();
            }
            var initial=ContainerScanner.scanDetailed(level,player,center,32);
            check(initial.containers().size()==21,"21 chest scan: "+initial.summary());
            check(initial.physicalBlocks()==21,"physical count");
            check(initial.containers().stream().anyMatch(c->c.pos().distSqr(center)>64),"remote inventory");
            BlockPos newPos=center.offset(32,0,32);
            place(level,newPos);placed.add(newPos);
            var updated=ContainerScanner.scanDetailed(level,player,center,32);
            check(updated.containers().size()==22,"fresh 22 chest scan: "+updated.summary());
            check(updated.emptyInventories()==1,"empty new chest: "+updated.summary());
            check(updated.containers().stream().anyMatch(c->c.pos().equals(newPos)),"new chest at square boundary");
            long before=count(updated.containers());
            var session=WarehouseVacuumSorter.begin(updated.containers());
            check(session.batchCount()==1,"22-chest warehouse split");
            while(!session.step(level,move->{},120,4_000_000L)){}
            check(session.rejectedTransfers()==0,"transaction rejected: "+session.lastFailure());
            check(session.movedItems()>0,"no real cross-chest items moved");
            check(count(updated.containers())==before,"item count changed");
            // Extra storage is available, but a sparse warehouse already
            // has enough capacity. Do not force an unnecessary extra chest.
            check(session.unresolvedMixedChests()==0,"two unrelated groups still mixed");
            // Rebuild the test area as 40 completely full real chests,
            // interleaving ten vertical columns and two metals.
            for(BlockPos p:placed){
                ((ChestBlockEntity)level.getBlockEntity(p)).clearContent();
                level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            }
            placed.clear();
            for(int y=0;y<4;y++)for(int col=0;col<10;col++){
                BlockPos p=center.offset(2+col*3,y,2);
                ChestBlockEntity chest=place(level,p);placed.add(p);
                for(int slot=0;slot<27;slot++){
                    boolean isIron=(slot+y+col)%2==0;
                    chest.setItem(slot,new ItemStack(isIron?Items.IRON_INGOT:Items.COPPER_INGOT,64));
                }
                chest.setChanged();
            }
            var full=ContainerScanner.scanDetailed(level,player,center,32);
            check(full.containers().size()==40,"40 real chest scan: "+full.summary());
            long fullCount=count(full.containers());
            long ironCount=countItem(full.containers(),Items.IRON_INGOT);
            var spatial=WarehouseVacuumSorter.begin(full.containers());
            check(spatial.batchCount()==1,"40 chests must use one global plan");
            while(!spatial.step(level,move->{},120,4_000_000L)){}
            check(spatial.rejectedTransfers()==0,"global transfer rejected: "+spatial.lastFailure());
            check(spatial.movedItems()>0,"40 full chests not rearranged");
            check(count(full.containers())==fullCount&&countItem(full.containers(),Items.IRON_INGOT)==ironCount,
                    "global transaction item conservation");
            java.util.Map<Integer,java.util.Set<net.minecraft.world.item.Item>> columns=new java.util.HashMap<>();
            for(ContainerRef ref:full.containers()){
                var set=columns.computeIfAbsent(ref.pos().getX(),key->new java.util.HashSet<>());
                for(int slot=0;slot<ref.slots();slot++){
                    var stack=ref.handler().getStackInSlot(slot);
                    if(!stack.isEmpty())set.add(stack.getItem());
                }
            }
            check(columns.size()==10&&columns.values().stream().allMatch(set->set.size()==1),
                    "real vertical columns not pure: "+columns);
            check(spatial.totalComponents()>0,"missing transfer components");
            var repeated=WarehouseVacuumSorter.begin(full.containers());
            while(!repeated.step(level,move->{},120,4_000_000L)){}
            check(repeated.rejectedTransfers()==0&&repeated.movedItems()==0,
                    "unchanged real warehouse must not reshuffle");
            check(repeated.quality().mixed()==0&&repeated.quality().score()==100,"pure real warehouse quality");
            var first=(ChestBlockEntity)level.getBlockEntity(full.containers().get(0).pos());
            var second=(ChestBlockEntity)level.getBlockEntity(full.containers().get(1).pos());
            ItemStack marked=new ItemStack(Items.IRON_INGOT,64);
            marked.getOrCreateTag().putString("sorter_test","variant_a");
            ItemStack markedB=new ItemStack(Items.COPPER_INGOT,64);
            markedB.getOrCreateTag().putString("sorter_test","variant_b");
            first.setItem(0,marked);second.setItem(0,markedB);
            var taggedBefore=ledger(full.containers());
            var tagged=WarehouseVacuumSorter.begin(full.containers());
            while(!tagged.step(level,move->{},120,4_000_000L)){}
            check(tagged.rejectedTransfers()==0&&taggedBefore.equals(ledger(full.containers())),
                    "tagged item conservation");
            // Independent synthetic Forge capability at real chest positions.
            var a=full.containers().get(0);var b=full.containers().get(1);
            var ha=new net.minecraftforge.items.ItemStackHandler(4);
            var hb=new net.minecraftforge.items.ItemStackHandler(4){
                boolean failed=false;
                @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
                    if(!simulate&&!failed){failed=true;return stack.copy();}
                    return super.insertItem(slot,stack,simulate);
                }
            };
            for(int i=0;i<4;i++){
                ha.setStackInSlot(i,new ItemStack(i%2==0?Items.IRON_INGOT:Items.COPPER_INGOT,64));
                hb.setStackInSlot(i,new ItemStack(i%2==0?Items.COPPER_INGOT:Items.IRON_INGOT,64));
            }
            var ra=new ContainerRef(a.pos(),new com.kirill.simplesorter.sorting.ForgeItemHandlerAdapter(ha),a.identity(),level);
            var rb=new ContainerRef(b.pos(),new com.kirill.simplesorter.sorting.ForgeItemHandlerAdapter(hb),b.identity(),level);
            var fake=List.of(ra,rb);
            var original=ledger(fake);
            var rejected=WarehouseVacuumSorter.begin(fake);
            int guard=0;
            while(!rejected.step(level,move->{},120,4_000_000L)){check(++guard<200,"unbounded rollback test");}
            check(rejected.rejectedTransfers()>0&&!rejected.unsafe(),"expected recoverable insertion rejection");
            check(original.equals(ledger(fake)),"rollback lost or duplicated NBT items");
            var stale=WarehouseVacuumSorter.begin(fake);
            check(!stale.step(level,move->{},1,4_000_000L),"snapshot should yield");
            ha.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,17));
            var externallyEdited=ledger(fake);
            guard=0;
            while(!stale.step(level,move->{},1,4_000_000L)){check(++guard<200,"unbounded stale snapshot test");}
            check(stale.rejectedTransfers()>0&&stale.movedItems()==0,"stale snapshot must not commit");
            check(externallyEdited.equals(ledger(fake)),"external edit overwritten");
            org.slf4j.LoggerFactory.getLogger("SimpleSorter/CoreLiveTest").info(
                    "SORTER_CORE_LIVE_TEST_PASSED: live 21->22 chest scan, remote access, new empty chest, classification and item count; moved {} items",session.movedItems());
        } finally {
            for(BlockPos p:placed){
                if(level.getBlockEntity(p) instanceof ChestBlockEntity chest)chest.clearContent();
                level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            }
        }
    }
    private static java.util.Map<net.minecraft.nbt.CompoundTag,Long> ledger(List<ContainerRef> refs){
        List<ItemStack> all=new ArrayList<>();
        for(ContainerRef ref:refs)all.addAll(com.kirill.simplesorter.sorting.InventoryConservation.snapshot(ref.handler()));
        return com.kirill.simplesorter.sorting.InventoryConservation.count(all);
    }
    private static long countItem(List<ContainerRef> refs,net.minecraft.world.item.Item item){
        long total=0;for(ContainerRef ref:refs)for(int s=0;s<ref.slots();s++){
            var stack=ref.handler().getStackInSlot(s);if(stack.is(item))total+=stack.getCount();
        }return total;
    }
    private static long count(List<ContainerRef> refs) {
        long count=0;
        for(ContainerRef ref:refs)for(int slot=0;slot<ref.slots();slot++)
            count+=ref.handler().getStackInSlot(slot).getCount();
        return count;
    }
}
