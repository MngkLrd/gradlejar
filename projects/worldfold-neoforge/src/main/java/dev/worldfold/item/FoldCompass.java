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

public final class FoldCompass extends Item {
    public FoldCompass(Properties p){super(p);}
    private static int size(CompoundTag t){return Math.floorMod(t.getInt("size"),SpatialMath.WIDTHS.length);}
    private static int mode(CompoundTag t){return Math.floorMod(t.getInt("scale"),SpatialMath.SCALES.length);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!c.getLevel().getBlockState(c.getClickedPos()).is(WorldFold.ANCHOR.get()))return InteractionResult.PASS;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
        ItemStack stack=c.getItemInHand();CompoundTag t=ToolData.read(stack);
        if(!ToolData.owner(t,p)){FoldNetwork.message(p,"owner");return InteractionResult.CONSUME;}
        try{
            ServerLevel l=p.serverLevel();FoldData data=FoldData.get(p.getServer());
            if(p.isShiftKeyDown()){
                ToolData.clearSelection(t);ToolData.write(stack,t);FoldNetwork.message(p,"selection_clear");return InteractionResult.CONSUME;
            }
            FoldLink existing=data.find(l.dimension(),c.getClickedPos());
            if(existing!=null){FoldNetwork.status(p,existing);return InteractionResult.CONSUME;}
            if(!t.contains("selection")){
                int i=size(t);Anchor a=Anchor.at(l,c.getClickedPos(),SpatialMath.WIDTHS[i],SpatialMath.HEIGHTS[i],1);
                a.loadChunks(l);if(!a.clear(l))throw new IllegalArgumentException("blocked");
                if(!data.free(a))throw new IllegalArgumentException("overlap");
                t.put("selection",a.save());t.putUUID("selection_owner",p.getUUID());ToolData.write(stack,t);
                FoldNetwork.message(p,"anchor_selected",a.width(),a.height());return InteractionResult.CONSUME;
            }
            Anchor a=Anchor.read(t.getCompound("selection"));
            Anchor b=Anchor.at(l,c.getClickedPos(),a.w(),a.h(),SpatialMath.SCALES[mode(t)]);
            FoldLink f=FoldNetwork.create(p,a,b);
            ToolData.clearSelection(t);ToolData.write(stack,t);
            FoldNetwork.message(p,"linked",f.id.toString().substring(0,8));
        }catch(IllegalArgumentException ex){ToolData.error(p,ex);}
        catch(ReflectiveOperationException|RuntimeException ex){LogUtils.getLogger().error("World Fold compass failed",ex);FoldNetwork.message(p,"failure");}
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
        ItemStack s=p.getItemInHand(h);if(l.isClientSide)return InteractionResultHolder.success(s);
        if(!(p instanceof ServerPlayer sp))return InteractionResultHolder.pass(s);
        CompoundTag t=ToolData.read(s);
        if(!ToolData.owner(t,sp)){FoldNetwork.message(p,"owner");return InteractionResultHolder.consume(s);}
        if(p.isShiftKeyDown()){
            t.putInt("scale",(mode(t)+1)%SpatialMath.SCALES.length);FoldNetwork.message(p,"scale",SpatialMath.SCALES[mode(t)]);
        }else if(t.contains("selection")){
            ToolData.clearSelection(t);FoldNetwork.message(p,"selection_clear");
        }else{
            t.putInt("size",(size(t)+1)%SpatialMath.WIDTHS.length);
            FoldNetwork.message(p,"size",SpatialMath.WIDTHS[size(t)],SpatialMath.HEIGHTS[size(t)]);
        }
        ToolData.write(s,t);return InteractionResultHolder.consume(s);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        CompoundTag t=ToolData.read(s);int i=size(t);
        lines.add(ToolData.text("size",SpatialMath.WIDTHS[i],SpatialMath.HEIGHTS[i]));
        lines.add(ToolData.text("scale",SpatialMath.SCALES[mode(t)]));
        if(t.contains("selection"))lines.add(ToolData.text("pending"));
        lines.add(ToolData.text("compass_help"));lines.add(ToolData.text("compass_help2"));
    }
}
