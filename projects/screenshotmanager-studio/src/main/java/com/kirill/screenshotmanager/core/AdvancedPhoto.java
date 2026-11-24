package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** All adjustment coordinates are normalized; previews and full-resolution exports use identical parameters. */
public final class AdvancedPhoto {
    private AdvancedPhoto(){}
    public enum Kind {
        HSL(18), WHEELS(6), TILT(6), DETAIL(4), GRADIENT(5);
        public final int count;Kind(int n){count=n;}
    }
    private static int clip(double v){return (int)Math.round(Math.max(0,Math.min(255,v)));}
    private static double unit(double v){return Math.max(0,Math.min(1,v));}
    private static double smooth(double v){v=unit(v);return v*v*(3-2*v);}
    private static int rgb(int c,double r,double g,double b){return (c&0xff000000)|(clip(r)<<16)|(clip(g)<<8)|clip(b);}
    private static double lum(int c){return .2126*((c>>>16)&255)+.7152*((c>>>8)&255)+.0722*(c&255);}
    public record Adjust(Kind kind,int[] values) implements ImageDocument.Operation{
        public Adjust{if(values.length!=kind.count)throw new IllegalArgumentException("Invalid adjustment");values=values.clone();for(int i=0;i<values.length;i++)values[i]=Math.max(-100,Math.min(100,values[i]));}
        public int[] values(){return values.clone();}
        public String label(){return switch(kind){case HSL->"HSL mixer";case WHEELS->"Color wheels";case TILT->"Tilt Shift";case DETAIL->"Clarity / Dehaze";case GRADIENT->"Gradient filter";};}
        public BufferedImage apply(BufferedImage img){
            int w=img.getWidth(),h=img.getHeight();int[] a=values;
            if(Arrays.stream(a).allMatch(v->v==0))return img;
            BufferedImage blur=null;
            if(kind==Kind.TILT||kind==Kind.DETAIL){
                double strength=kind==Kind.TILT?Math.max(0,a[3]):Math.abs(a[0]);
                if(strength>0)blur=softBlur(img,kind==Kind.TILT?Math.max(2,(int)(2+strength*.28)):Math.max(2,(int)(2+strength*.06)));
            }
            double angle=Math.toRadians(kind==Kind.TILT?a[2]*1.8:kind==Kind.GRADIENT?a[1]*1.8:0);
            double sin=Math.sin(angle),cos=Math.cos(angle);
            int[] row=new int[w],other=blur==null?null:new int[w];float[] hsv=new float[3];
            for(int y=0;y<h;y++){
                img.getRGB(0,y,w,1,row,0,w);if(blur!=null)blur.getRGB(0,y,w,1,other,0,w);
                for(int x=0;x<w;x++){
                    int c=row[x];double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
                    double nx=(x+.5)/w-.5,ny=(y+.5)/h-.5,v=lum(c)/255;
                    switch(kind){
                        case TILT -> {
                            double axis=ny*cos+nx*sin-a[0]/200.0;
                            double band=.025+Math.max(0,a[1])*.0035,feather=.035+Math.max(0,a[4])*.004;
                            double t=smooth((Math.abs(axis)-band)/feather)*Math.max(0,a[3])/100.0;
                            if(other!=null){int d=other[x];r=r*(1-t)+((d>>>16)&255)*t;g=g*(1-t)+((d>>>8)&255)*t;b=b*(1-t)+(d&255)*t;}
                            double gray=.2126*r+.7152*g+.0722*b,s=1+a[5]/100.0;
                            r=gray+(r-gray)*s;g=gray+(g-gray)*s;b=gray+(b-gray)*s;
                        }
                        case HSL -> {
                            Color.RGBtoHSB((int)r,(int)g,(int)b,hsv);
                            double sector=hsv[0]*6;int first=(int)sector%6,next=(first+1)%6;double t=sector-Math.floor(sector);
                            double hue=(a[first*3]*(1-t)+a[next*3]*t)/600.0;
                            double sat=(a[first*3+1]*(1-t)+a[next*3+1]*t)/100.0;
                            double light=(a[first*3+2]*(1-t)+a[next*3+2]*t)/100.0;
                            if(hsv[1]>.025){int d=Color.HSBtoRGB((float)((hsv[0]+hue+1)%1),(float)unit(hsv[1]*(1+sat)),(float)unit(hsv[2]+light*.4));r=(d>>>16)&255;g=(d>>>8)&255;b=d&255;}
                        }
                        case WHEELS -> {
                            double shadows=(1-v)*(1-v),mid=4*v*(1-v),high=v*v;
                            double temp=(shadows*a[0]+mid*a[2]+high*a[4])*.45,tint=(shadows*a[1]+mid*a[3]+high*a[5])*.35;
                            r+=temp+tint*.5;g-=tint;b-=temp-tint*.5;
                        }
                        case DETAIL -> {
                            double edge=other==null?0:(lum(c)-lum(other[x]))*a[0]/80.0;
                            double haze=a[1]/100.0,contrast=1+haze*.55;
                            r=(r-128)*contrast+128+edge-haze*10;g=(g-128)*contrast+128+edge-haze*10;b=(b-128)*contrast+128+edge-haze*10;
                            double gray=.2126*r+.7152*g+.0722*b,sat=1+a[2]/100.0;
                            r=gray+(r-gray)*sat+a[3]*.5;g=gray+(g-gray)*sat+a[3]*.5;b=gray+(b-gray)*sat+a[3]*.5;
                        }
                        case GRADIENT -> {
                            double axis=ny*cos+nx*sin-a[0]/200.0;
                            double t=smooth(.5+axis/(.08+Math.max(0,a[2])*.009));
                            r+=t*(a[3]*.9+a[4]*.5);g+=t*a[3]*.9;b+=t*(a[3]*.9-a[4]*.5);
                        }
                    }row[x]=rgb(c,r,g,b);
                }img.setRGB(0,y,w,1,row,0,w);
            }return img;
        }
    }
    private static BufferedImage softBlur(BufferedImage img,int divisor){
        int w=img.getWidth(),h=img.getHeight();
        var low=ImageDocument.resize(img,Math.max(1,Math.max(w,h)/divisor));
        var out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(low,0,0,w,h,null);g.dispose();return out;
    }
    public record Canvas(int width,int height,boolean cover) implements ImageDocument.Operation{
        public Canvas{if(width<64||height<64||width>4096||height>4096||(long)width*height>16_000_000)throw new IllegalArgumentException("Canvas size outside limits");}
        public String label(){return "Canvas "+width+" x "+height;}
        public BufferedImage apply(BufferedImage img){
            var out=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=out.createGraphics();double k=cover?Math.max(width/(double)img.getWidth(),height/(double)img.getHeight()):Math.min(width/(double)img.getWidth(),height/(double)img.getHeight());
            int w=Math.max(1,(int)Math.round(img.getWidth()*k)),h=Math.max(1,(int)Math.round(img.getHeight()*k));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(img,(width-w)/2,(height-h)/2,w,h,null);g.dispose();return out;
        }
    }
    public record Caption(String text,int x,int y,int size,int color,int style,int angle) implements ImageDocument.Operation{
        public Caption{if(text.length()>500)text=text.substring(0,500);size=Math.max(8,Math.min(512,size));style=Math.max(0,Math.min(5,style));angle=Math.max(-180,Math.min(180,angle));}
        public String label(){return "Title: "+text.replace('\n',' ');}
        public BufferedImage apply(BufferedImage img){
            Graphics2D g=img.createGraphics();
            try{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.translate(x,y);g.rotate(Math.toRadians(angle));
                g.setFont(new Font(style==5?"Monospaced":"SansSerif",style==4?Font.BOLD|Font.ITALIC:Font.BOLD,size));
                int baseline=0;
                for(String line:text.split("\\n",-1)){
                    var glyph=g.getFont().createGlyphVector(g.getFontRenderContext(),line);Shape shape=glyph.getOutline(0,baseline);
                    if(style==3){Rectangle b=shape.getBounds();g.setColor(new Color(0,0,0,175));g.fillRoundRect(b.x-8,b.y-5,b.width+16,b.height+10,8,8);}
                    if(style==2){g.setColor(new Color(0,0,0,180));g.fill(AffineTransform.getTranslateInstance(size*.06,size*.07).createTransformedShape(shape));}
                    if(style==1){g.setColor(new Color(0,0,0,230));g.setStroke(new BasicStroke(Math.max(1,size*.09f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(shape);}
                    g.setColor(new Color(color,true));g.fill(shape);baseline+=(int)(size*1.25);
                }
            }finally{g.dispose();}return img;
        }
    }
}
