package com.kirill.screenshotmanager.core;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ShotFiles {
    private ShotFiles(){}
    public static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"screenshotmanager-io");t.setDaemon(true);return t;});
    private static final DateTimeFormatter STAMP=DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    public record Shot(Path path,long time){
        public String name(){return path.getFileName().toString();}
    }
    public static List<Shot> list(Path root) throws IOException {
        if(!Files.isDirectory(root))return List.of();
        List<Shot> out=new ArrayList<>();
        for(Path dir:List.of(root,root.resolve("edited"))){
            if(!Files.isDirectory(dir))continue;
            try(Stream<Path> paths=Files.list(dir)){
                paths.filter(Files::isRegularFile).filter(ShotFiles::isImage).forEach(p->{try{out.add(new Shot(p,Files.getLastModifiedTime(p).toMillis()));}catch(IOException ignored){}});
            }
        }
        out.sort(Comparator.comparingLong(Shot::time).reversed().thenComparing(s->s.path().toString()));
        return List.copyOf(out);
    }
    private static boolean isImage(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg");}
    public static BufferedImage read(Path path,int thumbnailMax) throws IOException{
        try(ImageInputStream in=ImageIO.createImageInputStream(path.toFile())){
            if(in==null)throw new IOException("Cannot open image");
            Iterator<ImageReader> it=ImageIO.getImageReaders(in);
            if(!it.hasNext())throw new IOException("Unsupported image");
            ImageReader reader=it.next();
            try{
                reader.setInput(in,true,true);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||(long)w*h>64_000_000L)throw new IOException("Image exceeds 64 MP safety limit");
                var p=reader.getDefaultReadParam();
                if(thumbnailMax>0){
                    int sub=Math.max(1,Math.max(w,h)/thumbnailMax);
                    p.setSourceSubsampling(sub,sub,0,0);
                }
                BufferedImage img=reader.read(0,p);
                return thumbnailMax>0?ImageDocument.resize(img,thumbnailMax):img;
            }finally{reader.dispose();}
        }
    }
    public static Path export(Path root,Shot shot,BufferedImage img,boolean jpeg,int max) throws IOException{
        Path folder=root.resolve("edited");Files.createDirectories(folder);
        String name=shot.name().replaceFirst("\\.[^.]+$","");
        Path out=folder.resolve(name+"_edited_"+LocalDateTime.now().format(STAMP)+"_"+UUID.randomUUID().toString().substring(0,6)+(jpeg?".jpg":".png"));
        BufferedImage result=max>0?ImageDocument.resize(img,max):img;
        Path tmp=Files.createTempFile(folder,"sm-",".tmp");
        try{
            if(jpeg){
                BufferedImage rgb=new BufferedImage(result.getWidth(),result.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=rgb.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,rgb.getWidth(),rgb.getHeight());g.drawImage(result,0,0,null);g.dispose();
                var writer=ImageIO.getImageWritersByFormatName("jpeg").next();
                try(ImageOutputStream stream=ImageIO.createImageOutputStream(tmp.toFile())){
                    writer.setOutput(stream);var p=writer.getDefaultWriteParam();p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);p.setCompressionQuality(.92f);
                    writer.write(null,new IIOImage(rgb,null,null),p);
                }finally{writer.dispose();}
            }else if(!ImageIO.write(result,"png",tmp.toFile()))throw new IOException("PNG writer unavailable");
            Files.move(tmp,out);
            return out;
        }finally{Files.deleteIfExists(tmp);}
    }
    public static void copyImage(BufferedImage img){
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable(){
            public DataFlavor[] getTransferDataFlavors(){return new DataFlavor[]{DataFlavor.imageFlavor};}
            public boolean isDataFlavorSupported(DataFlavor f){return DataFlavor.imageFlavor.equals(f);}
            public Object getTransferData(DataFlavor f)throws UnsupportedFlavorException{if(!isDataFlavorSupported(f))throw new UnsupportedFlavorException(f);return img;}
        },null);
    }
}
