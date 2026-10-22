package dev.minemods.snowleopard.client;
import dev.minemods.snowleopard.SnowLeopardMod;
import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class SnowLeopardRenderer extends MobRenderer<SnowLeopardEntity, LivingEntityRenderState, SnowLeopardModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(SnowLeopardMod.MOD_ID, "textures/entity/snow_leopard.png");

    public SnowLeopardRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowLeopardModel(context.bakeLayer(SnowLeopardModel.LAYER)), 0.55f);
    }
    @Override public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
