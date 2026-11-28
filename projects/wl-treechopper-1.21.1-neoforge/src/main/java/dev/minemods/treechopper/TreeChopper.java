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
    private static final Map<String,DeferredBlock<FallenLogBlock>> FALLEN=new LinkedHashMap<>();
    private static final Map<String,DeferredBlock<TwigBlock>> TWIGS=new LinkedHashMap<>();
    private static final String COOLDOWN="wl_treechopper:last_hit";
    static final String FALL="wl_treechopper_fall";
    private static final int FALL_TICKS=36;
    static {
        for(String name:SPECIES){
            TWIGS.put(name,BLOCKS.register("twig_"+name,()->new TwigBlock(BlockBehaviour.Properties.of().strength(0).sound(SoundType.WOOD).noCollission().noOcclusion(),name)));
            CUTS.put(name,BLOCKS.register("cut_"+name,()->new CutBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion())));
            STUMPS.put(name,BLOCKS.register("stump_"+name,()->new StumpBlock(BlockBehaviour.Properties.of().strength(1.5f).sound(SoundType.WOOD).noOcclusion())));
            UPPERS.put(name,BLOCKS.register("fallen_upper_"+name,()->new Block(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion().noCollission())));
            FALLEN.put(name,BLOCKS.register("fallen_"+name,()->new FallenLogBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion(),name)));
        }
    }
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE,ID);
    static final Supplier<EntityType<TimberCollider>> COLLIDER=ENTITIES.register("timber_collider",()->EntityType.Builder.<TimberCollider>of(TimberCollider::new,MobCategory.MISC).sized(1,1).noSave().noSummon().clientTrackingRange(10).updateInterval(1).build(ID+":timber_collider"));
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,ID);
    static final Supplier<BlockEntityType<CutProgress>> PROGRESS=BLOCK_ENTITIES.register("cut_progress",()->BlockEntityType.Builder.of(CutProgress::new,CUTS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    static final Supplier<BlockEntityType<FallenProgress>> FALLEN_PROGRESS=BLOCK_ENTITIES.register("fallen_progress",()->BlockEntityType.Builder.of(FallenProgress::new,FALLEN.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    static BlockState fallenState(String species,Direction.Axis axis,boolean marker){
        return FALLEN.get(species).get().defaultBlockState().setValue(BlockStateProperties.AXIS,axis).setValue(FallenLogBlock.MARKER,marker);
    }
    public TreeChopper(IEventBus modBus){
        BLOCKS.register(modBus);
        ENTITIES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ChopNetwork::register);
        NeoForge.EVENT_BUS.addListener(TreeChopper::hit);
        NeoForge.EVENT_BUS.addListener(TreeChopper::guardBreak);
        NeoForge.EVENT_BUS.addListener(TreeChopper::tickDisplay);
        NeoForge.EVENT_BUS.addListener(FallingTree::attack);
        NeoForge.EVENT_BUS.addListener(TreeQA::register);
        NeoForge.EVENT_BUS.addListener(ForestSticks::tick);
    }
    static BlockState twigState(BlockState leaf,Direction facing){
        var id=BuiltInRegistries.BLOCK.getKey(leaf.getBlock());
        String name=id.getPath().replaceFirst("_leaves$","");
        var twig=TWIGS.get(name);
        if(twig==null||!id.getNamespace().equals(CUSTOM.contains(name)?"worldlogicterrain":"minecraft"))return null;
        return twig.get().defaultBlockState().setValue(TwigBlock.FACING,facing);
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
        if(state.getBlock() instanceof FallenLogBlock)return true;
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
    static boolean allowed(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state){
        if(!level.mayInteract(player,pos))return false;
        PROTECTION_CHECK.set(true);
        try{return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled();}
        finally{PROTECTION_CHECK.remove();}
    }
    private static void guardBreak(BlockEvent.BreakEvent event){
        if(PROTECTION_CHECK.get() || event.getPlayer().isCreative() || event.getPlayer().isSpectator() || !(event.getLevel() instanceof ServerLevel level))return;
        BlockState state=event.getState();
        if(state.getBlock() instanceof FallenLogBlock){event.setCanceled(true);return;}
        if(!(event.getPlayer().getMainHandItem().getItem() instanceof AxeItem)){
            if(requiresAxe(state))event.setCanceled(true);
            return;
        }
        String s=species(state);
        if(s!=null && (state.getBlock() instanceof CutBlock || !scan(level,event.getPos(),s).isEmpty()))event.setCanceled(true);
    }
    static boolean requiresAxe(BlockState state){
        return state.is(BlockTags.LOGS)||species(state)!=null||state.getBlock() instanceof StumpBlock||state.getBlock() instanceof FallenLogBlock;
    }
    private static int upperLogs(List<Piece> tree){return (int)tree.stream().filter(Piece::log).count();}
    private static boolean ground(BlockState state){
        return state.is(BlockTags.DIRT)||state.is(Blocks.MUD)||state.is(Blocks.CLAY)||state.is(BlockTags.SAND);
    }
    private static boolean baseLog(BlockState b,String species,BlockState source){
        return sameLog(b,source)||b.is(CUTS.get(species).get());
    }
    /** Contiguous, square, soil-supported 2x2/3x3 footprints. Never merge scattered roots. */
    private static List<BlockPos> roots(Level l,BlockPos base,String species){
        BlockState source=original(species);
        if(!l.hasChunkAt(base.below())||!ground(l.getBlockState(base.below())))return List.of(base);
        Set<BlockPos> found=new LinkedHashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        found.add(base);queue.add(base);
        while(!queue.isEmpty()){
            BlockPos p=queue.remove();
            for(Direction d:SIDES){
                BlockPos n=p.relative(d);
                if(found.contains(n)||!l.hasChunkAt(n)||!l.hasChunkAt(n.below())||!l.hasChunkAt(n.above()))continue;
                if(!ground(l.getBlockState(n.below()))||!baseLog(l.getBlockState(n),species,source)||!sameLog(l.getBlockState(n.above()),source))continue;
                if(found.size()>=9)return List.of(base);
                found.add(n);queue.add(n);
            }
        }
        int minX=found.stream().mapToInt(BlockPos::getX).min().orElse(base.getX()),maxX=found.stream().mapToInt(BlockPos::getX).max().orElse(base.getX());
        int minZ=found.stream().mapToInt(BlockPos::getZ).min().orElse(base.getZ()),maxZ=found.stream().mapToInt(BlockPos::getZ).max().orElse(base.getZ());
        int width=maxX-minX+1;
        if(width<2||width>3||width!=maxZ-minZ+1||found.size()!=width*width)return List.of(base);
        if(!sameLog(l.getBlockState(base.above()),source))return List.of(base);
        return List.copyOf(found);
    }
    private static List<BlockPos> exposedSupports(List<BlockPos> roots){
        if(roots.size()!=9)return roots;
        Set<BlockPos> cells=new HashSet<>(roots);
        return roots.stream().filter(p->Arrays.stream(SIDES).anyMatch(d->!cells.contains(p.relative(d)))).toList();
    }
    private static int readySupports(Level l,List<BlockPos> supports,BlockPos changed,int[] work){
        int n=0;
        for(BlockPos p:supports){
            BlockState b=l.getBlockState(p);
            if(p.equals(changed)?ChopEffort.severed(work):b.getBlock() instanceof CutBlock&&ChopEffort.severed(CutProgress.read(l,p,b)))n++;
        }
        return n;
    }
    private static boolean canPay(ServerPlayer player,ItemStack axe,int cost){
        if(!axe.isDamageableItem()||axe.getMaxDamage()-axe.getDamageValue()>=cost)return true;
        player.displayClientMessage(Component.translatable("message.wl_treechopper.axe_worn",cost),true);
        return false;
    }
    private static void hit(PlayerInteractEvent.LeftClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || player.isCreative() || player.isSpectator())return;
        BlockPos pos=event.getPos();
        BlockState old=level.getBlockState(pos);
        if(old.getBlock() instanceof FallenLogBlock){
            event.setCanceled(true);
            if(event.getAction()==PlayerInteractEvent.LeftClickBlock.Action.START&&player.mayBuild()&&player.getMainHandItem().getItem() instanceof AxeItem){
                double power=WorldLogicIntegration.effort(player).power();
                long now=level.getGameTime(); CompoundTag data=player.getPersistentData();
                if(power>0&&(!data.contains(COOLDOWN)||now<data.getLong(COOLDOWN)||now-data.getLong(COOLDOWN)>=ChopEffort.cooldown(power))){
                    if(FallenTimber.process(level,player,pos)){
                        data.putLong(COOLDOWN,now);
                        ((ChopSwing)player).wl$markSwing();
                        if(!(player instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new ChopNetwork.Swing(player.getId()));
                        player.swing(InteractionHand.MAIN_HAND,true);
                    }
                }
            }
            return;
        }
        ItemStack axe=player.getMainHandItem();
        if(!(axe.getItem() instanceof AxeItem)){
            if(requiresAxe(old))event.setCanceled(true);
            return;
        }
        String s=species(old);
        if(s==null)return;
        boolean cut=old.getBlock() instanceof CutBlock;
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
        List<BlockPos> roots=roots(level,pos,s), supports=exposedSupports(roots);
        if(!supports.contains(pos))return;
        int extraCore=roots.size()-supports.size();
        if(cut&&ChopEffort.severed(work)){
            if(readySupports(level,supports,pos,work)<supports.size()){
                player.displayClientMessage(Component.translatable("message.wl_treechopper.supports",readySupports(level,supports,pos,work),supports.size()),true);return;
            }
            int wear=ChopEffort.fellingWear(upperLogs(tree)+extraCore,false);
            if(canPay(player,axe,wear)&&fall(level,player,pos,s,old,tree,side,true))axe.hurtAndBreak(wear,player,EquipmentSlot.MAINHAND);
            return;
        }
        float speed=Math.max(axe.getDestroySpeed(original(s)),((AxeItem)axe.getItem()).getTier().getSpeed());
        MaterialPhysics.Data wood=MaterialPhysics.read(level,pos,original(s),true,CutProgress.moisture(level,pos));
        double resistance=MaterialPhysics.resistance(wood.hardnessN(),MaterialPhysics.oakHardnessN());
        MassData mass=measure(level,roots,tree,s,pos,wood);
        double equivalent=MaterialPhysics.equivalentLogs(mass.complete?mass.fullMassKg:Double.NaN,MaterialPhysics.oakMassKg(),upperLogs(tree)+roots.size())/supports.size();
        int idx=CutProgress.index(side);
        int guide=FallBalance.guideForStrike(work,idx,CutProgress.guide(level,pos));
        int proposed=Math.min(ChopEffort.COMPLETE,work[idx]+ChopEffort.workPerHit(speed,resistance,power,Math.max(4,equivalent),level.random.nextDouble(),tree.stream().filter(Piece::log).mapToInt(p->p.pos.getY()-pos.getY()).max().orElse(1)));
        work[idx]=FallBalance.counterWork(work,idx,guide,proposed);
        if(!FallBalance.validGuide(work,guide))guide=-1;
        damaged=CutBlock.withWork(damaged,work);
        boolean severed=ChopEffort.severed(work);
        boolean allReady=severed&&readySupports(level,supports,pos,work)==supports.size();
        int wear=allReady?ChopEffort.fellingWear(upperLogs(tree)+extraCore,true):1;
        if(!canPay(player,axe,wear))return;
        if(!old.equals(damaged)&&!level.setBlock(pos,damaged,Block.UPDATE_ALL))return;
        if(!(level.getBlockEntity(pos) instanceof CutProgress progress))return;
        progress.write(work,guide);
        progress.rememberMoisture(wood.moisturePercent());
        ((ChopSwing)player).wl$markSwing();
        if(!(player instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new ChopNetwork.Swing(player.getId()));
        player.swing(InteractionHand.MAIN_HAND,true);
        if(!(player instanceof FakePlayer)){
            if(severed&&!allReady)player.displayClientMessage(Component.translatable("message.wl_treechopper.supports",readySupports(level,supports,pos,work),supports.size()),true);

        }
        level.playSound(null,pos,SoundEvents.WOOD_HIT,SoundSource.BLOCKS,1f,.8f+level.random.nextFloat()*.25f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,original(s)),pos.getX()+.5+side.getStepX()*.47,pos.getY()+.5,pos.getZ()+.5+side.getStepZ()*.47,8,.12,.12,.12,.08);
        boolean felled=allReady&&fall(level,player,pos,s,damaged,tree,side,false);
        // Every real strike costs one point; every additional harvested log costs one
        // more, only after successful felling. Vanilla damage processing handles enchantments.
        axe.hurtAndBreak(felled?wear:1,player,EquipmentSlot.MAINHAND);
    }
    private record Piece(BlockPos pos,BlockState state,boolean log){}
    static boolean naturalLeaf(BlockState b){
        return b.is(BlockTags.LEAVES)&&b.hasProperty(BlockStateProperties.PERSISTENT)&&!b.getValue(BlockStateProperties.PERSISTENT);
    }
    private static boolean sameLog(BlockState b,BlockState source){return !source.isAir()&&b.is(source.getBlock());}
    /** Bounded connected crown search; no forced chunk loads. Only logs above the chosen cut are removed. */
    private static List<Piece> scan(Level l,BlockPos base,String species){
        BlockState source=original(species);
        if(source.isAir()||!l.hasChunkAt(base.below()))return List.of();
        BlockState below=l.getBlockState(base.below());
        boolean previousCut=below.getBlock() instanceof StumpBlock||below.getBlock() instanceof CutBlock||l.getBlockState(base).getBlock() instanceof CutBlock;
        if(!(previousCut||below.isAir()||below.is(BlockTags.DIRT)||below.is(BlockTags.LOGS)||below.is(Blocks.MUD)||below.is(Blocks.CLAY)||below.is(BlockTags.SAND)))return List.of();
        Set<BlockPos> logs=new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        List<BlockPos> roots=roots(l,base,species);
        Set<BlockPos> rootSet=new HashSet<>(roots);
        queue.addAll(roots);logs.addAll(roots);
        while(!queue.isEmpty()){
            BlockPos p=queue.removeFirst();
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dy==0&&dz==0)continue;
                // Diagonal upward branches are normal; do not jump sideways between separate same-level trunks.
                if(dy==0&&Math.abs(dx)+Math.abs(dz)>1)continue;
                BlockPos n=p.offset(dx,dy,dz);
                if(n.equals(base)||logs.contains(n)||n.getY()<=base.getY())continue;
                if(n.getY()-base.getY()>64||Math.abs(n.getX()-base.getX())>16||Math.abs(n.getZ()-base.getZ())>16){
                    if(l.hasChunkAt(n)&&sameLog(l.getBlockState(n),source))return List.of(); // never cut a truncated crown
                    continue;
                }
                if(!l.hasChunkAt(n))continue;
                if(sameLog(l.getBlockState(n),source)){
                    if(logs.size()>=256)return List.of();
                    logs.add(n);queue.addLast(n);
                }
            }
        }
        if(logs.size()<=roots.size())return List.of();
        // Reject independently supported neighboring columns (incl. uncut giant trunks).
        for(BlockPos log:logs)if(log.getY()==base.getY()+1){
            BlockPos foot=log.below();
            if(!rootSet.contains(foot)&&l.hasChunkAt(foot)&&baseLog(l.getBlockState(foot),species,source))return List.of();
        }
        var crown=CrownOwnership.scan(l,logs);
        if(!crown.complete()||(!crown.natural()&&!previousCut))return List.of();
        Set<BlockPos> leaves=crown.owned();
        List<Piece> out=new ArrayList<>(logs.size()+leaves.size());
        for(BlockPos n:logs)if(!rootSet.contains(n))out.add(new Piece(n,l.getBlockState(n),true));
        for(BlockPos n:leaves)out.add(new Piece(n,l.getBlockState(n),false));
        return out;
    }

    private record MassData(Map<BlockPos,MaterialPhysics.Data> values,boolean complete,double fullMassKg){}
    private static MassData measure(ServerLevel l,List<BlockPos> roots,List<Piece> tree,String species,BlockPos cachedPos,MaterialPhysics.Data cached){
        Map<BlockPos,MaterialPhysics.Data> values=new HashMap<>();
        boolean complete=true;double total=0;
        for(BlockPos p:roots){
            MaterialPhysics.Data d=p.equals(cachedPos)&&cached!=null?cached:MaterialPhysics.read(l,p,original(species),true,CutProgress.moisture(l,p));
            values.put(p,d);if(d.known())total+=d.massKg();else complete=false;
        }
        for(Piece p:tree){
            MaterialPhysics.Data d=MaterialPhysics.read(l,p.pos,p.state,p.log,Double.NaN);
            values.put(p.pos,d);if(d.known())total+=d.massKg();else complete=false;
        }
        return new MassData(values,complete,complete?total:Double.NaN);
    }
    private record Balance(Direction direction,double centerX,double centerZ,double leanX,double leanZ,int guides,double fallingMassKg){}
    private static Balance balance(ServerLevel l,List<BlockPos> roots,List<Piece> tree,String species,Direction latest){
        MassData mass=measure(l,roots,tree,species,null,null);
        double rootX=roots.stream().mapToDouble(p->p.getX()+.5).average().orElse(0);
        double rootZ=roots.stream().mapToDouble(p->p.getZ()+.5).average().orElse(0);
        int rootY=roots.get(0).getY();
        double total=0,mx=0,mz=0,upperWeight=0,ux=0,uz=0;
        int maxY=tree.stream().filter(Piece::log).mapToInt(p->p.pos.getY()-rootY).max().orElse(1);
        for(Piece p:tree){
            Direction.Axis axis=p.state.hasProperty(BlockStateProperties.AXIS)?p.state.getValue(BlockStateProperties.AXIS):Direction.Axis.Y;
            double m=mass.complete?mass.values.get(p.pos).massKg():FallBalance.mass(p.log,axis),x=p.pos.getX()+.5-rootX,z=p.pos.getZ()+.5-rootZ;
            total+=m;mx+=m*x;mz+=m*z;
            if(p.log&&axis==Direction.Axis.Y&&p.pos.getY()-rootY>=Math.max(1,maxY*.5)){
                upperWeight+=m;ux+=m*x;uz+=m*z;
            }
        }
        // The severed upper portions of the bases are also part of the falling mass.
        for(BlockPos p:roots){
            // Existing 12/16 cap model is exactly 75% of the original full log volume.
            double m=.75*(mass.complete?mass.values.get(p).massKg():1d);
            total+=m;mx+=m*(p.getX()+.5-rootX);mz+=m*(p.getZ()+.5-rootZ);
        }
        double cx=total>0?mx/total:0,cz=total>0?mz/total:0;
        double lx=upperWeight>0?ux/upperWeight:0,lz=upperWeight>0?uz/upperWeight:0;
        double cutX=0,cutZ=0;int guided=0;
        List<BlockPos> supports=exposedSupports(roots);
        for(BlockPos p:supports){
            BlockState b=l.getBlockState(p);
            int[] work=CutProgress.read(l,p,b);int guide=CutProgress.guide(l,p);
            if(FallBalance.validGuide(work,guide))guided++;
            double[] v=FallBalance.cutVector(work,guide);cutX+=v[0];cutZ+=v[1];
        }
        double n=Math.max(1,supports.size());
        return new Balance(FallBalance.choose(cutX/n,cutZ/n,cx,cz,lx,lz,latest),cx,cz,lx,lz,guided,mass.complete?total:Double.NaN);
    }
    static Direction qaBalance(ServerLevel l,BlockPos base,String species,Direction fallback){
        List<Piece> tree=scan(l,base,species);
        return tree.isEmpty()?null:balance(l,roots(l,base,species),tree,species,fallback).direction;
    }
    private static boolean fall(ServerLevel l,ServerPlayer player,BlockPos hitBase,String species,BlockState cut,List<Piece> tree,Direction latest,boolean retry){
        if(tree.stream().anyMatch(p->!l.getBlockState(p.pos).equals(p.state)||!allowed(l,player,p.pos,p.state)))return false;
        List<BlockPos> roots=roots(l,hitBase,species);
        Set<BlockPos> rootSet=new HashSet<>(roots);
        if(roots.stream().anyMatch(p->!p.equals(hitBase)&&!allowed(l,player,p,l.getBlockState(p))))return false;
        Balance balance=balance(l,roots,tree,species,latest);
        Direction direction=balance.direction;
        BlockPos base=roots.stream().max(Comparator.comparingInt(p->p.getX()*direction.getStepX()+p.getZ()*direction.getStepZ())).orElse(hitBase);
        Set<BlockPos> source=new HashSet<>(roots);
        for(Piece p:tree)source.add(p.pos);
        ListTag plan=new ListTag();
        Set<BlockPos> occupied=new HashSet<>();boolean gridAllowed=true;
        for(Piece p:tree)if(p.log){
            BlockPos end=landingPosition(base,p.pos,direction,1f);
            if(rootSet.contains(end)||!occupied.add(end)||!l.hasChunkAt(end)||!l.mayInteract(player,end))gridAllowed=false;
            Direction.Axis originalAxis=p.state.getValue(BlockStateProperties.AXIS);
            Vector3f axis=rotation(direction,(float)(Math.PI*.5)).transform(new Vector3f(originalAxis==Direction.Axis.X?1:0,originalAxis==Direction.Axis.Y?1:0,originalAxis==Direction.Axis.Z?1:0));
            Direction.Axis endAxis=Math.abs(axis.x)>.5?Direction.Axis.X:Math.abs(axis.y)>.5?Direction.Axis.Y:Direction.Axis.Z;
            CompoundTag entry=new CompoundTag();entry.putLong("pos",end.asLong());entry.putLong("source",p.pos.asLong());entry.putString("axis",endAxis.getName());
            plan.add(entry);
        }
        plan.sort(Comparator.comparingInt(t->{
            BlockPos pos=BlockPos.of(((CompoundTag)t).getLong("pos"));
            return (pos.getX()-base.getX())*direction.getStepX()+(pos.getZ()-base.getZ())*direction.getStepZ();
        }));
        List<Piece> pieces=new ArrayList<>(tree);
        // Preserve the complete upper 12px of the cut block. The lower 4px stays in the world.
        // This display has no item yield; the actual stump accounts for the original log.
        for(BlockPos root:roots)pieces.add(0,new Piece(root,UPPERS.get(species).get().defaultBlockState(),false));
        List<Display.BlockDisplay> displays=new ArrayList<>();
        ListTag members=new ListTag();Display.BlockDisplay master=null;
        // All pieces share a common pivot, so rotation preserves the tree silhouette.
        int animatedLeaves=0;
        for(Piece piece:pieces){
            boolean cap=rootSet.contains(piece.pos);
            // Removal is authoritative for the whole owned crown; particles/displays are capped.
            if(!cap&&!piece.log&&animatedLeaves++>=384)continue;
            boolean coordinator=piece.pos.equals(base);
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
            fall.putBoolean("planned",true);
            fall.putBoolean("dynamic",!leaf);
            if(coordinator){fall.put("landing",plan.copy());fall.putUUID("group",UUID.randomUUID());fall.putLong("base",base.asLong());fall.putBoolean("gridAllowed",gridAllowed);fall.putUUID("owner",player.getUUID());fall.putLongArray("roots",roots.stream().mapToLong(BlockPos::asLong).toArray());fall.putDouble("centerX",balance.centerX);fall.putDouble("centerZ",balance.centerZ);fall.putDouble("leanX",balance.leanX);fall.putDouble("leanZ",balance.leanZ);fall.putInt("guides",balance.guides);if(Double.isFinite(balance.fallingMassKg))fall.putDouble("massKg",balance.fallingMassKg);}
            fall.putBoolean("coordinator",coordinator);
            fall.putBoolean("hinge",true);fall.putBoolean("cap",cap);fall.putBoolean("wholeUpper",true);
            fall.putString("direction",direction.getName());fall.putBoolean("log",piece.log);fall.putString("species",species);fall.putInt("age",0);
            d.getPersistentData().put(FALL,fall);
            if(!l.addFreshEntity(d)){displays.forEach(Entity::discard);return false;}
            displays.add(d);
            if(coordinator)master=d;
            if(!leaf){
                CompoundTag member=new CompoundTag();member.putUUID("entity",d.getUUID());member.putLong("source",piece.pos.asLong());member.putBoolean("log",piece.log);member.putBoolean("cap",cap);member.put("state",NbtUtils.writeBlockState(piece.state));members.add(member);
            }
        }
        if(master==null){displays.forEach(Entity::discard);return false;}
        master.getPersistentData().getCompound(FALL).put("members",members);
        for(BlockPos root:roots)l.setBlock(root,STUMPS.get(species).get().defaultBlockState(),Block.UPDATE_ALL);
        // Remove all tree blocks before notifying neighbors to avoid leaf decay/drop duplication.
        for(Piece p:tree)l.setBlock(p.pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        for(Piece p:tree)l.updateNeighborsAt(p.pos,Blocks.AIR);
        l.playSound(null,base,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.62f);
        return true;
    }
    /** World-space bounds of the transformed wood, shared by display and physical collider. */
    static AABB timberBounds(BlockPos base,BlockPos piece,Direction dir,float progress,boolean cap,double dy){
        Vector3f off=fallOffset(piece.getX()-base.getX(),piece.getY()-base.getY(),piece.getZ()-base.getZ(),dir,progress,cap,true,true);
        Quaternionf q=rotation(dir,(float)(Math.PI*.5*progress*progress));
        double ox=base.getX()+.5+.5*dir.getStepX()+off.x,oy=base.getY()+.25+off.y+dy,oz=base.getZ()+.5+.5*dir.getStepZ()+off.z;
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
        for(int x=0;x<=1;x++)for(int y=0;y<=1;y++)for(int z=0;z<=1;z++){
            Vector3f v=q.transform(new Vector3f(x,cap?y*.75f:y,z));
            minX=Math.min(minX,ox+v.x);maxX=Math.max(maxX,ox+v.x);
            minY=Math.min(minY,oy+v.y);maxY=Math.max(maxY,oy+v.y);
            minZ=Math.min(minZ,oz+v.z);maxZ=Math.max(maxZ,oz+v.z);
        }
        return new AABB(minX,minY,minZ,maxX,maxY,maxZ);
    }
    static boolean sweepClear(ServerLevel l,BlockPos base,BlockPos piece,Direction dir,float progress,Set<BlockPos> source){
        return sweepClear(l,base,piece,dir,progress,source,0);
    }
    /** Actual rotated cube vs world voxel shapes, not just its enlarged AABB.
     * Rotation is in the vertical cardinal plane, so world and cube face normals suffice for SAT. */
    static boolean sweepClear(ServerLevel l,BlockPos base,BlockPos piece,Direction dir,float progress,Set<BlockPos> source,double dy){
        AABB bounds=timberBounds(base,piece,dir,progress,false,dy);
        double e=.0001;AABB box=bounds.deflate(e);
        var center=bounds.getCenter();
        Quaternionf q=rotation(dir,(float)(Math.PI*.5*progress*progress));
        Vector3f[] axes={q.transform(new Vector3f(1,0,0)),q.transform(new Vector3f(0,1,0)),q.transform(new Vector3f(0,0,1))};
        for(int x=(int)Math.floor(box.minX);x<=Math.floor(box.maxX);x++)for(int y=(int)Math.floor(box.minY);y<=Math.floor(box.maxY);y++)for(int z=(int)Math.floor(box.minZ);z<=Math.floor(box.maxZ);z++){
            BlockPos p=new BlockPos(x,y,z);
            if(source.contains(p))continue;
            if(!l.hasChunkAt(p)||l.isOutsideBuildHeight(p))return false;
            BlockState state=l.getBlockState(p);
            if(state.isAir()||naturalLeaf(state))continue;
            for(AABB shape:state.getCollisionShape(l,p).toAabbs()){
                AABB world=shape.move(x,y,z);if(!box.intersects(world))continue;
                Vec3 delta=world.getCenter().subtract(center);boolean separated=false;
                for(Vector3f axis:axes){
                    double radius=.5*(Math.abs(axis.x)*world.getXsize()+Math.abs(axis.y)*world.getYsize()+Math.abs(axis.z)*world.getZsize());
                    if(Math.abs(delta.x*axis.x+delta.y*axis.y+delta.z*axis.z)>=.5+radius-e){separated=true;break;}
                }
                if(!separated)return false;
            }
        }
        return true;
    }
    private static BlockPos landingPosition(BlockPos base,BlockPos piece,Direction dir,float progress){
        Vector3f off=fallOffset(piece.getX()-base.getX(),piece.getY()-base.getY(),piece.getZ()-base.getZ(),dir,progress,false,true,true);
        off.add(rotation(dir,(float)(Math.PI*.5*progress*progress)).transform(new Vector3f(.5f,.5f,.5f)));
        return BlockPos.containing(base.getX()+.5+.5*dir.getStepX()+off.x,base.getY()+.25+off.y,base.getZ()+.5+.5*dir.getStepZ()+off.z);
    }
    private static ListTag floats(float... v){
        ListTag list=new ListTag();for(float x:v)list.add(FloatTag.valueOf(x));return list;
    }
    static Quaternionf rotation(Direction dir,float angle){
        return new Quaternionf().rotationAxis(angle,dir.getStepZ(),0,-dir.getStepX());
    }
    private static float settle(float progress){
        float t=Math.max(0,Math.min(1,(progress-.75f)*4));
        return t*t*(3-2*t);
    }
    /** Transform relative to the hinge. Old in-flight displays retain their original pivot. */
    static Vector3f fallOffset(float x,float y,float z,Direction dir,float progress,boolean cap,boolean hinge,boolean wholeUpper){
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
    static CompoundTag fallTransform(float x,float y,float z,Direction dir,float progress,boolean cap,boolean hinge,boolean wholeUpper){
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
        if(f.getBoolean("dynamic")){
            if(f.getBoolean("coordinator"))FallingTree.tick(l,d,f);
            return;
        }
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
            if(f.getBoolean("planned")){
                if((f.getBoolean("coordinator")||f.contains("landing"))&&!FallenTimber.land(l,d.blockPosition(),f))return;
                d.discard();return;
            }
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
