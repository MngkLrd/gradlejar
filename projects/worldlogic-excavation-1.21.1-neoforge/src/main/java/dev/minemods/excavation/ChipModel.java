package dev.minemods.excavation;
import java.util.*;
import java.util.function.Function;
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

/** Chunk-baked, shared pixel-scale geometry; no block display or per-frame entity renderer. */
@EventBusSubscriber(modid=Excavation.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ChipModel {
    @SubscribeEvent public static void register(ModelEvent.RegisterGeometryLoaders event){
        event.register(ResourceLocation.fromNamespaceAndPath(Excavation.ID,"chip"),new Loader());
    }
    private static final class Loader implements IGeometryLoader<Geometry>{
        @Override public Geometry read(JsonObject json,JsonDeserializationContext context){return new Geometry(json.has("core_mineral")&&json.get("core_mineral").getAsBoolean());}
    }
    private record Geometry(boolean coreMineral) implements IUnbakedGeometry<Geometry>{
        @Override public BakedModel bake(IGeometryBakingContext context,ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,ItemOverrides overrides){
            return new Rock(sprites.apply(context.getMaterial("surface")),sprites.apply(context.getMaterial("host")),sprites.apply(context.getMaterial("mineral")),coreMineral,state,overrides);
        }
    }
    private record Mesh(List<BakedQuad> inner,List<List<BakedQuad>> sides){}
    private static final class Rock implements IDynamicBakedModel {
        private final TextureAtlasSprite surface,host,mineral;
        private final boolean coreMineral;
        private final ModelState state;
        private final ItemOverrides overrides;
        private final FaceBakery bakery=new FaceBakery();
        private final Map<Integer,Mesh> cache=new LinkedHashMap<>(8,.75f,true);
        Rock(TextureAtlasSprite surface,TextureAtlasSprite host,TextureAtlasSprite mineral,boolean coreMineral,ModelState state,ItemOverrides overrides){
            this.surface=surface;this.host=host;this.mineral=mineral;this.coreMineral=coreMineral;this.state=state;this.overrides=overrides;
        }
        @Override public boolean useAmbientOcclusion(){return true;}
        @Override public boolean isGui3d(){return true;}
        @Override public boolean usesBlockLight(){return true;}
        @Override public boolean isCustomRenderer(){return false;}
        @Override public TextureAtlasSprite getParticleIcon(){return surface;}
        @Override public ItemOverrides getOverrides(){return overrides;}
        @Override public ItemTransforms getTransforms(){return ItemTransforms.NO_TRANSFORMS;}
        @Override public List<BakedQuad> getQuads(BlockState block,Direction side,RandomSource random,ModelData data,RenderType type){
            Integer key=data.get(ChipProgress.DEPTHS);Mesh m=mesh(key==null?0:key);
            return side==null?m.inner():m.sides().get(side.ordinal());
        }
        private synchronized Mesh mesh(int raw){
            int key=FractureMath.sanitize(raw);Mesh found=cache.get(key);if(found!=null)return found;
            List<BakedQuad> inner=new ArrayList<>();List<List<BakedQuad>> sides=new ArrayList<>();
            for(int i=0;i<6;i++)sides.add(new ArrayList<>());
            for(var f:NotchGeometry.get(key).faces()){
                Direction d=Direction.values()[f.side()];
                List<BakedQuad> out=f.exterior()?sides.get(f.side()):inner;
                face(out,f.box(),d,f.exterior()?surface:host,0,false);
                if(coreMineral)fleck(out,f.box(),d);
            }
            found=new Mesh(List.copyOf(inner),sides.stream().map(List::copyOf).toList());
            cache.put(key,found);if(cache.size()>16)cache.remove(cache.keySet().iterator().next());
            return found;
        }
        private void fleck(List<BakedQuad> out,NotchGeometry.Box b,Direction d){
            int u0=d.getAxis()==Direction.Axis.X?b.z0():b.x0();
            int u1=d.getAxis()==Direction.Axis.X?b.z1():b.x1();
            int v0=d.getAxis()==Direction.Axis.Y?b.z0():b.y0();
            int v1=d.getAxis()==Direction.Axis.Y?b.z1():b.y1();
            // Deterministic mineral-colored islands on actual remaining exposed material.
            if(u1-u0<4||v1-v0<4)return;
            int u=u0+1,v=v0+1;
            NotchGeometry.Box patch=d.getAxis()==Direction.Axis.X?new NotchGeometry.Box(b.x0(),v,u,b.x1(),v+2,u+3):
                d.getAxis()==Direction.Axis.Y?new NotchGeometry.Box(u,b.y0(),v,u+3,b.y1(),v+2):
                    new NotchGeometry.Box(u,v,b.z0(),u+3,v+2,b.z1());
            face(out,patch,d,mineral,.015f,true);
        }
        private void face(List<BakedQuad> out,NotchGeometry.Box b,Direction d,TextureAtlasSprite sprite,float offset,boolean patch){
            int x0=b.x0(),x1=b.x1(),y0=b.y0(),y1=b.y1(),z0=b.z0(),z1=b.z1();
            float[] uv=patch?new float[]{6,6,9,8}:switch(d){
                case NORTH->new float[]{16-x1,16-y1,16-x0,16-y0};
                case SOUTH->new float[]{x0,16-y1,x1,16-y0};
                case WEST->new float[]{z0,16-y1,z1,16-y0};
                case EAST->new float[]{16-z1,16-y1,16-z0,16-y0};
                case UP->new float[]{x0,z0,x1,z1};
                case DOWN->new float[]{x0,16-z1,x1,16-z0};
            };
            var f=new BlockElementFace(null,-1,"#surface",new BlockFaceUV(uv,0));
            float dx=d.getStepX()*offset,dy=d.getStepY()*offset,dz=d.getStepZ()*offset;
            out.add(bakery.bakeQuad(new Vector3f(x0+dx,y0+dy,z0+dz),new Vector3f(x1+dx,y1+dy,z1+dz),f,sprite,d,state,null,true));
        }
    }
}
