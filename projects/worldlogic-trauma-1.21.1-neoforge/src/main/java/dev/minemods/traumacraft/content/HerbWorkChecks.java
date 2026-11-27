package dev.minemods.traumacraft.content;
import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
public final class HerbWorkChecks {
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException("Herb QA: "+label);}
    private static HerbWorkpieceEntity piece(ServerLevel l,BlockPos pos){return (HerbWorkpieceEntity)l.getBlockEntity(pos);}
    public static void verify(ServerLevel l,ServerPlayer p){
        var support=new BlockPos(10,99,8);var at=support.above();var box=new AABB(at).inflate(1.5);
        var hand=p.getMainHandItem().copy();var old=l.getBlockState(support);var oldAbove=l.getBlockState(at);
        try{
            l.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(support,Blocks.DIRT.defaultBlockState());
            ItemStack raw=new ItemStack(ModItems.RAW_HERBS.get(),2);
            check(!HerbInteraction.place(l,support,p,raw)&&raw.getCount()==2,"invalid surface no consumption");
            l.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
            var body=dev.minemods.traumacraft.health.BodyStates.get(p);
            if(dev.minemods.traumacraft.integration.WorldLogicBridge.functionsEnabled(p)){
                var arm=dev.minemods.traumacraft.integration.WorldLogicBridge.mainArm(p);
                body.damage(arm,arm.maximum);
                check(!HerbInteraction.place(l,support,p,raw)&&raw.getCount()==2&&l.isEmptyBlock(at),"destroyed main arm rejects placement without consumption");
                body.reset();
            }
            check(HerbInteraction.place(l,support,p,raw)&&raw.getCount()==1,"placement consumes one");
            check(!HerbInteraction.place(l,support,p,raw)&&raw.getCount()==1,"occupied surface rejected");
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));
            check(HerbInteraction.strike(l,at,p,5,false).equals("ignored")&&piece(l,at).mask()==0,"wrong tool rejected");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            check(HerbInteraction.strike(l,at,p,5,false).equals("changed")&&piece(l,at).mask()==(1<<5),"hand crush single cell");
            check(HerbInteraction.strike(l,at,p,5,false).equals("ignored"),"repeat cell ignored");
            var saved=piece(l,at).saveWithoutMetadata(l.registryAccess());
            var reload=new HerbWorkpieceEntity(at,l.getBlockState(at));reload.loadWithComponents(saved,l.registryAccess());
            check(reload.mask()==(1<<5),"NBT preserves partial");
            check(HerbInteraction.strike(l,at,p,5,true).equals("removed")&&l.isEmptyBlock(at),"lift partial");
            var drops=l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ModItems.RAW_HERBS.get()));
            check(drops.size()==1&&HerbWorkpieceEntity.fromItem(drops.getFirst().getItem())==(1<<5),"exact partial drop");
            var recovered=drops.getFirst().getItem().copy();drops.getFirst().discard();
            check(HerbInteraction.place(l,support,p,recovered)&&piece(l,at).mask()==(1<<5),"resume partial");
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FLINT));
            check(HerbInteraction.strike(l,at,p,10,false).equals("changed")&&HerbMask.progress(piece(l,at).mask())==6,"flint crushes neighboring cells");
            check(HerbInteraction.strike(l,at,p,10,false).equals("ignored"),"repeat no progress");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            for(int i=0;i<16;i++)if(!HerbMask.done(piece(l,at).mask(),i))check(HerbInteraction.strike(l,at,p,i,false).equals("changed"),"remaining cell "+i);
            check(HerbMask.finished(piece(l,at).mask()),"all cells required");
            check(HerbInteraction.strike(l,at,p,0,false).equals("removed")&&l.isEmptyBlock(at),"final strike collects");
            var result=l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ModItems.CRUSHED_HERBS.get()));
            check(result.stream().mapToInt(e->e.getItem().getCount()).sum()==2,"exact two processed herbs");
            check(HerbInteraction.strike(l,at,p,0,false).equals("ignored"),"no duplicate collection");result.forEach(ItemEntity::discard);
            check(HerbInteraction.place(l,support,p,raw),"place again");
            l.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());check(l.isEmptyBlock(at),"support loss removes workpiece");
            check(l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ModItems.RAW_HERBS.get())).stream().mapToInt(e->e.getItem().getCount()).sum()==1,"support loss single drop");
        }finally{
            l.setBlockAndUpdate(at,oldAbove);l.setBlockAndUpdate(support,old);p.setItemSlot(EquipmentSlot.MAINHAND,hand);
            l.getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ModItems.RAW_HERBS.get())||e.getItem().is(ModItems.CRUSHED_HERBS.get())).forEach(ItemEntity::discard);
        }
    }
}
