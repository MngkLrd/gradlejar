package dev.minemods.worldlogicterrain.tree;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Purpose-built silhouettes. Plan and validate every block before placing, then compute native leaf distance. */
public final class TropicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    static {
        for(String id:new String[]{"palm_young","palm_leaning","date_palm_young","date_palm_tall","baobab_young","baobab_ancient"})
            FEATURES.register(id,()->new TropicalTreeFeature(id));
    }
    private final String form;
    private TropicalTreeFeature(String form){super(NoneFeatureConfiguration.CODEC);this.form=form;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var level=c.level();var p=c.origin();var random=c.random();
        boolean baobab=form.startsWith("baobab"),date=form.startsWith("date_palm"),mature=!form.endsWith("young");
        String species=baobab?"baobab":date?"date_palm":"palm";
        var log=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",species+"_log")).defaultBlockState();
        var leaf=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",species+"_leaves")).defaultBlockState();
        if(!support(level,p.below(),baobab))return false;
        Map<BlockPos,BlockState> logs=new LinkedHashMap<>();
        var foliage=new HashSet<BlockPos>();
        int h=(baobab?(mature?12:7):date?(mature?12:7):(mature?11:7))+random.nextInt(3);
        if(baobab){
            int min=mature?-1:0,max=1;
            for(int y=0;y<h;y++){
                int radius=y<h-4?max:y<h-2?1:0;
                for(int x=(radius==0?0:min);x<=radius;x++)for(int z=(radius==0?0:min);z<=radius;z++){
                    if(radius==1&&mature&&y>h-6&&Math.abs(x)+Math.abs(z)==2)continue;
                    BlockPos q=p.offset(x,y,z);
                    if(y==0&&!support(level,q.below(),true))return false;
                    logs.put(q,log);
                }
            }
            crown(foliage,p.above(h),2);
            for(Direction dir:Direction.Plane.HORIZONTAL){
                BlockPos branch=p.above(h-3+random.nextInt(2));
                int len=mature?3:2;
                for(int n=1;n<=len;n++){
                    branch=branch.relative(dir);
                    logs.put(branch,log.setValue(RotatedPillarBlock.AXIS,dir.getAxis()));
                }
                logs.put(branch.above(),log);
                crown(foliage,branch.above(2),mature?2:1);
            }
        }else{
            Direction lean=Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos stem=p;
            for(int y=0;y<h;y++){
                if(y>0)stem=stem.above();
                logs.put(stem,log);
                if(!date&&mature&&(y==h/3||y==2*h/3)){
                    stem=stem.relative(lean);
                    logs.put(stem,log.setValue(RotatedPillarBlock.AXIS,lean.getAxis()));
                }
            }
            foliage.add(stem.above());
            int length=mature?4:3;
            for(Direction dir:Direction.Plane.HORIZONTAL){
                for(int n=1;n<=length;n++){
                    int dy=date?(n<=2?1:0):(n<=2?0:-1);
                    BlockPos q=stem.relative(dir,n).offset(0,dy,0);
                    foliage.add(q);
                    if(n==1){foliage.add(q.above());foliage.add(q.below());}
                    if(n==2&&!date){foliage.add(q.relative(dir.getClockWise()));}
                    if(n==length&&!date)foliage.add(q.below());
                    if(n==3)foliage.add(q.above());
                }
            }
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
                foliage.add(stem.offset(x,0,z));
                foliage.add(stem.offset(x*2,date?1:-1,z*2));
                foliage.add(stem.offset(x*2,0,z));
            }
        }
        foliage.removeAll(logs.keySet());
        Map<BlockPos,Integer> distances=new LinkedHashMap<>();
        var queue=new ArrayDeque<BlockPos>();
        for(BlockPos q:logs.keySet()){distances.put(q,0);queue.add(q);}
        while(!queue.isEmpty()){
            BlockPos q=queue.removeFirst();int next=distances.get(q)+1;if(next>6)continue;
            for(Direction dir:Direction.values()){
                BlockPos r=q.relative(dir);
                if(foliage.contains(r)&&!distances.containsKey(r)){distances.put(r,next);queue.add(r);}
            }
        }
        foliage.retainAll(distances.keySet());
        if(foliage.size()<6)return false;
        for(BlockPos q:logs.keySet())if(!replaceable(level,q))return false;
        for(BlockPos q:foliage)if(!replaceable(level,q))return false;
        for(var e:logs.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
        for(BlockPos q:foliage)level.setBlock(q,leaf.setValue(LeavesBlock.DISTANCE,distances.get(q)),2);
        return true;
    }
    private static void crown(HashSet<BlockPos> set,BlockPos center,int r){
        for(int y=-1;y<=1;y++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)
            if(Math.abs(x)+Math.abs(z)+Math.abs(y)<=r+1)set.add(center.offset(x,y,z));
    }
    private static boolean support(WorldGenLevel l,BlockPos p,boolean baobab){
        if(l.isOutsideBuildHeight(p)||!l.ensureCanWrite(p))return false;
        var s=l.getBlockState(p);return EcologicalTreeFeature.soil(s)||(!baobab&&s.is(BlockTags.SAND));
    }
    private static boolean replaceable(WorldGenLevel l,BlockPos p){
        if(l.isOutsideBuildHeight(p)||!l.ensureCanWrite(p))return false;
        var s=l.getBlockState(p);return s.isAir()||s.is(Blocks.SHORT_GRASS)||s.is(Blocks.SNOW);
    }
}
