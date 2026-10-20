package dev.timberfall.registry;
import dev.timberfall.Timberfall;
import dev.timberfall.entity.TopplingTreeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class TimberfallEntities {
    public static final DeferredRegister.Entities REGISTRY=DeferredRegister.createEntities(Timberfall.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<TopplingTreeEntity>> TOPPLING_TREE=
        REGISTRY.registerEntityType("toppling_tree",TopplingTreeEntity::new,MobCategory.MISC,
            builder -> builder.sized(1.0f,1.0f).clientTrackingRange(10).updateInterval(1));
}
