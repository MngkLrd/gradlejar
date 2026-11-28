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
        var core=net.neoforged.fml.ModList.get().getModContainerById("worldlogic").orElseThrow().getModInfo().getVersion().toString();
        if(core.equals("1.2.1"))require(MaterialBridge.available()&&dev.minemods.traumacraft.integration.ContactPhysics.available(),"exact 1.2.1 must expose both material revisions");
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
        if(dev.minemods.traumacraft.integration.ContactPhysics.available()){
            var level=p.serverLevel();var at=p.blockPosition().above(3);
            require(level.hasChunkAt(at),"loaded surface fixture");
            var previous=level.getBlockState(at);var base=level.getBlockState(at.below());
            try{
                level.setBlockAndUpdate(at.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var stone=dev.minemods.traumacraft.api.TraumaApi.surfaceResponse(level,at).orElseThrow();
                var nominal=MaterialBridge.surface(level,at).orElseThrow();
                require(nominal.surface().isPresent()&&stone.stoppingDistanceM()==nominal.surface().get().stoppingDistanceM(),"actual Core response attached to fatal surface evidence");
                level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState());
                var wool=dev.minemods.traumacraft.api.TraumaApi.surfaceResponse(level,at).orElseThrow();
                require(wool.stoppingDistanceM()>stone.stoppingDistanceM(),"Core soft vs hard contact response");
                level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                require(dev.minemods.traumacraft.api.TraumaApi.surfaceResponse(level,at).isEmpty(),"air unknown");
                level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                require(dev.minemods.traumacraft.api.TraumaApi.surfaceResponse(level,at).isEmpty(),"fluid unknown");
            }finally{level.setBlockAndUpdate(at,previous);level.setBlockAndUpdate(at.below(),base);}
            require(dev.minemods.traumacraft.api.TraumaApi.surfaceResponse(level,at.offset(300000,0,300000)).isEmpty(),"unloaded contact unknown");
            require(dev.minemods.traumacraft.api.TraumaApi.impactEnergyJ(2,3).orElseThrow()==9,"Core computes supplied SI energy");
            require(dev.minemods.traumacraft.api.TraumaApi.impactEnergyJ(-2,3).isEmpty()&&dev.minemods.traumacraft.api.TraumaApi.impactEnergyJ(2,Double.NaN).isEmpty(),"invalid motion rejected");
            var i=dev.minemods.traumacraft.api.TraumaApi.itemImpact(new ItemStack(Items.STICK,16),0,.01,2,3).orElseThrow();
            require(i.kineticEnergyJ()==9&&i.contactAreaM2()==.01&&i.componentMassKg()>0,"explicit component, area and energy");
            require(dev.minemods.traumacraft.api.TraumaApi.itemImpact(new ItemStack(Items.STICK),100,.01,2,3).isEmpty(),"unknown component rejected");
            require(dev.minemods.traumacraft.api.TraumaApi.itemImpact(new ItemStack(Items.STICK),0,0,2,3).isEmpty(),"invalid area rejected");
            System.out.println("TRAUMA CORE "+core+" CONTACT PROVIDER PASS: surface, energy, item contact");
        }else{
            require(dev.minemods.traumacraft.api.TraumaApi.impactEnergyJ(2,3).isEmpty()&&dev.minemods.traumacraft.api.TraumaApi.itemImpact(new ItemStack(Items.STICK),0,.01,2,3).isEmpty(),"old Core no invented contact data");
        }
        var observation=MaterialBridge.observation(kind,"minecraft:stick","ESTIMATED",.12,.0002,600).orElseThrow();
        var report=DeathReport.capture(b,DeathReport.Cause.VANILLA,DeathReport.Origin.OTHER,"").withMaterial(java.util.Optional.of(observation));
        var buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
        try{
            DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report));
            require(DeathReportPacket.CODEC.decode(buf).report().material().orElseThrow().equals(observation),"SI report codec");
            buf.clear();DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report));buf.writerIndex(buf.writerIndex()-1);
            require(DeathReportPacket.CODEC.decode(buf).report().material().orElseThrow().equals(observation),"0.8.9 archive migration");
            var surface=new ImpactMaterial(ImpactMaterial.Kind.LANDING_SURFACE,"minecraft:stone","BUILTIN",2650,1,2650).withSurface(java.util.Optional.of(new dev.minemods.traumacraft.integration.ContactPhysics.Surface("worldlogic:stone","ESTIMATED",.008,0)));
            buf.clear();DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report.withMaterial(java.util.Optional.of(surface))));
            require(DeathReportPacket.CODEC.decode(buf).report().material().orElseThrow().equals(surface),"surface response packet roundtrip");
            buf.clear();DeathReportPacket.CODEC.encode(buf,new DeathReportPacket(report.withMaterial(java.util.Optional.empty())));
            buf.writerIndex(buf.writerIndex()-1);
            require(DeathReportPacket.CODEC.decode(buf).report().material().isEmpty(),"0.8.8 archive migration");
        }finally{buf.release();}
    }
}
