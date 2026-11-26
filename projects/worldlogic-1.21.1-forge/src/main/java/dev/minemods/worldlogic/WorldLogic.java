package dev.minemods.worldlogic;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
@Mod("worldlogic")
public final class WorldLogic {
 public WorldLogic() {
  Rules.load();
  var bus=MinecraftForge.EVENT_BUS;
  bus.addListener(Structure::onBreak);
  bus.addListener(Structure::onImpact);
  bus.addListener(Structure::onExplosion);
  bus.addListener(Structure::onTick);
  bus.addListener(Combat::knockback);
  bus.addListener(Combat::hurt);
  bus.addListener(Combat::fall);
  bus.addListener(Combat::shield);
  bus.addListener(Combat::visibility);
  bus.addListener(Body::tick);
  bus.addListener(Body::jump);
  bus.addListener(Body::breakSpeed);
  bus.addListener(Body::onBreak);
  bus.addListener(Body::logout);
  bus.addListener(Weather::tick);
  bus.addListener(Ecology::crop);
  bus.addListener(Ecology::hurt);
  bus.addListener(Ecology::tick);
  bus.addListener(Ecology::smelt);
  bus.addListener(Ecology::eat);
  bus.addListener(ObjectsPhysics::tick);
  bus.addListener(ObjectsPhysics::onInteract);
  bus.addListener(AirAndFire::tick);
  bus.addListener(AirAndFire::onExplosion);
 }
}
