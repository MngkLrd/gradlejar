package com.kirill.screenshotmanager.core;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;
public final class CoreSmoke {
    private static void check(boolean result,String label){if(!result)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        BufferedImage img=new BufferedImage(40,30,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<30;y++)for(int x=0;x<40;x++)img.setRGB(x,y,0xff89abcd);
        var doc=new ImageDocument(img);
        doc.add(ImageDocument.mark(new ImageDocument.Mark(ImageDocument.Tool.REDACT,5,5,15,15,0x00112233,3,8,"",List.of())));
        var edited=doc.render(doc.snapshot());
        check(edited.getRGB(8,8)==0xff112233,"Opaque redaction");
        check(img.getRGB(8,8)==0xff89abcd,"Original untouched");
        doc.undo();check(doc.render(doc.snapshot()).getRGB(8,8)==0xff89abcd,"Undo");
        doc.redo();check(doc.render(doc.snapshot()).getRGB(8,8)==0xff112233,"Redo");
        doc.add(ImageDocument.mark(new ImageDocument.Mark(ImageDocument.Tool.CROP,2,3,20,15,0,2,8,"",List.of())));
        var cropped=doc.render(doc.snapshot());check(cropped.getWidth()==19&&cropped.getHeight()==13,"Crop");
        doc.add(ImageDocument.filter(ImageDocument.Filter.ROTATE));var rotated=doc.render(doc.snapshot());
        check(rotated.getWidth()==13&&rotated.getHeight()==19,"Rotate");
        var root=Files.createTempDirectory("screenshotmanager-test");var shot=new ShotFiles.Shot(root.resolve("original.png"),0);
        var png=ShotFiles.export(root,shot,rotated,false,0);check(ShotFiles.read(png,0).getWidth()==13,"PNG export");
        var jpg=ShotFiles.export(root,shot,rotated,true,10);check(ShotFiles.read(jpg,0).getHeight()==10,"JPEG resize");
        check(ShotFiles.list(root).size()==2,"Gallery scans edited exports");
        Files.delete(png);Files.delete(jpg);Files.delete(root.resolve("edited"));Files.delete(root);
        System.out.println("Screenshot Manager core smoke tests passed");
    }
}
