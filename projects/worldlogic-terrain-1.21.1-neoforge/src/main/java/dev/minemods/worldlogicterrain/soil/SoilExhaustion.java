package dev.minemods.worldlogicterrain.soil;

import dev.minemods.worldlogicterrain.block.BerryBushBlock;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Agricultural use history, not a second climate or soil-chemistry model. Server thread only. */
public final class SoilExhaustion extends SavedData {
    static final long DAY=24000L, REST=2*DAY;
    static final int LIMIT=3;
    private static final String DATA_ID="worldlogicterrain_soil_use";
    private final LinkedHashMap<Long,Entry> cells=new LinkedHashMap<>();
    private record Entry(int cycles,long harvest,long fallow,String plant,boolean exhausted){}
    public static void register(){
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,SoilExhaustion::placed);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,SoilExhaustion::drops);
        NeoForge.EVENT_BUS.addListener(SoilExhaustion::beforeGrowth);
        NeoForge.EVENT_BUS.addListener(SoilExhaustion::afterGrowth);
        NeoForge.EVENT_BUS.addListener(SoilExhaustion::hoe);
        NeoForge.EVENT_BUS.addListener(SoilExhaustion::tick);
        NeoForge.EVENT_BUS.addListener(SoilChecks::register);
    }
    static SoilExhaustion get(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SoilExhaustion::new,SoilExhaustion::load,null),DATA_ID);
    }
    static SoilExhaustion load(CompoundTag root,HolderLookup.Provider lookup){
        var result=new SoilExhaustion();
        var rows=root.getList("cells",Tag.TAG_COMPOUND);
        for(int i=0;i<rows.size();i++){
            var n=rows.getCompound(i);
            result.cells.put(n.getLong("pos"),new Entry(Math.max(0,Math.min(LIMIT,n.getInt("cycles"))),n.getLong("harvest"),n.getLong("fallow"),n.getString("plant"),n.getBoolean("exhausted")));
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
        var rows=new ListTag();
        cells.forEach((key,e)->{var n=new CompoundTag();n.putLong("pos",key);n.putInt("cycles",e.cycles());n.putLong("harvest",e.harvest());n.putLong("fallow",e.fallow());n.putString("plant",e.plant());n.putBoolean("exhausted",e.exhausted());rows.add(n);});
        root.put("cells",rows);return root;
    }
    private void put(BlockPos pos,Entry e){var old=cells.put(pos.asLong(),e);if(!e.equals(old))setDirty();}
    void remove(BlockPos pos){if(cells.remove(pos.asLong())!=null)setDirty();}
    int cycles(BlockPos pos){var e=cells.get(pos.asLong());return e==null?0:e.cycles();}
    public static boolean exhausted(BlockState s){return s.is(TerrainContent.EXHAUSTED_DIRT.get())||s.is(TerrainContent.EXHAUSTED_FARMLAND.get());}
    static boolean eligible(BlockState s){return exhausted(s)||s.is(Blocks.DIRT)||s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.FARMLAND);}
    private static String key(BlockState s){return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();}
    private static boolean melon(BlockState s){return s.is(Blocks.MELON)||s.is(Blocks.PUMPKIN);}
    private static boolean cultivated(BlockState s){return s.getBlock() instanceof CropBlock||s.getBlock() instanceof StemBlock||s.getBlock() instanceof AttachedStemBlock||s.getBlock() instanceof BerryBushBlock||s.getBlock() instanceof dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock||s.is(Blocks.SWEET_BERRY_BUSH)||melon(s);}
    private static long now(ServerLevel l){return l.getDayTime();}
    static boolean uninterrupted(long last,long next){return last>=0&&next>=last&&next-last<=DAY;}
    void plant(ServerLevel l,BlockPos soil,BlockState plant,long time){
        if(!eligible(l.getBlockState(soil)))return;
        var e=cells.get(soil.asLong());
        if(e!=null&&e.plant().equals(key(plant)))return;
        boolean worn=exhausted(l.getBlockState(soil))||e!=null&&e.exhausted();
        int count=worn?LIMIT:e!=null&&uninterrupted(e.harvest(),time)?e.cycles():0;
        put(soil,new Entry(count,e==null?-1:e.harvest(),-1,key(plant),worn));
    }
    /** Records exactly one crop harvest; a planted or naturally grown marker is consumed. */
    boolean harvest(ServerLevel l,BlockPos soil,BlockState crop,long time,boolean perennial){
        var floor=l.getBlockState(soil);
        if(!eligible(floor))return false;
        var e=cells.get(soil.asLong());
        if(!perennial&&(e==null||!e.plant().equals(key(crop))))return false;
        boolean wasWorn=exhausted(floor)||e!=null&&e.exhausted();
        int prior=wasWorn?LIMIT:e==null?0:perennial&&!uninterrupted(e.harvest(),time)?0:e.cycles();
        int count=Math.min(LIMIT,prior+1);
        boolean worn=count>=LIMIT;
        put(soil,new Entry(count,time,perennial?-1:time,"",worn));
        if(worn)applyAppearance(l,soil,true);
        return true;
    }
    static void applyAppearance(ServerLevel l,BlockPos soil,boolean worn){
        var floor=l.getBlockState(soil);
        if(!eligible(floor))return;
        BlockState target;
        if(floor.getBlock() instanceof FarmBlock){
            target=(worn?TerrainContent.EXHAUSTED_FARMLAND.get():Blocks.FARMLAND).defaultBlockState().setValue(FarmBlock.MOISTURE,floor.getValue(FarmBlock.MOISTURE));
        }else{
            if(!worn&&!exhausted(floor))return;
            target=(worn?TerrainContent.EXHAUSTED_DIRT.get():Blocks.DIRT).defaultBlockState();
        }
        if(!floor.equals(target))l.setBlock(soil,target,Block.UPDATE_ALL);
    }
    public static boolean allowGrowth(ServerLevel l,BlockPos soil,RandomSource random){
        return !exhausted(l.getBlockState(soil))||random.nextFloat()>=.35f;
    }
    public static void plantGrowth(ServerLevel l,BlockPos pos,BlockState state){get(l).plant(l,pos.below(),state,now(l));}
    public static void berryHarvest(ServerLevel l,BlockPos pos){
        get(l).harvest(l,pos.below(),l.getBlockState(pos),now(l),true);
    }
    private static void placed(BlockEvent.EntityPlaceEvent e){
        if(!(e.getLevel() instanceof ServerLevel l)||!(e.getEntity() instanceof Player p)||p.isCreative()||p.isSpectator())return;
        var state=e.getPlacedBlock();var pos=e.getPos();
        if(state.getBlock() instanceof CropBlock c&&!c.isMaxAge(state)||state.getBlock() instanceof StemBlock||state.getBlock() instanceof BerryBushBlock||state.getBlock() instanceof dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock&&state.getValue(dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock.AGE)<3)
            get(l).plant(l,pos.below(),state,now(l));
        else if(eligible(state)&&!exhausted(state))get(l).remove(pos);
    }
    private static void beforeGrowth(CropGrowEvent.Pre e){
        if(!(e.getLevel() instanceof ServerLevel l))return;
        var state=e.getState();var pos=e.getPos();
        if(state.getBlock() instanceof CropBlock||state.getBlock() instanceof StemBlock||state.is(Blocks.SWEET_BERRY_BUSH)){
            if(!allowGrowth(l,pos.below(),l.random)||!dev.minemods.worldlogicterrain.growth.SeasonalGrowth.allowSeason(l,pos,state,l.random))e.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }
    private static void afterGrowth(CropGrowEvent.Post e){
        if(!(e.getLevel() instanceof ServerLevel l))return;
        var pos=e.getPos();var before=e.getOriginalState();var current=l.getBlockState(pos);
        if(before.getBlock() instanceof CropBlock c&&!c.isMaxAge(before)&&current.is(before.getBlock()))
            get(l).plant(l,pos.below(),before,now(l));
        if(before.getBlock() instanceof StemBlock&&current.getBlock() instanceof AttachedStemBlock){
            var fruit=pos.relative(current.getValue(BlockStateProperties.HORIZONTAL_FACING));
            if(l.hasChunkAt(fruit)&&melon(l.getBlockState(fruit))){
                if(!allowGrowth(l,fruit.below(),l.random)){
                    // This is the newly created fruit of this exact stem tick, not an existing player block.
                    l.setBlock(fruit,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                }else get(l).plant(l,fruit.below(),l.getBlockState(fruit),now(l));
            }
        }
    }
    private static void drops(BlockDropsEvent e){
        var l=e.getLevel();var pos=e.getPos();var state=e.getState();
        if(eligible(state)){get(l).remove(pos);return;}
        if(!(e.getBreaker() instanceof Player p)||p.isCreative()||p.isSpectator()||e.getDrops().isEmpty())return;
        var soil=pos.below();boolean worn=exhausted(l.getBlockState(soil));
        boolean ripe=state.getBlock() instanceof CropBlock c&&c.isMaxAge(state)||melon(state)||state.getBlock() instanceof dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock&&state.getValue(dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock.AGE)==3;
        if(!ripe){
            if(state.getBlock() instanceof CropBlock||state.getBlock() instanceof BerryBushBlock||state.getBlock() instanceof dev.minemods.worldlogicterrain.growth.SeasonalPlantBlock)get(l).abandoned(soil,now(l));
            return;
        }
        if(!get(l).harvest(l,soil,state,now(l),false)||!worn)return;
        // Keep the normal minimum harvest and one seed; suppress only extra units.
        var totals=new java.util.HashMap<net.minecraft.world.item.Item,Integer>();
        var iter=e.getDrops().iterator();
        while(iter.hasNext()){
            var drop=iter.next();var stack=drop.getItem();var item=stack.getItem();
            int cap=stack.is(Items.MELON_SLICE)?3:stack.is(Items.CARROT)||stack.is(Items.POTATO)?2:1;
            int left=Math.max(0,cap-totals.getOrDefault(item,0));
            if(left==0){iter.remove();continue;}
            if(stack.getCount()>left)stack.setCount(left);
            totals.merge(item,stack.getCount(),Integer::sum);
        }
    }
    private void abandoned(BlockPos soil,long time){
        var e=cells.get(soil.asLong());if(e==null)return;
        put(soil,new Entry(e.cycles(),e.harvest(),time,"",e.exhausted()));
    }
    private static void hoe(BlockEvent.BlockToolModificationEvent e){
        if(e.getItemAbility()==ItemAbilities.HOE_TILL&&e.getState().is(TerrainContent.EXHAUSTED_DIRT.get())&&e.getContext().getClickedFace()!=Direction.DOWN&&e.getLevel().getBlockState(e.getPos().above()).isAir())
            e.setFinalState(TerrainContent.EXHAUSTED_FARMLAND.get().defaultBlockState());
    }
    private static void tick(LevelTickEvent.Post e){
        if(e.getLevel() instanceof ServerLevel l&&l.getGameTime()%20==0)get(l).maintain(l,now(l),256);
    }
    void maintain(ServerLevel l,long time,int budget){
        // Rotate a bounded batch, including unloaded entries without force-loading.
        var batch=new ArrayList<Map.Entry<Long,Entry>>();
        var it=cells.entrySet().iterator();
        while(it.hasNext()&&batch.size()<budget){var e=it.next();batch.add(Map.entry(e.getKey(),e.getValue()));it.remove();}
        for(var pair:batch){
            var soil=BlockPos.of(pair.getKey());var e=pair.getValue();
            if(!l.hasChunkAt(soil)){cells.put(pair.getKey(),e);continue;}
            var floor=l.getBlockState(soil);if(!eligible(floor)){setDirty();continue;}
            if(e.exhausted())applyAppearance(l,soil,true);
            boolean busy=cultivated(l.getBlockState(soil.above()));
            if(busy){
                var current=l.getBlockState(soil.above());
                // Do not enroll manually placed melon/pumpkin. A real planting/age transition does it.
                if(e.fallow()!=-1){e=new Entry(e.cycles(),e.harvest(),-1,e.plant(),e.exhausted());setDirty();}
            }else{
                if(e.fallow()<0||time<e.fallow()){e=new Entry(e.cycles(),e.harvest(),time,"",e.exhausted());setDirty();}
                if(time-e.fallow()>=REST){if(e.exhausted())applyAppearance(l,soil,false);setDirty();continue;}
                if(!e.exhausted()&&e.harvest()>=0&&time-e.harvest()>DAY&&e.cycles()!=0){e=new Entry(0,e.harvest(),e.fallow(),"",false);setDirty();}
            }
            cells.put(pair.getKey(),e);
        }
    }
}
