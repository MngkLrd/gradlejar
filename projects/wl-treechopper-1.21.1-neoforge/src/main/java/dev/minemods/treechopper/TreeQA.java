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
                &&ChopEffort.workPerHit(6,1,full,4,.8)>ChopEffort.workPerHit(6,1,tired,4,.8)
                &&ChopEffort.workPerHit(6,1,disabled,4,.99)==0)){
                c.getSource().sendFailure(Component.literal("TreeChopper effort regression FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper effort regression PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_axe_tiers").executes(c->{
            Item[] axes={Items.WOODEN_AXE,Items.STONE_AXE,Items.IRON_AXE,Items.DIAMOND_AXE,Items.NETHERITE_AXE,Items.GOLDEN_AXE};
            int previous=Integer.MAX_VALUE;
            for(Item item:axes){
                double speed=((AxeItem)item).getTier().getSpeed();
                int total=0;
                for(int sample=0;sample<100;sample++){
                    int depth=0,hits=0;
                    while(depth<1000&&hits<64){
                        depth+=ChopEffort.workPerHit(speed,1,1,4,((sample+hits*31)%100+.5)/100);
                        hits++;
                    }
                    if(depth<1000){c.getSource().sendFailure(Component.literal("TreeChopper axe tier progression FAILED"));return 0;}
                    total+=hits;
                }
                if(total>=previous||(item==Items.WOODEN_AXE&&total<600)){c.getSource().sendFailure(Component.literal("TreeChopper axe tier order FAILED"));return 0;}
                previous=total;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper axe tier progression PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_swing").executes(c->{
            var fake=FakePlayerFactory.getMinecraft(c.getSource().getLevel());
            ChopSwing swing=(ChopSwing)fake;
            swing.wl$markSwing();
            if(swing.wl$swingDuration()!=12||ChopEffort.cooldown(1)<12){
                c.getSource().sendFailure(Component.literal("TreeChopper slow swing FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper slow swing PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_count").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("logs",IntegerArgumentType.integer(4,32)).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            int logs=IntegerArgumentType.getInteger(c,"logs");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            ItemStack axe=new ItemStack(Items.WOODEN_AXE);
            fake.setItemSlot(EquipmentSlot.MAINHAND,axe);
            int hits=0;
            while(hits<24&&!(l.getBlockState(p).getBlock() instanceof StumpBlock)){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
                hits++;
            }
            int min=logs==4?7:11,max=logs==4?8:13;
            if(hits<min||hits>max||!(l.getBlockState(p).getBlock() instanceof StumpBlock)||axe.getDamageValue()!=hits+logs-1){
                c.getSource().sendFailure(Component.literal("TreeChopper size-dependent hits FAILED: "+hits+" / "+axe.getDamageValue()));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper size-dependent hits PASS"),false);return 1;
          }))))
          .then(Commands.literal("qa_wear").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("logs",IntegerArgumentType.integer(2,32)).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            int logs=IntegerArgumentType.getInteger(c,"logs");
            var fake=FakePlayerFactory.getMinecraft(l);
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            // Start one step before severing. A worn axe must not gain the tree for free.
            var source=l.getBlockState(p);
            if(!(l.getBlockEntity(p) instanceof CutProgress saved)){c.getSource().sendFailure(Component.literal("TreeChopper cut storage FAILED"));return 0;}
            saved.write(new int[]{999,0,0,0});
            ItemStack weak=new ItemStack(Items.WOODEN_AXE);
            weak.setDamageValue(weak.getMaxDamage()-1);
            fake.setItemSlot(EquipmentSlot.MAINHAND,weak);
            TreeChopper.qaResetCooldown(fake);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            if(!source.equals(l.getBlockState(p))||weak.getDamageValue()!=weak.getMaxDamage()-1||!l.getBlockState(p.above()).is(net.minecraft.world.level.block.Blocks.OAK_LOG)){
                c.getSource().sendFailure(Component.literal("TreeChopper insufficient durability FAILED"));return 0;
            }
            ItemStack strong=new ItemStack(Items.IRON_AXE);
            fake.setItemSlot(EquipmentSlot.MAINHAND,strong);
            TreeChopper.qaResetCooldown(fake);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            if(!(l.getBlockState(p).getBlock() instanceof StumpBlock)||strong.getDamageValue()!=logs||!l.getBlockState(p.above()).isAir()){
                c.getSource().sendFailure(Component.literal("TreeChopper per-log durability FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper per-log durability PASS"),false);return 1;
          }))))
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
            for(int i=0;i<12;i++){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
                var state=l.getBlockState(p);
                if(state.getBlock() instanceof StumpBlock){
                    c.getSource().sendSuccess(()->Component.literal("TreeChopper single-side progression PASS"),false);return 1;
                }
                int actual=CutProgress.read(l,p,state)[0];
                if(!(state.getBlock() instanceof CutBlock)||actual<=depth)break;
                depth=actual;
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
