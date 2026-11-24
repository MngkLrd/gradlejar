package dev.fractal.cell;
import java.util.*;
public final class PreviewCodec {
 public static final int WIDTH=21,HEIGHT=11,VOLUME=WIDTH*WIDTH*HEIGHT,MAX_ENCODED=60000;
 public static int index(int x,int y,int z){return (y*WIDTH+z)*WIDTH+x;}
 public static String pack(Collection<String> palette,short[] voxels){
  if(voxels.length!=PreviewCodec.VOLUME)throw new IllegalArgumentException("volume");
  byte[] data=new byte[voxels.length*2];
  for(int i=0;i<voxels.length;i++){data[i*2]=(byte)(voxels[i]>>>8);data[i*2+1]=(byte)voxels[i];}
  String encoded="v3:"+String.join(",",palette)+";"+Base64.getEncoder().encodeToString(data);
  return encoded.length()<=PreviewCodec.MAX_ENCODED?encoded:"";
 }
 public record Decoded(String[] palette,short[] voxels){}
 public static Decoded unpack(String encoded){
  if(encoded==null||encoded.length()>PreviewCodec.MAX_ENCODED||!encoded.startsWith("v3:"))return null;
  try{
   int sep=encoded.indexOf(';');if(sep<0)return null;
   String[] pal=encoded.substring(3,sep).split(",",-1);
   if(pal.length<1||pal.length>PreviewCodec.VOLUME+1||!pal[0].equals("minecraft:air"))return null;
   byte[] data=Base64.getDecoder().decode(encoded.substring(sep+1));
   if(data.length!=PreviewCodec.VOLUME*2)return null;
   short[] tiles=new short[PreviewCodec.VOLUME];
   for(int i=0;i<PreviewCodec.VOLUME;i++){int id=((data[i*2]&255)<<8)|(data[i*2+1]&255);if(id>=pal.length)return null;tiles[i]=(short)id;}
   return new Decoded(pal,tiles);
  }catch(IllegalArgumentException e){return null;}
 }
 public static void main(String[] args){
  short[] a=new short[PreviewCodec.VOLUME];
  for(int y=0;y<PreviewCodec.HEIGHT;y++)a[index(10,y,10)]=(short)(1+y%2);
  a[index(20,10,20)]=2;
  var d=unpack(pack(List.of("minecraft:air","minecraft:stone","minecraft:copper_block"),a));
  if(d==null||!Arrays.equals(d.voxels(),a)||d.voxels()[index(20,10,20)]!=2)throw new AssertionError("3D roundtrip");
  for(int y=0;y<PreviewCodec.HEIGHT;y++)if(d.voxels()[index(10,y,10)]!=1+y%2)throw new AssertionError("vertical slice");
  if(unpack("v3:minecraft:air;garbage")!=null)throw new AssertionError("invalid data");
  System.out.println("All 11 layers and palette roundtrip verified");
 }
}
