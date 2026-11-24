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
            check(!((ChestBlockEntity)level.getBlockEntity(newPos)).isEmpty(),"new chest not used");
            org.slf4j.LoggerFactory.getLogger("SimpleSorter/CoreLiveTest").info(
                    "SORTER_CORE_LIVE_TEST_PASSED: live 21->22 chest scan, remote access, new empty chest, classification and item count; moved {} items",session.movedItems());
        } finally {
            for(BlockPos p:placed){
                if(level.getBlockEntity(p) instanceof ChestBlockEntity chest)chest.clearContent();
                level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            }
        }
    }
    private static long count(List<ContainerRef> refs) {
        long count=0;
        for(ContainerRef ref:refs)for(int slot=0;slot<ref.slots();slot++)
            count+=ref.handler().getStackInSlot(slot).getCount();
        return count;
    }
}
