package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.medical.TreatmentManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record ApplyMedicalPacket(int bodyPartOrdinal) implements CustomPacketPayload {
    public static final Type<ApplyMedicalPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"apply_medicine"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ApplyMedicalPacket> STREAM_CODEC=StreamCodec.of(
        (buf,m)->buf.writeVarInt(m.bodyPartOrdinal()),buf->new ApplyMedicalPacket(buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(ApplyMedicalPacket packet,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer p)||!p.isAlive()||p.isCreative()||p.isSpectator()||p.containerMenu!=p.inventoryMenu)return;
        int i=packet.bodyPartOrdinal();
        if(i<0||i>=BodyPart.values().length)return;
        if(!(p.containerMenu.getCarried().getItem() instanceof MedicalItem medicine))return;
        if(!TreatmentManager.begin(p,medicine,BodyPart.values()[i],true,null))
            p.displayClientMessage(Component.translatable("message.traumacraft.not_applicable"),true);
    }
}
