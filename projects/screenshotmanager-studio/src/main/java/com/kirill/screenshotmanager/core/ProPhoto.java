package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class ProPhoto {
    private ProPhoto(){}
    public static final int TONE_COUNT=17,EFFECT_COUNT=7;
    private static int clamp(int n,int min,int max){return Math.max(min,Math.min(max,n));}
    private static int byteValue(double v){return clamp((int)Math.round(v),0,255);}
    private static int rgb(int original,double r,double g,double b){return (original&0xff000000)|(byteValue(r)<<16)|(byteValue(g)<<8)|byteValue(b);}
    private static double luma(int c){return .2126*((c>>>16)&255)+.7152*((c>>>8)&255)+.0722*(c&255);}
    private static int[] validated(int[] input,int count){if(input.length!=count)throw new IllegalArgumentException("Invalid effect settings");int[] a=input.clone();for(int i=0;i<a.length;i++)a[i]=clamp(a[i],-100,100);return a;}
    public record Tone(int[] values) implements ImageDocument.Operation{
        public Tone{values=validated(values,TONE_COUNT);}
        public int[] values(){return values.clone();}
        public String label(){return "Tone / RGB curves";}
        public BufferedImage apply(BufferedImage img){
            int[] a=values;int[][] lut=new int[3][256];
            int black=clamp(a[3],0,100),white=255-clamp(a[4],0,100);
            double gamma=Math.pow(2,a[5]/100.0);
            for(int c=0;c<3;c++)for(int i=0;i<256;i++){
                double v=255*Math.pow(Math.max(0,Math.min(1,(i-black)/(double)(white-black))),1/gamma);
                v=curve(v,a[6],a[7],a[8]);v=curve(v,a[9+c*2],0,a[10+c*2]);
                lut[c][i]=byteValue(v);
            }
            int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
            for(int y=0;y<h;y++){
                img.getRGB(0,y,w,1,row,0,w);
                for(int x=0;x<w;x++){
                    int c=row[x];double v=luma(c)/255;
                    double delta=a[0]*.7*Math.pow(1-v,3)+a[1]*.7*4*v*(1-v)+a[2]*.7*v*v*v;
                    double r=lut[0][(c>>>16)&255]+delta,g=lut[1][(c>>>8)&255]+delta,b=lut[2][c&255]+delta;
                    double gray=.2126*r+.7152*g+.0722*b,max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));
                    double s=1+a[15]/100.0+a[16]/100.0*(1-Math.min(1,(max-min)/255));
                    row[x]=rgb(c,gray+(r-gray)*s,gray+(g-gray)*s,gray+(b-gray)*s);
                }
                img.setRGB(0,y,w,1,row,0,w);
            }return img;
        }
    }
    public static double curve(double v,int low,int mid,int high){
        double[] nodes={0,clamp(64+low,0,255),clamp(128+mid,0,255),clamp(192+high,0,255),255};
        // Keep the curve monotonic, even when the user crosses control points.
        for(int i=1;i<nodes.length;i++)nodes[i]=Math.max(nodes[i-1],nodes[i]);
        double x=Math.max(0,Math.min(255,v));int k=Math.min(3,(int)(x/64));double t=(x-k*64)/(k==3?63:64);
        return nodes[k]+(nodes[k+1]-nodes[k])*t;
    }
    public record Effects(int[] values) implements ImageDocument.Operation{
        public Effects{values=validated(values,EFFECT_COUNT);}
        public int[] values(){return values.clone();}
        public String label(){return "Cinematic effects";}
        public BufferedImage apply(BufferedImage img){
            int[] a=values;int w=img.getWidth(),h=img.getHeight();
            BufferedImage small=ImageDocument.resize(img,Math.max(1,Math.max(w,h)/12));
            int sw=small.getWidth(),sh=small.getHeight();
            if(a[0]>0){
                for(int y=0;y<sh;y++)for(int x=0;x<sw;x++){
                    int c=small.getRGB(x,y);double k=Math.max(0,(luma(c)-150)/105);
                    small.setRGB(x,y,rgb(c,((c>>>16)&255)*k,((c>>>8)&255)*k,(c&255)*k));
                }
            }
            BufferedImage src=(a[2]>0||a[4]>0)?ImageDocument.copy(img):img;
            int[] row=new int[w];int shift=Math.max(0,a[2])*Math.max(w,h)/45000;
            for(int y=0;y<h;y++){
                img.getRGB(0,y,w,1,row,0,w);
                for(int x=0;x<w;x++){
                    int c=row[x];double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
                    if(a[0]>0){int glow=small.getRGB(Math.min(sw-1,x*sw/w),Math.min(sh-1,y*sh/h));double t=a[0]/100.0*.65;
                        r+=((glow>>>16)&255)*t;g+=((glow>>>8)&255)*t;b+=(glow&255)*t;}
                    if(shift>0){r=(src.getRGB(clamp(x+shift,0,w-1),y)>>>16)&255;b=src.getRGB(clamp(x-shift,0,w-1),y)&255;}
                    double nx=2.0*x/Math.max(1,w-1)-1,ny=2.0*y/Math.max(1,h-1)-1;
                    double v=1-Math.max(0,a[3])/100.0*.7*Math.min(1,nx*nx+ny*ny);
                    int hash=x*374761393+y*668265263;hash=(hash^(hash>>>13))*1274126177;
                    double noise=((hash&255)/255.0-.5)*Math.max(0,a[1])*.65;
                    if(a[4]>0){
                        double edge=luma(src.getRGB(x,y))-(luma(src.getRGB(Math.max(0,x-1),y))+luma(src.getRGB(Math.min(w-1,x+1),y))+luma(src.getRGB(x,Math.max(0,y-1)))+luma(src.getRGB(x,Math.min(h-1,y+1))))/4;
                        r+=edge*a[4]/75.0;g+=edge*a[4]/75.0;b+=edge*a[4]/75.0;
                    }
                    double fog=Math.max(0,a[5])/100.0*.18;
                    row[x]=rgb(c,(r*v+noise)*(1-fog)+175*fog,(g*v+noise)*(1-fog)+192*fog,(b*v+noise)*(1-fog)+215*fog);
                    if(a[6]>0&&(y<h*a[6]/1000||y>=h-h*a[6]/1000))row[x]=0xff000000;
                }img.setRGB(0,y,w,1,row,0,w);
            }return img;
        }
    }
    public record ImageLayer(String name,byte[] png,int x,int y,int size,int rotation,int opacity) implements ImageDocument.Operation{
        public ImageLayer{
            if(png.length<8||png.length>1_500_000)throw new IllegalArgumentException("Image layer exceeds 1.5 MB");
            png=png.clone();size=clamp(size,10,200);rotation=clamp(rotation,-180,180);opacity=clamp(opacity,0,100);
            if(name.length()>128)name=name.substring(0,128);
        }
        public byte[] png(){return png.clone();}
        public String label(){return "Image: "+name;}
        public ImageLayer transform(int x,int y,int size,int rotation,int opacity){return new ImageLayer(name,png,x,y,size,rotation,opacity);}
        public static ImageLayer load(java.nio.file.Path path,int x,int y)throws java.io.IOException{
            BufferedImage image=ShotFiles.read(path,768);
            for(int side:new int[]{768,640,512,384,256}){
                var out=new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(ImageDocument.resize(image,side),"png",out);
                if(out.size()<=1_500_000)return new ImageLayer(path.getFileName().toString(),out.toByteArray(),x,y,100,0,100);
            }throw new java.io.IOException("Image is too large");
        }
        public BufferedImage apply(BufferedImage img){
            try(var in=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(png))){
                var it=javax.imageio.ImageIO.getImageReaders(in);if(!it.hasNext())throw new java.io.IOException("Invalid image layer");
                var reader=it.next();BufferedImage layer;
                try{reader.setInput(in,true,true);if(reader.getWidth(0)>768||reader.getHeight(0)>768)throw new java.io.IOException("Layer dimensions exceed limit");layer=reader.read(0);}
                finally{reader.dispose();}
                double k=Math.min(img.getWidth()*.5/layer.getWidth(),img.getHeight()*.5/layer.getHeight())*size/100.0;
                Graphics2D g=img.createGraphics();
                try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,opacity/100f));
                    g.translate(x,y);g.rotate(Math.toRadians(rotation));g.scale(k,k);g.drawImage(layer,-layer.getWidth()/2,-layer.getHeight()/2,null);
                }finally{g.dispose();}
                return img;
            }catch(java.io.IOException e){throw new IllegalStateException("Could not decode imported image",e);}
        }
    }
    /** Explicit image-space focus, not a depth-buffer effect. */
    public record Focus(int x,int y,int radius,int amount,boolean dark) implements ImageDocument.Operation {
        public Focus{radius=clamp(radius,5,100);amount=clamp(amount,0,100);}
        public String label(){return dark?"Smart focus / shade":"Smart focus / blur";}
        public BufferedImage apply(BufferedImage img){
            int w=img.getWidth(),h=img.getHeight();
            BufferedImage small=ImageDocument.resize(img,Math.max(1,Math.max(w,h)/Math.max(2,amount/5)));
            BufferedImage blurred=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=blurred.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(small,0,0,w,h,null);g.dispose();
            double r=Math.max(1,Math.min(w,h)*radius/100.0);
            for(int iy=0;iy<h;iy++)for(int ix=0;ix<w;ix++){
                double t=Math.max(0,Math.min(1,(Math.hypot(ix-(double)x,iy-(double)y)-r)/r));t=t*t*(3-2*t)*amount/100.0;
                int c=img.getRGB(ix,iy),d=dark?0xff000000:blurred.getRGB(ix,iy);
                img.setRGB(ix,iy,rgb(c,((c>>>16)&255)*(1-t)+((d>>>16)&255)*t,((c>>>8)&255)*(1-t)+((d>>>8)&255)*t,(c&255)*(1-t)+(d&255)*t));
            }return img;
        }
    }
}
