package dev.minemods.redstonevision.client;

import java.util.Optional;
import dev.minemods.redstonevision.item.GogglesPower;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid="redstonevision",value=Dist.CLIENT)
public final class VisionHud {
    private VisionHud() {}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        if (!VisionState.active()) return;
        Minecraft mc=Minecraft.getInstance();
        int seconds=(GogglesPower.ticks(mc.player.getItemBySlot(EquipmentSlot.HEAD))+19)/20;
        event.getGuiGraphics().drawString(mc.font,
            Component.translatable("hud.redstonevision.active",seconds),8,8,0xFFDD5544,true);
        Vec3 start=mc.player.getEyePosition();
        Vec3 end=start.add(mc.player.getLookAngle().scale(VisionState.RANGE));
        double nearest=Double.MAX_VALUE;
        CircuitScanner.Hit selected=null;
        for (CircuitScanner.Hit hit:CircuitScanner.hits()) {
            Optional<Vec3> cut=new AABB(hit.pos()).inflate(0.01).clip(start,end);
            if (cut.isPresent()) {
                double d=cut.get().distanceToSqr(start);
                if (d<nearest) {nearest=d;selected=hit;}
            }
        }
        if (selected==null) return;
        BlockState state=selected.state();
        String name=state.getBlock().getName().getString();
        int value=CircuitScanner.signal(state);
        String power=value>=0?"Power: "+value+"/15":"Signal: n/a";
        if (state.hasProperty(BlockStateProperties.DELAY))
            power+=" | Delay: "+state.getValue(BlockStateProperties.DELAY)+" ticks";
        int x=event.getGuiGraphics().guiWidth()/2;
        int y=event.getGuiGraphics().guiHeight()/2+18;
        event.getGuiGraphics().drawCenteredString(mc.font,name,x,y,0xFFEEDDAA);
        event.getGuiGraphics().drawCenteredString(mc.font,power,x,y+11,0xFFFF6666);
    }
}
