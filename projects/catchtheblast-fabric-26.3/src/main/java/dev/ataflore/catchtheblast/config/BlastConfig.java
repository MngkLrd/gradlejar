package dev.ataflore.catchtheblast.config;
import java.nio.file.*;
import java.util.Properties;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
public final class BlastConfig {
 private static Path path;
 private static boolean terrain=true;
 private static double maxResistance=8;
 public static boolean terrainDamage(){return terrain;}
 public static double maxResistance(){return maxResistance;}
 public static void load(MinecraftServer server){path=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/catchtheblast-server.properties");terrain=true;maxResistance=8;try{if(Files.exists(path)){Properties p=new Properties();try(var in=Files.newInputStream(path)){p.load(in);}terrain=Boolean.parseBoolean(p.getProperty("terrainDamage","true"));maxResistance=Math.max(0,Math.min(100,Double.parseDouble(p.getProperty("maxResistance","8"))));}}catch(Exception ignored){}}
 public static void setTerrain(boolean value){terrain=value;try{if(path!=null){Files.createDirectories(path.getParent());Properties p=new Properties();p.setProperty("terrainDamage",Boolean.toString(terrain));p.setProperty("maxResistance",Double.toString(maxResistance));try(var out=Files.newOutputStream(path)){p.store(out,"Catch the Blast server configuration");}}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
}