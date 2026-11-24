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

public final class ChamberSeed extends Item {
    public ChamberSeed(Properties p){super(p);}
    private static int tier(CompoundTag t){return Math.floorMod(t.getInt("room_tier"),PocketSpace.SIZES.length);}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!c.getLevel().getBlockState(c.getClickedPos()).is(WorldFold.ANCHOR.get()))return InteractionResult.PASS;
        if(c.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;
        ItemStack stack=c.getItemInHand();int tier=tier(ToolData.read(stack)),cost=PocketSpace.COSTS[tier];
        if(!p.getAbilities().instabuild&&stack.getCount()<cost){FoldNetwork.message(p,"seed_cost",cost);return InteractionResult.CONSUME;}
        try{
            ServerLevel l=p.serverLevel();FoldData data=FoldData.get(p.getServer());
            if(l.dimension().equals(PocketSpace.KEY)){FoldNetwork.message(p,"no_nested_seed");return InteractionResult.CONSUME;}
            if(data.limit(p.getUUID()))throw new IllegalArgumentException("limit");
            Anchor a=Anchor.at(l,c.getClickedPos(),3,3,1);a.loadChunks(l);
            if(!a.clear(l))throw new IllegalArgumentException("blocked");if(!data.free(a))throw new IllegalArgumentException("overlap");
            int index=data.allocate(a);
            Anchor b=PocketSpace.build(p.getServer(),index,tier);
            FoldNetwork.create(p,a,b);
            if(!p.getAbilities().instabuild)stack.shrink(cost);
            FoldNetwork.message(p,"room_created",PocketSpace.SIZES[tier],PocketSpace.SIZES[tier]);
        }catch(IllegalArgumentException e){ToolData.error(p,e);}
        catch(ReflectiveOperationException|RuntimeException e){LogUtils.getLogger().error("World Fold chamber creation failed",e);FoldNetwork.message(p,"failure");}
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
        ItemStack s=p.getItemInHand(h);if(l.isClientSide)return InteractionResultHolder.success(s);
        CompoundTag t=ToolData.read(s);t.putInt("room_tier",(tier(t)+1)%PocketSpace.SIZES.length);ToolData.write(s,t);
        int i=tier(t);FoldNetwork.message(p,"room_size",PocketSpace.SIZES[i],PocketSpace.SIZES[i],PocketSpace.COSTS[i]);
        return InteractionResultHolder.consume(s);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        int i=tier(ToolData.read(s));lines.add(ToolData.text("room_size",PocketSpace.SIZES[i],PocketSpace.SIZES[i],PocketSpace.COSTS[i]));
        lines.add(ToolData.text("seed_help"));
    }
}
