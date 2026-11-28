package dev.minemods.aquaculture;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Editable species geometry. Coordinates and UVs are in Minecraft model pixels. */
public final class FishVisualGeometry {
 private FishVisualGeometry(){}
 public record Part(String name,String parent,int u,int v,int w,int h,int d,
   float ox,float oy,float oz,float bx,float by,float bz,float rx,float ry,float rz){}
 public record Shape(String id,String motion,int pixels,List<Part> parts){}
 public static final Map<String,Shape> ALL=load();
 public static Shape get(String id){return Objects.requireNonNull(ALL.get(id),id);}
 private static String name(String code){return switch(code){
  case "b"->"body";case "h"->"head";case "r"->"rear";case "t"->"tail";case "d"->"dorsal";case "e"->"dorsal2";
  case "l"->"left_fin";case "f"->"right_fin";case "a"->"anal";case "m"->"barbel0";case "n"->"barbel1";
  case "o"->"chin0";case "p"->"chin1";case "c"->"chin";case "x"->"finlet0";case "y"->"finlet1";
  case "z"->"finlet2";case "j"->"jaw";default->throw new IllegalArgumentException(code);
 };}
 private static String parent(String code){return switch(code){
  case "b"->"root";case "t","x","y","z"->"rear";case "m","n","o","p","c","j"->"head";default->"body";
 };}
 private static Map<String,Shape> load(){
  Map<String,Shape> map=new LinkedHashMap<>();
  try(var stream=FishVisualGeometry.class.getResourceAsStream("/assets/aquaculture/fish_geometry.tsv")){
   if(stream==null)throw new IOException("Missing geometry");
   try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
    for(String line;(line=reader.readLine())!=null;){
     if(line.isBlank())continue;
     String[] row=line.split("[|]");
     if(row.length!=4)throw new IOException("Bad geometry row");
     int pixels=Integer.parseInt(row[2]);
     if(pixels<8||pixels>40||!FishMotion.MODES.contains(row[1]))throw new IOException("Bad dimensions or motion "+row[0]);
     List<Part> parts=new ArrayList<>();Set<String> seen=new HashSet<>();seen.add("root");
     boolean[][] uv=new boolean[64][64];
     for(String s:row[3].split(";")){
      String[] t=s.split(",");if(t.length!=12&&t.length!=15)throw new IOException("Bad part "+s);
      int u=Integer.parseInt(t[1]),v=Integer.parseInt(t[2]),w=Integer.parseInt(t[3]),h=Integer.parseInt(t[4]),d=Integer.parseInt(t[5]);
      float[] f=new float[9];for(int i=6;i<t.length;i++){f[i-6]=Float.parseFloat(t[i]);if(!Float.isFinite(f[i-6]))throw new IOException("Bad coordinate");}
      String n=name(t[0]),p=parent(t[0]);
      int uw=2*(w+d),vh=h+d;
      if(u<0||v<0||w<0||h<0||d<0||(w==0?1:0)+(h==0?1:0)+(d==0?1:0)>1||u+uw>64||v+vh>64||!seen.contains(p)||!seen.add(n))throw new IOException("Invalid part "+row[0]+"/"+n);
      for(int y=v;y<v+vh;y++)for(int x=u;x<u+uw;x++){if(uv[y][x])throw new IOException("Overlapping UV "+row[0]+"/"+n);uv[y][x]=true;}
      parts.add(new Part(n,p,u,v,w,h,d,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8]));
     }
     if(!seen.containsAll(Set.of("body","head","rear","tail"))||map.put(row[0],new Shape(row[0],row[1],pixels,List.copyOf(parts)))!=null)throw new IOException("Invalid shape "+row[0]);
    }
   }
   if(!map.keySet().equals(Species.BY_ID.keySet()))throw new IOException("Species geometry mismatch");
   return Collections.unmodifiableMap(map);
  }catch(Exception e){throw new IllegalStateException("Invalid Aqua fish geometry",e);}
 }
}
