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
          .then(Commands.literal("qa_effort").executes(c->{
            double full=ChopEffort.power(1,1,1), tired=ChopEffort.power(.1,1,1);
            double oneArm=ChopEffort.power(1,1,0), disabled=ChopEffort.power(1,0,1);
            if(!(full>oneArm&&oneArm>tired&&disabled==0&&ChopEffort.cooldown(tired)>ChopEffort.cooldown(full)
                &&ChopEffort.depthIncrement(1,6,1,full,.8)>ChopEffort.depthIncrement(1,6,1,tired,.8)
                &&ChopEffort.depthIncrement(0,6,1,disabled,.99)==0)){
                c.getSource().sendFailure(Component.literal("TreeChopper effort regression FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper effort regression PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_landing").executes(c->{
            if(!TreeChopper.qaLandingPose()){c.getSource().sendFailure(Component.literal("TreeChopper landing geometry FAILED"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper landing geometry PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_network").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            fake.setPos(p.getX()+.5,p.getY()-1,p.getZ()-2);
            fake.setYRot(0);fake.setXRot(4);
            var original=l.getBlockState(p);
            TreeChopper.qaResetCooldown(fake);
            TreeChopper.networkStrike(fake,p);
            var first=l.getBlockState(p);
            if(!(first.getBlock() instanceof CutBlock)||first.equals(original)){c.getSource().sendFailure(Component.literal("TreeChopper network initial strike FAILED"));return 0;}
            TreeChopper.networkStrike(fake,p);
            if(!first.equals(l.getBlockState(p))){c.getSource().sendFailure(Component.literal("TreeChopper network cooldown FAILED"));return 0;}
            fake.setPos(p.getX()+.5,p.getY()-1,p.getZ()-20);
            TreeChopper.qaResetCooldown(fake);TreeChopper.networkStrike(fake,p);
            if(!first.equals(l.getBlockState(p))){c.getSource().sendFailure(Component.literal("TreeChopper network range FAILED"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper network validation PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_progress").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            int depth=0;
            for(int i=0;i<4;i++){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
                var state=l.getBlockState(p);
                if(state.getBlock() instanceof StumpBlock){
                    c.getSource().sendSuccess(()->Component.literal("TreeChopper single-side progression PASS"),false);return 1;
                }
                if(!(state.getBlock() instanceof CutBlock)||CutBlock.depth(state,Direction.NORTH)<=depth)break;
                depth=CutBlock.depth(state,Direction.NORTH);
            }
            c.getSource().sendFailure(Component.literal("TreeChopper single-side progression FAILED"));return 0;
          })))
          .then(Commands.literal("qa_break").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            fake.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var click=NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            boolean destroyed=fake.gameMode.destroyBlock(p);
            if(click.isCanceled()||!destroyed||!l.getBlockState(p).isAir()){c.getSource().sendFailure(Component.literal("TreeChopper hand break FAILED"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper hand break allowed"),false);return 1;
          })))
          .then(Commands.literal("qa_axe_break").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            var before=l.getBlockState(p);
            if(fake.gameMode.destroyBlock(p)||!before.equals(l.getBlockState(p))){
                c.getSource().sendFailure(Component.literal("TreeChopper axe bypass FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper axe bypass blocked"),false);return 1;
          })))
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
