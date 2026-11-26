package dev.minemods.worldlogic;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
@Mod("worldlogic")
public final class WorldLogic {
 public WorldLogic(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
  Content.PARTICLES.register(context.getModEventBus());
  Content.BLOCKS.register(context.getModEventBus());
  Content.ITEMS.register(context.getModEventBus());
  Rules.load();
  var bus=MinecraftForge.EVENT_BUS;
  bus.addListener(Structure::onBreak);
  bus.addListener(Trees::breakBase);
  bus.addListener(Trees::placed);
  bus.addListener(Trees::tick);
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
  bus.addListener(Torches::tick);
  bus.addListener(Torches::relight);
  bus.addListener(AirAndFire::tick);
  bus.addListener(AirAndFire::smokeTick);
  bus.addListener(AirAndFire::onExplosion);
 }
}
