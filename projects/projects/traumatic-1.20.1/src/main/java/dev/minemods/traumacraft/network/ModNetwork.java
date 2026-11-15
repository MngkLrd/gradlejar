package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;

public final class ModNetwork {
    private static final String VERSION="1";
    private static final SimpleChannel NET=NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TraumaCraft.MOD_ID,"network"),()->VERSION,VERSION::equals,VERSION::equals);
    private ModNetwork(){}
    public static void register(){
        NET.registerMessage(0,Sync.class,Sync::encode,Sync::decode,ModNetwork::receiveSync);
        NET.registerMessage(1,Treatment.class,Treatment::encode,Treatment::decode,ModNetwork::receiveTreatment);
    }
    public record Sync(float[] hp,byte[] bleed,boolean[] fracture) {
        public static void encode(Sync p,FriendlyByteBuf b){
            for(int i=0;i<7;i++){b.writeFloat(p.hp[i]);b.writeByte(p.bleed[i]);b.writeBoolean(p.fracture[i]);}
        }
        public static Sync decode(FriendlyByteBuf b){
            float[] h=new float[7];byte[] d=new byte[7];boolean[] f=new boolean[7];
            for(int i=0;i<7;i++){h[i]=b.readFloat();d[i]=b.readByte();f[i]=b.readBoolean();}
            return new Sync(h,d,f);
        }
    }
    public record Treatment(int part) {
        public static void encode(Treatment p,FriendlyByteBuf b){b.writeVarInt(p.part);}
        public static Treatment decode(FriendlyByteBuf b){return new Treatment(b.readVarInt());}
    }
    private static void receiveSync(Sync p,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();
        context.enqueueWork(()->ClientBodyState.accept(p.hp,p.bleed,p.fracture));
        context.setPacketHandled(true);
    }
    private static void receiveTreatment(Treatment p,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();
        context.enqueueWork(()->{
            ServerPlayer player=context.getSender();
            if(player==null||!player.isAlive()||player.isSpectator())return;
            if(p.part<0||p.part>=BodyPart.values().length||player.containerMenu!=player.inventoryMenu)return;
            ItemStack stack=player.containerMenu.getCarried();
            if(!(stack.getItem() instanceof MedicalItem medical))return;
            BodyPart zone=BodyPart.values()[p.part];
            BodyState state=BodyStates.get(player);
            if(!MedicalAction.apply(player,state,medical.medicalType(),zone)){
                player.displayClientMessage(Component.translatable("message.traumacraft.not_applicable"),true);
                return;
            }
            BodyStates.save(player,state);
            if(!player.getAbilities().instabuild){stack.shrink(1);player.containerMenu.setCarried(stack);player.containerMenu.broadcastChanges();}
            sync(player);
        });
        context.setPacketHandled(true);
    }
    public static void sync(ServerPlayer p){
        BodyState state=BodyStates.get(p);
        float[] hp=new float[7];byte[] bleeding=new byte[7];boolean[] broken=new boolean[7];
        for(BodyPart zone:BodyPart.values()){int i=zone.ordinal();hp[i]=state.health(zone);bleeding[i]=(byte)state.bleeding(zone);broken[i]=state.fractured(zone);}
        NET.send(PacketDistributor.PLAYER.with(()->p),new Sync(hp,bleeding,broken));
    }
    public static void treat(BodyPart zone){NET.sendToServer(new Treatment(zone.ordinal()));}
}
