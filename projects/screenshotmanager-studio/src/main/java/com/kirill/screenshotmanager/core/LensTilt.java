package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Multi-radius optical blur approximation. Focus geometry is expressed in normalized image coordinates. */
public record LensTilt(int[] values) implements ImageDocument.Operation{
    public static final int COUNT=9;
    // Position, width, angle, foreground, background, feather, saturation, clarity, highlight bloom.
    public LensTilt{if(values.length!=COUNT)throw new IllegalArgumentException("Invalid tilt");values=values.clone();for(int i=0;i<COUNT;i++)values[i]=Math.max(i==0||i==2||i==6?-100:0,Math.min(100,values[i]));}
    public int[] values(){return values.clone();}
    public static int[] miniature(){return new int[]{0,14,0,95,85,28,22,20,16};}
    public String label(){return "Lens Tilt Shift";}
    private static double unit(double v){return Math.max(0,Math.min(1,v));}
    private static double smooth(double v){v=unit(v);return v*v*(3-2*v);}
    private static int clip(double v){return (int)Math.round(Math.max(0,Math.min(255,v)));}
    public double axis(double x,double y){double a=Math.toRadians(values[2]*1.8);return (y-.5)*Math.cos(a)+(x-.5)*Math.sin(a)-values[0]/200.0;}
    public double band(){return .015+values[1]*.0027;}
    public double feather(){return .035+values[5]*.0032;}
    public double radius(double x,double y){double z=axis(x,y);return smooth((Math.abs(z)-band())/feather())*(z>=0?values[3]:values[4])/100.0;}
    private static BufferedImage down(BufferedImage img,int divisor){
        int w=Math.max(1,img.getWidth()/divisor),h=Math.max(1,img.getHeight()/divisor);
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(img,0,0,w,h,null);g.dispose();return out;
    }
    // Three box passes approximate a Gaussian. Edge samples clamp; dimensions never change.
    private static BufferedImage blur(BufferedImage img,int r){
        int w=img.getWidth(),h=img.getHeight();int[] a=img.getRGB(0,0,w,h,null,0,w),b=new int[a.length];
        for(int pass=0;pass<3;pass++){box(a,b,w,h,r,true);box(b,a,w,h,r,false);}
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);out.setRGB(0,0,w,h,a,0,w);return out;
    }
    private static void box(int[] a,int[] b,int w,int h,int r,boolean horizontal){
        int lines=horizontal?h:w,n=horizontal?w:h,step=horizontal?1:w,div=2*r+1;
        for(int l=0;l<lines;l++){
            int start=horizontal?l*w:l;int aa=0,rr=0,gg=0,bb=0;
            for(int j=-r;j<=r;j++){int c=a[start+Math.max(0,Math.min(n-1,j))*step];aa+=c>>>24;rr+=(c>>>16)&255;gg+=(c>>>8)&255;bb+=c&255;}
            for(int k=0;k<n;k++){
                b[start+k*step]=(aa/div<<24)|(rr/div<<16)|(gg/div<<8)|bb/div;
                int old=a[start+Math.max(0,k-r)*step],next=a[start+Math.min(n-1,k+r+1)*step];
                aa+=(next>>>24)-(old>>>24);rr+=((next>>>16)&255)-((old>>>16)&255);gg+=((next>>>8)&255)-((old>>>8)&255);bb+=(next&255)-(old&255);
            }
        }
    }
    private static double sample(BufferedImage img,double x,double y,int shift){
        double sx=unit(x)*(img.getWidth()-1),sy=unit(y)*(img.getHeight()-1);int ix=(int)sx,iy=(int)sy,jx=Math.min(img.getWidth()-1,ix+1),jy=Math.min(img.getHeight()-1,iy+1);
        double a=(img.getRGB(ix,iy)>>>shift)&255,b=(img.getRGB(jx,iy)>>>shift)&255,c=(img.getRGB(ix,jy)>>>shift)&255,d=(img.getRGB(jx,jy)>>>shift)&255;
        return (a+(b-a)*(sx-ix))*(1-(sy-iy))+(c+(d-c)*(sx-ix))*(sy-iy);
    }
    public BufferedImage apply(BufferedImage img){
        if(Arrays.stream(values).allMatch(v->v==0))return img;
        int w=img.getWidth(),h=img.getHeight();var src=ImageDocument.copy(img);
        // A pyramid of genuinely different blur radii, not opacity blending one blurred image.
        BufferedImage[] levels=new BufferedImage[6];levels[0]=src;
        for(int i=1;i<levels.length;i++){int d=1<<i;levels[i]=blur(down(src,d),Math.max(1,(int)Math.round(Math.min(w,h)*.0018)));}
        int[] row=new int[w];
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                double nx=x/(double)Math.max(1,w-1),ny=y/(double)Math.max(1,h-1),t=radius(nx,ny);
                double index=Math.pow(t,.7)*5;int lo=Math.min(4,(int)index),hi=lo+1;double frac=index-lo;
                int c=src.getRGB(x,y);double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
                if(t>0){r=sample(levels[lo],nx,ny,16)*(1-frac)+sample(levels[hi],nx,ny,16)*frac;g=sample(levels[lo],nx,ny,8)*(1-frac)+sample(levels[hi],nx,ny,8)*frac;b=sample(levels[lo],nx,ny,0)*(1-frac)+sample(levels[hi],nx,ny,0)*frac;}
                double gray=.2126*r+.7152*g+.0722*b,sat=1+values[6]/100.0;
                double clarity=1+values[7]/100.0*.24*(1-t);
                double glow=Math.max(0,gray-180)*values[8]/100.0*t*.28;
                row[x]=(c&0xff000000)|(clip((gray+(r-gray)*sat-128)*clarity+128+glow)<<16)|(clip((gray+(g-gray)*sat-128)*clarity+128+glow)<<8)|clip((gray+(b-gray)*sat-128)*clarity+128+glow);
            }img.setRGB(0,y,w,1,row,0,w);
        }return img;
    }
}
