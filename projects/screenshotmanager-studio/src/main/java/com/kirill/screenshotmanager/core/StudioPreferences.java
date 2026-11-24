package com.kirill.screenshotmanager.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;

public final class StudioPreferences {
    private final Path file,root;
    private final Properties data=new Properties();
    public StudioPreferences(Path config,Path root){
        file=config;this.root=root.toAbsolutePath().normalize();
        try(InputStream in=Files.newInputStream(file)){data.load(in);}catch(IOException|IllegalArgumentException ignored){}
    }
    private String id(Path shot){
        return Base64.getUrlEncoder().withoutPadding().encodeToString(root.relativize(shot.toAbsolutePath().normalize()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public synchronized boolean favorite(Path path){return Boolean.parseBoolean(data.getProperty("favorite."+id(path),"false"));}
    public synchronized void toggleFavorite(Path path)throws IOException{
        String k="favorite."+id(path);if(favorite(path))data.remove(k);else data.setProperty(k,"true");save();
    }
    public synchronized String album(Path path){return data.getProperty("album."+id(path),"");}
    public synchronized void album(Path path,String name)throws IOException{
        String k="album."+id(path);name=name.strip();
        if(name.isEmpty())data.remove(k);else data.setProperty(k,name.substring(0,Math.min(64,name.length())));
        save();
    }
    public synchronized List<String> albums(){
        return data.stringPropertyNames().stream().filter(k->k.startsWith("album.")).map(data::getProperty).filter(s->!s.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
    public synchronized void relocate(Path from,Path to)throws IOException{
        for(String key:List.of("favorite.","album.")){String old=key+id(from),value=data.getProperty(old);if(value!=null){data.remove(old);data.setProperty(key+id(to),value);}}
        save();
    }
    public synchronized int number(String key,int fallback,int min,int max){
        try{return Math.max(min,Math.min(max,Integer.parseInt(data.getProperty(key,Integer.toString(fallback)))));}catch(NumberFormatException e){return fallback;}
    }
    public synchronized void number(String key,int value)throws IOException{data.setProperty(key,Integer.toString(value));save();}
    private void save()throws IOException{
        Files.createDirectories(file.toAbsolutePath().getParent());Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"sm-",".tmp");
        try{
            try(OutputStream out=Files.newOutputStream(temp)){data.store(out,"Screenshot Manager Studio");}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
