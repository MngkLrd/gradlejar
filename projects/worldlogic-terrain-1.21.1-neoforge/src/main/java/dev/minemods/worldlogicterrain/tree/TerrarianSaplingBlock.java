package dev.minemods.worldlogicterrain.tree;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

public final class TerrarianSaplingBlock extends SaplingBlock {
    public TerrarianSaplingBlock(String species,Properties properties) {
        super(new TreeGrower("worldlogicterrain:"+species,Optional.empty(),
          Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+(species+"_young")))),Optional.empty()),properties);
    }
}
