package com.kirill.lurelantern.client;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;

/** Render-state-based preview, client-only and never adds an entity to the world. */
public final class CapturedMobRenderer implements BlockEntityRenderer<LureLanternBlockEntity,CapturedMobRenderer.State> {
  public static final class State extends BlockEntityRenderState {
    public EntityRenderState displayEntity;
    public float scale,scaledHeight,spin;
    public boolean hanging;
  }
  private record Snapshot(int hash,Level level,Entity entity){}
  private final Map<LureLanternBlockEntity,Snapshot> cache=new WeakHashMap<>();
  private final EntityRenderDispatcher dispatcher;
  public CapturedMobRenderer(BlockEntityRendererProvider.Context context){
    dispatcher=context.entityRenderer();
  }
  @Override public State createRenderState(){return new State();}
  @Override public void extractRenderState(LureLanternBlockEntity be,State state,float partial,Vec3 camera,
       ModelFeatureRenderer.CrumblingOverlay breaking){
    BlockEntityRenderer.super.extractRenderState(be,state,partial,camera,breaking);
    state.displayEntity=null;
    Level level=be.getLevel();
    CompoundTag tag=be.previewMobTag();
    if(level==null||tag==null){cache.remove(be);return;}
    int hash=tag.hashCode();
    Snapshot snapshot=cache.get(be);
    if(snapshot==null||snapshot.hash()!=hash||snapshot.level()!=level){
      try{
        Identifier id=Identifier.parse(tag.getStringOr("id","minecraft:pig"));
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(id).map(net.minecraft.core.Holder::value).orElse(null);
        if(type==null){cache.remove(be);return;}
        Entity visual=EntityType.loadEntityRecursive(type,tag.copy(),level,EntitySpawnReason.LOAD,e->e);
        if(visual==null){cache.remove(be);return;}
        visual.setNoGravity(true);
        snapshot=new Snapshot(hash,level,visual);
        cache.put(be,snapshot);
      }catch(RuntimeException err){cache.remove(be);return;}
    }
    try{
      Entity visual=snapshot.entity();
      visual.snapTo(Vec3.atCenterOf(be.getBlockPos()),visual.getYRot(),visual.getXRot());
      state.displayEntity=dispatcher.extractEntity(visual,partial);
      float width=Math.max(.05f,visual.getBbWidth());
      float height=Math.max(.05f,visual.getBbHeight());
      state.scale=Math.min(.75f,Math.min(.28f/width,.28f/height));
      state.scaledHeight=height*state.scale;
      state.hanging=be.getBlockState().getValue(LanternBlock.HANGING);
      state.spin=(level.getGameTime()+partial)*2.0f;
    }catch(RuntimeException e){state.displayEntity=null;}
  }
  @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
    if(state.displayEntity==null)return;
    pose.pushPose();
    try{
      pose.translate(.5,state.hanging?.375:.3125,.5);
      pose.rotateDegrees(Axis.YP,state.spin);
      pose.translate(0,-state.scaledHeight*.5,0);
      pose.scale(state.scale,state.scale,state.scale);
      dispatcher.submit(state.displayEntity,camera,0,0,0,pose,collector);
    }finally{pose.popPose();}
  }
}
