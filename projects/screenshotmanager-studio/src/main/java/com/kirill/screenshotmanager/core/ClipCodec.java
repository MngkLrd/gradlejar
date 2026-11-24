package com.kirill.screenshotmanager.core;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class ClipCodec {
    private ClipCodec(){}
    public record Frame(BufferedImage image,long millis){}
    public record Result(Path path,int frames,int width,int height,long bytes){}
    public static Result save(Path root,List<Frame> source,int from,int to,int fps,int maxSide,long limit)throws IOException{
        if(from<0||to>source.size()||to<=from||fps<1||fps>30||maxSide<64||limit<10000)throw new IOException("Invalid clip settings");
        // Preserve the recorded timing while capping the requested export frame rate.
        List<Frame> sampled=new ArrayList<>();
        for(int i=from;i<to;i++){
            Frame f=source.get(i);
            if(sampled.isEmpty()||f.millis()-sampled.getLast().millis()>=Math.round(1000.0/fps)-1)sampled.add(f);
        }
        source=sampled;from=0;to=source.size();
        Files.createDirectories(root);
        Path temp=Files.createTempFile(root,"clip-",".gif");
        try{
            int side=maxSide,skip=1;
            for(int attempt=0;attempt<12;attempt++){
                int count=0,w=0,h=0;
                var writer=ImageIO.getImageWritersByFormatName("gif").next();
                try(ImageOutputStream out=ImageIO.createImageOutputStream(temp.toFile())){
                    writer.setOutput(out);writer.prepareWriteSequence(null);
                    for(int i=from;i<to;i+=skip){
                        BufferedImage frame=ImageDocument.resize(source.get(i).image(),side);
                        w=frame.getWidth();h=frame.getHeight();
                        var param=writer.getDefaultWriteParam();
                        var meta=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(frame),param);
                        String format=meta.getNativeMetadataFormatName();
                        IIOMetadataNode tree=(IIOMetadataNode)meta.getAsTree(format);
                        IIOMetadataNode control=node(tree,"GraphicControlExtension");
                        control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");
                        control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("transparentColorIndex","0");
                        long delta=i+skip<to?source.get(i+skip).millis()-source.get(i).millis():Math.round(1000.0*skip/fps);
                        control.setAttribute("delayTime",Long.toString(Math.max(2,Math.min(65535,Math.round(delta/10.0)))));
                        if(count==0){
                            var extensions=node(tree,"ApplicationExtensions");var app=new IIOMetadataNode("ApplicationExtension");
                            app.setAttribute("applicationID","NETSCAPE");app.setAttribute("authenticationCode","2.0");app.setUserObject(new byte[]{1,0,0});extensions.appendChild(app);
                        }
                        meta.setFromTree(format,tree);writer.writeToSequence(new IIOImage(frame,null,meta),param);count++;
                    }
                    writer.endWriteSequence();
                }finally{writer.dispose();}
                long size=Files.size(temp);
                if(size<=limit){
                    Path target=root.resolve("clip_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,6)+".gif");
                    Files.move(temp,target);return new Result(target,count,w,h,size);
                }
                if(side>180)side=Math.max(160,(int)(side*.76));else skip++;
                Files.delete(temp);Files.createFile(temp);
            }
            throw new IOException("Cannot meet size limit; trim the clip or raise the limit");
        }finally{Files.deleteIfExists(temp);}
    }
    private static IIOMetadataNode node(IIOMetadataNode root,String name){
        for(int i=0;i<root.getLength();i++)if(root.item(i).getNodeName().equals(name))return (IIOMetadataNode)root.item(i);
        var result=new IIOMetadataNode(name);root.appendChild(result);return result;
    }
}
