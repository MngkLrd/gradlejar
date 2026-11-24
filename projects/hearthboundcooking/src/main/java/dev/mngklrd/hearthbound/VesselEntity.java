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
    private ItemStack finished=ItemStack.EMPTY;
    private float heat=0, incoming=0, progress=0;
    private int hotTicks,overheatTicks,finishedHotTicks;
    private ResourceLocation activeRecipe;
    private long lastIncoming=Long.MIN_VALUE,lastTick=-1;
    private final LazyOptional<IHeatBlock> heatCap=LazyOptional.of(()->new IHeatBlock(){
        public float getTemperature(){return 0;}
        public void setTemperature(float t){if(level!=null&&!level.isClientSide){incoming=Math.max(0,t);lastIncoming=level.getGameTime();}}
    });

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
    private void say(Player p,String key,Object... args){p.displayClientMessage(Component.translatable("hearthbound."+key,args),true);}
    private boolean lid(){return getBlockState().getValue(VesselBlock.LID);}
    private void sound(SoundEvent sound){if(level!=null)level.playSound(null,worldPosition,sound,SoundSource.BLOCKS,.7f,1f);}
    private void stage(int n){
        if(level!=null&&getBlockState().getValue(VesselBlock.STAGE)!=n)level.setBlock(worldPosition,getBlockState().setValue(VesselBlock.STAGE,n),3);
    }
    private int batchCount(){
        int n=-1,distinct=0;
        for(ItemStack s:ingredients)if(!s.isEmpty()){
            distinct++;if(n<0)n=s.getCount();else if(n!=s.getCount())return 0;
        }
        return distinct>=3&&n<=capacity()?n:0;
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
            default -> 0;
        };
    }
    public boolean canUse(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
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
                ItemStack portion=finished.copyWithCount(1);
                if(FoodCapability.get(portion) instanceof DynamicBowlHandler bowl)bowl.setBowl(held);
                if(!player.getAbilities().instabuild)held.shrink(1);
                finished.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player,portion);
                sound(SoundEvents.BUCKET_FILL);
                if(finished.isEmpty()){finished=ItemStack.EMPTY;finishedHotTicks=0;stage(tank.isEmpty()?0:1);}
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
    public int addIngredient(Player player,ItemStack held,boolean all){
        if(!canUse(player)||lid()||!finished.isEmpty()||!held.is(SOUP_INPUT)||FoodCapability.get(held)==null||!Quality.fresh(held))return 0;
        if(!held.getCraftingRemainingItem().isEmpty()){say(player,"container_ingredient");return 0;}
        int index=-1,empty=-1;
        for(int i=0;i<5;i++){
            ItemStack s=ingredients.get(i);
            if(s.isEmpty()){if(empty<0)empty=i;}
            else if(FoodCapability.areStacksStackableExceptCreationDate(s,held)){index=i;break;}
        }
        if(index<0)index=empty;
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
            old.grow(n);
        }
        if(!player.getAbilities().instabuild)held.shrink(n);
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
        PotBlockEntity virtual=new PotBlockEntity(worldPosition,TFCBlocks.POT.get().defaultBlockState()){
            @Override public void setAndUpdateSlots(int slot){}
        };
        PotBlockEntity.PotInventory inv=new PotBlockEntity.PotInventory(virtual);
        inv.getFluidHandler().fill(new FluidStack(tank.getFluid().getFluid(),1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        int slot=4;
        for(ItemStack item:ingredients)if(!item.isEmpty())inv.setStackInSlot(slot++,item.copyWithCount(1));
        return inv;
    }
    private PotRecipe recipeFor(PotBlockEntity.PotInventory inv){
        if(level==null)return null;
        PotRecipe r=level.getRecipeManager().getRecipeFor(TFCRecipeTypes.POT.get(),inv,level).orElse(null);
        return r instanceof SoupPotRecipe?r:null;
    }
    private void finish(PotRecipe recipe,PotBlockEntity.PotInventory inv,int n){
        if(!recipe.matches(inv,level))return;
        PotRecipe.Output result;
        RecipeHelpers.setCraftingInput(inv,4,9);
        try { result=recipe.getOutput(inv); } finally { RecipeHelpers.clearCraftingInput(); }
        if(!(result instanceof SoupPotRecipe.SoupOutput soup)||soup.stack().isEmpty())return;
        ItemStack out=soup.stack();
        int servings=out.getCount()*n;
        if(servings>out.getMaxStackSize()){
            // TFC soups may have a low physical stack limit. The vessel stores a private portion count in NBT instead.
        }
        int score=0,count=0;
        for(int i=0;i<5;i++)if(!ingredients.get(i).isEmpty()){
            score+=Quality.stageScore(Quality.role(ingredients.get(i)),exposure[i],Math.max(1,hotTicks));count++;
        }
        int stageScore=count==0?70:score/count;
        int tempScore=Mth.clamp(100-Math.round(100f*overheatTicks/Math.max(1,hotTicks)),0,100);
        int q=Math.round(stageScore*.55f+tempScore*.35f+10);
        finished=out.copyWithCount(servings);Quality.finish(finished,q,ingredients);
        for(int i=0;i<5;i++){ingredients.set(i,ItemStack.EMPTY);exposure[i]=0;}
        tank.drain(n*1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        resetProgress();finishedHotTicks=0;stage(3);sound(SoundEvents.GENERIC_EXTINGUISH_FIRE);setChanged();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,VesselEntity v){
        // Heat is delivered by native TFC firepits and Firmalife oven bottoms. The direct
        // firepit read also removes order-of-ticking jitter without creating any extra fuel.
        long now=level.getGameTime();
        if(v.lastTick>=0&&now-v.lastTick>40){v.heat*=Math.pow(v.lid()?.997:.992,Math.min(10000,now-v.lastTick));}
        v.lastTick=now;
        float target=(level.getGameTime()-v.lastIncoming<=2&&v.lastIncoming!=Long.MIN_VALUE)?v.incoming:0;
        if(level.getBlockEntity(pos.below()) instanceof AbstractFirepitBlockEntity<?> fire)target=Math.max(target,fire.getTemperature());
        float coefficient=target>v.heat?(v.capacity()==6?.018f:.012f):(v.lid()?.003f:.008f);
        v.heat+=(target-v.heat)*coefficient;
        if(v.heat<.1f)v.heat=0;
        if(!v.finished.isEmpty()){
            if(level.getGameTime()%20==0)v.stage(v.heat>=180?3:4);
            if(v.heat>450&&++v.finishedHotTicks>=200){v.finishedHotTicks=0;Quality.set(v.finished,Quality.get(v.finished)-5);v.setChanged();}
            return;
        }
        if(v.heat>=180&&v.tank.getFluidAmount()>0){
            for(int i=0;i<5;i++)if(!v.ingredients.get(i).isEmpty())v.exposure[i]=Math.min(100000,v.exposure[i]+1);
        }
        // Resolve the real datapack recipe once per second, then advance in 20-tick steps.
        if(level.getGameTime()%20!=0)return;
        int n=v.batchCount();
        if(n==0||v.tank.getFluidAmount()<n*1000){
            v.resetProgress();v.stage(v.amount()>0||!v.tank.isEmpty()?1:0);v.setChanged();return;
        }
        if(v.ingredients.stream().anyMatch(s->!s.isEmpty()&&!Quality.fresh(s))){
            v.resetProgress();v.stage(1);v.setChanged();return;
        }
        if(v.heat<180){v.stage(1);v.setChanged();return;}
        PotBlockEntity.PotInventory inv=v.sample();
        PotRecipe recipe=v.recipeFor(inv);
        if(recipe==null){v.resetProgress();v.stage(1);v.setChanged();return;}
        if(!recipe.getId().equals(v.activeRecipe)){v.resetProgress();v.activeRecipe=recipe.getId();}
        if(recipe.isHotEnough(v.heat)){
            v.hotTicks+=20;
            if(recipe.isHotEnough(v.heat-170))v.overheatTicks+=20;
            float speed=v.lid()?.62f:1f;
            v.progress+=20*speed/(1f+.22f*(n-1));
            v.stage(2);
            if(v.progress>=recipe.getDuration())v.finish(recipe,inv,n);
        }else v.stage(1); // progress pauses, not resets, while cooling
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
        tank.readFromNBT(t.getCompound("Tank"));heat=t.getFloat("Heat");progress=t.getFloat("Progress");hotTicks=t.getInt("Hot");overheatTicks=t.getInt("Overheat");finishedHotTicks=t.getInt("FinishedHot");lastTick=t.contains("LastTick")?t.getLong("LastTick"):-1;
        activeRecipe=t.contains("Recipe")?ResourceLocation.tryParse(t.getString("Recipe")):null;
        finished=ItemStack.of(t.getCompound("Finished"));if(!finished.isEmpty())finished.setCount(Mth.clamp(t.getInt("Servings"),0,capacity()*3));
    }
}
