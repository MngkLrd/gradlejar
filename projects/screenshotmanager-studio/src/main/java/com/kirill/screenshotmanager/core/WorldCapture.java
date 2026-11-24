package com.kirill.screenshotmanager.core;
import java.nio.file.*;
import java.io.IOException;
import java.util.Locale;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class WorldCapture {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public record Scene(String dimension,double x,double y,double z,float yaw,float pitch,int fov,long dayTime,boolean rain,boolean thunder,boolean hideHud,long capturedAt){}
    private static Path sidecar(Path image){return image.resolveSibling(image.getFileName()+".world.json");}
    public static void save(Path image,Scene scene)throws IOException{
        Path target=sidecar(image),tmp=Files.createTempFile(target.getParent(),"scene-",".tmp");
        try{Files.writeString(tmp,GSON.toJson(scene));try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(tmp);}
    }
    public static String read(Path image)throws IOException{
        Path file=sidecar(image);if(Files.size(file)>65536)throw new IOException("Scene metadata too large");
        Scene s=GSON.fromJson(Files.readString(file),Scene.class);
        if(s==null)return "No scene metadata";
        return String.format(Locale.ROOT,"%s | %.1f %.1f %.1f | FOV %d | time %d",s.dimension(),s.x(),s.y(),s.z(),s.fov(),s.dayTime());
    }
}
