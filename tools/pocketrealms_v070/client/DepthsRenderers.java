package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.entity.Knocker;
import com.kirill.pocketrealms.entity.CeilingLurker;
import com.kirill.pocketrealms.entity.ChoirWraith;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Client-only entity renderers; all three bake independent complex cuboid layer trees. */
public final class DepthsRenderers {
    private DepthsRenderers(){}
    public static final class KnockerRenderer extends MobRenderer<Knocker,DepthsModel<Knocker>> {
        private static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("pocketrealms","textures/entity/knocker.png");
        public KnockerRenderer(EntityRendererProvider.Context ctx){super(ctx,new DepthsModel<>(ctx.bakeLayer(DepthsModel.KNOCKER)),.8F);}
        @Override public ResourceLocation getTextureLocation(Knocker entity){return TEX;}
    }
    public static final class LurkerRenderer extends MobRenderer<CeilingLurker,DepthsModel<CeilingLurker>> {
        private static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("pocketrealms","textures/entity/ceiling_lurker.png");
        public LurkerRenderer(EntityRendererProvider.Context ctx){super(ctx,new DepthsModel<>(ctx.bakeLayer(DepthsModel.LURKER)),.3F);}
        @Override public ResourceLocation getTextureLocation(CeilingLurker entity){return TEX;}
    }
    public static final class ChoirRenderer extends MobRenderer<ChoirWraith,DepthsModel<ChoirWraith>> {
        private static final ResourceLocation TEX=ResourceLocation.fromNamespaceAndPath("pocketrealms","textures/entity/choir_wraith.png");
        public ChoirRenderer(EntityRendererProvider.Context ctx){super(ctx,new DepthsModel<>(ctx.bakeLayer(DepthsModel.CHOIR)),.35F);}
        @Override public ResourceLocation getTextureLocation(ChoirWraith entity){return TEX;}
    }
}
