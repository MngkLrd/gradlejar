package dev.mngklrd.hearthbound;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemHandlerHelper;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blockentities.*;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.common.capabilities.heat.*;
import net.dries007.tfc.common.recipes.*;
import javax.annotation.Nullable;

public final class VesselEntity extends BlockEntity {
    private static final TagKey<Item> SOUP_INPUT=TagKey.create(Registries.ITEM,new ResourceLocation("tfc","foods/usable_in_soup"));
    private final NonNullList<ItemStack> ingredients=NonNullList.withSize(5,ItemStack.EMPTY);
    private final int[] exposure=new int[5];
    private final FluidTank tank;
    private PotBlockEntity.PotInventory sampleInventory;
    private int recipeState,plannedBatches,plannedServings;
    private ItemStack finished=ItemStack.EMPTY;
    private float heat=0, incoming=0, progress=0;
    private int hotTicks,overheatTicks,finishedHotTicks;
    private ResourceLocation activeRecipe;
    private long lastIncoming=Long.MIN_VALUE,lastTick=-1;
    private LazyOptional<IHeatBlock> heatCap=createHeatCap();
    private LazyOptional<IHeatBlock> createHeatCap(){return LazyOptional.of(()->new IHeatBlock(){
        public float getTemperature(){return heat;}
        public void setTemperature(float t){if(level!=null&&!level.isClientSide&&Float.isFinite(t)){incoming=Math.max(0,t);lastIncoming=level.getGameTime();}}
    });}

    public VesselEntity(BlockPos p,BlockState s){
        super(Hearthbound.VESSEL.get(),p,s);
        tank=new FluidTank(capacity()*1000,fluid->fluid.getFluid().is(TFCTags.Fluids.USABLE_IN_POT)){
            @Override protected void onContentsChanged(){setChanged();}
        };
    }
    private int capacity(){return ((VesselBlock)getBlockState().getBlock()).batches;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){
        if(cap==HeatCapability.BLOCK_CAPABILITY&&(side==null||side==Direction.DOWN))return heatCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();heatCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();heatCap=createHeatCap();}
    private void say(Player p,String key,Object... args){p.displayClientMessage(Component.translatable("hearthbound."+key,args),true);}
    private boolean lid(){return getBlockState().getValue(VesselBlock.LID);}
    private void sound(SoundEvent sound){if(level!=null)level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,.7f,1f);}
    private void stage(int n){
        if(level!=null&&getBlockState().getValue(VesselBlock.STAGE)!=n)level.setBlock(worldPosition,getBlockState().setValue(VesselBlock.STAGE,n),3);
    }
    private int batchCount(){
        return QualityMath.batches(capacity(),tank.getFluidAmount(),ingredients.stream().mapToInt(ItemStack::getCount).toArray());
    }
    private int amount(){int n=0;for(ItemStack s:ingredients)n+=s.getCount();return n;}
    private void resetProgress(){progress=0;hotTicks=0;overheatTicks=0;activeRecipe=null;}
    public ItemStack preview(int i){return ingredients.get(i).copy();}
    public int displayData(int i){
        return switch(i){
            case 0 -> tank.getFluidAmount(); case 1 -> tank.getCapacity();
            case 2 -> getBlockState().getValue(VesselBlock.STAGE);
            case 3 -> lid()?1:0;
            case 4 -> finished.getCount(); case 5 -> finished.isEmpty()?-1:Quality.get(finished);
            case 6 -> heat<180?0:heat<300?1:2;
            case 7 -> recipeState; case 8 -> plannedBatches; case 9 -> plannedServings;
            default -> 0;
        };
    }
    public boolean canUse(Player p){return level!=null&&!level.isClientSide&&p.level()==level&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    public void menuToggle(Player p){if(canUse(p)){toggleLid();}}
    public void removeIngredient(Player p,int i){
        if(!canUse(p)||lid()||!finished.isEmpty()||i<0||i>=5||ingredients.get(i).isEmpty())return;
        ItemHandlerHelper.giveItemToPlayer(p,ingredients.get(i));ingredients.set(i,ItemStack.EMPTY);exposure[i]=0;resetProgress();setChanged();
    }
    private void openMenu(Player p){
        if(p instanceof net.minecraft.server.level.ServerPlayer server)
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,player)->new VesselMenu(id,inv,this),getBlockState().getBlock().getName()));
    }
    private void status(Player p){
        if(!finished.isEmpty()){say(p,"ready",finished.getCount(),Quality.get(finished));return;}
        int n=batchCount();
        String state=heat<180?"cold":heat<300?"warming":lid()?"simmering":"boiling";
        say(p,"status",amount(),capacity()*5,tank.getFluidAmount(),tank.getCapacity(),Component.translatable("hearthbound.state."+state),n==0?Component.translatable("hearthbound.unbalanced"):Component.translatable("hearthbound.batches",n));
    }
    public InteractionResult interact(Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(!finished.isEmpty()){
            if(held.is(TFCTags.Items.SOUP_BOWLS)){
                if(lid()){say(player,"open_lid");return InteractionResult.CONSUME;}
                ItemStack portion=finished.copyWithCount(1);
                if(FoodCapability.get(portion) instanceof DynamicBowlHandler bowl)bowl.setBowl(held);
                if(!player.getAbilities().instabuild)held.shrink(1);
                finished.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player,portion);
                sound(SoundEvents.BUCKET_FILL);
                if(finished.isEmpty()){finished=ItemStack.EMPTY;finishedHotTicks=0;stage(tank.isEmpty()&&amount()==0?0:1);}
                setChanged();return InteractionResult.CONSUME;
            }
            if(held.isEmpty()&&player.isShiftKeyDown()){toggleLid();status(player);return InteractionResult.CONSUME;}
            if(held.isEmpty())openMenu(player);else status(player);
            return InteractionResult.CONSUME;
        }
        if(held.isEmpty()){
            if(player.isShiftKeyDown()){toggleLid();status(player);}
            else openMenu(player);
            return InteractionResult.CONSUME;
        }
        if(lid()){say(player,"open_lid");return InteractionResult.CONSUME;}
        if(held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()){
            if(FluidUtil.interactWithFluidHandler(player,hand,tank)){resetProgress();setChanged();stage(1);status(player);return InteractionResult.CONSUME;}
        }
        if(player.isShiftKeyDown()&&held.is(Items.STICK)){
            for(int i=4;i>=0;i--)if(!ingredients.get(i).isEmpty()){
                ItemHandlerHelper.giveItemToPlayer(player,ingredients.get(i));ingredients.set(i,ItemStack.EMPTY);exposure[i]=0;
                resetProgress();setChanged();status(player);return InteractionResult.CONSUME;
            }
        }
        addIngredient(player,held,player.isShiftKeyDown());status(player);return InteractionResult.CONSUME;
    }
    public int addIngredient(Player player,ItemStack held,boolean all){return addIngredient(player,held,all,-1,false);}
    public int addIngredient(Player player,ItemStack held,boolean all,int preferred,boolean fromInventory){
        if(!canUse(player)||lid()||!finished.isEmpty()||held.isEmpty()||FoodCapability.get(held)==null||!Quality.fresh(held))return 0;
        if(!held.getCraftingRemainingItem().isEmpty()){say(player,"container_ingredient");return 0;}
        int index=-1,empty=-1;
        if(preferred>=0&&preferred<5){
            ItemStack selected=ingredients.get(preferred);
            if(selected.isEmpty()||Quality.ingredientCompatible(selected,held))index=preferred;
            else {say(player,"slot_mismatch");return 0;}
        }else{
            for(int i=0;i<5;i++){
                ItemStack s=ingredients.get(i);
                if(s.isEmpty()){if(empty<0)empty=i;}
                else if(s.getCount()<Math.min(capacity(),held.getMaxStackSize())&&Quality.ingredientCompatible(s,held)){index=i;break;}
            }
            if(index<0)index=empty;
        }
        if(index<0){say(player,"full");return 0;}
        ItemStack old=ingredients.get(index);
        int max=Math.min(capacity(),held.getMaxStackSize());
        if(old.getCount()>=max){say(player,"full");return 0;}
        int n=Math.min(all?held.getCount():1,max-old.getCount());ItemStack add=held.copyWithCount(n);
        if(old.isEmpty()){ingredients.set(index,add);exposure[index]=0;}
        else{
            IFood a=FoodCapability.get(old),b=FoodCapability.get(add);
            if(a!=null&&b!=null)a.setCreationDate(Math.min(a.getCreationDate(),b.getCreationDate()));
            exposure[index]=(int)((long)exposure[index]*old.getCount()/(old.getCount()+n));
            int q=Quality.has(old)&&Quality.has(add)?QualityMath.merged(Quality.get(old),old.getCount(),Quality.get(add),n):-1;
            old.grow(n);if(q>=0)Quality.set(old,q);
        }
        if(fromInventory||!player.getAbilities().instabuild)held.shrink(n);
        resetProgress();setChanged();sound(SoundEvents.GENERIC_SPLASH);stage(1);return n;
    }
    private void toggleLid(){
        level.setBlock(worldPosition,getBlockState().setValue(VesselBlock.LID,!lid()),3);
        sound(SoundEvents.WOODEN_TRAPDOOR_CLOSE);setChanged();
    }
    public void spill(){
        if(level==null)return;
        for(int i=0;i<5;i++){Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,ingredients.get(i));ingredients.set(i,ItemStack.EMPTY);}
        finished=ItemStack.EMPTY; // a broken vessel spills its liquid; never generate free bowls
        tank.setFluid(FluidStack.EMPTY);
    }
    private PotBlockEntity.PotInventory sample(){
        // An unplaced, isolated TFC inventory: no level, ticker, block mutation or capability side effects.
        if(sampleInventory==null){
            PotBlockEntity virtual=new PotBlockEntity(worldPosition,TFCBlocks.POT.get().defaultBlockState()){
                @Override public void setAndUpdateSlots(int slot){}
            };
            sampleInventory=new PotBlockEntity.PotInventory(virtual);
        }
        var inv=sampleInventory;
        var fluid=inv.getFluidHandler();
        fluid.drain(1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if(tank.getFluidAmount()>=1000)fluid.fill(new FluidStack(tank.getFluid().getFluid(),1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        for(int i=0;i<5;i++)inv.setStackInSlot(4+i,ingredients.get(i).copyWithCount(1));
        return inv;
    }
    private PotRecipe recipeFor(PotBlockEntity.PotInventory inv){
        if(level==null)return null;
        PotRecipe r=level.getRecipeManager().getRecipeFor(TFCRecipeTypes.POT.get(),inv,level).orElse(null);
        if(r instanceof SoupPotRecipe)return r;
        for(PotRecipe candidate:level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.POT.get()))
            if(candidate instanceof SoupPotRecipe&&candidate.matches(inv,level))return candidate;
        return null;
    }
    private void finish(PotRecipe recipe,PotBlockEntity.PotInventory inv,int n){
        if(!recipe.matches(inv,level))return;
        PotRecipe.Output result;
        RecipeHelpers.setCraftingInput(inv,4,9);
        try { result=recipe.getOutput(inv); } finally { RecipeHelpers.clearCraftingInput(); }
        if(!(result instanceof SoupPotRecipe.SoupOutput soup)||soup.stack().isEmpty())return;
        ItemStack out=soup.stack();
        int servings=out.getCount()*n;
        if(servings<=0||servings>capacity()*3)return;
        int score=0,count=0;
        for(int i=0;i<5;i++)if(!ingredients.get(i).isEmpty()){
            score+=Quality.stageScore(Quality.role(ingredients.get(i)),exposure[i],Math.max(1,hotTicks));count++;
        }
        int stageScore=count==0?70:score/count;
        int tempScore=Mth.clamp(100-Math.round(100f*overheatTicks/Math.max(1,hotTicks)),0,100);
        int q=Math.round(stageScore*.55f+tempScore*.35f+10);
        java.util.List<ItemStack> consumed=new java.util.ArrayList<>();
        for(ItemStack ingredient:ingredients)if(!ingredient.isEmpty())consumed.add(ingredient.copyWithCount(n));
        finished=out.copyWithCount(servings);Quality.finish(finished,q,consumed);
        for(int i=0;i<5;i++){
            ItemStack ingredient=ingredients.get(i);
            if(ingredient.isEmpty())continue;
            ingredient.shrink(n);
            if(ingredient.isEmpty())ingredients.set(i,ItemStack.EMPTY);
            exposure[i]=0;
        }
        tank.drain(n*1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        resetProgress();finishedHotTicks=0;stage(3);sound(SoundEvents.GENERIC_EXTINGUISH_FIRE);setChanged();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,VesselEntity v){
        long now=level.getGameTime();
        if(Math.floorMod(now+pos.asLong(),20L)!=0)return;
        long elapsed=v.lastTick<0?20:Math.max(1,now-v.lastTick);
        int ticks=(int)Math.min(20,elapsed);
        if(elapsed>40)v.heat=QualityMath.advanceHeat(v.heat,0,v.lid()?.003f:.008f,(int)Math.min(10000,elapsed-20));
        v.lastTick=now;
        float target=v.lastIncoming!=Long.MIN_VALUE&&now-v.lastIncoming>=0&&now-v.lastIncoming<=2?v.incoming:0;
        if(level.getBlockEntity(pos.below()) instanceof AbstractFirepitBlockEntity<?> fire)target=Math.max(target,fire.getTemperature());
        float coefficient=target>v.heat?(v.capacity()==6?.018f:.012f):(v.lid()?.003f:.008f);
        v.heat=QualityMath.advanceHeat(v.heat,target,coefficient,ticks);
        if(v.heat<.1f)v.heat=0;
        if(!v.finished.isEmpty()){
            v.stage(v.heat>=180?3:4);
            if(v.heat>450&&Quality.has(v.finished)&&Quality.fresh(v.finished)){
                v.finishedHotTicks+=ticks;
                if(v.finishedHotTicks>=200){v.finishedHotTicks=0;Quality.set(v.finished,Quality.get(v.finished)-5);}
            }else v.finishedHotTicks=0;
            v.setChanged();return;
        }
        v.plannedBatches=0;v.plannedServings=0;
        if(v.amount()==0){v.recipeState=0;v.resetProgress();v.stage(v.tank.isEmpty()?0:1);v.setChanged();return;}
        if(v.tank.getFluidAmount()<1000){v.recipeState=1;v.resetProgress();v.stage(1);v.setChanged();return;}
        if(v.ingredients.stream().anyMatch(s->!s.isEmpty()&&!Quality.fresh(s))){
            v.recipeState=4;v.resetProgress();v.stage(1);v.setChanged();return;
        }
        // Preserve early ingredient exposure even before the final recipe matches.
        if(v.heat>=180)for(int i=0;i<5;i++)if(!v.ingredients.get(i).isEmpty())
            v.exposure[i]=Math.min(100000,v.exposure[i]+ticks);
        int n=v.batchCount();
        PotBlockEntity.PotInventory inv=v.sample();
        PotRecipe recipe=v.recipeFor(inv);
        if(n==0||recipe==null){v.recipeState=2;v.resetProgress();v.stage(1);v.setChanged();return;}
        v.recipeState=3;v.plannedBatches=n;
        v.plannedServings=n*(int)(v.ingredients.stream().filter(s->!s.isEmpty()).count()/2+1);
        if(!recipe.getId().equals(v.activeRecipe)){v.resetProgress();v.activeRecipe=recipe.getId();}
        if(recipe.isHotEnough(v.heat)){
            v.hotTicks+=ticks;
            if(recipe.isHotEnough(v.heat-170))v.overheatTicks+=ticks;
            float speed=v.lid()?.62f:1f;
            v.progress+=ticks*speed/(1f+.22f*(n-1));
            v.stage(2);
            if(v.progress>=Math.max(1,recipe.getDuration()))v.finish(recipe,inv,n);
        }else v.stage(1);
        v.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag t){
        super.saveAdditional(t);ContainerHelper.saveAllItems(t,ingredients);t.putIntArray("Exposure",exposure);
        t.put("Tank",tank.writeToNBT(new CompoundTag()));t.putFloat("Heat",heat);t.putFloat("Progress",progress);t.putInt("Hot",hotTicks);t.putInt("Overheat",overheatTicks);t.putInt("FinishedHot",finishedHotTicks);t.putLong("LastTick",lastTick);
        if(activeRecipe!=null)t.putString("Recipe",activeRecipe.toString());
        if(!finished.isEmpty()){t.put("Finished",finished.copyWithCount(1).save(new CompoundTag()));t.putInt("Servings",finished.getCount());}
    }
    @Override public void load(CompoundTag t){
        super.load(t);ingredients.clear();ContainerHelper.loadAllItems(t,ingredients);
        Arrays.fill(exposure,0);int[] a=t.getIntArray("Exposure");System.arraycopy(a,0,exposure,0,Math.min(a.length,5));
        tank.readFromNBT(t.getCompound("Tank"));heat=Float.isFinite(t.getFloat("Heat"))?Math.max(0,t.getFloat("Heat")):0;progress=Float.isFinite(t.getFloat("Progress"))?Math.max(0,t.getFloat("Progress")):0;hotTicks=Math.max(0,t.getInt("Hot"));overheatTicks=Mth.clamp(t.getInt("Overheat"),0,hotTicks);finishedHotTicks=Mth.clamp(t.getInt("FinishedHot"),0,199);lastTick=t.contains("LastTick")?t.getLong("LastTick"):-1;
        activeRecipe=t.contains("Recipe")?ResourceLocation.tryParse(t.getString("Recipe")):null;
        finished=ItemStack.of(t.getCompound("Finished"));if(!finished.isEmpty())finished.setCount(Mth.clamp(t.getInt("Servings"),0,capacity()*3));
    }
}
