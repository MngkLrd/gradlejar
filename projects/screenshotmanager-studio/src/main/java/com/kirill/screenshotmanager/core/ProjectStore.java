package com.kirill.screenshotmanager.core;
import java.io.*;
import java.nio.file.*;
import java.awt.Point;
import java.util.*;
import java.security.MessageDigest;
import static com.kirill.screenshotmanager.core.ImageDocument.*;

public final class ProjectStore {
    private ProjectStore(){}
    private static Path sidecar(Path source){return source.resolveSibling(source.getFileName()+".smproject");}
    public static boolean exists(Path source){return Files.isRegularFile(sidecar(source));}
    private static String fingerprint(Path source)throws IOException{
        try(var in=Files.newInputStream(source)){
            var digest=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[65536];int n;
            while((n=in.read(buf))!=-1)digest.update(buf,0,n);
            return HexFormat.of().formatHex(digest.digest());
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    public static void save(Path source,List<Operation> ops)throws IOException{
        if(ops.size()>100)throw new IOException("Too many layers");
        Path target=sidecar(source),tmp=Files.createTempFile(target.getParent(),"project-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
                out.writeUTF("SMPROJECT1");out.writeUTF(fingerprint(source));out.writeInt(ops.size());
                for(var wrapped:ops){
                    out.writeBoolean(visible(wrapped));var op=unwrap(wrapped);
                    if(op instanceof MarkOperation m){
                        var v=m.value();out.writeByte(1);out.writeUTF(v.tool().name());
                        for(int i:new int[]{v.x1(),v.y1(),v.x2(),v.y2(),v.color(),v.thickness(),v.strength()})out.writeInt(i);
                        out.writeUTF(v.text());out.writeInt(v.points().size());
                        for(Point point:v.points()){out.writeInt(point.x);out.writeInt(point.y);}
                    }else if(op instanceof FilterOperation f){out.writeByte(2);out.writeUTF(f.value().name());}
                    else if(op instanceof Adjustment a){out.writeByte(3);for(int i:new int[]{a.brightness(),a.contrast(),a.saturation(),a.warmth(),a.gamma(),a.vignette()})out.writeInt(i);}
                    else if(op instanceof ProPhoto.Tone a){out.writeByte(4);for(int v:a.values())out.writeInt(v);}
                    else if(op instanceof ProPhoto.Effects a){out.writeByte(5);for(int v:a.values())out.writeInt(v);}
                    else if(op instanceof ProPhoto.Focus a){out.writeByte(6);out.writeInt(a.x());out.writeInt(a.y());out.writeInt(a.radius());out.writeInt(a.amount());out.writeBoolean(a.dark());}
                    else if(op instanceof ProPhoto.ImageLayer a){out.writeByte(7);out.writeUTF(a.name());for(int v:new int[]{a.x(),a.y(),a.size(),a.rotation(),a.opacity()})out.writeInt(v);byte[] png=a.png();out.writeInt(png.length);out.write(png);}
                    else if(op instanceof DepthMap.Focus a){out.writeByte(8);out.writeInt(a.distance());out.writeInt(a.range());out.writeInt(a.amount());a.map().write(out);}
                    else if(op instanceof AdvancedPhoto.Adjust a){out.writeByte(9);out.writeUTF(a.kind().name());for(int v:a.values())out.writeInt(v);}
                    else if(op instanceof AdvancedPhoto.Canvas a){out.writeByte(10);out.writeInt(a.width());out.writeInt(a.height());out.writeBoolean(a.cover());}
                    else if(op instanceof AdvancedPhoto.Caption a){out.writeByte(11);out.writeUTF(a.text());for(int v:new int[]{a.x(),a.y(),a.size(),a.color(),a.style(),a.angle()})out.writeInt(v);}
                    else throw new IOException("Unsupported layer");
                }
            }
            if(Files.size(tmp)>8_000_000)throw new IOException("Project exceeds 8 MB; remove some image layers");
            try{Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
    public static List<Operation> load(Path source)throws IOException{
        Path file=sidecar(source);
        if(Files.size(file)>8_000_000)throw new IOException("Project too large");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            if(!in.readUTF().equals("SMPROJECT1")||!in.readUTF().equals(fingerprint(source)))throw new IOException("Original image changed");
            int count=in.readInt();if(count<0||count>100)throw new IOException("Invalid layer count");
            List<Operation> ops=new ArrayList<>();
            for(int i=0;i<count;i++){
                boolean visible=in.readBoolean();Operation op;
                switch(in.readByte()){
                    case 1 -> {
                        Tool tool=Tool.valueOf(in.readUTF());int x=in.readInt(),y=in.readInt(),x2=in.readInt(),y2=in.readInt(),c=in.readInt(),th=in.readInt(),st=in.readInt();
                        String text=in.readUTF();int points=in.readInt();
                        if(points<0||points>4096||th<1||th>80||st<3||st>96||text.length()>160)throw new IOException("Invalid mark");
                        List<Point> p=new ArrayList<>();for(int k=0;k<points;k++)p.add(new Point(in.readInt(),in.readInt()));
                        op=mark(new Mark(tool,x,y,x2,y2,c,th,st,text,p));
                    }
                    case 2 -> op=filter(Filter.valueOf(in.readUTF()));
                    case 3 -> {int[] a=new int[6];for(int k=0;k<6;k++){a[k]=in.readInt();if(a[k]<-100||a[k]>100)throw new IOException("Invalid adjustment");}op=new Adjustment(a[0],a[1],a[2],a[3],a[4],a[5]);}
                    case 4 -> {int[] a=new int[ProPhoto.TONE_COUNT];for(int k=0;k<a.length;k++)a[k]=in.readInt();op=new ProPhoto.Tone(a);}
                    case 5 -> {int[] a=new int[ProPhoto.EFFECT_COUNT];for(int k=0;k<a.length;k++)a[k]=in.readInt();op=new ProPhoto.Effects(a);}
                    case 6 -> op=new ProPhoto.Focus(in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readBoolean());
                    case 7 -> {String name=in.readUTF();int x=in.readInt(),y=in.readInt(),size=in.readInt(),rotation=in.readInt(),opacity=in.readInt(),n=in.readInt();if(n<8||n>1_500_000)throw new IOException("Invalid image layer");byte[] data=in.readNBytes(n);if(data.length!=n)throw new IOException("Truncated layer");op=new ProPhoto.ImageLayer(name,data,x,y,size,rotation,opacity);}
                    case 8 -> {int distance=in.readInt(),range=in.readInt(),amount=in.readInt();op=new DepthMap.Focus(DepthMap.read(in),distance,range,amount);}
                    case 9 -> {var kind=AdvancedPhoto.Kind.valueOf(in.readUTF());int[] a=new int[kind.count];for(int k=0;k<a.length;k++)a[k]=in.readInt();op=new AdvancedPhoto.Adjust(kind,a);}
                    case 10 -> op=new AdvancedPhoto.Canvas(in.readInt(),in.readInt(),in.readBoolean());
                    case 11 -> op=new AdvancedPhoto.Caption(in.readUTF(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt());
                    default -> throw new IOException("Unknown layer");
                }
                ops.add(new Layer(op,visible||geometry(op)));
            }
            return List.copyOf(ops);
        }catch(IllegalArgumentException e){throw new IOException("Invalid project",e);}
    }
}
