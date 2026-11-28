package dev.minemods.aquaculture;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MarineGeometry {
 private MarineGeometry(){}
 public record Part(String name,String parent,int u,int v,int w,int h,int d,float ox,float oy,float oz,float x,float y,float z){}
 public record Shape(float pixels,List<Part> parts){}
 public static final Map<String,Shape> ALL=load();
 private static Map<String,Shape> load(){
  Map<String,Shape> result=new LinkedHashMap<>();
  try(var stream=MarineGeometry.class.getResourceAsStream("/assets/aquaculture/marine_geometry.tsv")){
   if(stream==null)throw new IOException("Missing marine geometry");
   try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
    for(String line;(line=reader.readLine())!=null;){
     if(line.isBlank())continue;
     String[] row=line.split("[|]");
     if(row.length!=4)throw new IOException("Invalid geometry row");
     MarineSpecies s=MarineSpecies.byId(row[0]);if(s==null||!s.shape.equals(row[1]))throw new IOException("Unknown species "+row[0]);
     List<Part> parts=new ArrayList<>();Set<String> seen=new HashSet<>();seen.add("root");
     boolean[][] uv=new boolean[128][128];
     Map<String,Float> pivots=new HashMap<>();pivots.put("root",0f);float min=0,max=0;
     for(String data:row[3].split(";")){
      String[] t=data.split(",");if(t.length!=13)throw new IOException("Invalid part "+data);
      int[] n=new int[5];for(int i=0;i<5;i++)n[i]=Integer.parseInt(t[i+2]);float[] f=new float[6];for(int i=0;i<6;i++){f[i]=Float.parseFloat(t[i+7]);if(!Float.isFinite(f[i]))throw new IOException("Invalid coordinate");}
      int u=n[0],v=n[1],w=n[2],h=n[3],d=n[4],uw=2*(w+d),vh=h+d;
      if(u<0||v<0||w<1||h<1||d<1||u+uw>128||v+vh>128||!seen.contains(t[1])||!seen.add(t[0]))throw new IOException("Invalid UV or parent "+s.id+"/"+t[0]);
      for(int y=v;y<v+vh;y++)for(int x=u;x<u+uw;x++){if(uv[y][x])throw new IOException("Overlapping UV "+s.id);uv[y][x]=true;}
      parts.add(new Part(t[0],t[1],u,v,w,h,d,f[0],f[1],f[2],f[3],f[4],f[5]));
      float pivot=pivots.get(t[1])+f[2];pivots.put(t[0],pivot);
      if(!t[0].equals("tusk")){min=Math.min(min,pivot+f[5]);max=Math.max(max,pivot+f[5]+d);}
     }
     if(!seen.containsAll(Set.of("body","head","rear","tail"))||max-min<10||result.put(s.id,new Shape(max-min,List.copyOf(parts)))!=null)throw new IOException("Invalid shape "+s.id);
    }
   }
   if(result.size()!=MarineSpecies.values().length)throw new IOException("Marine geometry count");
   return Collections.unmodifiableMap(result);
  }catch(Exception e){throw new IllegalStateException("Marine geometry",e);}
 }
}
