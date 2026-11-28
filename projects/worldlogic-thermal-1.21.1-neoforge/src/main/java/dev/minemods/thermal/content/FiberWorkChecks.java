package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
public final class FiberWorkChecks {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException("Fiber QA: "+label);}
    private static FiberWorkpieceEntity piece(ServerLevel l,BlockPos at){return (FiberWorkpieceEntity)l.getBlockEntity(at);}
    public static void verify(ServerLevel l,ServerPlayer p){
        var support=new BlockPos(10,99,8);var at=support.above();
        var box=new AABB(at).inflate(1.5);
        var hand=p.getMainHandItem().copy();var old=l.getBlockState(support);
        try{
            l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(support,Blocks.DIRT.defaultBlockState());
            ItemStack raw=new ItemStack(ThermalContent.RAW_FIBER.get(),2);
            check(!FiberInteraction.place(l,support,p,raw)&&raw.getCount()==2,"dirt rejected without item consumption");
            l.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
            check(FiberInteraction.place(l,support,p,raw)&&raw.getCount()==1,"place consumes exactly one bundle");
            check(!FiberInteraction.place(l,support,p,raw)&&raw.getCount()==1,"occupied surface rejected");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            check(FiberInteraction.strike(l,at,p,9,false).equals("changed"),"hand scrapes targeted cell");
            check(piece(l,at).mask()==(1L<<9),"only selected cell changes");
            check(FiberInteraction.strike(l,at,p,9,false).equals("ignored"),"repeat cell no effect");
            var saved=piece(l,at).saveWithoutMetadata(l.registryAccess());
            var reloaded=new FiberWorkpieceEntity(at,l.getBlockState(at));reloaded.loadWithComponents(saved,l.registryAccess());
            check(reloaded.mask()==piece(l,at).mask(),"NBT reload preserves exact mask");
            ItemStack partial=piece(l,at).result();
            check(partial.is(ThermalContent.RAW_FIBER.get())&&FiberWorkpieceEntity.fromItem(partial)==(1L<<9),"partial result preserves work");
            check(FiberInteraction.strike(l,at,p,9,true).equals("removed")&&l.isEmptyBlock(at),"sneak strike lifts partial");
            var drops=l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ThermalContent.RAW_FIBER.get())&&FiberWorkpieceEntity.fromItem(e.getItem())==(1L<<9));
            check(drops.size()==1,"partial removal drops once");
            ItemStack recovered=drops.getFirst().getItem().copy();drops.getFirst().discard();
            check(FiberInteraction.place(l,support,p,recovered)&&piece(l,at).mask()==(1L<<9),"replaced bundle restores exact mask");
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ThermalContent.SCRAPER.get()));
            check(FiberInteraction.strike(l,at,p,27,false).equals("changed"),"scraper processes neighboring cells");
            check(FiberMask.progress(piece(l,at).mask())==6&&p.getMainHandItem().getDamageValue()==1,"five cells and one durability");
            check(FiberInteraction.strike(l,at,p,27,false).equals("ignored")&&p.getMainHandItem().getDamageValue()==1,"no wear on cleaned area");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            for(int i=0;i<64;i++)if(!FiberMask.clean(piece(l,at).mask(),i))check(FiberInteraction.strike(l,at,p,i,false).equals("changed"),"clean remaining cell "+i);
            check(FiberMask.finished(piece(l,at).mask()),"full mask requires all cells");
            check(piece(l,at).result().is(ThermalContent.FIBER.get())&&piece(l,at).result().getCount()==4,"complete result four fibers");
            check(FiberInteraction.strike(l,at,p,0,false).equals("removed")&&l.isEmptyBlock(at),"extra strike removes finished work");
            var fibers=l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ThermalContent.FIBER.get()));
            check(fibers.stream().mapToInt(e->e.getItem().getCount()).sum()==4,"four fibers dropped once");
            check(FiberInteraction.strike(l,at,p,0,false).equals("ignored"),"no duplicate collection");
            fibers.forEach(ItemEntity::discard);
            check(FiberInteraction.place(l,support,p,new ItemStack(ThermalContent.RAW_FIBER.get())),"second placement");
            l.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());
            check(l.isEmptyBlock(at),"support removal detaches work");
        }finally{
            l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(support,old);p.setItemSlot(EquipmentSlot.MAINHAND,hand);
            l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ThermalContent.RAW_FIBER.get())||e.getItem().is(ThermalContent.FIBER.get())).forEach(ItemEntity::discard);
        }
    }
}
