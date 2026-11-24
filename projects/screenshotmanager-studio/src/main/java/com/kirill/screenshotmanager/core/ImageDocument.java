package com.kirill.screenshotmanager.core;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Immutable operations in document-pixel coordinates. Original pixels are never modified. */
public final class ImageDocument {
    public enum Tool { PAN, CROP, RECT, ELLIPSE, ARROW, LINE, PEN, TEXT, HIGHLIGHT, PIXELATE, BLUR, REDACT, NUMBER, COPY_REGION }
    public enum Filter { BRIGHTER, DARKER, CONTRAST, SATURATION, MONO, WARM, VIGNETTE, ROTATE, FLIP }
    public record Mark(Tool tool, int x1, int y1, int x2, int y2, int color, int thickness, int strength, String text, List<Point> points) {
        public Mark { points=points==null?List.of():points.stream().map(Point::new).toList(); text=text==null?"":text; }
    }
    public interface Operation { BufferedImage apply(BufferedImage img); String label(); }
    private final BufferedImage original;
    private final ArrayList<List<Operation>> revisions=new ArrayList<>();
    private int cursor=0;
    public ImageDocument(BufferedImage image) { original=copy(image);revisions.add(List.of()); }
    public BufferedImage originalPreview(int max){return resize(copy(original),max);}
    public record Layer(Operation operation,boolean visible) implements Operation {
        public String label(){return operation.label();}
        public BufferedImage apply(BufferedImage img){return visible?operation.apply(img):img;}
    }
    public record MarkOperation(Mark value) implements Operation {
        public String label(){return value.tool().name();}
        public BufferedImage apply(BufferedImage img){return draw(img,value);}
    }
    public record FilterOperation(Filter value) implements Operation {
        public String label(){return value.name();}
        public BufferedImage apply(BufferedImage img){return adjust(img,value);}
    }
    public record Adjustment(int brightness,int contrast,int saturation,int warmth,int gamma,int vignette) implements Operation {
        public String label(){return "Color grade";}
        public BufferedImage apply(BufferedImage img){return grade(img,this);}
    }
    public static Operation unwrap(Operation op){return op instanceof Layer layer?layer.operation():op;}
    public static boolean visible(Operation op){return !(op instanceof Layer layer)||layer.visible();}
    // Geometry operations are fixed in the pipeline: moving/removing them would invalidate later coordinates.
    public static boolean geometry(Operation op){
        op=unwrap(op);
        return op instanceof MarkOperation m&&m.value().tool()==Tool.CROP
            ||op instanceof FilterOperation f&&(f.value()==Filter.ROTATE||f.value()==Filter.FLIP);
    }
    private boolean valid(int index){return index>=0&&index<count();}
    private boolean commit(List<Operation> next){
        revisions.subList(cursor+1,revisions.size()).clear();
        revisions.add(List.copyOf(next));cursor++;
        if(revisions.size()>101){revisions.removeFirst();cursor--;}
        return true;
    }
    public boolean toggle(int i){
        if(!valid(i)||geometry(snapshot().get(i)))return false;
        var next=new ArrayList<>(snapshot());Operation old=next.get(i);
        next.set(i,new Layer(unwrap(old),!visible(old)));return commit(next);
    }
    public boolean delete(int i){
        if(!valid(i)||geometry(snapshot().get(i)))return false;
        var next=new ArrayList<>(snapshot());next.remove(i);return commit(next);
    }
    public boolean move(int i,int direction){
        int to=i+direction;
        if(!valid(i)||!valid(to)||Math.abs(direction)!=1||geometry(snapshot().get(i))||geometry(snapshot().get(to)))return false;
        var next=new ArrayList<>(snapshot());java.util.Collections.swap(next,i,to);return commit(next);
    }
    public boolean replace(int i,Operation value){
        if(!valid(i)||geometry(snapshot().get(i))||geometry(value))return false;
        var next=new ArrayList<>(snapshot());next.set(i,new Layer(unwrap(value),visible(next.get(i))));return commit(next);
    }
    public boolean translate(int i,int dx,int dy){
        if(!valid(i)||!(unwrap(snapshot().get(i)) instanceof MarkOperation mark)||geometry(mark))return false;
        Mark m=mark.value();
        return replace(i,mark(new Mark(m.tool(),m.x1()+dx,m.y1()+dy,m.x2()+dx,m.y2()+dy,m.color(),m.thickness(),m.strength(),m.text(),m.points().stream().map(p->new Point(p.x+dx,p.y+dy)).toList())));
    }
    public static BufferedImage comparison(BufferedImage before,BufferedImage after){
        before=resize(before,1920);after=resize(after,1920);
        int panelW=Math.max(before.getWidth(),after.getWidth()),panelH=Math.max(before.getHeight(),after.getHeight());
        BufferedImage out=new BufferedImage(panelW*2+8,panelH+34,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();g.setColor(new Color(20,25,32));g.fillRect(0,0,out.getWidth(),out.getHeight());
        g.drawImage(before,(panelW-before.getWidth())/2,30+(panelH-before.getHeight())/2,null);
        g.drawImage(after,panelW+8+(panelW-after.getWidth())/2,30+(panelH-after.getHeight())/2,null);
        g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString("BEFORE",8,22);g.drawString("AFTER",panelW+16,22);g.dispose();return out;
    }
    public BufferedImage compare(BufferedImage after){return comparison(original,after);}
    public boolean canUndo(){return cursor>0;}
    public boolean canRedo(){return cursor+1<revisions.size();}
    public int count(){return snapshot().size();}
    public List<String> labels(){return snapshot().stream().map(Operation::label).toList();}
    public boolean add(Operation op){
        if(count()>=100)return false;
        var next=new ArrayList<>(snapshot());next.add(new Layer(op,true));return commit(next);
    }
    public void undo(){if(canUndo())cursor--;}
    public void redo(){if(canRedo())cursor++;}
    public void reset(){if(count()>0)commit(List.of());}
    public List<Operation> snapshot(){return revisions.get(cursor);}
    public BufferedImage render(List<Operation> ops){
        BufferedImage img=copy(original);
        for(Operation op:ops)img=op.apply(img);
        return img;
    }
    public static Operation mark(Mark m){return new MarkOperation(m);}
    public static Operation filter(Filter f){return new FilterOperation(f);}
    private static BufferedImage grade(BufferedImage img,Adjustment a){
        int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
        double contrast=Math.pow(2,a.contrast()/100.0),sat=Math.max(0,1+a.saturation()/100.0),gamma=Math.pow(2,a.gamma()/100.0);
        int[] lut=new int[256];
        for(int i=0;i<256;i++)lut[i]=clamp((int)Math.round(255*Math.pow(i/255.0,1/gamma)),0,255);
        for(int y=0;y<h;y++){
            img.getRGB(0,y,w,1,row,0,w);
            for(int x=0;x<w;x++){
                int c=row[x];double r=lut[(c>>16)&255],g=lut[(c>>8)&255],b=lut[c&255];
                double gray=.2126*r+.7152*g+.0722*b;
                r=gray+(r-gray)*sat;g=gray+(g-gray)*sat;b=gray+(b-gray)*sat;
                double dx=2.0*x/Math.max(1,w-1)-1,dy=2.0*y/Math.max(1,h-1)-1;
                double v=1-Math.max(0,a.vignette())/100.0*.65*Math.min(1,(dx*dx+dy*dy)/2);
                r=((r-128)*contrast+128+a.brightness()*.8+a.warmth()*.45)*v;
                g=((g-128)*contrast+128+a.brightness()*.8)*v;
                b=((b-128)*contrast+128+a.brightness()*.8-a.warmth()*.45)*v;
                row[x]=(c&0xff000000)|(clamp((int)Math.round(r),0,255)<<16)|(clamp((int)Math.round(g),0,255)<<8)|clamp((int)Math.round(b),0,255);
            }
            img.setRGB(0,y,w,1,row,0,w);
        }
        return img;
    }
    public static BufferedImage copy(BufferedImage img){
        BufferedImage out=new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.drawImage(img,0,0,null);g.dispose();return out;
    }
    public static BufferedImage crop(BufferedImage img,int x,int y,int w,int h){
        x=Math.max(0,Math.min(img.getWidth()-1,x));y=Math.max(0,Math.min(img.getHeight()-1,y));
        w=Math.max(1,Math.min(img.getWidth()-x,w));h=Math.max(1,Math.min(img.getHeight()-y,h));
        return copy(img.getSubimage(x,y,w,h));
    }
    public static BufferedImage resize(BufferedImage img,int max){
        double scale=Math.min(1.0,max/(double)Math.max(img.getWidth(),img.getHeight()));
        if(scale==1.0)return img;
        BufferedImage out=new BufferedImage(Math.max(1,(int)Math.round(img.getWidth()*scale)),Math.max(1,(int)Math.round(img.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img,0,0,out.getWidth(),out.getHeight(),null);g.dispose();return out;
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static BufferedImage draw(BufferedImage img,Mark m){
        int ax=clamp(m.x1(),0,img.getWidth()-1),ay=clamp(m.y1(),0,img.getHeight()-1);
        int bx=clamp(m.x2(),0,img.getWidth()-1),by=clamp(m.y2(),0,img.getHeight()-1);
        int x=Math.min(ax,bx),y=Math.min(ay,by),w=Math.max(1,Math.abs(bx-ax)+1),h=Math.max(1,Math.abs(by-ay)+1);
        int stroke=Math.max(1,m.thickness());
        if(m.tool()==Tool.CROP)return crop(img,x,y,w,h);
        if(m.tool()==Tool.PIXELATE||m.tool()==Tool.BLUR){
            int factor=Math.max(3,m.strength());
            BufferedImage small=new BufferedImage(Math.max(1,w/factor),Math.max(1,h/factor),BufferedImage.TYPE_INT_ARGB);
            Graphics2D down=small.createGraphics();down.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            down.drawImage(img,0,0,small.getWidth(),small.getHeight(),x,y,x+w,y+h,null);down.dispose();
            Graphics2D up=img.createGraphics();
            up.setComposite(AlphaComposite.Src);
            up.setRenderingHint(RenderingHints.KEY_INTERPOLATION,m.tool()==Tool.BLUR?RenderingHints.VALUE_INTERPOLATION_BILINEAR:RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            up.drawImage(small,x,y,w,h,null);up.dispose();return img;
        }
        Graphics2D g=img.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(m.color(),true));g.setStroke(new BasicStroke(stroke,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(m.tool()){
                case RECT -> g.drawRect(x,y,w-1,h-1);
                case ELLIPSE -> g.drawOval(x,y,w-1,h-1);
                case LINE -> g.drawLine(ax,ay,bx,by);
                case ARROW -> {
                    g.drawLine(ax,ay,bx,by);
                    double a=Math.atan2(by-ay,bx-ax),len=Math.max(12,stroke*4);
                    g.drawLine(bx,by,(int)(bx-len*Math.cos(a-0.52)),(int)(by-len*Math.sin(a-0.52)));
                    g.drawLine(bx,by,(int)(bx-len*Math.cos(a+0.52)),(int)(by-len*Math.sin(a+0.52)));
                }
                case PEN -> {
                    Path2D.Double p=new Path2D.Double();p.moveTo(ax,ay);
                    for(Point point:m.points()) p.lineTo(point.x,point.y);
                    p.lineTo(bx,by);g.draw(p);
                }
                case HIGHLIGHT -> {g.setColor(new Color((m.color()&0xffffff)|0x55000000,true));g.fillRect(x,y,w,h);}
                case REDACT -> {g.setComposite(AlphaComposite.Src);g.setColor(new Color(m.color()|0xff000000,true));g.fillRect(x,y,w,h);}
                case TEXT -> {
                    int size=Math.max(12,stroke*4);
                    g.setFont(new Font("SansSerif",Font.BOLD,size));g.setColor(new Color(0,0,0,190));g.drawString(m.text(),ax+2,ay+size+2);
                    g.setColor(new Color(m.color(),true));g.drawString(m.text(),ax,ay+size);
                }
                case NUMBER -> {
                    int d=Math.max(20,stroke*5);g.fillOval(ax-d/2,ay-d/2,d,d);
                    g.setFont(new Font("SansSerif",Font.BOLD,Math.max(12,d/2)));
                    FontMetrics fm=g.getFontMetrics();g.setColor(Color.WHITE);
                    g.drawString(m.text(),ax-fm.stringWidth(m.text())/2,ay+(fm.getAscent()-fm.getDescent())/2);
                }
                default -> {}
            }
        }finally{g.dispose();}
        return img;
    }
    private static BufferedImage adjust(BufferedImage img,Filter f){
        int w=img.getWidth(),h=img.getHeight();
        if(f==Filter.ROTATE||f==Filter.FLIP){
            BufferedImage out=new BufferedImage(f==Filter.ROTATE?h:w,f==Filter.ROTATE?w:h,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=out.createGraphics();
            if(f==Filter.ROTATE){g.translate(h,0);g.rotate(Math.PI/2);}
            else{g.translate(w,0);g.scale(-1,1);}
            g.drawImage(img,0,0,null);g.dispose();return out;
        }
        int[] row=new int[w];
        for(int y=0;y<h;y++){
            img.getRGB(0,y,w,1,row,0,w);
            for(int x=0;x<w;x++){
                int c=row[x],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
                double gray=0.2126*r+0.7152*g+0.0722*b;
                switch(f){
                    case BRIGHTER -> {r+=18;g+=18;b+=18;}
                    case DARKER -> {r-=18;g-=18;b-=18;}
                    case CONTRAST -> {r=(int)((r-128)*1.18+128);g=(int)((g-128)*1.18+128);b=(int)((b-128)*1.18+128);}
                    case SATURATION -> {r=(int)(gray+(r-gray)*1.25);g=(int)(gray+(g-gray)*1.25);b=(int)(gray+(b-gray)*1.25);}
                    case MONO -> {r=(int)gray;g=r;b=r;}
                    case WARM -> {r+=12;g+=3;b-=12;}
                    case VIGNETTE -> {
                        double dx=2.0*x/Math.max(1,w-1)-1,dy=2.0*y/Math.max(1,h-1)-1;
                        double k=1-0.32*Math.min(1,(dx*dx+dy*dy)/2);r=(int)(r*k);g=(int)(g*k);b=(int)(b*k);
                    }
                    default -> {}
                }
                row[x]=(c&0xff000000)|(clamp(r,0,255)<<16)|(clamp(g,0,255)<<8)|clamp(b,0,255);
            }
            img.setRGB(0,y,w,1,row,0,w);
        }
        return img;
    }
}
