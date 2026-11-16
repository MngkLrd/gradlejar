package dev.bearback.client;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
public class BearPolarRenderer extends PolarBearRenderer {
    public BearPolarRenderer(EntityRendererProvider.Context context){
        super(context);
        this.addLayer(new SaddleLayer(this));
        this.addLayer(new SaddlebagsLayer(this));
    }
}
