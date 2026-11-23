package com.kirill.simplesorter.network;

import com.kirill.simplesorter.client.ClientFlyingItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-to-client animation request, sent only for successful transfers. */
public record FlyingItemsPayload(List<Flight> flights) {
    public static final int MAX_FLIGHTS = 24;

    public record Flight(BlockPos from, BlockPos to, ItemStack icon, int movedItems) {}

    public FlyingItemsPayload {
        flights = List.copyOf(flights);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(MAX_FLIGHTS, flights.size()));
        for (int i = 0; i < Math.min(MAX_FLIGHTS, flights.size()); i++) {
            Flight flight = flights.get(i);
            buffer.writeBlockPos(flight.from());
            buffer.writeBlockPos(flight.to());
            buffer.writeItem(flight.icon());
            buffer.writeVarInt(Math.max(1, flight.movedItems()));
        }
    }

    public static FlyingItemsPayload read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_FLIGHTS) {
            throw new IllegalArgumentException("Invalid SimpleSorter flight count " + count);
        }
        List<Flight> flights = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos from = buffer.readBlockPos();
            BlockPos to = buffer.readBlockPos();
            ItemStack icon = buffer.readItem();
            int moved = Math.min(4096, Math.max(1, buffer.readVarInt()));
            if (!icon.isEmpty() && !from.equals(to)) {
                icon.setCount(1);
                flights.add(new Flight(from, to, icon, moved));
            }
        }
        return new FlyingItemsPayload(flights);
    }

    public static void handle(FlyingItemsPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFlyingItems.accept(payload)));
        context.setPacketHandled(true);
    }
}
