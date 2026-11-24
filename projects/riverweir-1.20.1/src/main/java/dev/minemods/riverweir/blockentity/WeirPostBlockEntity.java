package dev.minemods.riverweir.blockentity;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.logic.WeirTier;
import dev.minemods.riverweir.logic.WeirCapture;
import net.minecraft.nbt.ListTag;
import dev.minemods.riverweir.menu.WeirMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;

public final class WeirPostBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler items = new ItemStackHandler(27) {
        @Override protected void onContentsChanged(int slot) { setChanged(); refreshCatchMarker(); }
    };
    public BlockPos other;
    public boolean master;
    public int wear;
    public int netY;
    private long lastScan;
    private long lastWear;
    private int[] columnDepths = new int[0];
    public BlockPos catchMarker;
    public int rollsUsed;
    public int tierId;
    public int clog;
    public int baitCharges;
    public long lastCatch;
    // In-flight entanglements are intentionally transient. Unloaded fish swim freely.
    public WeirCapture.Hold hold; 
    public WeirTier getTier() { return WeirTier.byId(tierId); }

    public WeirPostBlockEntity(BlockPos pos, BlockState state) { super(RiverWeirMod.POST_BE.get(),pos,state); }

    public void link(BlockPos linked, boolean isMaster, int y, int[] depths, int rolls, WeirTier kind) {
        other=linked.immutable();
        master=isMaster;
        wear=0;
        netY=y;
        lastScan=0;
        lastWear=0;
        columnDepths=depths.clone();
        rollsUsed=rolls;
        tierId=kind.id;
        clog=0;
        baitCharges=0;
        lastCatch=0;
        catchMarker=null;
        setChanged();
    }
    public void clearLink() {
        other=null;master=false;wear=0;netY=0;lastScan=0;lastWear=0;
        columnDepths=new int[0];
        rollsUsed=0;
        tierId=0;
        clog=0;
        baitCharges=0;
        lastCatch=0;
        catchMarker=null;
        setChanged();
    }
    public int depthAt(int index) {
        return index >= 0 && index < columnDepths.length ? columnDepths[index] : 3;
    }
    public int maxDepth() {
        if (columnDepths.length == 0) return 3; // v0.1 save compatibility
        return Arrays.stream(columnDepths).max().orElse(3);
    }

    public boolean hasFish() {
        for (int i = 0; i < items.getSlots(); i++) if (!items.getStackInSlot(i).isEmpty()) return true;
        return false;
    }
    public void markCatch(BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        if (catchMarker != null && !catchMarker.equals(pos)) WeirLogic.setCatchVisual(server, catchMarker, false);
        catchMarker = pos.immutable();
        WeirLogic.setCatchVisual(server, catchMarker, true);
        setChanged();
    }
    private void refreshCatchMarker() {
        if (!(level instanceof ServerLevel server) || !master || catchMarker == null || hasFish()) return;
        WeirLogic.setCatchVisual(server, catchMarker, false);
        catchMarker = null;
        setChanged();
    }

    /**
     * The v0.4 Jute net had nine visible slots. When it is upgraded to two,
     * merge old catches first and drop any leftover stacks at the stake.
     * Never discard a catch silently or leave inaccessible hidden slots.
     */
    private void migrateExcessCatches(ServerLevel server) {
        int capacity = getTier().storage;
        if (capacity >= items.getSlots()) return;
        for (int index = capacity; index < items.getSlots(); index++) {
            ItemStack stack = items.getStackInSlot(index);
            if (stack.isEmpty()) continue;
            ItemStack moving = items.extractItem(index, stack.getCount(), false);
            for (int target = 0; target < capacity && !moving.isEmpty(); target++)
                moving = items.insertItem(target, moving, false);
            if (!moving.isEmpty())
                Containers.dropItemStack(server, worldPosition.getX() + .5,
                    worldPosition.getY() + .5, worldPosition.getZ() + .5, moving);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (other != null) tag.putLong("OtherPost",other.asLong());
        tag.putBoolean("Master",master);
        tag.putInt("Wear",wear);
        tag.putInt("NetY",netY);
        tag.putLong("LastScan",lastScan);
        tag.putLong("LastWear",lastWear);
        tag.putIntArray("ColumnDepths",columnDepths);
        tag.putInt("RollsUsed",rollsUsed);
        tag.putInt("NetTier",tierId);
        tag.putInt("Clog",clog);
        tag.putInt("BaitCharges",baitCharges);
        tag.putLong("LastCatch",lastCatch);
        if (catchMarker != null) tag.putLong("CatchMarker",catchMarker.asLong());
        tag.put("Inventory",items.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        other=tag.contains("OtherPost")?BlockPos.of(tag.getLong("OtherPost")):null;
        master=tag.getBoolean("Master");
        wear=tag.getInt("Wear");
        netY=tag.getInt("NetY");
        lastScan=tag.getLong("LastScan");
        lastWear=tag.getLong("LastWear");
        columnDepths=tag.contains("ColumnDepths")?tag.getIntArray("ColumnDepths"):new int[0];
        rollsUsed=tag.getInt("RollsUsed");
        tierId=tag.getInt("NetTier");
        clog=tag.getInt("Clog");
        baitCharges=tag.getInt("BaitCharges");
        lastCatch=tag.getLong("LastCatch");
        catchMarker=tag.contains("CatchMarker")?BlockPos.of(tag.getLong("CatchMarker")):null;
        if(tag.contains("Inventory")){
            // Forge's handler deserialization resizes to the saved Size (9 for older nets).
            // Read individual slots instead, preserving all v0.1-v0.3 catches in 27 slots.
            ListTag list=tag.getCompound("Inventory").getList("Items",10);
            for(int i=0;i<items.getSlots();i++)items.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);
            for(int i=0;i<list.size();i++){
                CompoundTag entry=list.getCompound(i);
                int slot=entry.getInt("Slot");
                if(slot>=0&&slot<items.getSlots())items.setStackInSlot(slot,net.minecraft.world.item.ItemStack.of(entry));
            }
        }
    }
    @Override public Component getDisplayName() { return Component.translatable("container.riverweir.weir"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeirMenu(id,inventory,worldPosition,getTier().id);
    }
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,WeirPostBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !be.master || be.other==null) return;
        // A post may be broken while the far end is unloaded. Reconcile as soon
        // as both ends are available, without force-loading chunks.
        be.migrateExcessCatches(server);
        if (server.hasChunkAt(be.other) && !WeirLogic.isValidPartner(server,be)) {
            WeirLogic.dismantle(server,pos);
            return;
        }
        if (be.hold != null) WeirCapture.tick(server,be);
        if (be.wear>=be.getTier().endurance) return;
        long time=server.getGameTime();
        // Only process the actual loaded world; never grant fish for time spent away.
        if (time-be.lastScan<20 && time>=be.lastScan) return;
        be.lastScan=time;
        if (!WeirLogic.isFishingEnvironment(server,be)) return;
        if (time-be.lastWear>=2400 || time<be.lastWear) {
            be.lastWear=time;
            WeirLogic.damage(server,be,1);
            if(server.random.nextFloat()<0.30f){be.clog=Math.min(100,be.clog+1);be.setChanged();}
        }
        if(be.wear<be.getTier().endurance && be.clog<90) WeirLogic.catchNearbyFish(server,be);
    }
}
