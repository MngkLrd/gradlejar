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
    static void register(RegisterCommandsEvent registrationEvent){
        registrationEvent.getDispatcher().register(Commands.literal("wltreechopper").requires(s->s.hasPermission(2))
          .then(Commands.literal("qa_buck").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("expected",IntegerArgumentType.integer(0,1)).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);fake.setPos(p.getX()+.5,p.getY(),p.getZ()-1.5);
            ItemStack axe=new ItemStack(Items.IRON_AXE);fake.setItemSlot(EquipmentSlot.MAINHAND,axe);
            var before=l.getBlockState(p);TreeChopper.qaResetCooldown(fake);
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            boolean changed=!before.equals(l.getBlockState(p));
            if(changed!=(IntegerArgumentType.getInteger(c,"expected")==1)||axe.getDamageValue()!=0){
                c.getSource().sendFailure(Component.literal("TreeChopper bucking FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper bucking PASS"),false);return 1;
          }))))
          .then(Commands.literal("qa_resting").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getTags().contains(FallingTree.REST));
            if(list.size()!=1){c.getSource().sendFailure(Component.literal("TreeChopper resting coordinator FAILED: "+list.size()));return 0;}
            var f=list.get(0).getPersistentData().getCompound(TreeChopper.FALL);
            float progress=f.getFloat("progress");
            if(!(progress>0&&progress<1)||f.getList("markers",net.minecraft.nbt.Tag.TAG_COMPOUND).size()!=3){
                c.getSource().sendFailure(Component.literal("TreeChopper inclined pose FAILED: "+progress));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper resting pose PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_inclined_buck").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("section",IntegerArgumentType.integer(0,2)).then(Commands.argument("expected",IntegerArgumentType.integer(0,1)).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getTags().contains(FallingTree.REST));
            if(list.size()!=1)return 0;
            var master=list.get(0);var f=master.getPersistentData().getCompound(TreeChopper.FALL);
            int section=IntegerArgumentType.getInteger(c,"section");boolean expected=IntegerArgumentType.getInteger(c,"expected")==1;
            var marks=f.getList("markers",net.minecraft.nbt.Tag.TAG_COMPOUND);
            net.minecraft.world.entity.Interaction marker=null;
            for(int i=0;i<marks.size();i++){var mark=marks.getCompound(i);if(mark.getInt("section")==section&&l.getEntity(mark.getUUID("entity")) instanceof net.minecraft.world.entity.Interaction e2)marker=e2;}
            if(marker==null){if(!expected&&(f.getInt("processedMask")&(1<<section))!=0){c.getSource().sendSuccess(()->Component.literal("TreeChopper inclined bucking PASS"),false);return 1;}return 0;}
            var fake=FakePlayerFactory.getMinecraft(l);fake.setPos(marker.getX(),marker.getY()-1,marker.getZ()-1);
            ItemStack axe=new ItemStack(Items.IRON_AXE);fake.setItemSlot(EquipmentSlot.MAINHAND,axe);
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(fake,marker));
            if(marker.isRemoved()!=expected||axe.getDamageValue()!=0){
                c.getSource().sendFailure(Component.literal("TreeChopper inclined bucking FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper inclined bucking PASS"),false);return 1;
          })))))
          .then(Commands.literal("qa_resumed_landing").then(Commands.argument("base",BlockPosArgument.blockPos()).then(Commands.argument("target",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"base"),target=BlockPosArgument.getLoadedBlockPos(c,"target");
            if(l.getBlockState(target).getBlock() instanceof FallenLogBlock){
                c.getSource().sendSuccess(()->Component.literal("TreeChopper resumed landing PASS"),false);return 1;
            }
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getPersistentData().getCompound(TreeChopper.FALL).getBoolean("coordinator"));
            String state=list.isEmpty()?"no coordinator":list.get(0).getPersistentData().getCompound(TreeChopper.FALL).toString();
            c.getSource().sendFailure(Component.literal("TreeChopper resumed landing FAILED: "+state));return 0;
          }))))
          .then(Commands.literal("qa_branch_support").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getTags().contains(FallingTree.REST));
            boolean supported=false;
            if(list.size()==1){
                var data=list.get(0).getPersistentData().getCompound(TreeChopper.FALL);
                var members=data.getList("members",net.minecraft.nbt.Tag.TAG_COMPOUND);int logs=0;
                for(int i=0;i<members.size();i++)if(members.getCompound(i).getBoolean("log")&&!members.getCompound(i).getBoolean("removed"))logs++;
                supported=logs==9&&data.getFloat("progress")>.6f&&FallingTree.qaSupported(l,data);
            }else if(list.isEmpty()){
                // On an integral final grid, the branch may land as solid blocks instead of displays.
                java.util.Set<BlockPos> logs=new java.util.HashSet<>();java.util.Set<java.util.UUID> groups=new java.util.HashSet<>();
                for(int y=0;y<=5;y++)for(int z=-12;z<=-1;z++){
                    BlockPos cell=p.offset(0,y,z);
                    if(l.getBlockEntity(cell) instanceof FallenProgress fp&&fp.group!=null){logs.add(cell);groups.add(fp.group);}
                }
                boolean contact=logs.stream().anyMatch(cell->!logs.contains(cell.below())&&!l.getBlockState(cell.below()).getCollisionShape(l,cell.below()).isEmpty());
                supported=logs.size()==9&&groups.size()==1&&contact;
            }
            if(!supported){c.getSource().sendFailure(Component.literal("TreeChopper branched terrain support FAILED"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper branched terrain support PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_support").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getTags().contains(FallingTree.REST));
            if(list.size()!=1||!FallingTree.qaSupported(l,list.get(0).getPersistentData().getCompound(TreeChopper.FALL)))return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper timber supported PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_timber_collision").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var list=l.getEntitiesOfClass(TimberCollider.class,new net.minecraft.world.phys.AABB(p).inflate(8),t->t.isAlive());
            if(list.isEmpty()||list.stream().anyMatch(t->!t.canBeCollidedWith()||t.isPickable()))return 0;
            var fake=FakePlayerFactory.getMinecraft(l);var box=list.get(0).getBoundingBox();
            if(l.getEntityCollisions(fake,box.deflate(.05)).isEmpty())return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper physical timber collision PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_multi").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("width",IntegerArgumentType.integer(2,3)).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");int width=IntegerArgumentType.getInteger(c,"width");
            var fake=FakePlayerFactory.getMinecraft(l);
            ItemStack axe=new ItemStack(Items.DIAMOND_AXE);fake.setItemSlot(EquipmentSlot.MAINHAND,axe);
            java.util.List<BlockPos> feet=new java.util.ArrayList<>();
            for(int x=0;x<width;x++)for(int z=0;z<width;z++)if(width==2||x!=1||z!=1)feet.add(p.offset(x,0,z));
            int hits=0;
            for(int i=0;i<feet.size();i++){
                BlockPos foot=feet.get(i);fake.setPos(foot.getX()+.5,foot.getY(),foot.getZ()-1.5);
                int guard=0;
                while(guard++<32&&!(l.getBlockState(foot).getBlock() instanceof StumpBlock)&&!ChopEffort.severed(CutProgress.read(l,foot,l.getBlockState(foot)))){
                    TreeChopper.qaResetCooldown(fake);
                    NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,foot,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));hits++;
                }
                if(i<feet.size()-1&&(!l.getBlockState(foot.above()).is(net.minecraft.world.level.block.Blocks.OAK_LOG)||!ChopEffort.severed(CutProgress.read(l,foot,l.getBlockState(foot))))){
                    c.getSource().sendFailure(Component.literal("TreeChopper premature multi-root fall FAILED"));return 0;
                }
            }
            for(int x=0;x<width;x++)for(int z=0;z<width;z++){
                if(!(l.getBlockState(p.offset(x,0,z)).getBlock() instanceof StumpBlock)||!l.getBlockState(p.offset(x,1,z)).isAir()){
                    c.getSource().sendFailure(Component.literal("TreeChopper complete multi-root fall FAILED"));return 0;
                }
            }
            int expected=hits+width*width*3+(width==3?1:0);
            if(axe.getDamageValue()!=expected){c.getSource().sendFailure(Component.literal("TreeChopper multi-root wear FAILED: "+axe.getDamageValue()+" expected "+expected));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper multi-root PASS"),false);return 1;
          }))))
          .then(Commands.literal("qa_notch_shape").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("opposite",IntegerArgumentType.integer(0,1)).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var snapshot=TreeChopperApi.cut(l,p);if(snapshot.isEmpty())return 0;
            var cut=snapshot.get();var state=l.getBlockState(p);
            boolean opposite=IntegerArgumentType.getInteger(c,"opposite")==1;
            if(cut.north()<=0||(opposite?cut.south()<=0:cut.south()!=0)||cut.west()!=0||cut.east()!=0
                ||CutBlock.depth(l,p,Direction.NORTH)!=ChopEffort.depth(cut.north())||CutBlock.depth(l,p,Direction.SOUTH)!=ChopEffort.depth(cut.south()))return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper notch geometry PASS"),false);return 1;
          }))))
          .then(Commands.literal("qa_crosscut").executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=new BlockPos(1002,200,1017);
            l.setBlock(p,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
            l.setBlock(p.above(),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
            l.setBlock(p.above(2),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
            l.setBlock(p.above(3),net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,1),3);
            var fake=FakePlayerFactory.getMinecraft(l);fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            int previous=0;boolean felled=false;
            for(int i=0;i<12;i++){
                Direction side=i%2==0?Direction.NORTH:Direction.EAST;
                fake.setPos(p.getX()+.5+side.getStepX()*2,p.getY(),p.getZ()+.5+side.getStepZ()*2);
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,side,PlayerInteractEvent.LeftClickBlock.Action.START));
                if(l.getBlockState(p).getBlock() instanceof StumpBlock){felled=true;break;}
                var snapshot=TreeChopperApi.cut(l,p);
                if(snapshot.isEmpty()||snapshot.get().totalPermille()<=previous||(i==1&&(snapshot.get().north()==0||snapshot.get().east()==0)))return 0;
                previous=snapshot.get().totalPermille();
            }
            if(!felled||ChopEffort.progress(new int[]{300,0,300,0})!=600||!ChopEffort.severed(new int[]{500,0,500,0})||!ChopEffort.severed(new int[]{620,130,0,0}))return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper cumulative cut PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_forest_start").executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=new BlockPos(995,200,1007);
            var oak=net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
            var leaves=net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,1);
            l.setBlock(p,oak,3);l.setBlock(p.above(),oak,3);l.setBlock(p.above(2),oak,3);
            l.setBlock(p.above(2).east(),leaves,3);l.setBlock(p.above(2).west(),leaves,3);
            if(!ForestSticks.offer(l,p.getX()+1,p.getZ(),false)||ForestSticks.offer(l,p.getX()+1,p.getZ(),false))return 0;
            var ground=l.getBlockState(p.east());
            if(!(ground.getBlock() instanceof TwigBlock twig)||!twig.species.equals("oak"))return 0;
            var fake=FakePlayerFactory.getMinecraft(l);fake.setPos(p.getX()+1.5,p.getY(),p.getZ()+.5);
            fake.getInventory().clearContent();
            twig.useWithoutItem(ground,l,p.east(),fake,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p.east()),Direction.UP,p.east(),false));
            if(!l.getBlockState(p.east()).isAir()||!fake.getInventory().contains(new ItemStack(Items.STICK)))return 0;
            if(!ForestSticks.offer(l,p.getX()-1,p.getZ(),false))return 0;
            if(!fake.gameMode.destroyBlock(p.west())||!l.getBlockState(p.west()).isAir())return 0;
            if(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(p.west()).inflate(2),drop->drop.getItem().is(Items.STICK)).isEmpty())return 0;
            if(!ForestSticks.offer(l,p.getX()+1,p.getZ(),true))return 0;
            if(l.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,new net.minecraft.world.phys.AABB(p).inflate(5),falling->falling.getBlockState().getBlock() instanceof TwigBlock).isEmpty())return 0;
            var input=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(new ItemStack(Items.FLINT),ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.STICK)));
            var recipe=l.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,l);
            if(recipe.isEmpty()||!recipe.get().id().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"primitive_axe"))||!recipe.get().value().assemble(input,l.registryAccess()).is(Items.STONE_AXE))return 0;
            var invalid=net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.STICK)));
            if(l.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,invalid,l).isPresent())return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper forest start PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_crown_ownership").executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=new BlockPos(1020,210,1015);
            var oak=net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
            var leaves=net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState();
            l.setBlock(p,oak,3);l.setBlock(p.east(4),oak,3);
            l.setBlock(p.east(),leaves.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,1),3);
            l.setBlock(p.east(2),leaves.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,2),3);
            l.setBlock(p.east(3),leaves.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,1),3);
            l.setBlock(p.north(),leaves.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true),3);
            var owned=CrownOwnership.owned(l,java.util.Set.of(p));
            if(!owned.contains(p.east())||owned.contains(p.east(2))||owned.contains(p.east(3))||owned.contains(p.north()))return 0;
            // At the upright pose the transformed upper log occupies exactly the chosen source cell.
            // A natural leaf is not a timber support; an intentionally placed leaf remains solid.
            BlockPos base=new BlockPos(1024,215,1015),target=base.above();
            l.setBlock(target,leaves,3);
            boolean naturalClear=TreeChopper.sweepClear(l,base,target,Direction.NORTH,0,java.util.Set.of());
            l.setBlock(target,leaves.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true),3);
            boolean placedClear=TreeChopper.sweepClear(l,base,target,Direction.NORTH,0,java.util.Set.of());
            l.setBlock(target,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            if(!naturalClear||placedClear)return 0;
            var fake=FakePlayerFactory.getMinecraft(l);
            l.setBlock(target,leaves,3);l.setBlock(target.east(),oak,3);
            var box=new net.minecraft.world.phys.AABB(target).expandTowards(1,0,0);
            if(FallingTree.crushLeaves(l,fake,box)!=1||!l.getBlockState(target).isAir()||!l.getBlockState(target.east()).is(oak.getBlock()))return 0;
            if(ChopEffort.workPerHit(2,1,1,8,.5,16)>=ChopEffort.workPerHit(2,1,1,8,.5,4)||ChopEffort.depth(999)!=7)return 0;
            var cutBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"cut_oak"));
            if(cutBlock.getStateDefinition().getPossibleStates().size()!=1)return 0;
            for(int n=0;n<=8;n++)for(int s=0;s<=8;s++)for(int w=0;w<=8;w++)for(int e=0;e<=8;e++){
                int[] d={n,s,w,e};
                int key=CutGeometry.key(new int[]{125*n,125*s,125*w,125*e});
                for(int face=0;face<4;face++)if(CutGeometry.depth(key,face)!=d[face])return 0;
                // Exercise representative profiles, including opposed cuts and fully severed but protected roots.
                if((n+s+w+e)%5==0){
                    var g=CutGeometry.geometry(key);
                    if(g!=CutGeometry.geometry(key)||g.boxes().size()>5||g.shape().isEmpty())return 0;
                }
            }
            if(CutGeometry.cacheSize()>CutGeometry.CACHE_LIMIT)return 0;
            BlockPos fixture=new BlockPos(1024,215,1017);
            l.setBlock(fixture,cutBlock.defaultBlockState(),3);
            if(!(l.getBlockEntity(fixture) instanceof CutProgress stored))return 0;
            var state=l.getBlockState(fixture);
            int[] work={510,130,0,0};
            stored.write(work,0);
            var expected=CutGeometry.geometry(CutGeometry.key(work));
            if(state.getCollisionShape(l,fixture)!=expected.shape()||!state.equals(l.getBlockState(fixture)))return 0;
            var nbt=stored.saveWithoutMetadata(l.registryAccess());
            var clone=new CutProgress(fixture,state);
            clone.loadAdditional(nbt,l.registryAccess());
            if(!java.util.Arrays.equals(nbt.getIntArray("work"),work)||!java.util.Arrays.equals(clone.saveWithoutMetadata(l.registryAccess()).getIntArray("work"),work)
                ||clone.getUpdateTag(l.registryAccess()).getInt("depthKey")!=CutGeometry.key(work))return 0;
            var previous=stored.getUpdateTag(l.registryAccess()).getInt("depthKey");
            stored.write(new int[]{760,130,0,0},0);
            if(previous==stored.getUpdateTag(l.registryAccess()).getInt("depthKey")||state.getCollisionShape(l,fixture)==expected.shape())return 0;
            l.removeBlock(fixture,false);
            c.getSource().sendSuccess(()->Component.literal("TreeChopper crown ownership PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_leafless_cut").executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=new BlockPos(1038,203,1004);
            var oak=net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
            // An existing notched log remains a valid tree section even if the foliage is already gone.
            l.setBlock(p.below(),oak,3);
            l.setBlock(p,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"cut_oak")).defaultBlockState(),3);
            l.setBlock(p.above(),oak,3);l.setBlock(p.above(2),oak,3);
            var fake=FakePlayerFactory.getMinecraft(l);fake.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()-2);
            for(int i=0;i<12&&!(l.getBlockState(p).getBlock() instanceof StumpBlock);i++){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            }
            if(!(l.getBlockState(p).getBlock() instanceof StumpBlock)||!l.getBlockState(p.above()).isAir()||!l.getBlockState(p.above(2)).isAir())return 0;
            c.getSource().sendSuccess(()->Component.literal("TreeChopper leafless section PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_balance_math").executes(c->{
            int[] deep={620,0,0,0};int guide=FallBalance.guideForStrike(deep,1,-1);
            int back=FallBalance.counterWork(deep,1,guide,400);
            int[] result={620,back,0,0};
            double[] v=FallBalance.cutVector(result,guide);
            double[] plain=FallBalance.cutVector(new int[]{1000,0,0,0},-1);
            if(guide!=0||back!=130||!FallBalance.validGuide(result,guide)||!ChopEffort.severed(result)
                ||FallBalance.guideForStrike(new int[]{250,0,0,0},1,-1)!=-1
                ||FallBalance.guideForStrike(deep,0,-1)!=-1
                ||FallBalance.choose(v[0],v[1],0,0,0,0,Direction.SOUTH)!=Direction.NORTH
                ||FallBalance.choose(plain[0],plain[1],0,0,0,0,Direction.SOUTH)!=Direction.NORTH
                ||FallBalance.choose(plain[0],plain[1],3,0,1,0,Direction.NORTH)!=Direction.EAST
                ||FallBalance.choose(0,0,0,0,0,-3,Direction.SOUTH)!=Direction.NORTH
                ||FallBalance.mass(true,Direction.Axis.X)<=FallBalance.mass(false,Direction.Axis.Y)){
                c.getSource().sendFailure(Component.literal("TreeChopper directional balance FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper directional balance PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_guide").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var fake=FakePlayerFactory.getMinecraft(l);fake.setPos(p.getX()+.5,p.getY(),p.getZ()-2);
            ItemStack axe=new ItemStack(Items.IRON_AXE);fake.setItemSlot(EquipmentSlot.MAINHAND,axe);
            int count=0;
            while(CutProgress.read(l,p,l.getBlockState(p))[0]<500&&count++<5){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            }
            var before=l.getBlockState(p);int[] work=CutProgress.read(l,p,before);
            if(!(before.getBlock() instanceof CutBlock)||CutBlock.depth(l,p,Direction.NORTH)<2||work[1]!=0||CutProgress.guide(l,p)!=-1){
                c.getSource().sendFailure(Component.literal("TreeChopper initial directional notch FAILED"));return 0;
            }
            fake.setPos(p.getX()+.5,p.getY(),p.getZ()+2);
            for(int i=0;i<4&&!(l.getBlockState(p).getBlock() instanceof StumpBlock);i++){
                TreeChopper.qaResetCooldown(fake);
                NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.SOUTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            }
            var list=l.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,new net.minecraft.world.phys.AABB(p).inflate(2),d->d.getPersistentData().getCompound(TreeChopper.FALL).getBoolean("coordinator"));
            if(!(l.getBlockState(p).getBlock() instanceof StumpBlock)||list.size()!=1){
                c.getSource().sendFailure(Component.literal("TreeChopper guided fall FAILED"));return 0;
            }
            var f=list.get(0).getPersistentData().getCompound(TreeChopper.FALL);
            if(!f.getString("direction").equals("north")||f.getInt("guides")!=1){
                c.getSource().sendFailure(Component.literal("TreeChopper guided direction FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper guided fall PASS"),false);return 1;
          })))
          .then(Commands.literal("qa_mass").then(Commands.argument("pos",BlockPosArgument.blockPos()).then(Commands.argument("expected",StringArgumentType.word()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            Direction expected=Direction.byName(StringArgumentType.getString(c,"expected"));
            Direction found=TreeChopper.qaBalance(l,p,"oak",Direction.NORTH);
            if(found!=expected){c.getSource().sendFailure(Component.literal("TreeChopper world mass direction FAILED: "+found));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper world mass direction PASS"),false);return 1;
          }))))
          .then(Commands.literal("qa_physics_fallback").executes(c->{
            ServerLevel l=c.getSource().getLevel();
            var data=MaterialPhysics.read(l,BlockPos.ZERO,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),true,Double.NaN);
            if(MaterialPhysics.coreAvailable()||MaterialPhysics.woodAvailable()||data.known()||Double.isFinite(data.massKg())
                ||MaterialPhysics.resistance(Double.NaN,Double.NaN)!=1
                ||MaterialPhysics.equivalentLogs(Double.NaN,Double.NaN,12)!=12
                ||MaterialPhysics.equivalentLogs(1800,600,12)!=3
                ||MaterialPhysics.resistance(6000,3000)!=2){
                c.getSource().sendFailure(Component.literal("TreeChopper physical fallback FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper physical fallback PASS"),false);return 1;
          }))
          .then(Commands.literal("qa_physics_integration").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var state=l.getBlockState(p);
            var dry=MaterialPhysics.read(l,p,state,true,12);
            var wet=MaterialPhysics.read(l,p,state,true,90);
            var actual=MaterialPhysics.read(l,p,state,true,Double.NaN);
            if(!MaterialPhysics.coreAvailable()||!dry.known()||!wet.known()||!actual.known()
                ||(MaterialPhysics.canonicalAvailable()&&(!dry.source().equals("core")||!wet.source().equals("core")||!actual.source().equals("core")))
                ||(!MaterialPhysics.canonicalAvailable()&&!MaterialPhysics.woodAvailable())
                ||Math.abs(dry.volumeM3()-wet.volumeM3())>1e-8||wet.massKg()<=dry.massKg()
                ||!Double.isFinite(dry.hardnessN())||wet.hardnessN()>=dry.hardnessN()
                ||!Double.isFinite(actual.moisturePercent())||!Double.isFinite(MaterialPhysics.oakMassKg())){
                c.getSource().sendFailure(Component.literal("TreeChopper real Core/Terrarian physical API FAILED"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper real Core/Terrarian physical API PASS: "+actual.source()+" "+actual.massKg()+" kg"),false);return 1;
          })))
          .then(Commands.literal("qa_canonical_wood").then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();BlockPos p=BlockPosArgument.getLoadedBlockPos(c,"pos");
            var state=l.getBlockState(p);
            var dry=MaterialPhysics.read(l,p,state,true,12);
            var wet=MaterialPhysics.read(l,p,state,true,90);
            var invalid=MaterialPhysics.read(l,p,state,true,251);
            var actual=MaterialPhysics.read(l,p,state,true,Double.NaN);
            boolean oak=state.is(net.minecraft.world.level.block.Blocks.OAK_LOG);
            if(!MaterialPhysics.canonicalAvailable()||!dry.known()||!wet.known()||!actual.known()
                ||!dry.source().equals("core")||!wet.source().equals("core")||!actual.source().equals("core")
                ||Math.abs(dry.volumeM3()-wet.volumeM3())>1e-8||wet.massKg()<=dry.massKg()
                ||!Double.isFinite(dry.hardnessN())||wet.hardnessN()>=dry.hardnessN()
                ||invalid.known()||!Double.isFinite(actual.moisturePercent())
                ||(oak&&(Math.abs(dry.massKg()/dry.volumeM3()-710)>1e-6||Math.abs(dry.hardnessN()-5900)>1e-6))){
                c.getSource().sendFailure(Component.literal("TreeChopper canonical wood FAILED: "+dry+" / "+wet+" / "+actual));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal("TreeChopper canonical wood PASS"),false);return 1;
          })))
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
            // Compare observed strikes to the deterministic min/max of the new height-aware work function.
            int minWork=ChopEffort.workPerHit(2,1,1,logs,0,logs-1);
            int maxWork=ChopEffort.workPerHit(2,1,1,logs,1,logs-1);
            int min=(1000+maxWork-1)/maxWork,max=(1000+minWork-1)/minWork;
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
            int[] firstWork=CutProgress.read(l,p,first);
            TreeChopper.networkStrike(fake,p);
            if(!first.equals(l.getBlockState(p))||!java.util.Arrays.equals(firstWork,CutProgress.read(l,p,l.getBlockState(p)))){c.getSource().sendFailure(Component.literal("TreeChopper network cooldown FAILED"));return 0;}
            fake.setPos(p.getX()+.5,p.getY()-1,p.getZ()-20);
            TreeChopper.qaResetCooldown(fake);TreeChopper.networkStrike(fake,p);
            if(!first.equals(l.getBlockState(p))||!java.util.Arrays.equals(firstWork,CutProgress.read(l,p,l.getBlockState(p)))){c.getSource().sendFailure(Component.literal("TreeChopper network range FAILED"));return 0;}
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
            var before=l.getBlockState(p);
            var click=NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(fake,p,Direction.NORTH,PlayerInteractEvent.LeftClickBlock.Action.START));
            boolean destroyed=fake.gameMode.destroyBlock(p);
            if(!click.isCanceled()||destroyed||!before.equals(l.getBlockState(p))){c.getSource().sendFailure(Component.literal("TreeChopper hand protection FAILED"));return 0;}
            c.getSource().sendSuccess(()->Component.literal("TreeChopper hand break blocked"),false);return 1;
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
