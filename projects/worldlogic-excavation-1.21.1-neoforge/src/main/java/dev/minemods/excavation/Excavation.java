package dev.minemods.excavation;
import java.util.*;
import java.util.function.Supplier;
import dev.minemods.worldlogic.api.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.*;

@Mod(Excavation.ID)
public final class Excavation {
    public static final String ID="worldlogicexcavation";
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,ID);
    private record Permit(ServerPlayer player,BlockPos pos){}
    private static final ThreadLocal<Permit> BREAK=new ThreadLocal<>(), CLICK=new ThreadLocal<>();
    private static final Map<ServerPlayer,Integer> LAST=new WeakHashMap<>();
    static final Supplier<BlockEntityType<ChipProgress>> PROGRESS=ENTITIES.register("fracture",()->BlockEntityType.Builder.of(ChipProgress::new,Catalog.CHIPS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
    static {
        for(var spec:Catalog.DATA.specs()){
            Catalog.CHIPS.put(spec.key(),BLOCKS.register("chipped_"+spec.key(),()->new ChipBlock(
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(spec.ore()?3f:1.5f,6f).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().dynamicShape(),spec)));
        }
        for(var family:Catalog.DATA.families()){
            Map<Integer,DeferredItem<Item>> items=new LinkedHashMap<>();
            for(int units:new int[]{1,4,9})items.put(units,ITEMS.register(Catalog.size(units)+"_"+family.id()+"_fragment",()->new Item(new Item.Properties())));
            Catalog.FRAGMENTS.put(family.id(),items);
        }
    }
    static final DeferredItem<Item> COPPER_NUGGET=ITEMS.register("copper_nugget",()->new Item(new Item.Properties()));
    public Excavation(IEventBus modBus){
        BLOCKS.register(modBus);ITEMS.register(modBus);ENTITIES.register(modBus);
        modBus.addListener(StrikeNetwork::register);modBus.addListener(Excavation::setup);modBus.addListener(Excavation::creative);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,Excavation::guardBreak);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,Excavation::guardClick);
        NeoForge.EVENT_BUS.addListener(ExcavationQA::register);
    }
    private static void setup(FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            ExcavationApi.register();
            for(var f:Catalog.DATA.families()){
                // Ancient debris is not refined scrap; do not copy refined physical properties.
                if(f.id().equals("ancient_debris"))continue;
                String reference=f.texture().startsWith("worldlogic:item/")?"worldlogic:"+f.id():
                    f.id().equals("iron")?"minecraft:raw_iron":f.id().equals("gold")?"minecraft:raw_gold":
                    f.id().equals("copper")?"minecraft:raw_copper":f.product();
                Item sample=BuiltInRegistries.ITEM.get(ResourceLocation.parse(reference));
                if(sample==Items.AIR)continue;
                var profile=PhysicalMaterials.item(new ItemStack(sample));
                if(profile.evidence()==PhysicalMaterials.Evidence.UNKNOWN||profile.components().isEmpty())continue;
                for(var entry:Catalog.FRAGMENTS.get(f.id()).entrySet()){
                    double scale=entry.getKey()/9.0;
                    var components=profile.components().stream().map(c->new PhysicalMaterials.Component(c.materialId(),c.volumeM3()*scale)).toList();
                    PhysicalMaterials.registerObject(PhysicalMaterials.Domain.ITEM,BuiltInRegistries.ITEM.getKey(entry.getValue().get()),components);
                }
            }
        });
    }
    private static void creative(BuildCreativeModeTabContentsEvent event){
        if(!event.getTabKey().equals(CreativeModeTabs.INGREDIENTS))return;
        for(var family:Catalog.FRAGMENTS.values())for(var item:family.values())event.accept(item.get());
        event.accept(COPPER_NUGGET.get());
    }
    static boolean target(BlockState state){return Catalog.spec(state)!=null;}
    private static boolean allowed(Permit permit,net.minecraft.world.entity.player.Player p,BlockPos pos){
        return permit!=null&&permit.player()==p&&permit.pos().equals(pos);
    }
    private static void guardClick(PlayerInteractEvent.LeftClickBlock event){
        if(event.getEntity().isCreative()||event.getEntity().isSpectator()||!target(event.getLevel().getBlockState(event.getPos())))return;
        if(!allowed(CLICK.get(),event.getEntity(),event.getPos()))event.setCanceled(true);
    }
    private static void guardBreak(BlockEvent.BreakEvent event){
        if(event.getPlayer().isCreative()||event.getPlayer().isSpectator()||!target(event.getState()))return;
        if(!allowed(BREAK.get(),event.getPlayer(),event.getPos()))event.setCanceled(true);
    }
    private static ExcavationApi.Result reject(ExcavationApi.Status status,ServerLevel l,BlockPos pos){
        return new ExcavationApi.Result(status,0,inspect(l,pos));
    }
    static Optional<ExcavationApi.Snapshot> inspect(ServerLevel level,BlockPos pos){
        if(!level.getServer().isSameThread()||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
        var state=level.getBlockState(pos);var spec=Catalog.spec(state);if(spec==null)return Optional.empty();
        if(state.getBlock() instanceof ChipBlock){
            if(!(level.getBlockEntity(pos) instanceof ChipProgress p)||p.read()==null)return Optional.empty();
            var f=p.read();return Optional.of(new ExcavationApi.Snapshot(ResourceLocation.parse(spec.source()),f.work(),f.key(),spec.ore()?9-f.extracted():0,p.energy()));
        }
        return Optional.of(new ExcavationApi.Snapshot(ResourceLocation.parse(spec.source()),0,0,spec.ore()?9:0,Double.NaN));
    }
    private static double incident(WorldLogicExcavation.Contract core,ItemStack tool){
        var result=PhysicalMaterials.itemResult(tool);var parts=result.object().components();
        int index=-1;double best=-1;
        for(int i=0;i<parts.size();i++){
            double mohs=PhysicalMaterials.material(parts.get(i).materialId()).map(PhysicalMaterials.MaterialProfile::mohs).orElse(Double.NaN);
            if(Double.isFinite(mohs)&&mohs>best){best=mohs;index=i;}
        }
        // No guessed SI output if physical data are missing. Area/swing speed are stated game assumptions.
        if(index<0||!Double.isFinite(result.massKg())||result.massKg()<=0)return Double.NaN;
        return core.strike(tool,index,.00015,Math.min(3,result.massKg()),4).map(WorldLogicExcavation.Strike::incidentEnergyPerAreaJm2).orElse(Double.NaN);
    }
    static ExcavationApi.Result strike(ServerPlayer player,BlockPos pos){
        ServerLevel level=(ServerLevel)player.level();
        if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!player.isAlive()||player.isCreative()||player.isSpectator()||player.isUsingItem()||!player.mayBuild()||!level.mayInteract(player,pos))return reject(ExcavationApi.Status.DENIED,level,pos);
        HitResult hit=player.pick(player.blockInteractionRange(),1f,false);
        if(!(hit instanceof BlockHitResult bh)||hit.getType()!=HitResult.Type.BLOCK||!bh.getBlockPos().equals(pos))return reject(ExcavationApi.Status.DENIED,level,pos);
        BlockState state=level.getBlockState(pos);
        Catalog.Spec spec=Catalog.spec(state);if(spec==null)return reject(ExcavationApi.Status.NOT_TARGET,level,pos);
        ItemStack tool=player.getMainHandItem();
        if(!tool.is(ItemTags.PICKAXES))return reject(ExcavationApi.Status.WRONG_TOOL,level,pos);
        var core=WorldLogicCapabilities.find(WorldLogicExcavation.KEY,0).orElse(null);
        if(core==null)return reject(ExcavationApi.Status.CORE_UNAVAILABLE,level,pos);
        BlockState original=spec.original();
        if(original.isAir()||!original.is(BlockTags.MINEABLE_WITH_PICKAXE)||original.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(original))
            return reject(ExcavationApi.Status.WRONG_TOOL,level,pos);
        boolean chipped=state.getBlock() instanceof ChipBlock;
        float hardness=original.getDestroySpeed(level,pos);
        if(!chipped){
            var inspected=core.inspect(level,pos,tool);
            if(inspected.isEmpty()||inspected.get().kind()!=WorldLogicExcavation.Kind.ROCK_OR_ORE||!inspected.get().toolSuitableForDrops())return reject(ExcavationApi.Status.WRONG_TOOL,level,pos);
            hardness=inspected.get().vanillaResistance();
        }
        float speed=tool.getDestroySpeed(original);
        if(!Float.isFinite(speed)||speed<=0||!Float.isFinite(hardness)||hardness<0)return reject(ExcavationApi.Status.WRONG_TOOL,level,pos);
        int required=FractureMath.hits(speed,hardness,spec.ore());
        int cooldown=Math.max(7,Math.min(22,5+2*required-(spec.ore()?0:2)));
        Integer last=LAST.get(player);if(last!=null&&player.tickCount>=last&&player.tickCount-last<cooldown)return reject(ExcavationApi.Status.COOLDOWN,level,pos);
        // When the ray strikes an interior shoulder, keep cutting from the actual entry face.
        Direction face=entryFace(player,pos,bh.getDirection());
        FractureMath.State old;
        if(chipped){
            if(!(level.getBlockEntity(pos) instanceof ChipProgress p)||p.read()==null)return reject(ExcavationApi.Status.INVALID_STATE,level,pos);
            old=p.read();
        }else old=new FractureMath.State(0,0,0);
        var plan=FractureMath.strike(old,required,face.ordinal(),spec.ore());
        var permit=new Permit(player,pos.immutable());
        CLICK.set(permit);
        boolean permitted;
        try{permitted=!NeoForge.EVENT_BUS.post(new PlayerInteractEvent.LeftClickBlock(player,pos,face,PlayerInteractEvent.LeftClickBlock.Action.START)).isCanceled();}
        finally{CLICK.remove();}
        if(!permitted||!level.getBlockState(pos).equals(state))return reject(ExcavationApi.Status.DENIED,level,pos);
        double energy=incident(core,tool);
        if(plan.complete()){
            // Normal vanilla final break event and durability path. The proxy has no loot table.
            if(!chipped)return reject(ExcavationApi.Status.INVALID_STATE,level,pos);
            BREAK.set(permit);
            boolean broke;
            try{broke=player.gameMode.destroyBlock(pos);}
            finally{BREAK.remove();}
            if(!broke||level.getBlockState(pos).getBlock()==state.getBlock())return reject(ExcavationApi.Status.DENIED,level,pos);
            if(spec.ore())emit(level,pos,spec,plan.units());
            else{
                Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.drop()));
                if(item!=Items.AIR)Block.popResource(level,pos,new ItemStack(item));
            }
        }else{
            // BreakEvent-only claim systems get one permission probe per nonfinal hit.
            // Core 1.5.0 also charges this event; do NOT add duplicate exertion until #264 arbitration is available.
            BREAK.set(permit);
            try{permitted=!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled();}
            finally{BREAK.remove();}
            if(!permitted||!level.getBlockState(pos).equals(state))return reject(ExcavationApi.Status.DENIED,level,pos);
            if(!chipped&&!level.setBlock(pos,Catalog.CHIPS.get(spec.key()).get().defaultBlockState(),Block.UPDATE_ALL))return reject(ExcavationApi.Status.INVALID_STATE,level,pos);
            if(!(level.getBlockEntity(pos) instanceof ChipProgress progress)){
                if(!chipped)level.setBlock(pos,state,Block.UPDATE_ALL);
                return reject(ExcavationApi.Status.INVALID_STATE,level,pos);
            }
            progress.write(plan,energy);
            if(spec.ore())emit(level,pos,spec,plan.units());
            tool.hurtAndBreak(1,player,EquipmentSlot.MAINHAND);
        }
        LAST.put(player,player.tickCount);
        player.swing(InteractionHand.MAIN_HAND,true);
        level.playSound(null,pos,original.getSoundType().getHitSound(),SoundSource.BLOCKS,1,.85f+level.random.nextFloat()*.2f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,original),pos.getX()+.5+face.getStepX()*.48,pos.getY()+.5+face.getStepY()*.48,pos.getZ()+.5+face.getStepZ()*.48,8,.12,.12,.12,.08);
        return new ExcavationApi.Result(plan.complete()?ExcavationApi.Status.BROKEN:ExcavationApi.Status.ACCEPTED,plan.units(),inspect(level,pos));
    }
    private static Direction entryFace(ServerPlayer p,BlockPos pos,Direction fallback){
        var eye=p.getEyePosition();
        var end=eye.add(p.getViewVector(1).scale(p.blockInteractionRange()));
        var entry=new AABB(pos).clip(eye,end);
        if(entry.isEmpty())return fallback;
        var v=entry.get();double x=v.x-pos.getX(),y=v.y-pos.getY(),z=v.z-pos.getZ();
        double[] d={Math.abs(y),Math.abs(y-1),Math.abs(z),Math.abs(z-1),Math.abs(x),Math.abs(x-1)};
        int best=0;for(int i=1;i<6;i++)if(d[i]<d[best])best=i;
        return Direction.values()[best];
    }
    private static void emit(ServerLevel level,BlockPos pos,Catalog.Spec spec,int units){
        // No normal ore loot or Fortune/Silk Touch duplicate; every emitted piece spends the same reserve.
        if(units>=4){Block.popResource(level,pos,Catalog.fragment(spec.family(),4,units/4));units%=4;}
        if(units>0)Block.popResource(level,pos,Catalog.fragment(spec.family(),1,units));
    }
    static void qaReset(ServerPlayer p){LAST.remove(p);}
}
