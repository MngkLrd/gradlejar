package dev.timberfall.client;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import java.util.List;
public final class TopplingTreeRenderState extends EntityRenderState {
    public Direction direction=Direction.NORTH;
    public float age;
    public List<RenderedPiece> blocks=List.of();
    public record RenderedPiece(int x,int y,int z,BlockModelRenderState model){}
}
