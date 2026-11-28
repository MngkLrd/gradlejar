package dev.minemods.treechopper;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
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
    private static final Map<String,DeferredBlock<Block>> UPPERS=new LinkedHashMap<>();
    private static final String COOLDOWN="wl_treechopper:last_hit", FALL="wl_treechopper_fall";
    private static final int FALL_TICKS=36;
    static {
        for(String name:SPECIES){
            CUTS.put(name,BLOCKS.register("cut_"+name,()->new CutBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion())));
            STUMPS.put(name,BLOCKS.register("stump_"+name,()->new StumpBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion())));
            UPPERS.put(name,BLOCKS.register("fallen_upper_"+name,()->new Block(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion().noCollission())));
        }
    }
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,ID);
    static final Supplier<BlockEntityType<CutProgress>> PROGRESS=BLOCK_ENTITIES.register("cut_progress",()->BlockEntityType.Builder.of(CutProgress::new,CUTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    public TreeChopper(IEventBus modBus){
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ChopNetwork::register);
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
    static boolean choppingTarget(Player player,BlockPos pos){
        if(player.isCreative()||player.isSpectator()||!(player.getMainHandItem().getItem() instanceof AxeItem)||!player.level().hasChunkAt(pos))return false;
        BlockState state=player.level().getBlockState(pos);
        String s=species(state);
        return s!=null&&(state.getBlock() instanceof CutBlock||!scan(player.level(),pos,s).isEmpty());
    }
    static boolean aimsAt(Player player,BlockPos pos,HitResult actual){
        if(actual==null)return false;
        Vec3 eye=player.getEyePosition();
        Vec3 end=eye.add(player.getViewVector(1f).scale(player.blockInteractionRange()));
        var entry=new AABB(pos).clip(eye,end);
        if(entry.isEmpty())return false;
        if(actual instanceof BlockHitResult block&&actual.getType()==HitResult.Type.BLOCK&&block.getBlockPos().equals(pos))return true;
        return actual.getLocation().distanceToSqr(eye)+.0001>=entry.get().distanceToSqr(eye);
    }
    static void networkStrike(ServerPlayer player,BlockPos pos){
        if(!player.isAlive()||player.isUsingItem()||!player.mayBuild()||!player.level().hasChunkAt(pos))return;
        HitResult actual=player.pick(player.blockInteractionRange(),1f,false);
        if(!aimsAt(player,pos,actual)||!choppingTarget(player,pos))return;
        double dx=player.getX()-pos.getX()-.5,dz=player.getZ()-pos.getZ()-.5;
        Direction side=Math.abs(dx)>Math.abs(dz)?(dx>0?Direction.EAST:Direction.WEST):(dz>0?Direction.SOUTH:Direction.NORTH);
        if(actual instanceof BlockHitResult block&&actual.getType()==HitResult.Type.BLOCK&&block.getBlockPos().equals(pos))side=block.getDirection();
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(player,pos,side,PlayerInteractEvent.LeftClickBlock.Action.START));
    }
    private static boolean allowed(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state){
        if(!level.mayInteract(player,pos))return false;
        PROTECTION_CHECK.set(true);
        try{return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled();}
        finally{PROTECTION_CHECK.remove();}
    }
    private static void guardBreak(BlockEvent.BreakEvent event){
        if(PROTECTION_CHECK.get() || !(event.getPlayer().getMainHandItem().getItem() instanceof AxeItem) || event.getPlayer().isCreative() || event.getPlayer().isSpectator() || !(event.getLevel() instanceof ServerLevel level))return;
        BlockState state=event.getState();
        String s=species(state);
        if(s!=null && (state.getBlock() instanceof CutBlock || !scan(level,event.getPos(),s).isEmpty()))event.setCanceled(true);
    }
    private static int upperLogs(List<Piece> tree){return (int)tree.stream().filter(Piece::log).count();}
    private static boolean canPay(ServerPlayer player,ItemStack axe,int cost){
        if(!axe.isDamageableItem()||axe.getMaxDamage()-axe.getDamageValue()>=cost)return true;
        player.displayClientMessage(Component.translatable("message.wl_treechopper.axe_worn",cost),true);
        return false;
    }
    private static void hit(PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isCreative() || player.isSpectator())return;
        BlockPos pos=event.getPos();
        BlockState old=level.getBlockState(pos);
        String s=species(old);
        if(s==null)return;
        boolean cut=old.getBlock() instanceof CutBlock;
        ItemStack axe=player.getMainHandItem();
        if(!(axe.getItem() instanceof AxeItem))return;
        List<Piece> tree=scan(level,pos,s);
        if(tree.isEmpty()&&!cut)return;
        event.setCanceled(true);
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
        // A previously fully severed hinge may have been held by a protection hook.
        // Retry felling without claiming a new cut or playing a phantom strike.
        int[] work=CutProgress.read(level,pos,damaged);
        if(cut&&ChopEffort.severed(work)){
            int wear=ChopEffort.fellingWear(upperLogs(tree),false);
            if(canPay(player,axe,wear)&&fall(level,player,pos,s,old,tree,side))axe.hurtAndBreak(wear,player,EquipmentSlot.MAINHAND);
            return;
        }
        float speed=Math.max(axe.getDestroySpeed(original(s)),((AxeItem)axe.getItem()).getTier().getSpeed());
        float resistance=s.equals("dark_oak")||s.equals("mangrove")?1.18f:s.equals("birch")?0.86f:1f;
        int idx=CutProgress.index(side);
        work[idx]=Math.min(ChopEffort.COMPLETE,work[idx]+ChopEffort.workPerHit(speed,resistance,power,1+upperLogs(tree),level.random.nextDouble()));
        damaged=damaged.setValue(CutBlock.side(side),ChopEffort.depth(work[idx]));
        boolean severed=ChopEffort.severed(work);
        int wear=severed?ChopEffort.fellingWear(upperLogs(tree),true):1;
        if(!canPay(player,axe,wear))return;
        if(!old.equals(damaged)&&!level.setBlock(pos,damaged,Block.UPDATE_ALL))return;
        if(!(level.getBlockEntity(pos) instanceof CutProgress progress))return;
        progress.write(work);
        ((ChopSwing)player).wl$markSwing();
        if(!(player instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new ChopNetwork.Swing(player.getId()));
        player.swing(InteractionHand.MAIN_HAND,true);
        if(!severed&&!(player instanceof FakePlayer))player.displayClientMessage(Component.translatable("message.wl_treechopper.cut_progress",work[idx]/10),true);
        level.playSound(null,pos,SoundEvents.WOOD_HIT,SoundSource.BLOCKS,1f,.8f+level.random.nextFloat()*.25f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,original(s)),pos.getX()+.5+side.getStepX()*.47,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()*.47,8,.12,.12,.12,.08);
        boolean felled=severed&&fall(level,player,pos,s,damaged,tree,side);
        // Every real strike costs one point; every additional harvested log costs one
        // more, only after successful felling. Vanilla damage processing handles enchantments.
        axe.hurtAndBreak(felled?wear:1,player,EquipmentSlot.MAINHAND);
    }
    private record Piece(BlockPos pos,BlockState state,boolean log){}
    private static boolean naturalLeaf(BlockState b){
        return b.is(BlockTags.LEAVES)&&b.hasProperty(BlockStateProperties.PERSISTENT)&&!b.getValue(BlockStateProperties.PERSISTENT);
    }
    private static boolean sameLog(BlockState b,BlockState source){return !source.isAir()&&b.is(source.getBlock());}
    /** Bounded connected crown search; no forced chunk loads. Only logs above the chosen cut are removed. */
    private static List<Piece> scan(Level l,BlockPos base,String species){
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
    private static boolean fall(ServerLevel l,ServerPlayer player,BlockPos base,String species,BlockState cut,List<Piece> tree,Direction latest){
        if(tree.stream().anyMatch(p->!l.getBlockState(p.pos).equals(p.state)||!allowed(l,player,p.pos,p.state)))return false;
        Direction direction=fallDirection(cut,latest);
        List<Piece> pieces=new ArrayList<>(tree);
        // Preserve the complete upper 12px of the cut block. The lower 4px stays in the world.
        // This display has no item yield; the actual stump accounts for the original log.
        pieces.add(0,new Piece(base,UPPERS.get(species).get().defaultBlockState(),false));
        List<Display.BlockDisplay> displays=new ArrayList<>();
        // All pieces share a common pivot, so rotation preserves the tree silhouette.
        for(Piece piece:pieces){
            boolean cap=piece.pos.equals(base);
            Display.BlockDisplay d=new Display.BlockDisplay(EntityType.BLOCK_DISPLAY,l);
            CompoundTag tag=new CompoundTag();
            tag.put("block_state",NbtUtils.writeBlockState(piece.state));
            tag.putFloat("view_range",2f);
            tag.putInt("interpolation_duration",2);
            tag.putInt("start_interpolation",0);
            tag.put("transformation",fallTransform(piece.pos.getX()-base.getX(),piece.pos.getY()-base.getY(),piece.pos.getZ()-base.getZ(),direction,0,cap,true,true));
            d.load(tag);
            d.setPos(base.getX()+.5+.5*direction.getStepX(),base.getY()+.25,base.getZ()+.5+.5*direction.getStepZ());
            d.setNoGravity(true);
            d.addTag(FALL);
            if(cap)d.addTag("wl_treechopper_cut_cap");
            boolean leaf=!cap&&!piece.log;
            if(leaf)d.addTag("wl_treechopper_leaf");
            CompoundTag fall=new CompoundTag();
            fall.putInt("x",piece.pos.getX()-base.getX());fall.putInt("y",piece.pos.getY()-base.getY());fall.putInt("z",piece.pos.getZ()-base.getZ());
            fall.putBoolean("leaf",leaf);
            fall.putBoolean("hinge",true);fall.putBoolean("cap",cap);fall.putBoolean("wholeUpper",true);
            fall.putString("direction",direction.getName());fall.putBoolean("log",piece.log);fall.putString("species",species);fall.putInt("age",0);
            d.getPersistentData().put(FALL,fall);
            if(!l.addFreshEntity(d)){displays.forEach(Entity::discard);return false;}
            displays.add(d);
        }
        if(!l.setBlock(base,STUMPS.get(species).get().defaultBlockState(),Block.UPDATE_ALL)){displays.forEach(Entity::discard);return false;}
        // Remove all tree blocks before notifying neighbors to avoid leaf decay/drop duplication.
        for(Piece p:tree)l.setBlock(p.pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        for(Piece p:tree)l.updateNeighborsAt(p.pos,Blocks.AIR);
        l.playSound(null,base,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.62f);
        return true;
    }
    private static ListTag floats(float... v){
        ListTag list=new ListTag();for(float x:v)list.add(FloatTag.valueOf(x));return list;
    }
    private static Quaternionf rotation(Direction dir,float angle){
        return new Quaternionf().rotationAxis(angle,dir.getStepZ(),0,-dir.getStepX());
    }
    private static float settle(float progress){
        float t=Math.max(0,Math.min(1,(progress-.75f)*4));
        return t*t*(3-2*t);
    }
    /** Transform relative to the hinge. Old in-flight displays retain their original pivot. */
    private static Vector3f fallOffset(float x,float y,float z,Direction dir,float progress,boolean cap,boolean hinge,boolean wholeUpper){
        float angle=(float)(Math.PI*.5*progress*progress);
        float edge=hinge?.5f:0;
        Vector3f off=rotation(dir,angle).transform(new Vector3f(x-.5f-edge*dir.getStepX(),y-.25f+(cap?(wholeUpper?.25f:.75f):0),z-.5f-edge*dir.getStepZ()));
        if(hinge){
            // During the last quarter, lower the trunk onto its local ground plane
            // and close the gap left by the missing central notch.
            float t=settle(progress);
            float close=wholeUpper?0:-.5f*t;
            off.add(close*dir.getStepX(),-.25f*t,close*dir.getStepZ());
        }
        return off;
    }
    private static CompoundTag fallTransform(float x,float y,float z,Direction dir,float progress,boolean cap,boolean hinge,boolean wholeUpper){
        float angle=(float)(Math.PI*.5*progress*progress);
        Quaternionf q=rotation(dir,angle);Vector3f off=fallOffset(x,y,z,dir,progress,cap,hinge,wholeUpper);
        CompoundTag t=new CompoundTag();
        t.put("translation",floats(off.x,off.y,off.z));t.put("left_rotation",floats(q.x,q.y,q.z,q.w));
        t.put("right_rotation",floats(0,0,0,1));t.put("scale",floats(1,1,1));return t;
    }
    static boolean qaLandingPose(){
        // Check initial cap height, shared contact plane and cap-to-log continuity
        // for the exact transform used by the real displays, in all four directions.
        for(Direction dir:SIDES){
            Vector3f start=fallOffset(0,0,0,dir,0,true,true,true);
            if(Math.abs(start.y+.25f-.25f)>.0001f)return false;
            Vector3f cap=fallOffset(0,0,0,dir,1,true,true,true);
            Vector3f log=fallOffset(0,1,0,dir,1,false,true,true);
            Vector3f span=rotation(dir,(float)(Math.PI*.5)).transform(new Vector3f(0,.75f,0));
            if(cap.add(span).distance(log)>.0001f)return false;
            // Each full log's eight transformed corners: bottom exactly at base Y.
            float low=Float.POSITIVE_INFINITY;
            for(int x=0;x<=1;x++)for(int y=0;y<=1;y++)for(int z=0;z<=1;z++){
                float h=.25f+log.y+rotation(dir,(float)(Math.PI*.5)).transform(new Vector3f(x,y,z)).y;
                low=Math.min(low,h);
            }
            if(Math.abs(low)>.0001f)return false;
        }
        return true;
    }
    private static void tickDisplay(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof Display.BlockDisplay d)||!(d.level() instanceof ServerLevel l)||!d.getPersistentData().contains(FALL))return;
        CompoundTag f=d.getPersistentData().getCompound(FALL);
        int age=f.getInt("age")+1;f.putInt("age",age);
        Direction dir=Direction.byName(f.getString("direction"));
        if(dir==null){d.discard();return;}
        float x=f.getInt("x"),y=f.getInt("y"),z=f.getInt("z");
        boolean hinge=f.getBoolean("hinge"),cap=f.getBoolean("cap"),wholeUpper=f.getBoolean("wholeUpper");
        float progress=Math.min(1,age/(float)FALL_TICKS);
        boolean leaf=f.getBoolean("leaf");
        float leafDrop=leaf?Math.min(8f,.055f*age*age):0;
        if(leaf&&age>=12){
            Vector3f off=fallOffset(x,y,z,dir,progress,false,hinge,wholeUpper);
            String name=f.getString("species");
            BlockState foliage=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CUSTOM.contains(name)?"worldlogicterrain":"minecraft",name+"_leaves")).defaultBlockState();
            if(!foliage.isAir())l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,foliage),d.getX()+off.x+.5,d.getY()+off.y+.5-leafDrop,d.getZ()+off.z+.5,4,.22,.35,.22,.06);
            d.discard();return;
        }
        if(age>=FALL_TICKS+3){
            if(f.getBoolean("log")&&CUTS.containsKey(f.getString("species"))){
                Vector3f off=fallOffset(x,y,z,dir,1,false,hinge,wholeUpper).add(rotation(dir,(float)(Math.PI*.5)).transform(new Vector3f(.5f,.5f,.5f)));
                BlockPos end=BlockPos.containing(d.getX()+off.x,d.getY()+off.y+.4,d.getZ()+off.z);
                if(!l.hasChunkAt(end)||!l.getBlockState(end).getCollisionShape(l,end).isEmpty())end=d.blockPosition().above();
                Block.popResource(l,end,new ItemStack(original(f.getString("species")).getBlock()));
            }
            d.discard();return;
        }
        if(age%2!=0)return;
        CompoundTag tag=d.saveWithoutId(new CompoundTag());
        tag.putInt("interpolation_duration",2);tag.putInt("start_interpolation",0);
        CompoundTag pose=fallTransform(x,y,z,dir,progress,cap,hinge,wholeUpper);
        if(leaf){Vector3f off=fallOffset(x,y,z,dir,progress,false,hinge,wholeUpper);pose.put("translation",floats(off.x,off.y-leafDrop,off.z));}
        tag.put("transformation",pose);
        d.load(tag);
    }
    static void qaResetCooldown(ServerPlayer player){player.getPersistentData().remove(COOLDOWN);}
}
