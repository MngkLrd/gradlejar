package com.kirill.screenshotmanager.core;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class MediaFiles{
    private static final List<String> SIDE=List.of(".smproject",".smrecovery",".smdepth",".world.json");
    public record Entry(Path path,boolean trash){}
    public static List<Entry> list(Path root)throws IOException{
        List<Entry> list=new ArrayList<>();
        for(String dir:List.of("","edited","clips","trash","trash/edited","trash/clips")){
            Path folder=root.resolve(dir);if(!Files.isDirectory(folder))continue;
            try(var stream=Files.list(folder)){stream.filter(Files::isRegularFile).filter(MediaFiles::media).limit(2000).forEach(path->list.add(new Entry(path,dir.startsWith("trash"))));}
        }
        list.sort(Comparator.comparing(e->e.path().getFileName().toString(),String.CASE_INSENSITIVE_ORDER));return List.copyOf(list);
    }
    private static boolean media(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".gif");}
    public static Path move(Path root,Entry entry,boolean trash)throws IOException{
        Path base=root.toAbsolutePath().normalize(),source=entry.path().toAbsolutePath().normalize();
        if(!source.startsWith(base)||!media(source)||entry.trash()==trash)throw new IOException("Invalid media operation");
        Path rel=entry.trash()?base.resolve("trash").relativize(source):base.relativize(source);
        Path target=(trash?base.resolve("trash"):base).resolve(rel).normalize();
        return moveBundle(source,target);
    }
    public static Path rename(Path root,Entry entry,String name)throws IOException{
        if(entry.trash()||!name.matches("[a-zA-Z0-9 _.-]{1,100}")||name.equals(".")||name.equals(".."))throw new IOException("Use a simple file name");
        Path source=entry.path().toAbsolutePath().normalize();if(!source.startsWith(root.toAbsolutePath().normalize())||!media(source))throw new IOException("Invalid path");
        String old=source.getFileName().toString(),ext=old.substring(old.lastIndexOf('.'));String next=name.endsWith(ext)?name:name+ext;
        return moveBundle(source,source.resolveSibling(next));
    }
    private static Path moveBundle(Path source,Path target)throws IOException{
        if(source.equals(target))return source;
        Files.createDirectories(target.getParent());
        Map<Path,Path> paths=new LinkedHashMap<>();paths.put(source,target);
        for(String suffix:SIDE){Path from=source.resolveSibling(source.getFileName()+suffix),to=target.resolveSibling(target.getFileName()+suffix);if(Files.exists(from))paths.put(from,to);}
        for(Path to:paths.values())if(Files.exists(to))throw new IOException("Destination exists");
        List<Map.Entry<Path,Path>> done=new ArrayList<>();
        try{for(var entry:paths.entrySet()){Files.move(entry.getKey(),entry.getValue());done.add(entry);}}
        catch(IOException e){Collections.reverse(done);for(var item:done)try{Files.move(item.getValue(),item.getKey());}catch(IOException ignored){}throw e;}
        return target;
    }
}
