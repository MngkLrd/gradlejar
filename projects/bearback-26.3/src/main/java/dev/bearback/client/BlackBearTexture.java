package dev.bearback.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.InputStream;

public final class BlackBearTexture {
    private static final Identifier VANILLA=Identifier.withDefaultNamespace("textures/entity/bear/polarbear.png");
    private static Identifier cached;
    private BlackBearTexture(){}
    public static Identifier texture(){
        if(cached!=null)return cached;
        try(InputStream stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(VANILLA).open()){
            NativeImage image=NativeImage.read(stream);
            int sx=Math.max(1,image.getWidth()/128),sy=Math.max(1,image.getHeight()/64);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int abgr=image.getPixelRGBA(x,y),a=(abgr>>>24)&255;
                if(a==0)continue;
                int r=abgr&255,g=(abgr>>>8)&255,b=(abgr>>>16)&255;
                int luminance=(r*30+g*59+b*11)/100;
                int shade=Math.min(47,Math.max(12,12+luminance/9));
                int rr=shade,gg=shade+1,bb=shade+2;
                float u=(x+.5F)/sx,v=(y+.5F)/sy,dx=(u-27)/4.4F,dy=(v-29.6F)/8.8F;
                float distance=dx*dx+dy*dy;
                if(u>=22&&u<32&&v>=19&&v<41&&distance<1){
                    int variation=((x*17+y*13)%7)-3;
                    boolean edge=distance>.64;
                    rr=(edge?157:202)+variation;gg=(edge?136:178)+variation;bb=(edge?109:140)+variation;
                }
                image.setPixelRGBA(x,y,(a<<24)|(bb<<16)|(gg<<8)|rr);
            }
            cached=Minecraft.getInstance().getTextureManager().register("bearback_black_bear",new DynamicTexture(image));
        }catch(Exception ex){cached=VANILLA;}
        return cached;
    }
}
