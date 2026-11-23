package dev.minemods.riverweir.blockentity;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.menu.WeirMenu;
import net.minecraft.core.BlockPos;
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
    public final ItemStackHandler items = new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public BlockPos other;
    public boolean master;
    public int wear;
    public int netY;
    private long lastScan;
    private long lastWear;
    private int[] columnDepths = new int[0];

    public WeirPostBlockEntity(BlockPos pos, BlockState state) { super(RiverWeirMod.POST_BE.get(),pos,state); }

    public void link(BlockPos linked, boolean isMaster, int y, int[] depths) {
        other=linked.immutable();
        master=isMaster;
        wear=0;
        netY=y;
        lastScan=0;
        lastWear=0;
        columnDepths=depths.clone();
        setChanged();
    }
    public void clearLink() {
        other=null;master=false;wear=0;netY=0;lastScan=0;lastWear=0;
        columnDepths=new int[0];
        setChanged();
    }
    public int depthAt(int index) {
        return index >= 0 && index < columnDepths.length ? columnDepths[index] : 3;
    }
    public int maxDepth() {
        if (columnDepths.length == 0) return 3; // v0.1 save compatibility
        return Arrays.stream(columnDepths).max().orElse(3);
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
        if(tag.contains("Inventory")) items.deserializeNBT(tag.getCompound("Inventory"));
    }
    @Override public Component getDisplayName() { return Component.translatable("container.riverweir.weir"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeirMenu(id,inventory,worldPosition);
    }
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,WeirPostBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !be.master || be.other==null || be.wear>=100) return;
        long time=server.getGameTime();
        // Only process the actual loaded world; never grant fish for time spent away.
        if (time-be.lastScan<10 && time>=be.lastScan) return;
        be.lastScan=time;
        if (!WeirLogic.isFishingEnvironment(server,be)) return;
        if (time-be.lastWear>=2400 || time<be.lastWear) {
            be.lastWear=time;
            WeirLogic.damage(server,be,1);
        }
        if(be.wear<100) WeirLogic.catchNearbyFish(server,be);
    }
}
