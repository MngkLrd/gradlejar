package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.medical.TreatmentManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
public record ApplyMedicalPacket(int bodyPartOrdinal) {
    public static void handle(ApplyMedicalPacket packet,ServerPlayer p) {
        if(p==null || !p.isAlive() || p.isSpectator() || p.containerMenu!=p.inventoryMenu) return;
        int i=packet.bodyPartOrdinal();
        if(i<0 || i>=BodyPart.values().length) return;
        if(!(p.containerMenu.getCarried().getItem() instanceof MedicalItem medicine)) return;
        if(!TreatmentManager.begin(p,medicine,BodyPart.values()[i],true,null))
            p.displayClientMessage(Component.translatable("message.traumacraft.not_applicable"),true);
    }
}
