package dev.minemods.aquaculture;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.server.level.ServerLevel;

public final class FishVisualChecks {
 private FishVisualChecks(){}
 public static void run(ServerLevel level){
  if(FishVisualGeometry.ALL.size()!=36)throw new IllegalStateException("Geometry count");
  Set<String> hashes=new HashSet<>();
  for(Species s:Species.ALL){
   var shape=FishVisualGeometry.get(s.id());
   String path="/assets/aquaculture/textures/entity/fish/"+s.id()+".png";
   try(var in=FishVisualChecks.class.getResourceAsStream(path)){
    if(in==null)throw new IOException(path);
    byte[] bytes=in.readAllBytes();
    var d=new DataInputStream(new ByteArrayInputStream(bytes));
    if(d.readLong()!=0x89504e470d0a1a0aL||d.readInt()!=13||d.readInt()!=0x49484452||d.readInt()!=64||d.readInt()!=64)throw new IOException("Invalid PNG "+path);
    if(!hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw new IOException("Duplicate texture "+s.id());
   }catch(Exception e){throw new IllegalStateException("Texture "+s.id(),e);}
   var entity=Aquaculture.TYPES.get(s.id()).get().create(level);
   if(!(entity instanceof Fish f))throw new IllegalStateException(s.id());
   var traits=Aquaculture.traits(f,s);
   double expected=traits.getDouble("wl_fish_length_m");
   if(f.visualLength(s)!=Fish.visualLengthOf(s,expected))throw new IllegalStateException("Initial size "+s.id());
   traits.putDouble("wl_fish_length_m",.24);Aquaculture.traits(f,s);
   float small=f.visualLength(s);traits.putDouble("wl_fish_length_m",.8);Aquaculture.traits(f,s);
   if(small>=f.visualLength(s)||f.visualLength(s)!=.8f)throw new IllegalStateException("Size sync "+s.id());
   f.discard();
   var slow=FishMotion.sample(shape.motion(),5,1,0,0,true,false,0);
   var fast=FishMotion.sample(shape.motion(),5,1,1,0,true,true,0);
   var dry=FishMotion.sample(shape.motion(),5,1,1,0,false,false,0);
   if(!Float.isFinite(fast.tail())||Math.abs(fast.tail())<Math.abs(slow.tail())||dry.roll()<1||fast.roll()!=0)throw new IllegalStateException("Animation math "+s.id());
  }
 }
}
