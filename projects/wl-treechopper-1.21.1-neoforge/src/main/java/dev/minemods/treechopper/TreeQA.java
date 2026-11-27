package dev.minemods.treechopper;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
final class TreeQA {
    static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("wltreechopper").requires(s->s.hasPermission(2))
          .then(Commands.literal("qa_hit").then(Commands.argument("pos",BlockPosArgument.blockPos())
          .then(Commands.argument("side",StringArgumentType.word()).then(Commands.argument("count",IntegerArgumentType.integer(1,16))
          .executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            Direction side=Direction.byName(StringArgumentType.getString(c,"side"));
            if(side==null||side.getAxis().isVertical())return 0;
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5+side.getStepX()*2,p.getY(),p.getZ()+.5+side.getStepZ()*2);
            fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            for(int i=0;i<IntegerArgumentType.getInteger(c,"count");i++){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,side,PlayerInteractEvent.LeftClickBlock.Action.START));
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper QA hit dispatched"),false);return 1;
          }))))));
    }
}
