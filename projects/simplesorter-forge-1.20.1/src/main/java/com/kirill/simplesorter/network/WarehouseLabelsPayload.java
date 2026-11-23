package com.kirill.simplesorter.network;

import com.kirill.simplesorter.client.ClientWarehouseLabels;
import com.kirill.simplesorter.label.WarehouseLabelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Overwrites the client-owned render labels for ONE watched chunk. */
public record WarehouseLabelsPayload(int chunkX, int chunkZ, List<Entry> labels) {
    private static final int LIMIT = 128;
    public record Entry(BlockPos pos, int style, String family, ItemStack icon, Direction facing) {}

    public static WarehouseLabelsPayload of(ChunkPos pos, List<WarehouseLabelData.Label> data) {
        List<Entry> entries = new ArrayList<>();
        for (WarehouseLabelData.Label label : data) {
            if (entries.size() >= LIMIT) break;
            entries.add(new Entry(label.pos(), label.style(), label.family(),
                    label.icon().copyWithCount(1), label.facing()));
        }
        return new WarehouseLabelsPayload(pos.x, pos.z, entries);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeVarInt(Math.min(LIMIT, labels.size()));
        for (int i = 0; i < Math.min(LIMIT, labels.size()); i++) {
            Entry entry = labels.get(i);
            buf.writeBlockPos(entry.pos());
            buf.writeVarInt(entry.style());
            buf.writeUtf(entry.family(), 48);
            buf.writeItem(entry.icon());
            buf.writeByte(entry.facing().get2DDataValue());
        }
    }

    public static WarehouseLabelsPayload read(FriendlyByteBuf buf) {
        int x = buf.readInt(), z = buf.readInt();
        int n = buf.readVarInt();
        if (n < 0 || n > LIMIT) throw new IllegalArgumentException("Label count out of range: " + n);
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos pos = buf.readBlockPos();
            int style = buf.readVarInt();
            String family = buf.readUtf(48);
            ItemStack icon = buf.readItem();
            Direction facing = Direction.from2DDataValue(buf.readUnsignedByte() & 3);
            if (style < 1 || style > 2) continue;
            if ((pos.getX() >> 4) != x || (pos.getZ() >> 4) != z) continue;
            entries.add(new Entry(pos, style, family, icon.copyWithCount(1), facing));
        }
        return new WarehouseLabelsPayload(x, z, entries);
    }

    public static void handle(WarehouseLabelsPayload payload, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context context = ctxSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientWarehouseLabels.accept(payload)));
        context.setPacketHandled(true);
    }
}
