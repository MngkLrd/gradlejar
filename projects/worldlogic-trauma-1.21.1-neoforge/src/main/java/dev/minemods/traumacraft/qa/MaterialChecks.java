package dev.minemods.traumacraft.qa;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.MaterialBridge;
import dev.minemods.traumacraft.network.DeathReportPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
public final class MaterialChecks {
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Material QA: "+message);}
    public static void verify(ServerPlayer p,BodyState b){
        var kind=ImpactMaterial.Kind.HELD_ITEM;
        require(MaterialBridge.observation(kind,"minecraft:stick","UNKNOWN",1,1,1).isEmpty(),"unknown not fabricated");
        require(MaterialBridge.observation(kind,"minecraft:stick","ESTIMATED",Double.NaN,1,1).isEmpty(),"NaN absent");
        require(MaterialBridge.observation(kind,"minecraft:stick","ESTIMATED",1,0,1).isEmpty(),"invalid volume absent");
        require(MaterialBridge.item(ItemStack.EMPTY).isEmpty(),"empty item absent");
        var stick=MaterialBridge.item(new ItemStack(Items.STICK,16));
        if(MaterialBridge.available()){
            var single=MaterialBridge.item(new ItemStack(Items.STICK));
            require(stick.isPresent()&&single.isPresent()&&stick.get().equals(single.get()),"one actual Core item, not stack mass");
            require(Math.abs(single.get().massKg()/single.get().volumeM3()-single.get().densityKgM3())<.01,"SI consistency");
            var pos=p.blockPosition().offset(300000,0,300000);
            require(MaterialBridge.surface(p.serverLevel(),pos).isEmpty(),"unloaded surface absent");
            var loaded=p.blockPosition();var previous=p.serverLevel().getBlockState(loaded);
            if(p.serverLevel().hasChunkAt(loaded))try{
                p.serverLevel().setBlockAndUpdate(loaded,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var stone=MaterialBridge.surface(p.serverLevel(),loaded);
                require(stone.isPresent()&&stone.get().massKg()>0&&stone.get().volumeM3()==1,"loaded Core stone profile");
            }finally{p.serverLevel().setBlockAndUpdate(loaded,previous);}
            System.out.println("TRAUMA MATERIAL PROVIDER PRESENT: actual item and loaded-surface checks");
        }else{
            require(stick.isEmpty()&&MaterialBridge.surface(p.serverLevel(),p.blockPosition()).isEmpty(),"old Core neutral, no invented physical values");
            System.out.println("TRAUMA MATERIAL PROVIDER ABSENT: fallback checked; Core 1.2.0 interoperability NOT_RUN");
        }
        var observation=MaterialBridge.observation(kind,"minecraft:stick","ESTIMATED",.12,.0002,600).orElseThrow();
        var report=DeathReport.capture(b,DeathReport.Cause.VANILLA,DeathReport.Origin.OTHER,"").withMaterial(java.util.Optional.of(observation));
        var buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
        try{
            DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report));
            require(DeathReportPacket.CODEC.decode(buf).report().material().orElseThrow().equals(observation),"SI report codec");
            buf.clear();DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report.withMaterial(java.util.Optional.empty())));
            buf.writerIndex(buf.writerIndex()-1);
            require(DeathReportPacket.CODEC.decode(buf).report().material().isEmpty(),"0.8.8 archive migration");
        }finally{buf.release();}
    }
}
