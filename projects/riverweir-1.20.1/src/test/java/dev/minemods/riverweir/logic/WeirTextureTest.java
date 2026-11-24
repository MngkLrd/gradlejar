package dev.minemods.riverweir.logic;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;

final class WeirTextureTest {
    private static final String ROOT="/assets/riverweir/textures/";
    private static BufferedImage load(String name,int width,int height) throws IOException {
        try(InputStream in=WeirTextureTest.class.getResourceAsStream(ROOT+name+".png")){
            if(in==null)throw new AssertionError("Missing texture "+name);
            BufferedImage im=ImageIO.read(in);
            if(im==null||im.getWidth()!=width||im.getHeight()!=height)
                throw new AssertionError("Invalid texture dimensions: "+name);
            return im;
        }
    }
    private static void compare(BufferedImage base,BufferedImage other,String name){
        boolean changed=false;
        for(int y=0;y<base.getHeight();y++)for(int x=0;x<base.getWidth();x++){
            int a=base.getRGB(x,y),b=other.getRGB(x,y);
            if((a>>>24)!=(b>>>24))throw new AssertionError("Changed silhouette: "+name);
            if((a>>>24)!=0&&a!=b)changed=true;
        }
        if(!changed)throw new AssertionError("Identical palette: "+name);
    }
    static void run(){
        try{
            BufferedImage roll=load("item/net_roll",32,32);
            compare(roll,load("item/bronze_roll",32,32),"bronze roll");
            compare(roll,load("item/iron_roll",32,32),"iron roll");
            for(String stage:new String[]{"intact","frayed","torn"}){
                BufferedImage mesh=load("block/net_"+stage,16,64);
                for(String tier:new String[]{"bronze","iron"}){
                    String path="block/"+tier+"_"+stage;
                    compare(mesh,load(path,16,64),path);
                    try(InputStream in=WeirTextureTest.class.getResourceAsStream(ROOT+path+".png.mcmeta")){
                        if(in==null)throw new AssertionError("Missing animation metadata: "+path);
                    }
                }
            }
        }catch(IOException e){throw new AssertionError("Invalid PNG asset",e);}
    }
}
