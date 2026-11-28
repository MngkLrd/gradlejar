package dev.minemods.treechopper;

import java.util.*;
import java.util.function.Supplier;
import java.lang.reflect.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.*;

final class LooseRocks {
    static final double PEBBLE_VOLUME=.00015, FLINT_VOLUME=.00003;
    private static final String[] VANILLA={"stone","granite","diorite","andesite","deepslate","tuff","calcite","basalt","smooth_basalt","blackstone","sandstone","red_sandstone","dripstone_block"};
    private static final String[] CORE={"limestone","shale","slate","gneiss","volcanic_tuff"};
    private static final Map<ResourceLocation,DeferredBlock<PebbleBlock>> ROCKS=new LinkedHashMap<>();
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TreeChopper.ID);
    private LooseRocks(){}
    static void register(DeferredRegister.Blocks blocks,IEventBus bus){
        for(String name:VANILLA)add(blocks,"minecraft",name,false);
        for(String name:CORE)add(blocks,"worldlogic",name,false);
        add(blocks,"minecraft","gravel",true);
        ITEMS.register(bus);
    }
    private static void add(DeferredRegister.Blocks blocks,String ns,String name,boolean flint){
        ResourceLocation source=ResourceLocation.fromNamespaceAndPath(ns,name);
        String id=flint?"loose_flint":"pebble_"+name;
        var block=blocks.register(id,()->new PebbleBlock(BlockBehaviour.Properties.of().strength(0).noCollission().noOcclusion().sound(SoundType.STONE),source,flint));
        ROCKS.put(source,block);
        if(!flint)ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));
    }
    static BlockState forSurface(BlockState surface){
        var rock=ROCKS.get(BuiltInRegistries.BLOCK.getKey(surface.getBlock()));
        return rock==null?null:rock.get().defaultBlockState();
    }
    static boolean offer(ServerLevel l,int x,int z){
        BlockPos column=new BlockPos(x,l.getMinBuildHeight(),z);
        if(!l.hasChunkAt(column))return false;
        int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        BlockPos ground=new BlockPos(x,y,z),pos=ground.above();
        if(l.isOutsideBuildHeight(pos)||!l.getBlockState(pos).isAir())return false;
        var state=forSurface(l.getBlockState(ground));
        if(state==null||!state.canSurvive(l,pos))return false;
        for(BlockPos near:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,1,2))){
            if(l.hasChunkAt(near)&&l.getBlockState(near).getBlock() instanceof PebbleBlock)return false;
        }
        return l.setBlock(pos,state,Block.UPDATE_ALL);
    }
    static boolean qaCoreMaterials(){
        try{
            Class<?> api=Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials"),profile=Class.forName(api.getName()+"$ObjectProfile"),component=Class.forName(api.getName()+"$Component"),state=Class.forName(api.getName()+"$State"),result=Class.forName(api.getName()+"$Result");
            Method block=api.getMethod("block",BlockState.class),item=api.getMethod("item",ItemStack.class),volume=profile.getMethod("volumeM3"),components=profile.getMethod("components"),material=component.getMethod("materialId"),evaluate=api.getMethod("evaluate",profile,state),mass=result.getMethod("massKg");
            Object reference=state.getMethod("reference").invoke(null);
            for(String key:List.of("minecraft:granite","worldlogic:limestone")){
                var source=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(key));
                if(source==Blocks.AIR)return false;
                var pebble=(PebbleBlock)forSurface(source.defaultBlockState()).getBlock();
                Object original=block.invoke(null,source.defaultBlockState()),ground=block.invoke(null,pebble.defaultBlockState()),held=item.invoke(null,pebble.collectedStack());
                var a=(List<?>)components.invoke(original);var b=(List<?>)components.invoke(ground);var c=(List<?>)components.invoke(held);
                if(a.isEmpty()||a.size()!=b.size()||b.size()!=c.size())return false;
                for(int i=0;i<a.size();i++)if(!material.invoke(a.get(i)).equals(material.invoke(b.get(i)))||!material.invoke(b.get(i)).equals(material.invoke(c.get(i))))return false;
                double va=((Number)volume.invoke(original)).doubleValue(),vb=((Number)volume.invoke(ground)).doubleValue(),vc=((Number)volume.invoke(held)).doubleValue();
                double ma=((Number)mass.invoke(evaluate.invoke(null,original,reference))).doubleValue(),mb=((Number)mass.invoke(evaluate.invoke(null,ground,reference))).doubleValue(),mc=((Number)mass.invoke(evaluate.invoke(null,held,reference))).doubleValue();
                if(!Double.isFinite(ma)||!Double.isFinite(mb)||!Double.isFinite(mc)||Math.abs(vb-PEBBLE_VOLUME)>1e-10||Math.abs(vc-vb)>1e-10||Math.abs(mb-ma*vb/va)>1e-8||Math.abs(mc-mb)>1e-8)return false;
            }
            return true;
        }catch(ReflectiveOperationException|LinkageError|RuntimeException failure){
            LogUtils.getLogger().warn("TreeChopper Core rock material diagnostic failed",failure);return false;
        }
    }
    /** Register addon objects against the existing authoritative material IDs, without duplicating geology. */
    @SuppressWarnings({"unchecked","rawtypes"})
    static void bindCore(){
        if(!ModList.get().isLoaded("worldlogic"))return;
        try{
            Class<?> api=Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials");
            if(api.getField("REVISION").getInt(null)!=2)return;
            Class<?> domain=Class.forName(api.getName()+"$Domain"),profile=Class.forName(api.getName()+"$ObjectProfile"),component=Class.forName(api.getName()+"$Component");
            Method block=api.getMethod("block",BlockState.class),item=api.getMethod("item",ItemStack.class),register=api.getMethod("registerObject",domain,ResourceLocation.class,List.class);
            Method components=profile.getMethod("components"),volume=profile.getMethod("volumeM3"),materialId=component.getMethod("materialId"),partVolume=component.getMethod("volumeM3");
            Constructor<?> constructor=component.getConstructor(ResourceLocation.class,double.class);
            Object blockDomain=Enum.valueOf((Class)domain,"BLOCK"),itemDomain=Enum.valueOf((Class)domain,"ITEM");
            for(var entry:ROCKS.entrySet()){
                PebbleBlock target=entry.getValue().get();Block source=BuiltInRegistries.BLOCK.get(entry.getKey());
                if(source==Blocks.AIR)continue;
                Object original=target.flint?item.invoke(null,new ItemStack(Items.FLINT)):block.invoke(null,source.defaultBlockState());
                double originalVolume=((Number)volume.invoke(original)).doubleValue();
                if(!Double.isFinite(originalVolume)||originalVolume<=0)continue;
                double targetVolume=target.flint?FLINT_VOLUME:PEBBLE_VOLUME;
                List<Object> parts=new ArrayList<>();
                for(Object part:(List<?>)components.invoke(original))
                    parts.add(constructor.newInstance(materialId.invoke(part),((Number)partVolume.invoke(part)).doubleValue()*targetVolume/originalVolume));
                if(parts.isEmpty())continue;
                ResourceLocation id=BuiltInRegistries.BLOCK.getKey(target);
                register.invoke(null,blockDomain,id,parts);
                if(!target.flint)register.invoke(null,itemDomain,id,parts);
            }
        }catch(ReflectiveOperationException|LinkageError|RuntimeException failure){
            LogUtils.getLogger().warn("TreeChopper loose rock Core material registration unavailable",failure);
        }
    }
}
