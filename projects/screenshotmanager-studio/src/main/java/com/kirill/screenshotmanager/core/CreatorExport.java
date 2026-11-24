package com.kirill.screenshotmanager.core;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.*;
import java.util.UUID;

public final class CreatorExport {
    private CreatorExport(){}
    public static Path save(Path root,BufferedImage original,boolean jpeg,int quality,int maxSide,long limit)throws IOException{
        Files.createDirectories(root);Path tmp=Files.createTempFile(root,"export-",".tmp");
        try{
            int side=maxSide>0?maxSide:Math.max(original.getWidth(),original.getHeight()),q=Math.max(30,Math.min(100,quality));
            for(int attempt=0;attempt<16;attempt++){
                BufferedImage img=ImageDocument.resize(original,side);
                Files.deleteIfExists(tmp);
                if(jpeg){
                    var rgb=new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_INT_RGB);
                    var g=rgb.createGraphics();g.drawImage(img,0,0,null);g.dispose();
                    var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
                    try(ImageOutputStream stream=ImageIO.createImageOutputStream(tmp.toFile())){
                        writer.setOutput(stream);var param=writer.getDefaultWriteParam();param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);param.setCompressionQuality(q/100f);
                        writer.write(null,new IIOImage(rgb,null,null),param);
                    }finally{writer.dispose();}
                }else if(!ImageIO.write(img,"png",tmp.toFile()))throw new IOException("PNG writer unavailable");
                if(limit<=0||Files.size(tmp)<=limit){
                    Path out=root.resolve("studio_"+System.currentTimeMillis()+"_"+UUID.randomUUID().toString().substring(0,6)+(jpeg?".jpg":".png"));
                    Files.move(tmp,out);return out;
                }
                if(jpeg&&q>45)q-=10;else{side=(int)(side*.8);if(side<64)break;}
            }
            throw new IOException("Cannot meet export size limit");
        }finally{Files.deleteIfExists(tmp);}
    }
    public static BufferedImage collage(java.util.List<BufferedImage> images,boolean vertical){
        if(images.size()<2||images.size()>6)throw new IllegalArgumentException("Choose 2 to 6 images");
        int cellW=960,cellH=540,cols=vertical?1:2,rows=(images.size()+cols-1)/cols;
        var out=new BufferedImage(cols*cellW+(cols+1)*8,rows*cellH+(rows+1)*8,BufferedImage.TYPE_INT_RGB);
        var g=out.createGraphics();g.setColor(new Color(18,22,28));g.fillRect(0,0,out.getWidth(),out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for(int i=0;i<images.size();i++){
            var im=images.get(i);double k=Math.min(cellW/(double)im.getWidth(),cellH/(double)im.getHeight());
            int w=Math.max(1,(int)(im.getWidth()*k)),h=Math.max(1,(int)(im.getHeight()*k));
            g.drawImage(im,8+(i%cols)*(cellW+8)+(cellW-w)/2,8+(i/cols)*(cellH+8)+(cellH-h)/2,w,h,null);
        }g.dispose();return out;
    }
}
