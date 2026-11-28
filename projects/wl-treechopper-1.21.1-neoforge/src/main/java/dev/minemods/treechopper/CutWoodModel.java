package dev.minemods.treechopper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.*;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Function;

/** One loader/model per wood material; lazy chunk-baked quads rather than per-frame BERs. */
@EventBusSubscriber(modid=TreeChopper.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class CutWoodModel {
    @SubscribeEvent public static void register(ModelEvent.RegisterGeometryLoaders event){
        event.register(ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"dynamic_cut"),new Loader());
    }
    // Opt-in diagnostic only, never runs during ordinary player sessions.
    @EventBusSubscriber(modid=TreeChopper.ID,value=Dist.CLIENT)
    public static final class ClientProbe {
        private static int readyTicks;
        private static boolean done;
        @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
            if(done||!Boolean.getBoolean("wl.treechopper.clientQa"))return;
            var client=net.minecraft.client.Minecraft.getInstance();
            if(!(client.screen instanceof net.minecraft.client.gui.screens.TitleScreen)||client.getOverlay()!=null){readyTicks=0;return;}
            if(++readyTicks<20)return;
            done=true;
            try{
                int count=0;
                for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK){
                    if(!(block instanceof CutBlock))continue;
                    var state=block.defaultBlockState();
                    var model=client.getBlockRenderer().getBlockModel(state);
                    if(!(model instanceof Wood woodModel))throw new IllegalStateException("Dynamic cut model not installed: "+block);
                    var empty=woodModel.quads(0);
                    var key=CutGeometry.key(new int[]{500,125,250,0});
                    var cut=woodModel.quads(key);
                    if(empty.isEmpty()||cut.isEmpty()||empty.equals(cut)||cut!=woodModel.quads(key)||!woodModel.getQuads(state,Direction.NORTH,RandomSource.create(0),ModelData.EMPTY,null).isEmpty())throw new IllegalStateException("Model geometry/cache failed: "+block);
                    var data=ModelData.builder().with(CutProgress.DEPTHS,key).build();
                    if(woodModel.getQuads(state,null,RandomSource.create(0),data,RenderType.solid())!=cut)throw new IllegalStateException("ModelData lookup failed: "+block);
                    if(++count==1){
                        for(int n=0;n<9;n++)for(int s=0;s<9;s++)for(int w=0;w<9;w++)woodModel.quads(n|(s<<4)|(w<<8));
                        if(woodModel.cache.size()>Wood.LIMIT)throw new IllegalStateException("Unbounded model cache");
                    }
                }
                if(count!=21)throw new IllegalStateException("Expected 21 cut models, found "+count);
                System.out.println("TREECHOPPER_CLIENT_QA_PASS: title screen, 21 dynamic models, ModelData and bounded quad cache");
            }catch(RuntimeException failure){
                System.err.println("TREECHOPPER_CLIENT_QA_FAILED: "+failure);
                throw failure;
            }
        }
    }
    private static final class Loader implements IGeometryLoader<Geometry>{
        @Override public Geometry read(JsonObject json,JsonDeserializationContext context){return new Geometry();}
    }
    private static final class Geometry implements IUnbakedGeometry<Geometry>{
        @Override public BakedModel bake(IGeometryBakingContext context,ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,ItemOverrides overrides){
            return new Wood(sprites.apply(context.getMaterial("bark")),sprites.apply(context.getMaterial("wood")),sprites.apply(context.getMaterial("end")),state,overrides);
        }
    }
    private static final class Wood implements IDynamicBakedModel {
        private static final int LIMIT=128;
        private final TextureAtlasSprite bark,wood,end;
        private final ModelState state;
        private final ItemOverrides overrides;
        private final FaceBakery bakery=new FaceBakery();
        private final Map<Integer,List<BakedQuad>> cache=new LinkedHashMap<>(16,.75f,true);
        Wood(TextureAtlasSprite bark,TextureAtlasSprite wood,TextureAtlasSprite end,ModelState state,ItemOverrides overrides){
            this.bark=bark;this.wood=wood;this.end=end;this.state=state;this.overrides=overrides;
        }
        @Override public boolean useAmbientOcclusion(){return true;}
        @Override public boolean isGui3d(){return true;}
        @Override public boolean usesBlockLight(){return true;}
        @Override public boolean isCustomRenderer(){return false;}
        @Override public TextureAtlasSprite getParticleIcon(){return bark;}
        @Override public ItemOverrides getOverrides(){return overrides;}
        @Override public ItemTransforms getTransforms(){return ItemTransforms.NO_TRANSFORMS;}
        @Override public List<BakedQuad> getQuads(BlockState block,Direction side,RandomSource random,ModelData data,RenderType type){
            if(side!=null)return List.of();
            Integer key=data.get(CutProgress.DEPTHS);return quads(key==null?0:key);
        }
        private synchronized List<BakedQuad> quads(int key){
            List<BakedQuad> found=cache.get(key);
            if(found!=null)return found;
            List<CutGeometry.Box> boxes=CutGeometry.geometry(key).boxes();
            List<BakedQuad> quads=new ArrayList<>(30);
            for(int i=0;i<boxes.size();i++){
                var b=boxes.get(i);
                face(quads,b,Direction.NORTH,b.z0()==0?bark:wood);
                face(quads,b,Direction.SOUTH,b.z1()==16?bark:wood);
                face(quads,b,Direction.WEST,b.x0()==0?bark:wood);
                face(quads,b,Direction.EAST,b.x1()==16?bark:wood);
                var prev=i==0?null:boxes.get(i-1);var next=i+1==boxes.size()?null:boxes.get(i+1);
                horizontal(quads,b,Direction.DOWN,prev!=null&&prev.y1()==b.y0()?prev:null);
                horizontal(quads,b,Direction.UP,next!=null&&next.y0()==b.y1()?next:null);
            }
            found=List.copyOf(quads);cache.put(key,found);
            if(cache.size()>LIMIT)cache.remove(cache.keySet().iterator().next());
            return found;
        }
        // Remove hidden interface faces; only the exposed shoulder strips remain.
        private void horizontal(List<BakedQuad> q,CutGeometry.Box b,Direction d,CutGeometry.Box adjacent){
            if(adjacent==null){face(q,b,d,end);return;}
            int x0=Math.max(b.x0(),adjacent.x0()),x1=Math.min(b.x1(),adjacent.x1());
            int z0=Math.max(b.z0(),adjacent.z0()),z1=Math.min(b.z1(),adjacent.z1());
            if(x0>=x1||z0>=z1){face(q,b,d,end);return;}
            strip(q,b,d,b.x0(),b.z0(),x0,b.z1());
            strip(q,b,d,x1,b.z0(),b.x1(),b.z1());
            strip(q,b,d,x0,b.z0(),x1,z0);
            strip(q,b,d,x0,z1,x1,b.z1());
        }
        private void strip(List<BakedQuad> q,CutGeometry.Box b,Direction d,int x0,int z0,int x1,int z1){
            if(x0<x1&&z0<z1)face(q,new CutGeometry.Box(x0,b.y0(),z0,x1,b.y1(),z1),d,end);
        }
        private void face(List<BakedQuad> q,CutGeometry.Box b,Direction d,TextureAtlasSprite sprite){
            int x0=b.x0(),x1=b.x1(),y0=b.y0(),y1=b.y1(),z0=b.z0(),z1=b.z1();
            float[] uv=switch(d){
                case NORTH->new float[]{16-x1,16-y1,16-x0,16-y0};
                case SOUTH->new float[]{x0,16-y1,x1,16-y0};
                case WEST->new float[]{z0,16-y1,z1,16-y0};
                case EAST->new float[]{16-z1,16-y1,16-z0,16-y0};
                case UP->new float[]{x0,z0,x1,z1};
                case DOWN->new float[]{x0,16-z1,x1,16-z0};
            };
            var face=new BlockElementFace(null,-1,"#wood",new BlockFaceUV(uv,0));
            q.add(bakery.bakeQuad(new Vector3f(x0,y0,z0),new Vector3f(x1,y1,z1),face,sprite,d,state,null,true));
        }
    }
}
