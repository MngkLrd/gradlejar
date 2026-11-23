package dev.minemods.riverweir.logic;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.block.WeirNetBlock;
import dev.minemods.riverweir.block.WeirPostBlock;
import dev.minemods.riverweir.logic.WeirWater;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;

public final class WeirLogic {
    public static final int MIN_SPAN=2,MAX_SPAN=32,MAX_DEPTH=12,PANEL_WIDTH=4,PANEL_HEIGHT=3,MAX_WEAR=100;
    private static final ResourceLocation JUTE_NET=new ResourceLocation("tfc","jute_net");
    private WeirLogic(){}

    public static boolean isJuteNet(ItemStack stack){
        return !stack.isEmpty()&&JUTE_NET.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
    private static WeirPostBlockEntity post(ServerLevel level,BlockPos pos){
        return level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof WeirPostBlockEntity p?p:null;
    }
    private static BlockPos at(BlockPos start,Direction dir,int length,int y){
        return new BlockPos(start.getX()+dir.getStepX()*length,y,start.getZ()+dir.getStepZ()*length);
    }
    private static Direction direction(BlockPos a,BlockPos b){
        if(a.getY()!=b.getY())return null;
        if(a.getX()==b.getX()&&a.getZ()!=b.getZ())return a.getZ()<b.getZ()?Direction.SOUTH:Direction.NORTH;
        if(a.getZ()==b.getZ()&&a.getX()!=b.getX())return a.getX()<b.getX()?Direction.EAST:Direction.WEST;
        return null;
    }
    private static int span(BlockPos a,BlockPos b){
        return Math.abs(a.getX()-b.getX())+Math.abs(a.getZ()-b.getZ())-1;
    }
    private static void say(Player player,String key,Object... args){
        player.displayClientMessage(Component.translatable(key,args),true);
    }
    public static int countRolls(Player player){
        int total=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(stack.is(RiverWeirMod.NET_ROLL.get()))total+=stack.getCount();
        }
        return total;
    }
    private static void consumeRolls(Player player,int amount){
        for(int i=0;i<player.getInventory().getContainerSize()&&amount>0;i++){
            ItemStack stack=player.getInventory().getItem(i);
            if(stack.is(RiverWeirMod.NET_ROLL.get())){
                int take=Math.min(stack.getCount(),amount);
                stack.shrink(take);
                amount-=take;
            }
        }
        player.getInventory().setChanged();
    }
    // Only replace pure freshwater fluid cells, not blocks, vegetation, stone or other waterlogged objects.
    private static boolean isFreeWater(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;
        BlockState state=level.getBlockState(pos);
        return state.getBlock() instanceof LiquidBlock && WeirWater.kind(level.getFluidState(pos))>=0
            && level.getFluidState(pos).isSource();
    }
    private static int[] survey(ServerLevel level,BlockPos from,Direction dir,int width,int topY){
        int[] depths=new int[width];
        for(int col=0;col<width;col++){
            int depth=0;
            for(int d=0;d<MAX_DEPTH;d++){
                BlockPos p=at(from,dir,col+1,topY-d);
                if(!level.hasChunkAt(p))return null;
                if(isFreeWater(level,p)){
                    depth++;
                }else{
                    // Don't cut down aquatic plants or replace a custom block that contains water.
                    if(!level.getFluidState(p).isEmpty())return null;
                    break;
                }
            }
            if(depth==0)return null;
            if(depth==MAX_DEPTH && isFreeWater(level,at(from,dir,col+1,topY-MAX_DEPTH)))return null;
            depths[col]=depth;
        }
        return depths;
    }
    private static int requiredRolls(int[] depths){
        int rolls=0;
        for(int start=0;start<depths.length;start+=PANEL_WIDTH){
            int deepest=0;
            for(int index=start;index<Math.min(depths.length,start+PANEL_WIDTH);index++)
                deepest=Math.max(deepest,depths[index]);
            rolls+=(deepest+PANEL_HEIGHT-1)/PANEL_HEIGHT;
        }
        return rolls;
    }
    public static boolean connect(ServerLevel level,BlockPos first,BlockPos second,Player player){
        WeirPostBlockEntity a=post(level,first),b=post(level,second);
        if(a==null||b==null||!level.getBlockState(first).is(RiverWeirMod.POST.get())
            ||!level.getBlockState(second).is(RiverWeirMod.POST.get())||a.other!=null||b.other!=null){
            say(player,"message.riverweir.posts");return false;
        }
        Direction dir=direction(first,second);
        int width=span(first,second);
        if(dir==null||width<MIN_SPAN||width>MAX_SPAN){
            say(player,"message.riverweir.distance");return false;
        }
        int[] depths=null;
        int waterY=0;
        for(int candidate:new int[]{first.getY(),first.getY()-1}){
            depths=survey(level,first,dir,width,candidate);
            if(depths!=null){waterY=candidate;break;}
        }
        if(depths==null){
            say(player,"message.riverweir.water",MAX_DEPTH);return false;
        }
        int required=requiredRolls(depths);
        int available=countRolls(player);
        if(!player.getAbilities().instabuild && available<required){
            say(player,"message.riverweir.rolls",required,available);
            return false;
        }
        int maxDepth=0;
        for(int d:depths)maxDepth=Math.max(maxDepth,d);
        if(!player.getAbilities().instabuild)consumeRolls(player,required);
        a.link(second,true,waterY,depths);
        b.link(first,false,waterY,depths);
        // Attach the rope from both stakes right to the actual mesh edge.
        setConnection(level,first,dir,true);
        setConnection(level,second,dir.getOpposite(),true);
        for(int i=0;i<width;i++)for(int d=0;d<depths[i];d++){
            BlockPos p=at(first,dir,i+1,waterY-d);
            level.setBlock(p,netState(dir.getAxis(),0,d==0,(d%PANEL_HEIGHT==PANEL_HEIGHT-1||d==depths[i]-1),level.getFluidState(p)),3);
        }
        say(player,"message.riverweir.connected",width,maxDepth,required);
        return true;
    }
    private static void setConnection(ServerLevel level,BlockPos postPos,Direction dir,boolean connected){
        BlockState old=level.getBlockState(postPos);
        if(old.is(RiverWeirMod.POST.get())){
            level.setBlock(postPos,old.setValue(WeirPostBlock.CONNECTED,connected)
                .setValue(WeirPostBlock.FACING,dir),3);
        }
    }
    private static BlockState netState(Direction.Axis axis,int stage,boolean top,boolean bottom,net.minecraft.world.level.material.FluidState fluid){
        return RiverWeirMod.NET.get().defaultBlockState()
            .setValue(WeirNetBlock.AXIS,axis)
            .setValue(WeirNetBlock.STAGE,stage)
            .setValue(WeirNetBlock.WATERLOGGED,true)
            .setValue(WeirNetBlock.WATER_KIND,Math.max(0,WeirWater.kind(fluid)))
            .setValue(WeirNetBlock.FLOW,WeirWater.flow(fluid))
            .setValue(WeirNetBlock.TOP,top)
            .setValue(WeirNetBlock.BOTTOM,bottom);
    }
    private static WeirPostBlockEntity master(ServerLevel level,BlockPos at){
        WeirPostBlockEntity be=post(level,at);
        if(be==null||be.other==null)return null;
        return be.master?be:post(level,be.other);
    }
    public static BlockPos findLinkedPost(ServerLevel level,BlockPos cell,Direction.Axis axis){
        Direction[] directions=axis==Direction.Axis.X?
            new Direction[]{Direction.EAST,Direction.WEST}:
            new Direction[]{Direction.NORTH,Direction.SOUTH};
        for(Direction toPost:directions)for(int distance=1;distance<=MAX_SPAN+1;distance++)
            for(int dy=0;dy<=MAX_DEPTH+1;dy++){
                BlockPos candidate=cell.relative(toPost,distance).above(dy);
                WeirPostBlockEntity post=post(level,candidate);
                if(post==null||post.other==null||post.netY<cell.getY())continue;
                Direction along=direction(candidate,post.other);
                if(along==null||along.getAxis()!=axis)continue;
                int index=Math.abs(cell.getX()-candidate.getX())+Math.abs(cell.getZ()-candidate.getZ())-1;
                if(index<0||index>=span(candidate,post.other))continue;
                if(post.netY-cell.getY()>=post.depthAt(index))continue;
                if(cell.getX()<Math.min(candidate.getX(),post.other.getX())||
                    cell.getX()>Math.max(candidate.getX(),post.other.getX())||
                    cell.getZ()<Math.min(candidate.getZ(),post.other.getZ())||
                    cell.getZ()>Math.max(candidate.getZ(),post.other.getZ()))continue;
                return candidate;
            }
        return null;
    }
    public static void openMenu(ServerLevel level,BlockPos post,ServerPlayer player){
        WeirPostBlockEntity be=master(level,post);
        if(be==null||be.other==null){
            say(player,"message.riverweir.unlinked");return;
        }
        NetworkHooks.openScreen(player,be,be.getBlockPos());
    }
    public static void tear(ServerLevel level,BlockPos post){
        WeirPostBlockEntity be=master(level,post);
        if(be!=null)damage(level,be,MAX_WEAR);
    }
    public static void damage(ServerLevel level,WeirPostBlockEntity be,int amount){
        int original=be.wear;
        be.wear=Math.min(MAX_WEAR,be.wear+amount);
        be.setChanged();
        if(stage(original)!=stage(be.wear))updateNet(level,be,false);
    }
    private static int stage(int wear){return wear>=100?2:wear>=55?1:0;}
    public static void repair(ServerLevel level,BlockPos post,Player player,InteractionHand hand){
        WeirPostBlockEntity be=master(level,post);
        if(be==null||be.wear==0)return;
        ItemStack material=player.getItemInHand(hand);
        if(!isJuteNet(material))return;
        if(!player.getAbilities().instabuild)material.shrink(1);
        be.wear=Math.max(0,be.wear-40);
        be.setChanged();
        updateNet(level,be,true);
        say(player,"message.riverweir.repaired");
    }
    public static void dismantle(ServerLevel level,BlockPos pos){
        WeirPostBlockEntity clicked=post(level,pos);
        if(clicked==null||clicked.other==null)return;
        WeirPostBlockEntity origin=master(level,pos);
        if(origin==null)return;
        BlockPos source=origin.getBlockPos(),opposite=origin.other;
        Direction dir=direction(source,opposite);
        if(dir==null)return;
        int count=span(source,opposite),waterY=origin.netY;
        int[] savedDepths=new int[count];
        for(int i=0;i<count;i++)savedDepths[i]=origin.depthAt(i);
        for(int i=0;i<origin.items.getSlots();i++){
            ItemStack loot=origin.items.extractItem(i,origin.items.getStackInSlot(i).getCount(),false);
            if(!loot.isEmpty())net.minecraft.world.Containers.dropItemStack(level,source.getX()+.5,
                source.getY()+.5,source.getZ()+.5,loot);
        }
        WeirPostBlockEntity endpoint=post(level,opposite);
        origin.clearLink();
        if(endpoint!=null)endpoint.clearLink();
        setConnection(level,source,dir,false);
        setConnection(level,opposite,dir.getOpposite(),false);
        // The blocks below the mesh were never changed; remove only the exact deployed cells.
        for(int col=0;col<count;col++)for(int d=0;d<savedDepths[col];d++){
            BlockPos panel=at(source,dir,col+1,waterY-d);
            if(level.hasChunkAt(panel)&&level.getBlockState(panel).is(RiverWeirMod.NET.get()))
                level.setBlock(panel,level.getBlockState(panel).getFluidState().createLegacyBlock(),3);
        }
    }
    private static void updateNet(ServerLevel level,WeirPostBlockEntity be,boolean rebuild){
        if(be.other==null)return;
        Direction dir=direction(be.getBlockPos(),be.other);
        if(dir==null)return;
        int count=span(be.getBlockPos(),be.other);
        for(int col=0;col<count;col++)for(int d=0;d<be.depthAt(col);d++){
            BlockPos p=at(be.getBlockPos(),dir,col+1,be.netY-d);
            if(!level.hasChunkAt(p))continue;
            BlockState state=level.getBlockState(p);
            BlockState wanted=netState(dir.getAxis(),stage(be.wear),d==0,(d%PANEL_HEIGHT==PANEL_HEIGHT-1||d==be.depthAt(col)-1),state.getFluidState());
            if(state.is(RiverWeirMod.NET.get())){
                if(!state.equals(wanted))level.setBlock(p,wanted,3);
            }else if(rebuild && isFreeWater(level,p))level.setBlock(p,wanted,3);
        }
    }
    public static boolean isFishingEnvironment(ServerLevel level,WeirPostBlockEntity be){
        if(be.other==null)return false;
        Direction dir=direction(be.getBlockPos(),be.other);
        if(dir==null)return false;
        int count=span(be.getBlockPos(),be.other);
        if(count<MIN_SPAN||count>MAX_SPAN)return false;
        for(int i=0;i<count;i++){
            BlockPos top=at(be.getBlockPos(),dir,i+1,be.netY);
            if(!level.hasChunkAt(top)||!level.getBlockState(top).is(RiverWeirMod.NET.get()))return false;
        }
        return true;
    }

    // TFC fish loot tables all require killed_by_player. A passive weir never counts as a
    // player kill, so querying those tables returns zero items. Resolve fresh TFC fish food
    // by its registered species ID instead; do not fake player kills or duplicate a drop.
    private static ItemStack catchItem(AbstractFish fish){
        ResourceLocation id=BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType());
        if(id==null)return ItemStack.EMPTY;
        if(id.getNamespace().equals("minecraft")){
            return switch(id.getPath()){
                case "salmon" -> new ItemStack(Items.SALMON);
                case "cod" -> new ItemStack(Items.COD);
                case "tropical_fish" -> new ItemStack(Items.TROPICAL_FISH);
                case "pufferfish" -> new ItemStack(Items.PUFFERFISH);
                default -> ItemStack.EMPTY;
            };
        }
        for(String candidate:new String[]{"food/"+id.getPath(),"raw_"+id.getPath()}) {
            ResourceLocation resource=new ResourceLocation(id.getNamespace(),candidate);
            if(ForgeRegistries.ITEMS.containsKey(resource)){
                Item item=ForgeRegistries.ITEMS.getValue(resource);
                if(item!=null&&item!=Items.AIR)return new ItemStack(item);
            }
        }
        // Some companion fish mods use a TFC food item registered under the tfc namespace.
        ResourceLocation tfcFood=new ResourceLocation("tfc","food/"+id.getPath());
        if(ForgeRegistries.ITEMS.containsKey(tfcFood)){
            Item item=ForgeRegistries.ITEMS.getValue(tfcFood);
            if(item!=null&&item!=Items.AIR)return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }
    public static void catchNearbyFish(ServerLevel level,WeirPostBlockEntity be){
        if(be.other==null||be.wear>=MAX_WEAR)return;
        BlockPos first=be.getBlockPos(),second=be.other;
        Direction dir=direction(first,second);
        if(dir==null)return;
        int count=span(first,second);
        int dmax=be.maxDepth();
        AABB search=new AABB(
            Math.min(first.getX(),second.getX())-1.5,be.netY-dmax-0.7,
            Math.min(first.getZ(),second.getZ())-1.5,
            Math.max(first.getX(),second.getX())+2.5,be.netY+1.7,
            Math.max(first.getZ(),second.getZ())+2.5);
        List<AbstractFish> fish=level.getEntitiesOfClass(AbstractFish.class,search,f->f.isAlive()&&f.isInWater());
        for(AbstractFish candidate:fish){
            double plane=dir.getAxis()==Direction.Axis.X?first.getZ()+.5:first.getX()+.5;
            double delta=Math.abs((dir.getAxis()==Direction.Axis.X?candidate.getZ():candidate.getX())-plane);
            if(delta>1.55)continue;
            int atIndex=(dir.getAxis()==Direction.Axis.X?
                Math.abs(candidate.blockPosition().getX()-first.getX()):
                Math.abs(candidate.blockPosition().getZ()-first.getZ()))-1;
            if(atIndex<0||atIndex>=count)continue;
            int localY=be.netY-candidate.blockPosition().getY();
            if(localY<0||localY>=be.depthAt(atIndex))continue;
            BlockPos mesh=at(first,dir,atIndex+1,be.netY-localY);
            if(!level.hasChunkAt(mesh)||!level.getBlockState(mesh).is(RiverWeirMod.NET.get()))continue;

            ItemStack caught=catchItem(candidate);
            if(caught.isEmpty())continue;
            ItemStackHandler trial=new ItemStackHandler(be.items.getSlots());
            for(int i=0;i<be.items.getSlots();i++)
                trial.setStackInSlot(i,be.items.getStackInSlot(i).copy());
            ItemStack remaining=caught.copy();
            for(int i=0;i<trial.getSlots()&&!remaining.isEmpty();i++)
                remaining=trial.insertItem(i,remaining,false);
            if(!remaining.isEmpty())return; // Inventory full: fish stays alive.
            for(int i=0;i<be.items.getSlots();i++)
                be.items.setStackInSlot(i,trial.getStackInSlot(i).copy());
            candidate.discard();
            damage(level,be,1+level.random.nextInt(3));
            return; // At most one fish per scan interval.
        }
    }
}
