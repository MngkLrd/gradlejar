package dev.bearback.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.world.entity.animal.polarbear.PolarBear;

public class BearPolarRenderer extends PolarBearRenderer {
    public BearPolarRenderer(EntityRendererProvider.Context ctx){
        super(ctx);
        this.addLayer(new SaddleLayer(this));
        this.addLayer(new SaddlebagsLayer(this));
    }
    @Override public PolarBearRenderState createRenderState(){return new BearRenderState();}
    @Override public void extractRenderState(PolarBear bear,PolarBearRenderState state,float partial){
        super.extractRenderState(bear,state,partial);
        if(state instanceof BearRenderState custom)
            {custom.bearbackSaddled=ClientHooks.isSaddled(bear);custom.bearbackBags=ClientHooks.hasBags(bear);}
    }
}
