package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public final class DepthMap {
    private final int width,height;
    private final short[] data;
    public DepthMap(int width,int height,short[] data){
        if(width<1||height<1||width>640||height>640||(long)width*height!=data.length)throw new IllegalArgumentException("Invalid depth map");
        this.width=width;this.height=height;this.data=data.clone();
    }
    private static int clamp(int i,int a,int b){return Math.max(a,Math.min(b,i));}
    public int valueAt(double x,double y){return data[clamp((int)Math.round(y*(height-1)),0,height-1)*width+clamp((int)Math.round(x*(width-1)),0,width-1)]&65535;}
    public void write(DataOutput out)throws IOException{out.writeInt(width);out.writeInt(height);for(short v:data)out.writeShort(v);}
    public static DepthMap read(DataInput in)throws IOException{
        int w=in.readInt(),h=in.readInt();if(w<1||h<1||w>640||h>640)throw new IOException("Invalid depth dimensions");
        short[] d=new short[w*h];for(int i=0;i<d.length;i++)d[i]=in.readShort();return new DepthMap(w,h,d);
    }
    private static Path sidecar(Path p){return p.resolveSibling(p.getFileName()+".smdepth");}
    public void save(Path p)throws IOException{
        Path target=sidecar(p),tmp=Files.createTempFile(target.getParent(),"depth-",".tmp");
        try{try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp)))){out.writeUTF("SMDEPTH1");write(out);}
            try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
    public static DepthMap load(Path p)throws IOException{
        Path source=sidecar(p);if(Files.size(source)>1_000_000)throw new IOException("Invalid depth file size");
        try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(source)))){if(!in.readUTF().equals("SMDEPTH1"))throw new IOException("Invalid depth map");return read(in);}
    }
    public record Focus(DepthMap map,int distance,int range,int amount) implements ImageDocument.Operation{
        public Focus{distance=clamp(distance,0,65535);range=clamp(range,5,100);amount=clamp(amount,0,100);}
        public String label(){return "Depth of field";}
        public BufferedImage apply(BufferedImage img){
            if(amount==0)return img;
            int w=img.getWidth(),h=img.getHeight();
            BufferedImage blurred=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            BufferedImage low=ImageDocument.resize(img,Math.max(1,Math.max(w,h)/Math.max(2,amount/5)));
            Graphics2D g=blurred.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(low,0,0,w,h,null);g.dispose();
            double tolerance=range*180.0;
            int[] src=new int[w],dst=new int[w];
            for(int y=0;y<h;y++){
                img.getRGB(0,y,w,1,src,0,w);blurred.getRGB(0,y,w,1,dst,0,w);
                for(int x=0;x<w;x++){
                    double d=Math.abs(map.valueAt(x/(double)Math.max(1,w-1),y/(double)Math.max(1,h-1))-distance);
                    double t=Math.max(0,Math.min(1,(d-tolerance)/(Math.max(1,tolerance*2))));t=t*t*(3-2*t)*amount/100.0;
                    int a=src[x],b=dst[x];
                    int r=(int)Math.round(((a>>>16)&255)*(1-t)+((b>>>16)&255)*t),gr=(int)Math.round(((a>>>8)&255)*(1-t)+((b>>>8)&255)*t),bl=(int)Math.round((a&255)*(1-t)+(b&255)*t);
                    src[x]=(a&0xff000000)|(r<<16)|(gr<<8)|bl;
                }img.setRGB(0,y,w,1,src,0,w);
            }return img;
        }
    }
}
