package dev.mngklrd.hearthbound;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
    private final NonNullList<ItemStack> ingredients=NonNullList.withSize(5,ItemStack.EMPTY);
    private final int[] exposure=new int[5];
    private final int[] excess=new int[5];
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
        ItemHandlerHelper.giveItemToPlayer(p,ingredients.get(i));ingredients.set(i,ItemStack.EMPTY);exposure[i]=0;excess[i]=0;setChanged();
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
        if(finished.isEmpty()&&held.is(TFCTags.Items.SOUP_BOWLS)){
            if(lid()){say(player,"open_lid");return InteractionResult.CONSUME;}
            if(amount()>0){
                if(tank.getFluidAmount()<1000||heat<180||!canServe()){say(player,"not_ready");return InteractionResult.CONSUME;}
                var inv=sample();finish(recipeFor(inv),inv,batchCount());
                if(finished.isEmpty()){say(player,"not_ready");return InteractionResult.CONSUME;}
            }
        }
        if(!finished.isEmpty()){
            if(held.is(TFCTags.Items.SOUP_BOWLS)){
                if(lid()){say(player,"open_lid");return InteractionResult.CONSUME;}
                ItemStack portion=finished.copyWithCount(1);
                if(FoodCapability.get(portion) instanceof DynamicBowlHandler bowl)bowl.setBowl(held.copyWithCount(1));
                if(!player.getAbilities().instabuild)held.shrink(1);
                finished.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(player,portion);
                CookingReport.notify(player,portion);
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
            if(FluidUtil.interactWithFluidHandler(player,hand,tank)){setChanged();stage(1);status(player);return InteractionResult.CONSUME;}
        }
        if(player.isShiftKeyDown()&&held.is(Items.STICK)){
            for(int i=4;i>=0;i--)if(!ingredients.get(i).isEmpty()){
                ItemHandlerHelper.giveItemToPlayer(player,ingredients.get(i));ingredients.set(i,ItemStack.EMPTY);exposure[i]=0;excess[i]=0;
                setChanged();status(player);return InteractionResult.CONSUME;
            }
        }
        addIngredient(player,held,player.isShiftKeyDown());status(player);return InteractionResult.CONSUME;
    }
    public int addIngredient(Player player,ItemStack held,boolean all){return addIngredient(player,held,all,-1,false);}
    public int addIngredient(Player player,ItemStack held,boolean all,int preferred,boolean fromInventory){
        if(!canUse(player))return 0;
        int n=insert(held,all?held.getCount():1,preferred);
        if(n>0&&(fromInventory||!player.getAbilities().instabuild))held.shrink(n);
        if(n==0)say(player,"cannot_add");
        return n;
    }
    // Physical pickup and menu use the same server-side transaction; no changes to source
    // before storage succeeds. Different cooking histories are never merged.
    private int insert(ItemStack held,int requested,int preferred){
        if(lid()||!finished.isEmpty()||held.isEmpty()||FoodCapability.get(held)==null||!Quality.fresh(held)||!held.getCraftingRemainingItem().isEmpty())return 0;
        int max=Math.min(capacity(),held.getMaxStackSize()),index=-1,empty=-1;
        if(preferred>=0&&preferred<5){
            ItemStack chosen=ingredients.get(preferred);
            if(chosen.isEmpty()||(exposure[preferred]==0&&Quality.ingredientCompatible(chosen,held)))index=preferred;
        }else{
            for(int i=0;i<5;i++){
                ItemStack old=ingredients.get(i);
                if(old.isEmpty()){if(empty<0)empty=i;}
                else if(exposure[i]==0&&old.getCount()<max&&Quality.ingredientCompatible(old,held)){index=i;break;}
            }
            if(index<0)index=empty;
        }
        if(index<0)return 0;
        ItemStack old=ingredients.get(index);
        int n=Math.min(Math.min(held.getCount(),requested),max-old.getCount());
        if(n<=0)return 0;
        ItemStack add=held.copyWithCount(n);
        if(old.isEmpty()){ingredients.set(index,add);exposure[index]=0;excess[index]=0;}
        else{
            IFood a=FoodCapability.get(old),b=FoodCapability.get(add);
            if(a!=null&&b!=null)a.setCreationDate(Math.min(a.getCreationDate(),b.getCreationDate()));
            int q=Quality.has(old)&&Quality.has(add)?QualityMath.merged(Quality.get(old),old.getCount(),Quality.get(add),n):-1;
            old.grow(n);if(q>=0)Quality.set(old,q);
        }
        setChanged();sound(SoundEvents.GENERIC_SPLASH);stage(1);
        if(level instanceof ServerLevel server)server.sendParticles(ParticleTypes.SPLASH,worldPosition.getX()+.5,worldPosition.getY()+.65,worldPosition.getZ()+.5,8,.2,.06,.2,.1);
        return n;
    }
    private void collectDropped(){
        if(lid()||!finished.isEmpty()||level==null)return;
        AABB mouth=new AABB(worldPosition.getX()+.18,worldPosition.getY()+.18,worldPosition.getZ()+.18,
            worldPosition.getX()+.82,worldPosition.getY()+.95,worldPosition.getZ()+.82);
        for(ItemEntity e:level.getEntitiesOfClass(ItemEntity.class,mouth,ItemEntity::isAlive)){
            if(e.getX()<mouth.minX||e.getX()>mouth.maxX||e.getZ()<mouth.minZ||e.getZ()>mouth.maxZ)continue;
            ItemStack stack=e.getItem();int n=insert(stack,stack.getCount(),-1);
            if(n>0){ItemStack remainder=stack.copy();remainder.shrink(n);if(remainder.isEmpty())e.discard();else e.setItem(remainder);}
        }
    }
    private int idealTicks(ItemStack item,int batches){
        return Math.round(Quality.soupTicks(item)*(1f+.10f*Math.max(0,batches-1)));
    }
    private boolean canServe(){
        int n=batchCount();if(n<=0)return false;
        for(int i=0;i<5;i++){
            ItemStack s=ingredients.get(i);
            if(!s.isEmpty()&&(!Quality.fresh(s)||FoodCapability.get(s)==null||exposure[i]<idealTicks(s,n)/2))return false;
        }
        return true;
    }
    private CookingReport report(int n){
        float under=0,over=0,temp=0;int groups=0;List<ItemStack> used=new ArrayList<>();
        for(int i=0;i<5;i++){
            ItemStack item=ingredients.get(i);if(item.isEmpty())continue;
            int ideal=idealTicks(item,n);float ratio=exposure[i]/(float)ideal;
            under+=Math.max(0,1-ratio)*80;
            over+=Math.max(0,ratio-1.25f)*55;
            temp+=Math.min(15,15f*excess[i]/Math.max(1,exposure[i]));
            used.add(item.copyWithCount(n));groups++;
        }
        int prior=Quality.inherited(used);
        return new CookingReport(Math.round(under/Math.max(1,groups)),Math.round(over/Math.max(1,groups)),
            Math.round(temp/Math.max(1,groups)),0,prior<0?0:Math.round((100-prior)*.2f));
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
    private SoupPotRecipe recipeFor(PotBlockEntity.PotInventory inv){
        // This vessel makes soup from food, not from a fixed set of registered recipes.
        // Preserve an exact native recipe when present; otherwise use TFC's own dynamic soup output.
        return FlexibleSoup.resolve(level,inv);
    }
    private void finish(SoupPotRecipe recipe,PotBlockEntity.PotInventory inv,int n){
        // The recipe/plan was resolved on this same server tick. A fallback deliberately
        // does not require native ingredient-tag or exact-cardinality matching.
        if(n<=0||n!=batchCount()||tank.getFluidAmount()<n*1000)return;
        for(ItemStack item:ingredients)if(!item.isEmpty()&&(FoodCapability.get(item)==null||!Quality.fresh(item)))return;
        PotRecipe.Output result;
        RecipeHelpers.setCraftingInput(inv,4,9);
        try { result=recipe.getOutput(inv); } finally { RecipeHelpers.clearCraftingInput(); }
        if(!(result instanceof SoupPotRecipe.SoupOutput soup)||soup.stack().isEmpty())return;
        ItemStack out=soup.stack();
        int servings=out.getCount()*n;
        if(servings<=0||servings>capacity()*3)return;
        finished=out.copyWithCount(servings);report(n).apply(finished);
        for(int i=0;i<5;i++){
            ItemStack ingredient=ingredients.get(i);
            if(ingredient.isEmpty())continue;
            ingredient.shrink(n);
            if(ingredient.isEmpty())ingredients.set(i,ItemStack.EMPTY);
            if(ingredient.isEmpty()){exposure[i]=0;excess[i]=0;}
            // Unconsumed food retains its history; it was heated in the same vessel.
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
        v.collectDropped();
        if(!v.finished.isEmpty()){
            v.stage(v.heat>=180&&v.finishedHotTicks>=160?4:3);
            if(v.heat>=180&&Quality.has(v.finished)&&Quality.fresh(v.finished)){
                v.finishedHotTicks=Math.min(100000,v.finishedHotTicks+ticks);
                if(v.finishedHotTicks>160&&(v.finishedHotTicks-160)%80==0)CookingReport.late(v.finished,5);
            }
            v.setChanged();return;
        }
        v.plannedBatches=0;v.plannedServings=0;
        if(v.amount()==0){v.recipeState=0;v.resetProgress();v.stage(v.tank.isEmpty()?0:v.heat>=180?2:1);v.setChanged();return;}
        if(v.tank.getFluidAmount()<1000){v.recipeState=1;v.resetProgress();v.stage(1);v.setChanged();return;}
        if(v.ingredients.stream().anyMatch(s->!s.isEmpty()&&!Quality.fresh(s))){
            v.recipeState=4;v.resetProgress();v.stage(1);v.setChanged();return;
        }
        int n=v.batchCount();
        v.recipeState=3;v.plannedBatches=n;
        v.plannedServings=n*(int)(v.ingredients.stream().filter(item->!item.isEmpty()).count()/2+1);
        if(v.heat>=180){
            int before=v.getBlockState().getValue(VesselBlock.STAGE);
            boolean ready=true,over=false;
            for(int i=0;i<5;i++){
                ItemStack item=v.ingredients.get(i);if(item.isEmpty())continue;
                v.exposure[i]=Math.min(100000,v.exposure[i]+ticks);
                if(v.heat>650)v.excess[i]=Math.min(v.exposure[i],v.excess[i]+ticks);
                int ideal=v.idealTicks(item,n);
                if(v.exposure[i]<ideal)ready=false;
                if(v.exposure[i]>ideal*1.25f)over=true;
            }
            int stage=over?4:ready?3:2;
            v.stage(stage);
            if(stage==3&&before!=3)v.sound(SoundEvents.BREWING_STAND_BREW);
            else if(stage==4&&before!=4)v.sound(SoundEvents.FIRE_EXTINGUISH);
            else if(now%80==Math.floorMod(-pos.asLong(),20L))v.sound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT);
        }else v.stage(1);
        v.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag t){
        super.saveAdditional(t);ContainerHelper.saveAllItems(t,ingredients);t.putIntArray("Exposure",exposure);t.putIntArray("Excess",excess);t.putInt("CookingModel",2);
        t.put("Tank",tank.writeToNBT(new CompoundTag()));t.putFloat("Heat",heat);t.putFloat("Progress",progress);t.putInt("Hot",hotTicks);t.putInt("Overheat",overheatTicks);t.putInt("FinishedHot",finishedHotTicks);t.putLong("LastTick",lastTick);
        if(activeRecipe!=null)t.putString("Recipe",activeRecipe.toString());
        if(!finished.isEmpty()){t.put("Finished",finished.copyWithCount(1).save(new CompoundTag()));t.putInt("Servings",finished.getCount());}
    }
    @Override public void load(CompoundTag t){
        super.load(t);ingredients.clear();ContainerHelper.loadAllItems(t,ingredients);
        Arrays.fill(exposure,0);int[] a=t.getIntArray("Exposure");if(t.getInt("CookingModel")==2)System.arraycopy(a,0,exposure,0,Math.min(a.length,5));
        Arrays.fill(excess,0);int[] b=t.getIntArray("Excess");System.arraycopy(b,0,excess,0,Math.min(b.length,5));
        for(int i=0;i<5;i++){exposure[i]=Mth.clamp(exposure[i],0,100000);excess[i]=Mth.clamp(excess[i],0,exposure[i]);}
        tank.readFromNBT(t.getCompound("Tank"));heat=Float.isFinite(t.getFloat("Heat"))?Math.max(0,t.getFloat("Heat")):0;progress=Float.isFinite(t.getFloat("Progress"))?Math.max(0,t.getFloat("Progress")):0;hotTicks=Math.max(0,t.getInt("Hot"));overheatTicks=Mth.clamp(t.getInt("Overheat"),0,hotTicks);finishedHotTicks=Mth.clamp(t.getInt("FinishedHot"),0,100000);lastTick=t.contains("LastTick")?t.getLong("LastTick"):-1;
        activeRecipe=t.contains("Recipe")?ResourceLocation.tryParse(t.getString("Recipe")):null;
        finished=ItemStack.of(t.getCompound("Finished"));if(!finished.isEmpty())finished.setCount(Mth.clamp(t.getInt("Servings"),0,capacity()*3));
    }
}
