package dev.timberfall.tree;
import dev.timberfall.entity.TreePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
public final class TreeScanner {
    public static final int MAX_HEIGHT=24, MAX_PARTS=130;
    public static List<TreePiece> scan(Level level,BlockPos root,Block original) {
        if(level.getBlockState(root.below()).is(original))return List.of();
        List<TreePiece> result=new ArrayList<>();
        BlockPos.MutableBlockPos probe=new BlockPos.MutableBlockPos();
        int height=0;
        for(;height<MAX_HEIGHT;height++){
            probe.set(root.getX(),root.getY()+height,root.getZ());
            if(height==0||level.getBlockState(probe).is(original))result.add(new TreePiece(0,height,0,original.defaultBlockState()));
            else break;
        }
        if(height<3||height==MAX_HEIGHT)return List.of();
        for(int y=Math.max(height-3,2);y<height+2;y++)
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                if(Math.abs(x)==2&&Math.abs(z)==2)continue;
                probe.set(root.getX()+x,root.getY()+y,root.getZ()+z);
                BlockState state=level.getBlockState(probe);
                if(state.is(BlockTags.LEAVES)&&result.size()<MAX_PARTS)result.add(new TreePiece(x,y,z,state));
            }
        return result;
    }
}
