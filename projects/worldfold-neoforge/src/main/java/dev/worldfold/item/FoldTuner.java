package dev.worldfold.item;

import com.mojang.logging.LogUtils;
import dev.worldfold.WorldFold;
import dev.worldfold.fold.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class FoldTuner extends Item {
    private static final int MODES=5;
    public FoldTuner(Properties p){super(p);}
    private static int mode(CompoundTag t){return Math.floorMod(t.getInt("tuner_mode"),MODES);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!c.getLevel().getBlockState(c.getClickedPos()).is(WorldFold.ANCHOR.get()))return InteractionResult.PASS;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
        ItemStack stack=c.getItemInHand();CompoundTag t=ToolData.read(stack);
        if(!ToolData.owner(t,p)){FoldNetwork.message(p,"owner");return InteractionResult.CONSUME;}
        FoldData data=FoldData.get(p.getServer());ServerLevel l=p.serverLevel();
        FoldLink f=data.find(l.dimension(),c.getClickedPos());
        try{
            switch(mode(t)){
                case 0->{if(f==null)FoldNetwork.message(p,"not_linked");else FoldNetwork.status(p,f);}
                case 1->{if(f==null)FoldNetwork.message(p,"not_linked");else FoldNetwork.flow(p,f);}
                case 2->{
                    if(t.hasUUID("branch_link")){
                        FoldLink selected=data.links.get(t.getUUID("branch_link"));
                        if(selected==null){ToolData.clearSelection(t);ToolData.write(stack,t);FoldNetwork.message(p,"not_linked");break;}
                        if(f!=null)throw new IllegalArgumentException("overlap");
                        Anchor a=Anchor.at(l,c.getClickedPos(),selected.b.w(),selected.b.h(),selected.b.scale());
                        FoldNetwork.addBranch(p,selected,a);
                        ToolData.clearSelection(t);ToolData.write(stack,t);FoldNetwork.message(p,"branch_added");
                    }else{
                        if(f==null){FoldNetwork.message(p,"not_linked");break;}
                        if(!FoldNetwork.owner(p,f))throw new IllegalArgumentException("owner");
                        if(f.c!=null)throw new IllegalArgumentException("already_branch");
                        if(!f.a.dimension().equals(l.dimension())||!f.a.floor().equals(c.getClickedPos())){FoldNetwork.message(p,"branch_source");break;}
                        t.putUUID("branch_link",f.id);t.putUUID("selection_owner",p.getUUID());ToolData.write(stack,t);FoldNetwork.message(p,"branch_pending");
                    }
                }
                case 3->{if(f==null)FoldNetwork.message(p,"not_linked");else if(!p.isShiftKeyDown())FoldNetwork.message(p,"confirm_remove");else FoldNetwork.dismantle(p,f);}
                case 4->{if(f==null)FoldNetwork.message(p,"not_linked");else FoldNetwork.repair(p,f);}
            }
        }catch(IllegalArgumentException e){ToolData.error(p,e);}
        catch(ReflectiveOperationException|RuntimeException e){LogUtils.getLogger().error("World Fold tuner failed",e);FoldNetwork.message(p,"failure");}
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
        ItemStack s=p.getItemInHand(h);if(l.isClientSide)return InteractionResultHolder.success(s);
        if(!(p instanceof ServerPlayer sp))return InteractionResultHolder.pass(s);
        CompoundTag t=ToolData.read(s);
        if(!ToolData.owner(t,sp)){FoldNetwork.message(p,"owner");return InteractionResultHolder.consume(s);}
        if(p.isShiftKeyDown()){ToolData.clearSelection(t);FoldNetwork.message(p,"selection_clear");}
        else{ToolData.clearSelection(t);t.putInt("tuner_mode",(mode(t)+1)%MODES);FoldNetwork.message(p,"tuner_mode",ToolData.text("tuner."+mode(t)));}
        ToolData.write(s,t);return InteractionResultHolder.consume(s);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        CompoundTag t=ToolData.read(s);lines.add(ToolData.text("tuner_mode",ToolData.text("tuner."+mode(t))));
        lines.add(ToolData.text("tuner_help"));if(t.hasUUID("branch_link"))lines.add(ToolData.text("branch_pending"));
    }
}
