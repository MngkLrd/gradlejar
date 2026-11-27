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
    private static final String[] SPECIES={"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","willow","larch","pine","fir","cedar","maple","beech","alder","palm","date_palm","baobab","ebony","mahogany"};
    private static final Set<String> CUSTOM=Set.of("willow","larch","pine","fir","cedar","maple","beech","alder","palm","date_palm","baobab","ebony","mahogany");
    private static final ThreadLocal<Boolean> PROTECTION_CHECK=ThreadLocal.withInitial(()->false);
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
        NeoForge.EVENT_BUS.addListener(TreeChopper::guardBreak);
        NeoForge.EVENT_BUS.addListener(TreeChopper::tickDisplay);
        NeoForge.EVENT_BUS.addListener(TreeQA::register);
    }
    static BlockState original(String name){
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CUSTOM.contains(name)?"worldlogicterrain":"minecraft",name+"_log")).defaultBlockState();
    }
    private static String species(BlockState state) {
        for(String s:SPECIES){
            if(state.is(CUTS.get(s).get())) return s;
            BlockState source=original(s);
            if(!source.isAir() && state.is(source.getBlock()) && state.hasProperty(BlockStateProperties.AXIS)) return s;
        }
        return null;
    }
    private static boolean allowed(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state){
        if(!level.mayInteract(player,pos))return false;
        PROTECTION_CHECK.set(true);
        try{return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled();}
        finally{PROTECTION_CHECK.remove();}
    }
    private static void guardBreak(BlockEvent.BreakEvent event){
        if(PROTECTION_CHECK.get() || event.getPlayer().isCreative() || event.getPlayer().isSpectator() || !(event.getLevel() instanceof ServerLevel level))return;
        BlockState state=event.getState();
        String s=species(state);
        if(s!=null && (state.getBlock() instanceof CutBlock || !scan(level,event.getPos(),s).isEmpty()))event.setCanceled(true);
    }
    private static void hit(PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isCreative() || player.isSpectator())return;
        BlockPos pos=event.getPos();
        BlockState old=level.getBlockState(pos);
        String s=species(old);
        if(s==null)return;
        boolean cut=old.getBlock() instanceof CutBlock;
        ItemStack axe=player.getMainHandItem();
        List<Piece> tree=scan(level,pos,s);
        if(tree.isEmpty()&&!cut)return;
        event.setCanceled(true);
        if(!(axe.getItem() instanceof AxeItem))return;
        if(event.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START || tree.isEmpty() || !player.mayBuild())return;
        Direction side=event.getFace();
        // The hit face can be a horizontal ledge of an existing notch; use the player's approach side.
        if(side==null||side.getAxis().isVertical()||cut){
            double dx=player.getX()-(pos.getX()+.5),dz=player.getZ()-(pos.getZ()+.5);
            side=Math.abs(dx)>Math.abs(dz)?(dx>0?Direction.EAST:Direction.WEST):(dz>0?Direction.SOUTH:Direction.NORTH);
        }
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
    private static boolean naturalLeaf(BlockState b){
        return b.is(BlockTags.LEAVES)&&b.hasProperty(BlockStateProperties.PERSISTENT)&&!b.getValue(BlockStateProperties.PERSISTENT);
    }
    private static boolean sameLog(BlockState b,BlockState source){return !source.isAir()&&b.is(source.getBlock());}
    /** Bounded connected crown search; no forced chunk loads. Only logs above the chosen cut are removed. */
    private static List<Piece> scan(ServerLevel l,BlockPos base,String species){
        BlockState source=original(species);
        if(source.isAir()||!l.hasChunkAt(base.below()))return List.of();
        BlockState below=l.getBlockState(base.below());
        if(!(below.is(BlockTags.DIRT)||below.is(BlockTags.LOGS)||below.is(Blocks.MUD)||below.is(Blocks.CLAY)||below.is(BlockTags.SAND)))return List.of();
        Set<BlockPos> logs=new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        queue.add(base);logs.add(base);
        while(!queue.isEmpty()){
            BlockPos p=queue.removeFirst();
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dy==0&&dz==0)continue;
                // Diagonal upward branches are normal; do not jump sideways between separate same-level trunks.
                if(dy==0&&Math.abs(dx)+Math.abs(dz)>1)continue;
                BlockPos n=p.offset(dx,dy,dz);
                if(n.equals(base)||logs.contains(n)||n.getY()<=base.getY())continue;
                if(n.getY()-base.getY()>32||Math.abs(n.getX()-base.getX())>10||Math.abs(n.getZ()-base.getZ())>10)continue;
                if(!l.hasChunkAt(n))continue;
                if(sameLog(l.getBlockState(n),source)){
                    if(logs.size()>=256)return List.of();
                    logs.add(n);queue.addLast(n);
                }
            }
        }
        if(logs.size()<2)return List.of();
        // Reject independently supported neighboring columns (incl. uncut giant trunks).
        for(BlockPos log:logs)if(log.getY()==base.getY()+1){
            BlockPos foot=log.below();
            if(!foot.equals(base)&&l.hasChunkAt(foot)&&sameLog(l.getBlockState(foot),source))return List.of();
        }
        String namespace=CUSTOM.contains(species)?"worldlogicterrain":"minecraft";
        var leafId=ResourceLocation.fromNamespaceAndPath(namespace,species+"_leaves");
        Set<BlockPos> leaves=new LinkedHashSet<>();
        for(BlockPos log:logs){
            if(log.equals(base))continue;
            for(int dy=-3;dy<=3;dy++)for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
                if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)>4)continue;
                BlockPos n=log.offset(dx,dy,dz);
                if(n.getY()<=base.getY()||leaves.contains(n)||!l.hasChunkAt(n))continue;
                BlockState b=l.getBlockState(n);
                if(naturalLeaf(b)&&BuiltInRegistries.BLOCK.getKey(b.getBlock()).equals(leafId)){
                    // Avoid collecting foliage immediately attached to an unrelated trunk.
                    boolean foreign=false;
                    for(Direction d:Direction.values()){
                        BlockPos near=n.relative(d);
                        if(l.hasChunkAt(near)&&l.getBlockState(near).is(BlockTags.LOGS)&&!logs.contains(near)){foreign=true;break;}
                    }
                    if(!foreign){if(leaves.size()>=384)return List.of();leaves.add(n);}
                }
            }
        }
        if(leaves.isEmpty())return List.of();
        List<Piece> out=new ArrayList<>(logs.size()+leaves.size());
        for(BlockPos n:logs)if(!n.equals(base))out.add(new Piece(n,l.getBlockState(n),true));
        for(BlockPos n:leaves)out.add(new Piece(n,l.getBlockState(n),false));
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
