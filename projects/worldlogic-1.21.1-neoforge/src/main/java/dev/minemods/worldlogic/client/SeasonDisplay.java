package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.AnalyticsSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
/** The same authoritative season sample already synchronized by Core analytics. */
public final class SeasonDisplay {
 private SeasonDisplay(){}
 public static Component label(){
  var mc=Minecraft.getInstance();var s=AnalyticsSync.client();
  if(mc.level==null||s==null||!s.dimension().equals(mc.level.dimension().location().toString())||s.tabs().size()<6)return Component.empty();
  long age=mc.level.getGameTime()-s.tick();if(age<0||age>200)return Component.empty();
  return s.tabs().get(5).stream().filter(v->v.matches("season_(spring|summer|autumn|winter)\\|")).findFirst()
   .map(v->(Component)Component.translatable("worldlogic.analytics."+v.substring(0,v.length()-1))).orElse(Component.empty());
 }
}
