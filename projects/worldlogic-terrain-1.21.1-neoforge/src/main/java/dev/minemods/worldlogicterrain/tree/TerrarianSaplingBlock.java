package dev.minemods.worldlogicterrain.tree;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

public final class TerrarianSaplingBlock extends SaplingBlock {
    private final boolean sand;
    public TerrarianSaplingBlock(String species,Properties properties) {
        super(new TreeGrower("worldlogicterrain:"+species,Optional.empty(),
          Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+(species+"_young")))),Optional.empty()),properties);
        sand=species.equals("palm")||species.equals("date_palm");
    }
    @Override protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState floor,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos){
        return super.mayPlaceOn(floor,level,pos)||(sand&&floor.is(net.minecraft.tags.BlockTags.SAND));
    }
}
