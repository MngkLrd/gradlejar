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
