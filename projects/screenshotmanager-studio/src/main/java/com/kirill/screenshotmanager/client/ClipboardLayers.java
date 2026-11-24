package com.kirill.screenshotmanager.client;
import com.kirill.screenshotmanager.core.ProPhoto;
import com.kirill.screenshotmanager.core.ImageDocument;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

final class ClipboardLayers{
    static ProPhoto.ImageLayer read(int x,int y)throws Exception{
        Object value=Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.imageFlavor);
        if(!(value instanceof Image image))throw new IOException("No image");
        int w=image.getWidth(null),h=image.getHeight(null);if(w<1||h<1||(long)w*h>32_000_000)throw new IOException("Invalid image size");
        double k=Math.min(1,768.0/Math.max(w,h));var scaled=new BufferedImage(Math.max(1,(int)(w*k)),Math.max(1,(int)(h*k)),BufferedImage.TYPE_INT_ARGB);
        var g=scaled.createGraphics();g.drawImage(image,0,0,scaled.getWidth(),scaled.getHeight(),null);g.dispose();
        for(int side:new int[]{768,640,512,384,256}){
            var out=new ByteArrayOutputStream();ImageIO.write(ImageDocument.resize(scaled,side),"png",out);
            if(out.size()<=1_500_000)return new ProPhoto.ImageLayer("Clipboard",out.toByteArray(),x,y,100,0,100);
        }throw new IOException("Image too large");
    }
}
