package dev.fractal.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.fractal.block.HostBlock;
import dev.fractal.block.HostEntity;
import dev.fractal.cell.Preview;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.*;

public final class HostRenderer implements BlockEntityRenderer<HostEntity> {
 private final BlockRenderDispatcher blocks;
 public HostRenderer(BlockEntityRendererProvider.Context ctx){blocks=ctx.getBlockRenderDispatcher();}
 @Override public int getViewDistance(){return 16;}
 @Override public void render(HostEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!e.getBlockState().getValue(HostBlock.CUTAWAY))return;
  float scale=1f/23f;
  // Keep a readable minimum ambient light, even when installed underground.
  int readable=LightTexture.pack(Math.max(10,LightTexture.block(light)),Math.max(8,LightTexture.sky(light)));
  for(HostEntity.Tile t:e.visible()){
   pose.pushPose();
   pose.translate((t.x()+1)*scale,.125f+t.y()*scale,(t.z()+1)*scale);
   pose.scale(scale,scale,scale);
   blocks.renderSingleBlock(t.state(),pose,buffers,readable,overlay);
   pose.popPose();
  }
 }
}
