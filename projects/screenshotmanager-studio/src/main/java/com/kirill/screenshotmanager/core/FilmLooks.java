package com.kirill.screenshotmanager.core;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Deterministic screen-space looks. No external textures, native codecs or shader dependency. */
public final class FilmLooks{
    private FilmLooks(){}
    public static final int COUNT=5;
    public enum Profile{
        VHS("VHS 1989",false,0,"Tape bleed / tracking / scanlines"),
        CRT("CRT monitor",false,0,"RGB mask / scanlines"),
        FILM("35mm film",false,0,"Warm film / grain"),
        SUPER8("Super 8",false,0,"Faded film / dust"),
        NOIR("Film noir",false,0,"Monochrome / deep shadows"),
        POLAROID("Instant film",false,0,"Faded colors / warm whites"),
        NIGHTVISION("Night vision",false,0,"Green phosphor / noise"),
        CYBER("Cyberpunk",false,0,"Cool shadows / warm highlights"),
        LANDSCAPE("Landscape",true,53,"24mm wide / f/8-f/11 reference"),
        ARCHITECTURE("Architecture",true,45,"28mm wide / f/8-f/11 reference"),
        STREET("Street",true,38,"35mm / f/5.6-f/8 reference"),
        PORTRAIT("Portrait",true,30,"Narrow framing / f/2-f/4 reference"),
        PRODUCT("Product",true,38,"Neutral colors / f/8-f/11 reference"),
        GOLDEN("Golden hour",true,45,"Warm light / soft contrast"),
        NIGHT("Night",true,45,"Protected highlights / cool shadows"),
        DOCUMENTARY("Documentary",true,38,"Neutral tone / natural contrast");
        public final String title,hint;public final boolean camera;public final int fov;
        Profile(String title,boolean camera,int fov,String hint){this.title=title;this.camera=camera;this.fov=fov;this.hint=hint;}
        public int[] defaults(){return switch(this){
            case VHS->new int[]{85,35,55,65,15};
            case CRT->new int[]{85,15,12,70,30};
            case FILM->new int[]{65,32,0,6,18};
            case SUPER8->new int[]{85,45,10,40,38};
            case NOIR->new int[]{100,20,0,0,35};
            case POLAROID->new int[]{80,15,0,4,14};
            case NIGHTVISION->new int[]{100,55,4,25,28};
            case CYBER->new int[]{75,8,5,0,22};
            default->new int[]{80,0,0,0,0};
        };}
    }
    public static Profile[] profiles(boolean camera){return Arrays.stream(Profile.values()).filter(p->p.camera==camera).toArray(Profile[]::new);}
    private static double unit(double v){return Math.max(0,Math.min(1,v));}
    private static int clip(double v){return (int)Math.round(Math.max(0,Math.min(255,v)));}
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    private static double luma(int c){return .2126*((c>>>16)&255)+.7152*((c>>>8)&255)+.0722*(c&255);}
    private static int hash(int x,int y){int a=x*374761393+y*668265263;a=(a^(a>>>13))*1274126177;return a^(a>>>16);}
    private static int pixel(BufferedImage img,int x,int y){return img.getRGB(Math.max(0,Math.min(img.getWidth()-1,x)),Math.max(0,Math.min(img.getHeight()-1,y)));}
    public record Look(Profile profile,int[] values) implements ImageDocument.Operation{
        public Look{if(values.length!=COUNT)throw new IllegalArgumentException("Invalid look");values=Arrays.stream(values).map(v->Math.max(0,Math.min(100,v))).toArray();}
        public int[] values(){return values.clone();}
        public String label(){return profile.title;}
        public BufferedImage apply(BufferedImage img){
            int[] a=values;double strength=a[0]/100.0;if(strength==0)return img;
            int w=img.getWidth(),h=img.getHeight();int[] row=new int[w];
            boolean tape=profile==Profile.VHS,crt=profile==Profile.CRT;
            BufferedImage src=ImageDocument.copy(img);
            double scale=h/480.0;int bleed=Math.max(1,(int)Math.round(w/320.0*a[2]/50.0));
            for(int y=0;y<h;y++){
                int line=(int)(y/Math.max(.15,scale));double ny=(y+.5)/h*2-1;
                double track=tape?Math.sin(line*.125)*w*.004*a[2]/100.0:0;
                if(tape&&line%173<5)track+=w*.017*a[2]/100.0;
                for(int x=0;x<w;x++){
                    int orig=src.getRGB(x,y);int c=pixel(src,x+(int)Math.round(track),y);double r=(c>>>16)&255,g=(c>>>8)&255,b=c&255,l=luma(c);
                    double v=l/255,shadow=(1-v)*(1-v),highlight=v*v;
                    double sat=1,contrast=1,lift=0,warm=0,tint=0;
                    switch(profile){
                        case VHS -> {
                            // Low-bandwidth chroma, shifted relative to sharp luminance.
                            int ca=pixel(src,x+(int)track-bleed,y),cb=pixel(src,x+(int)track+bleed,y);
                            double cr=(((ca>>>16)&255)-luma(ca)),cc=(cb&255)-luma(cb);
                            double s=a[2]/100.0;
                            r=mix(r,l+cr*.72,s);b=mix(b,l+cc*.72,s);g=mix(g,l-(.2126*cr+.0722*cc)/.7152,s*.65);
                            sat=.77;contrast=.87;lift=9;
                        }
                        case CRT -> {contrast=1.12;sat=1.15;}
                        case FILM -> {contrast=1.09;warm=12;lift=4;sat=.91;}
                        case SUPER8 -> {contrast=.89;warm=24;lift=15;sat=.76;}
                        case NOIR -> {sat=0;contrast=1.35;lift=-9;}
                        case POLAROID -> {contrast=.83;warm=14;lift=18;sat=.86;}
                        case NIGHTVISION -> {r=l*.34;g=l*1.2;b=l*.28;contrast=1.2;lift=4;}
                        case CYBER -> {r+=26*highlight-10*shadow;g-=11*shadow;b+=32*shadow;contrast=1.16;sat=1.12;}
                        case LANDSCAPE -> {sat=1.13;contrast=1.09;warm=2;}
                        case ARCHITECTURE -> {sat=.94;contrast=1.08;lift=3;}
                        case STREET -> {sat=.94;contrast=1.13;warm=3;}
                        case PORTRAIT -> {sat=.94;contrast=.94;warm=6;lift=5;}
                        case PRODUCT -> {sat=1.04;contrast=1.04;}
                        case GOLDEN -> {warm=22;contrast=.94;lift=7;sat=1.06;}
                        case NIGHT -> {r-=10*shadow;b+=17*shadow;contrast=1.03;lift=5;}
                        case DOCUMENTARY -> {sat=.99;contrast=1.03;}
                    }
                    double gray=.2126*r+.7152*g+.0722*b;
                    r=(gray+(r-gray)*sat-128)*contrast+128+lift+warm;
                    g=(gray+(g-gray)*sat-128)*contrast+128+lift+tint;
                    b=(gray+(b-gray)*sat-128)*contrast+128+lift-warm;
                    int nx=(int)(x*640.0/w),iy=(int)(y*480.0/h);double noise=((hash(nx,iy)&255)/255.0-.5)*a[1]*.85;
                    double pattern=1;
                    if(tape||crt||profile==Profile.NIGHTVISION){pattern=1-(line%2==0?.22:0)*a[3]/100.0;}
                    if(crt){int ch=nx%3;double d=.2*a[3]/100.0;if(ch!=0)r*=1-d;if(ch!=1)g*=1-d;if(ch!=2)b*=1-d;}
                    if(profile==Profile.SUPER8){if((hash(nx/2,0)&1023)==0)noise+=a[3]*.8;if((hash(nx,iy)&8191)==0)noise-=a[3]*1.8;}
                    if(tape&&line%173<5)noise+=((hash(nx,line)&255)/255.0-.5)*a[3]*1.2;
                    double xx=(x+.5)/w*2-1,shade=1-Math.min(1,(xx*xx+ny*ny)*.6)*a[4]/100.0*.65;
                    r=r*pattern*shade+noise;g=g*pattern*shade+noise;b=b*pattern*shade+noise;
                    row[x]=(orig&0xff000000)|(clip(mix((orig>>>16)&255,r,strength))<<16)|(clip(mix((orig>>>8)&255,g,strength))<<8)|clip(mix(orig&255,b,strength));
                }img.setRGB(0,y,w,1,row,0,w);
            }return img;
        }
    }
}
