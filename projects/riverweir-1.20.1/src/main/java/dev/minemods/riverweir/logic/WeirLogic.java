package dev.minemods.riverweir.logic;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.block.WeirNetBlock;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;

public final class WeirLogic {
    public static final int MIN_SPAN=2,MAX_SPAN=32,MAX_WEAR=100;
    private static final ResourceLocation JUTE_NET=new ResourceLocation("tfc","jute_net");
    private WeirLogic() {}

    public static boolean isJuteNet(ItemStack stack) {
        return !stack.isEmpty() && JUTE_NET.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    private static WeirPostBlockEntity post(ServerLevel level,BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof WeirPostBlockEntity p ? p : null;
    }
    private static BlockPos at(BlockPos start,Direction dir,int length,int y) {
        return new BlockPos(start.getX()+dir.getStepX()*length,y,start.getZ()+dir.getStepZ()*length);
    }
    private static Direction direction(BlockPos a,BlockPos b) {
        if(a.getY()!=b.getY()) return null;
        if(a.getX()==b.getX() && a.getZ()!=b.getZ()) return a.getZ()<b.getZ()?Direction.SOUTH:Direction.NORTH;
        if(a.getZ()==b.getZ() && a.getX()!=b.getX()) return a.getX()<b.getX()?Direction.EAST:Direction.WEST;
        return null;
    }
    private static int span(BlockPos a,BlockPos b) {return Math.abs(a.getX()-b.getX())+Math.abs(a.getZ()-b.getZ())-1;}

    public static boolean connect(ServerLevel level,BlockPos first,BlockPos second,Player player) {
        WeirPostBlockEntity a=post(level,first),b=post(level,second);
        if(a==null || b==null || !level.getBlockState(first).is(RiverWeirMod.POST.get()) ||
           !level.getBlockState(second).is(RiverWeirMod.POST.get()) || a.other!=null || b.other!=null) {
            error(player,"message.riverweir.posts");return false;
        }
        Direction dir=direction(first,second);
        int count=span(first,second);
        if(dir==null || count<MIN_SPAN || count>MAX_SPAN) {
            error(player,"message.riverweir.distance");return false;
        }
        int y=Integer.MIN_VALUE;
        for(int candidate : new int[]{first.getY(),first.getY()-1}) {
            boolean valid=true;
            for(int i=1;i<=count;i++) {
                BlockPos p=at(first,dir,i,candidate);
                if(!isFreeWater(level,p)) {valid=false;break;}
            }
            if(valid) {y=candidate;break;}
        }
        if(y==Integer.MIN_VALUE) {error(player,"message.riverweir.water");return false;}
        int needed=(count+3)/4;
        if (!player.getAbilities().instabuild && countRolls(player)<needed) {
            player.displayClientMessage(Component.translatable("message.riverweir.rolls",needed),true);
            return false;
        }
        // Reserve stock before changing any blocks; placement is now atomic on the server thread.
        if (!player.getAbilities().instabuild) consumeRolls(player,needed);
        a.link(second,true,y);
        b.link(first,false,y);
        for(int i=1;i<=count;i++) {
            for(int depth=0;depth<3;depth++) {
                BlockPos p=at(first,dir,i,y-depth);
                if(isFreeWater(level,p)) {
                    BlockState net=RiverWeirMod.NET.get().defaultBlockState()
                        .setValue(WeirNetBlock.AXIS,dir.getAxis())
                        .setValue(WeirNetBlock.STAGE,0)
                        .setValue(WeirNetBlock.WATERLOGGED,true);
                    level.setBlock(p,net,3);
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.riverweir.connected",count),true);
        return true;
    }
    private static void error(Player player,String key) {player.displayClientMessage(Component.translatable(key),true);}
    private static int countRolls(Player player) {
        int found=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack s=player.getInventory().getItem(i);
            if(s.is(RiverWeirMod.NET_ROLL.get())) found+=s.getCount();
        }
        return found;
    }
    private static void consumeRolls(Player player,int count) {
        for(int i=0;i<player.getInventory().getContainerSize()&&count>0;i++) {
            ItemStack s=player.getInventory().getItem(i);
            if(s.is(RiverWeirMod.NET_ROLL.get())) {
                int n=Math.min(count,s.getCount());
                s.shrink(n);count-=n;
            }
        }
        player.getInventory().setChanged();
    }
    private static boolean isFreeWater(ServerLevel level,BlockPos p) {
        if(!level.hasChunkAt(p)) return false;
        BlockState state=level.getBlockState(p);
        return level.getFluidState(p).is(FluidTags.WATER)
            && !state.is(RiverWeirMod.NET.get()) && state.getCollisionShape(level,p).isEmpty()
            && (state.canBeReplaced() || state.is(Blocks.WATER));
    }
    private static WeirPostBlockEntity master(ServerLevel level,BlockPos post) {
        WeirPostBlockEntity be=post(level,post);
        if(be==null || be.other==null) return null;
        return be.master ? be : post(level,be.other);
    }
    public static BlockPos findLinkedPost(ServerLevel level,BlockPos net,Direction.Axis axis) {
        for(Direction dir : axis==Direction.Axis.X ? new Direction[]{Direction.EAST,Direction.WEST} :
                new Direction[]{Direction.NORTH,Direction.SOUTH}) {
            for(int distance=1;distance<=MAX_SPAN+1;distance++) {
                for(int dy=0;dy<=3;dy++) {
                    BlockPos p=net.relative(dir,distance).above(dy);
                    WeirPostBlockEntity be=post(level,p);
                    if(be!=null && be.other!=null && be.netY>=net.getY()
                        && be.netY-net.getY()<=2
                        && direction(p,be.other)!=null
                        && net.getX()>=Math.min(p.getX(),be.other.getX())
                        && net.getX()<=Math.max(p.getX(),be.other.getX())
                        && net.getZ()>=Math.min(p.getZ(),be.other.getZ())
                        && net.getZ()<=Math.max(p.getZ(),be.other.getZ())) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
    public static void openMenu(ServerLevel level,BlockPos post,ServerPlayer player) {
        WeirPostBlockEntity be=master(level,post);
        if(be==null || be.other==null) {
            error(player,"message.riverweir.unlinked");return;
        }
        NetworkHooks.openScreen(player,be,be.getBlockPos());
    }
    public static void tear(ServerLevel level,BlockPos post) {
        WeirPostBlockEntity be=master(level,post);
        if(be!=null) damage(level,be,MAX_WEAR);
    }
    public static void damage(ServerLevel level,WeirPostBlockEntity master,int amount) {
        int old=master.wear;
        master.wear=Math.min(MAX_WEAR,master.wear+amount);
        master.setChanged();
        if(stage(old)!=stage(master.wear)) updateNet(level,master,false);
    }
    private static int stage(int wear) {return wear>=MAX_WEAR?2:wear>=55?1:0;}
    public static void repair(ServerLevel level,BlockPos post,Player player,InteractionHand hand) {
        WeirPostBlockEntity be=master(level,post);
        if(be==null || be.wear==0) return;
        ItemStack material=player.getItemInHand(hand);
        if(!isJuteNet(material)) return;
        if(!player.getAbilities().instabuild) material.shrink(1);
        be.wear=Math.max(0,be.wear-40);
        be.setChanged();
        updateNet(level,be,true);
        player.displayClientMessage(Component.translatable("message.riverweir.repaired"),true);
    }
    public static void dismantle(ServerLevel level,BlockPos postPos) {
        WeirPostBlockEntity clicked=post(level,postPos);
        if(clicked==null || clicked.other==null) return;
        WeirPostBlockEntity main=master(level,postPos);
        BlockPos opposite=clicked.other;
        if(main!=null) {
            for(int i=0;i<main.items.getSlots();i++) {
                ItemStack drop=main.items.extractItem(i,main.items.getStackInSlot(i).getCount(),false);
                if(!drop.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,main.getBlockPos().getX()+0.5,
                    main.getBlockPos().getY()+0.5,main.getBlockPos().getZ()+0.5,drop);
            }
        }
        int y=clicked.netY;
        // Clear both endpoints before removing panels so panel removal doesn't count as accidental tearing.
        clicked.clearLink();
        WeirPostBlockEntity other=post(level,opposite);
        if(other!=null) other.clearLink();
        Direction dir=direction(postPos,opposite);
        if(dir==null) return;
        int count=span(postPos,opposite);
        for(int i=1;i<=count;i++) for(int depth=0;depth<3;depth++) {
            BlockPos p=at(postPos,dir,i,y-depth);
            if(level.hasChunkAt(p) && level.getBlockState(p).is(RiverWeirMod.NET.get()))
                level.setBlock(p,Blocks.WATER.defaultBlockState(),3);
        }
    }
    private static void updateNet(ServerLevel level,WeirPostBlockEntity master,boolean replaceMissing) {
        if(master.other==null) return;
        Direction dir=direction(master.getBlockPos(),master.other);
        if(dir==null)return;
        int count=span(master.getBlockPos(),master.other),stateStage=stage(master.wear);
        for(int i=1;i<=count;i++) for(int depth=0;depth<3;depth++) {
            BlockPos p=at(master.getBlockPos(),dir,i,master.netY-depth);
            if(!level.hasChunkAt(p)) continue;
            BlockState current=level.getBlockState(p);
            if(current.is(RiverWeirMod.NET.get())) {
                if(current.getValue(WeirNetBlock.STAGE)!=stateStage)
                    level.setBlock(p,current.setValue(WeirNetBlock.STAGE,stateStage),3);
            } else if(replaceMissing && isFreeWater(level,p)) {
                level.setBlock(p,RiverWeirMod.NET.get().defaultBlockState()
                    .setValue(WeirNetBlock.AXIS,dir.getAxis())
                    .setValue(WeirNetBlock.STAGE,stateStage),3);
            }
        }
    }
    public static boolean isFishingEnvironment(ServerLevel level,WeirPostBlockEntity be) {
        if(be.other==null) return false;
        Direction dir=direction(be.getBlockPos(),be.other);
        if(dir==null) return false;
        int count=span(be.getBlockPos(),be.other);
        if(count<MIN_SPAN || count>MAX_SPAN) return false;
        int intact=0;
        for(int i=1;i<=count;i++) {
            BlockPos p=at(be.getBlockPos(),dir,i,be.netY);
            if(!level.hasChunkAt(p))return false;
            if(level.getBlockState(p).is(RiverWeirMod.NET.get()) && level.getFluidState(p).is(FluidTags.WATER))intact++;
        }
        return intact==count;
    }
    public static void catchNearbyFish(ServerLevel level,WeirPostBlockEntity be) {
        if(be.other==null || be.wear>=MAX_WEAR) return;
        BlockPos a=be.getBlockPos(),b=be.other;
        AABB detection=new AABB(
            Math.min(a.getX(),b.getX())-0.7,be.netY-3,
            Math.min(a.getZ(),b.getZ())-0.7,
            Math.max(a.getX(),b.getX())+1.7,be.netY+1.7,
            Math.max(a.getZ(),b.getZ())+1.7);
        List<AbstractFish> candidates=level.getEntitiesOfClass(AbstractFish.class,detection,
            fish->fish.isAlive() && fish.isInWater());
        if(candidates.isEmpty()) return;
        AbstractFish fish=candidates.get(level.random.nextInt(candidates.size()));
        ResourceLocation entityId=BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType());
        if(entityId==null)return;
        ResourceLocation tableId=new ResourceLocation(entityId.getNamespace(),"entities/"+entityId.getPath());
        LootTable table=level.getServer().getLootData().getLootTable(tableId);
        LootParams params=new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY,fish)
            .withParameter(LootContextParams.ORIGIN,fish.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE,fish.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        List<ItemStack> drops=table.getRandomItems(params);
        drops.removeIf(ItemStack::isEmpty);
        if(drops.isEmpty())return;
        // Simulate every insertion before removing the entity: never lose fish from a full inventory.
        ItemStackHandler simulated=new ItemStackHandler(9);
        for(int i=0;i<9;i++)simulated.setStackInSlot(i,be.items.getStackInSlot(i).copy());
        for(ItemStack item:drops) {
            ItemStack remaining=item.copy();
            for(int i=0;i<9 && !remaining.isEmpty();i++)remaining=simulated.insertItem(i,remaining,false);
            if(!remaining.isEmpty())return;
        }
        for(int i=0;i<9;i++)be.items.setStackInSlot(i,simulated.getStackInSlot(i).copy());
        fish.discard();
        damage(level,be,1+level.random.nextInt(3));
    }
}
