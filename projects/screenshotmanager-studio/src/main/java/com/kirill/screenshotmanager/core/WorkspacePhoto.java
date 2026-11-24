package com.kirill.screenshotmanager.core;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class WorkspacePhoto {
    private WorkspacePhoto(){}
    public enum Blend { NORMAL,MULTIPLY,SCREEN,OVERLAY,SOFT_LIGHT,HARD_LIGHT,ADD,DARKEN,LIGHTEN,DIFFERENCE }
    private static double unit(double v){return Math.max(0,Math.min(1,v));}
    private static int clip(double v){return (int)Math.round(Math.max(0,Math.min(255,v)));}
    private static int rgb(int c,double r,double g,double b){return (c&0xff000000)|(clip(r)<<16)|(clip(g)<<8)|clip(b);}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    public static double blend(double a,double b,Blend mode){
        return switch(mode){
            case NORMAL->b;case MULTIPLY->a*b;case SCREEN->1-(1-a)*(1-b);
            case OVERLAY->a<=.5?2*a*b:1-2*(1-a)*(1-b);
            case HARD_LIGHT->b<=.5?2*a*b:1-2*(1-a)*(1-b);
            case SOFT_LIGHT->b<=.5?a-(1-2*b)*a*(1-a):a+(2*b-1)*(Math.sqrt(a)-a);
            case ADD->Math.min(1,a+b);case DARKEN->Math.min(a,b);case LIGHTEN->Math.max(a,b);case DIFFERENCE->Math.abs(a-b);
        };
    }
    public static BufferedImage composite(BufferedImage base,BufferedImage layer,int opacity,Blend mode){
        if(base.getWidth()!=layer.getWidth()||base.getHeight()!=layer.getHeight())throw new IllegalArgumentException("Layer size mismatch");
        double strength=unit(opacity/100.0);
        if(strength==0)return base;
        if(mode==Blend.NORMAL){Graphics2D g=base.createGraphics();g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)strength));g.drawImage(layer,0,0,null);g.dispose();return base;}
        int w=base.getWidth(),h=base.getHeight();int[] src=new int[w],dst=new int[w];
        for(int y=0;y<h;y++){
            base.getRGB(0,y,w,1,dst,0,w);layer.getRGB(0,y,w,1,src,0,w);
            for(int x=0;x<w;x++){
                int d=dst[x],s=src[x];double ab=(d>>>24)/255.0,as=(s>>>24)/255.0*strength,ao=as+ab*(1-as);
                if(as==0||ao==0)continue;
                int out=clip(ao*255)<<24;
                for(int shift=0;shift<=16;shift+=8){
                    double cb=((d>>>shift)&255)/255.0,cs=((s>>>shift)&255)/255.0;
                    double v=(as*(1-ab)*cs+as*ab*blend(cb,cs,mode)+(1-as)*ab*cb)/ao;
                    out|=clip(v*255)<<shift;
                }dst[x]=out;
            }base.setRGB(0,y,w,1,dst,0,w);
        }return base;
    }
    public static final int CURVE_POINTS=9;
    public static int[][] identity(){
        int[][] a=new int[4][CURVE_POINTS];for(int[] c:a)for(int i=0;i<c.length;i++)c[i]=(int)Math.round(i*255.0/(c.length-1));return a;
    }
    /** Cubic Hermite interpolation, with local tangents constrained to avoid overshoot. */
    public static double curve(int[] nodes,double value){
        double p=unit(value/255)*(nodes.length-1);int i=Math.min(nodes.length-2,(int)p);double t=p-i;
        double a=nodes[i],b=nodes[i+1],d=b-a;
        double left=i==0?d:(b-nodes[i-1])*.5,right=i+2==nodes.length?d:(nodes[i+2]-a)*.5;
        if(d==0){left=right=0;}else{left=Math.copySign(Math.min(Math.abs(d)*2,Math.max(0,left*Math.signum(d))),d);right=Math.copySign(Math.min(Math.abs(d)*2,Math.max(0,right*Math.signum(d))),d);}
        double v=(2*t*t*t-3*t*t+1)*a+(t*t*t-2*t*t+t)*left+(-2*t*t*t+3*t*t)*b+(t*t*t-t*t)*right;
        return Math.max(Math.min(a,b),Math.min(Math.max(a,b),v));
    }
    public record Curves(int[][] nodes) implements ImageDocument.Operation{
        public Curves{if(nodes.length!=4)throw new IllegalArgumentException("Four curves required");nodes=Arrays.stream(nodes).map(a->{if(a.length!=CURVE_POINTS)throw new IllegalArgumentException("Bad curve");return Arrays.stream(a).map(v->clip(v)).toArray();}).toArray(int[][]::new);}
        public int[][] nodes(){return Arrays.stream(nodes).map(int[]::clone).toArray(int[][]::new);}
        public String label(){return "RGB curves";}
        public BufferedImage apply(BufferedImage img){
            int[][] lut=new int[3][256];
            for(int c=0;c<3;c++)for(int i=0;i<256;i++)lut[c][i]=clip(curve(nodes[c+1],curve(nodes[0],i)));
            int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
            for(int y=0;y<h;y++){img.getRGB(0,y,w,1,row,0,w);for(int x=0;x<w;x++){int a=row[x];row[x]=(a&0xff000000)|(lut[0][(a>>>16)&255]<<16)|(lut[1][(a>>>8)&255]<<8)|lut[2][a&255];}img.setRGB(0,y,w,1,row,0,w);}
            return img;
        }
    }
    public static int[][] histogram(BufferedImage img){
        int[][] counts=new int[4][256];int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
        for(int y=0;y<h;y++){img.getRGB(0,y,w,1,row,0,w);for(int c:row){if((c>>>24)==0)continue;int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;counts[0][clip(.2126*r+.7152*g+.0722*b)]++;counts[1][r]++;counts[2][g]++;counts[3][b]++;}}
        return counts;
    }
    public record Wheels(int[] values) implements ImageDocument.Operation{
        public Wheels{if(values.length!=9)throw new IllegalArgumentException("Three wheels required");values=Arrays.stream(values).map(v->Math.max(-100,Math.min(100,v))).toArray();}
        public int[] values(){return values.clone();}
        public String label(){return "Three-way color grading";}
        public BufferedImage apply(BufferedImage img){
            double[][] tint=new double[3][3];
            for(int i=0;i<3;i++){double x=values[3*i]/100.0,y=values[3*i+1]/100.0,s=Math.min(1,Math.hypot(x,y));
                int c=Color.HSBtoRGB((float)((Math.atan2(y,x)/(Math.PI*2)+1)%1),(float)s,1);
                double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255,l=.2126*r+.7152*g+.0722*b;
                tint[i][0]=(r-l)*.48;tint[i][1]=(g-l)*.48;tint[i][2]=(b-l)*.48;
            }
            int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
            for(int y=0;y<h;y++){img.getRGB(0,y,w,1,row,0,w);for(int x=0;x<w;x++){
                int c=row[x];double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255,l=(.2126*r+.7152*g+.0722*b)/255;
                double sh=(1-l)*(1-l),mi=2*l*(1-l),hi=l*l;
                double exp=(sh*values[2]+mi*values[5]+hi*values[8])*.8;
                row[x]=rgb(c,r+sh*tint[0][0]+mi*tint[1][0]+hi*tint[2][0]+exp,g+sh*tint[0][1]+mi*tint[1][1]+hi*tint[2][1]+exp,b+sh*tint[0][2]+mi*tint[1][2]+hi*tint[2][2]+exp);
            }img.setRGB(0,y,w,1,row,0,w);}return img;
        }
    }
    /** Projective correction around image center; transparent pixels outside the source. */
    public record Perspective(int horizontal,int vertical) implements ImageDocument.Operation{
        public Perspective{horizontal=Math.max(-100,Math.min(100,horizontal));vertical=Math.max(-100,Math.min(100,vertical));}
        public String label(){return "Perspective";}
        public BufferedImage apply(BufferedImage img){
            if(horizontal==0&&vertical==0)return img;
            int w=img.getWidth(),h=img.getHeight();var out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);int[] row=new int[w];
            double ax=horizontal*.004,ay=vertical*.004;
            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    double u=2*x/(double)Math.max(1,w-1)-1,v=2*y/(double)Math.max(1,h-1)-1,den=1+ax*u+ay*v;
                    double sx=(u/den+1)*.5*(w-1),sy=(v/den+1)*.5*(h-1);
                    if(sx<0||sy<0||sx>w-1||sy>h-1){row[x]=0;continue;}
                    int x0=(int)sx,y0=(int)sy,x1=Math.min(w-1,x0+1),y1=Math.min(h-1,y0+1);
                    int a=img.getRGB(x0,y0),b=img.getRGB(x1,y0),c=img.getRGB(x0,y1),d=img.getRGB(x1,y1),pixel=0;
                    for(int k=0;k<=24;k+=8)pixel|=clip(mix(mix((a>>>k)&255,(b>>>k)&255,sx-x0),mix((c>>>k)&255,(d>>>k)&255,sx-x0),sy-y0))<<k;
                    row[x]=pixel;
                }out.setRGB(0,y,w,1,row,0,w);
            }return out;
        }
    }
    public record Text(String text,String family,int x,int y,int size,int color,int style,int angle,int fontStyle) implements ImageDocument.Operation{
        public Text{if(text.length()>500)text=text.substring(0,500);if(family.length()>128)family=family.substring(0,128);size=Math.max(8,Math.min(512,size));style=Math.max(0,Math.min(3,style));angle=Math.max(-180,Math.min(180,angle));fontStyle=Math.max(0,Math.min(3,fontStyle));}
        public Text move(int dx,int dy){return new Text(text,family,x+dx,y+dy,size,color,style,angle,fontStyle);}
        public String label(){return "Text: "+text.replace('\n',' ');}
        public BufferedImage apply(BufferedImage img){
            Graphics2D g=img.createGraphics();
            try{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.translate(x,y);g.rotate(Math.toRadians(angle));g.setFont(new Font(family,fontStyle,size));
                int baseline=0;
                for(String line:text.split("\\n",-1)){
                    Shape glyph=g.getFont().createGlyphVector(g.getFontRenderContext(),line).getOutline(0,baseline);
                    if(style==1){g.setColor(new Color(0,0,0,230));g.setStroke(new BasicStroke(Math.max(1,size*.09f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(glyph);}
                    if(style==2){g.setColor(new Color(0,0,0,180));g.fill(AffineTransform.getTranslateInstance(size*.07,size*.07).createTransformedShape(glyph));}
                    if(style==3){Rectangle r=glyph.getBounds();g.setColor(new Color(0,0,0,180));g.fillRoundRect(r.x-8,r.y-5,r.width+16,r.height+10,8,8);}
                    g.setColor(new Color(color,true));g.fill(glyph);baseline+=(int)(size*1.25);
                }
            }finally{g.dispose();}return img;
        }
    }
    public static int hex(String value){String s=value.strip();if(s.startsWith("#"))s=s.substring(1);if(!s.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Use six HEX digits");return 0xff000000|Integer.parseInt(s,16);}
}
