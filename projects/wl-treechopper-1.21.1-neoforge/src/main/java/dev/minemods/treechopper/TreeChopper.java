package dev.minemods.treechopper;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.core.particles.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod(TreeChopper.ID)
public final class TreeChopper {
    public static final String ID="wl_treechopper";
    private static final String[] SPECIES={"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry"};
    private static final Direction[] SIDES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    private static final Map<String,DeferredBlock<CutBlock>> CUTS=new LinkedHashMap<>();
    private static final Map<String,DeferredBlock<StumpBlock>> STUMPS=new LinkedHashMap<>();
    private static final String COOLDOWN="wl_treechopper:last_hit", FALL="wl_treechopper_fall";
    private static final int FALL_TICKS=36;
    static {
        for(String name:SPECIES){
            CUTS.put(name,BLOCKS.register("cut_"+name,()->new CutBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion())));
            STUMPS.put(name,BLOCKS.register("stump_"+name,()->new StumpBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion())));
        }
    }
    public TreeChopper(IEventBus modBus){
        BLOCKS.register(modBus);
        NeoForge.EVENT_BUS.addListener(TreeChopper::hit);
        NeoForge.EVENT_BUS.addListener(TreeChopper::tickDisplay);
        NeoForge.EVENT_BUS.addListener(TreeQA::register);
    }
    static BlockState original(String name){
        return BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(name+"_log")).defaultBlockState();
    }
    private static String species(BlockState state) {
        for(String s:SPECIES){
            if(state.is(CUTS.get(s).get())) return s;
            if(state.is(original(s).getBlock()) && state.getValue(BlockStateProperties.AXIS)==Direction.Axis.Y) return s;
        }
        return null;
    }
    private static boolean allowed(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state){
        return level.mayInteract(player,pos) && !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled();
    }
    private static void hit(PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isCreative() || player.isSpectator())return;
        BlockPos pos=event.getPos();
        BlockState old=level.getBlockState(pos);
        String s=species(old);
        if(s==null)return;
        boolean cut=old.getBlock() instanceof CutBlock;
        ItemStack axe=player.getMainHandItem();
        if(!(axe.getItem() instanceof AxeItem)){if(cut)event.setCanceled(true);return;}
        List<Piece> tree=scan(level,pos,s);
        if(tree.isEmpty()&&!cut)return;
        event.setCanceled(true);
        if(event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START || tree.isEmpty() || !player.mayBuild())return;
        Direction side=event.getFace();
        if(side==null||side.getAxis().isVertical()) side=player.getDirection().getOpposite();
        double power=WorldLogicIntegration.effort(player).power();
        if(power<=0)return;
        long now=level.getGameTime(); CompoundTag data=player.getPersistentData();
        if(data.contains(COOLDOWN)&&now-data.getLong(COOLDOWN)<ChopEffort.cooldown(power)&&now>=data.getLong(COOLDOWN))return;
        if(!allowed(level,player,pos,old))return;
        data.putLong(COOLDOWN,now);
        BlockState damaged=cut?old:CUTS.get(s).get().defaultBlockState();
        int before=CutBlock.depth(damaged,side);
        float speed=axe.getDestroySpeed(original(s));
        float resistance=s.equals("dark_oak")||s.equals("mangrove")?1.18f:s.equals("birch")?0.86f:1f;
        double roll=level.random.nextDouble();
        int step=ChopEffort.depthIncrement(before,speed,resistance,power,roll);
        int next=Math.min(4,before+step);
        damaged=damaged.setValue(CutBlock.side(side),next);
        if(!level.setBlock(pos,damaged,Block.UPDATE_ALL) && !old.equals(damaged))return;
        axe.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        level.playSound(null,pos,SoundEvents.WOOD_HIT,SoundSource.BLOCKS,1f,.8f+level.random.nextFloat()*.25f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,original(s)),pos.getX()+.5+side.getStepX()*.47,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()*.47,8,.12,.12,.12,.08);
        if(CutBlock.falls(damaged))fall(level,player,pos,s,damaged,tree,side);
    }
    private record Piece(BlockPos pos,BlockState state,boolean log){}
    /** Conservative initial tree recognizer: straight, single-column trunks with a natural crown. Never loads chunks. */
    private static List<Piece> scan(ServerLevel l,BlockPos base,String species){
        if(!l.hasChunkAt(base.below()) || !(l.getBlockState(base.below()).is(BlockTags.DIRT)||l.getBlockState(base.below()).is(BlockTags.LOGS)||l.getBlockState(base.below()).is(Blocks.MUD)||l.getBlockState(base.below()).is(Blocks.CLAY))) return List.of();
        List<Piece> out=new ArrayList<>();
        BlockState original=original(species);
        int h=1;
        for(;h<24;h++){
            BlockPos p=base.above(h);
            if(!l.hasChunkAt(p))return List.of();
            BlockState b=l.getBlockState(p);
            if(!b.is(original.getBlock())||b.getValue(BlockStateProperties.AXIS)!=Direction.Axis.Y)break;
            out.add(new Piece(p,b,true));
        }
        if(h<3||h>=24)return List.of();
        int top=base.getY()+h-1;
        // Reject branching/neighboring trunks rather than accidentally remove a different tree.
        for(int dy=-2;dy<=2;dy++)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            BlockPos p=new BlockPos(base.getX()+x,top+dy,base.getZ()+z);
            if(!l.hasChunkAt(p))return List.of();
            BlockState b=l.getBlockState(p);
            if((x!=0||z!=0)&&b.is(BlockTags.LOGS))return List.of();
            if(b.is(BlockTags.LEAVES)&&b.hasProperty(BlockStateProperties.PERSISTENT)&&!b.getValue(BlockStateProperties.PERSISTENT))
                out.add(new Piece(p,b,false));
        }
        if(out.stream().noneMatch(p->!p.log))return List.of();
        return out;
    }
    private static Direction fallDirection(BlockState s, Direction latest){
        Direction best=latest;
        int max=CutBlock.depth(s,best);
        for(Direction d:SIDES){int n=CutBlock.depth(s,d);if(n>max){best=d;max=n;}}
        return best;
    }
    private static void fall(ServerLevel l,ServerPlayer player,BlockPos base,String species,BlockState cut,List<Piece> tree,Direction latest){
        if(tree.stream().anyMatch(p->!l.getBlockState(p.pos).equals(p.state)||!allowed(l,player,p.pos,p.state)))return;
        Direction direction=fallDirection(cut,latest);
        List<Piece> pieces=new ArrayList<>(tree);
        List<Display.BlockDisplay> displays=new ArrayList<>();
        // All pieces share a common pivot, so rotation preserves the tree silhouette.
        for(Piece piece:pieces){
            Display.BlockDisplay d=new Display.BlockDisplay(EntityType.BLOCK_DISPLAY,l);
            CompoundTag tag=new CompoundTag();
            tag.put("block_state",NbtUtils.writeBlockState(piece.state));
            tag.putFloat("view_range",2f);
            tag.putInt("interpolation_duration",2);
            tag.putInt("start_interpolation",0);
            tag.put("transformation",transform(piece.pos.getX()-base.getX()-.5f,piece.pos.getY()-base.getY()-.25f,piece.pos.getZ()-base.getZ()-.5f,direction,0));
            d.load(tag);
            d.setPos(base.getX()+.5,base.getY()+.25,base.getZ()+.5);
            d.setNoGravity(true);
            d.addTag(FALL);
            CompoundTag fall=new CompoundTag();
            fall.putInt("x",piece.pos.getX()-base.getX());fall.putInt("y",piece.pos.getY()-base.getY());fall.putInt("z",piece.pos.getZ()-base.getZ());
            fall.putString("direction",direction.getName());fall.putBoolean("log",piece.log);fall.putString("species",species);fall.putInt("age",0);
            d.getPersistentData().put(FALL,fall);
            if(!l.addFreshEntity(d)){displays.forEach(Entity::discard);return;}
            displays.add(d);
        }
        l.setBlock(base,STUMPS.get(species).get().defaultBlockState(),Block.UPDATE_ALL);
        // Remove all tree blocks before notifying neighbors to avoid leaf decay/drop duplication.
        for(Piece p:pieces)l.setBlock(p.pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        for(Piece p:pieces)l.updateNeighborsAt(p.pos,Blocks.AIR);
        l.playSound(null,base,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.62f);
    }
    private static ListTag floats(float... v){
        ListTag list=new ListTag();for(float x:v)list.add(FloatTag.valueOf(x));return list;
    }
    private static Quaternionf rotation(Direction dir,float angle){
        return new Quaternionf().rotationAxis(angle,dir.getStepZ(),0,-dir.getStepX());
    }
    private static CompoundTag transform(float x,float y,float z,Direction dir,float angle){
        Quaternionf q=rotation(dir,angle);Vector3f off=q.transform(new Vector3f(x,y,z));
        CompoundTag t=new CompoundTag();
        t.put("translation",floats(off.x,off.y,off.z));t.put("left_rotation",floats(q.x,q.y,q.z,q.w));
        t.put("right_rotation",floats(0,0,0,1));t.put("scale",floats(1,1,1));return t;
    }
    private static void tickDisplay(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof Display.BlockDisplay d)||!(d.level() instanceof ServerLevel l)||!d.getPersistentData().contains(FALL))return;
        CompoundTag f=d.getPersistentData().getCompound(FALL);
        int age=f.getInt("age")+1;f.putInt("age",age);
        Direction dir=Direction.byName(f.getString("direction"));
        if(dir==null){d.discard();return;}
        float x=f.getInt("x")-.5f,y=f.getInt("y")-.25f,z=f.getInt("z")-.5f;
        float progress=Math.min(1,age/(float)FALL_TICKS);
        float angle=(float)(Math.PI*.5*progress*progress);
        if(age>=FALL_TICKS+3){
            if(f.getBoolean("log")&&CUTS.containsKey(f.getString("species"))){
                Vector3f off=rotation(dir,(float)(Math.PI*.5)).transform(new Vector3f(x+.5f,y+.5f,z+.5f));
                BlockPos end=BlockPos.containing(d.getX()+off.x,d.getY()+off.y+.4,d.getZ()+off.z);
                if(!l.hasChunkAt(end)||!l.getBlockState(end).getCollisionShape(l,end).isEmpty())end=d.blockPosition().above();
                Block.popResource(l,end,new ItemStack(original(f.getString("species")).getBlock()));
            }
            d.discard();return;
        }
        if(age%2!=0)return;
        CompoundTag tag=d.saveWithoutId(new CompoundTag());
        tag.putInt("interpolation_duration",2);tag.putInt("start_interpolation",0);
        tag.put("transformation",transform(x,y,z,dir,angle));
        d.load(tag);
    }
    static void qaResetCooldown(ServerPlayer player){player.getPersistentData().remove(COOLDOWN);}
}
