package dev.fractal.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.fractal.block.HostBlock;
import dev.fractal.block.HostEntity;
import dev.fractal.cell.Preview;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class HostRenderer implements BlockEntityRenderer<HostEntity> {
 private final BlockRenderDispatcher blocks;
 public HostRenderer(BlockEntityRendererProvider.Context ctx){blocks=ctx.getBlockRenderDispatcher();}
 @Override public int getViewDistance(){return 24;}
 @Override public void render(HostEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(!e.getBlockState().getValue(HostBlock.CUTAWAY))return;
  BlockState[] tiles=e.tiles();if(tiles.length!=Preview.VOLUME)return;
  float scale=1f/9f;
  for(int y=0;y<Preview.HEIGHT;y++)for(int z=0;z<Preview.WIDTH;z++)for(int x=0;x<Preview.WIDTH;x++){
   BlockState s=tiles[Preview.index(x,y,z)];if(s.isAir())continue;
   pose.pushPose();
   pose.translate((x+1)*scale,.25f+y*scale,(z+1)*scale);
   pose.scale(scale,scale,scale);
   blocks.renderSingleBlock(s,pose,buffers,light,overlay);
   pose.popPose();
  }
 }
}
