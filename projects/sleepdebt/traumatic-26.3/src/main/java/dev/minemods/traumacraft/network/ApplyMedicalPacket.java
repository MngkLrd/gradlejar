package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ApplyMedicalPacket(int part) implements CustomPacketPayload {
    public static final Type<ApplyMedicalPacket> TYPE=new Type<>(Identifier.fromNamespaceAndPath(TraumaCraft.MOD_ID,"treat"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ApplyMedicalPacket> STREAM_CODEC=StreamCodec.of(
        (b,p)->b.writeVarInt(p.part),b->new ApplyMedicalPacket(b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(ApplyMedicalPacket packet,IPayloadContext context) {
        if(!(context.player() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator())return;
        if(player.containerMenu!=player.inventoryMenu||packet.part<0||packet.part>=7)return;
        ItemStack stack=player.containerMenu.getCarried();
        if(!(stack.getItem() instanceof MedicalItem medical))return;
        BodyState state=BodyStates.get(player);
        if(!MedicalAction.apply(player,state,medical.medicalType(),BodyPart.values()[packet.part]))return;
        BodyStates.save(player,state);
        if(!player.getAbilities().instabuild){stack.shrink(1);player.containerMenu.setCarried(stack);player.containerMenu.broadcastChanges();}
        ModNetwork.sync(player);
    }
}
