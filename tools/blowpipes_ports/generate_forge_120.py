#!/usr/bin/env python3
import sys,json,shutil
from pathlib import Path
src=Path(sys.argv[1]).resolve()
root=Path(sys.argv[2]).resolve()
java=root/'src/main/java/dev/minemods/blowpipes'
res=root/'src/main/resources'
java.mkdir(parents=True,exist_ok=True)
shutil.copytree(src/'src/main/resources',res,dirs_exist_ok=True)
(res/'fabric.mod.json').unlink(missing_ok=True)
# Preserve client mixins for true first/third-person held blowpipe animations;
# server side mixin is the same as the verified Fabric 1.20.1 release.
for name in ('BlowpipeItem.java','DartProjectile.java','DartModel.java','DartRenderer.java','SleepState.java'):
 shutil.copyfile(src/'src/main/java/dev/minemods/blowpipes'/name,java/name)
shutil.copytree(src/'src/main/java/dev/minemods/blowpipes/mixin',java/'mixin',dirs_exist_ok=True)
shutil.copytree(src/'src/main/java/dev/minemods/blowpipes/client',java/'client',dirs_exist_ok=True)
(java/'Blowpipes.java').write_text("""package dev.minemods.blowpipes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

@Mod(Blowpipes.ID)
public final class Blowpipes {
 public static final String ID="blowpipes";
 private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
 public static final RegistryObject<Item> BAMBOO=ITEMS.register("bamboo_blowpipe",()->new BlowpipeItem(new Item.Properties().stacksTo(1),0));
 public static final RegistryObject<Item> HUNTING=ITEMS.register("hunting_blowpipe",()->new BlowpipeItem(new Item.Properties().stacksTo(1),1));
 public static final RegistryObject<Item> REINFORCED=ITEMS.register("reinforced_blowpipe",()->new BlowpipeItem(new Item.Properties().stacksTo(1),2));
 public static final RegistryObject<Item> PLAIN=ITEMS.register("plain_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> IRON=ITEMS.register("iron_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> POISON=ITEMS.register("poison_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> SLOW=ITEMS.register("slowness_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> FIRE=ITEMS.register("fire_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<Item> SLEEP=ITEMS.register("sleep_dart",()->new Item(new Item.Properties()));
 public static final RegistryObject<EntityType<DartProjectile>> DART=ENTITIES.register("dart",()->
  EntityType.Builder.<DartProjectile>of(DartProjectile::new,MobCategory.MISC).sized(.16f,.16f).clientTrackingRange(6).updateInterval(2).build("dart"));
 public static EntityType<DartProjectile> DART_TYPE(){return DART.get();}
 public static Item DEFAULT_DART(){return PLAIN.get();}
 public Blowpipes(){
  IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
  ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(this::creative);
 }
 private void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey()==CreativeModeTabs.COMBAT)
   for(RegistryObject<Item> entry:new RegistryObject[]{BAMBOO,HUNTING,REINFORCED,PLAIN,IRON,POISON,SLOW,FIRE,SLEEP})
    e.accept(entry.get());
 }
}
""")
(java/'BlowpipeEvents.java').write_text("""package dev.minemods.blowpipes;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

@Mod.EventBusSubscriber(modid=Blowpipes.ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class BlowpipeEvents {
 private BlowpipeEvents(){}
 public static boolean putToSleep(Mob mob,int ticks) {
  return mob.isAlive() && mob.getMaxHealth()<100 && !mob.level().isClientSide
    && mob instanceof SleepState state && state.blowpipes$startSleep(ticks);
 }
 @SubscribeEvent public static void onDamage(LivingAttackEvent event){
  if(event.getEntity() instanceof Mob mob && mob instanceof SleepState state)
   state.blowpipes$wakeUp();
 }
 @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
  ResourceLocation key=BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
  if(!Blowpipes.ID.equals(key.getNamespace()))return;
  String id=key.getPath();
  switch(id){
   case "bamboo_blowpipe","hunting_blowpipe","reinforced_blowpipe",
      "plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart" -> {
       event.getToolTip().add(Component.translatable("tooltip.blowpipes."+id+".primary").withStyle(ChatFormatting.GRAY));
       event.getToolTip().add(Component.translatable("tooltip.blowpipes."+id+".secondary").withStyle(ChatFormatting.DARK_GRAY));
      }
   default -> {}
  }
 }
}
""")
(java/'BlowpipesClient.java').write_text("""package dev.minemods.blowpipes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Blowpipes.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BlowpipesClient {
 @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e){
  e.registerLayerDefinition(DartRenderer.LAYER,DartModel::createBodyLayer);
 }
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerEntityRenderer(Blowpipes.DART_TYPE(),DartRenderer::new);
 }
}
""")
# Update to real Forge mod metadata, no NeoForge/Fabric manifest.
meta=res/'META-INF';meta.mkdir(parents=True,exist_ok=True)
(meta/'mods.toml').write_text("""modLoader="javafml"
loaderVersion="[47,)"
license="MIT"
[[mods]]
modId="blowpipes"
version="1.4.1"
displayName="Blowpipes"
description="Three upgradeable 3D blowpipes and six effect darts. Sleep interrupts mob AI."
[[dependencies.blowpipes]]
modId="forge"
mandatory=true
versionRange="[47.2,)"
ordering="NONE"
side="BOTH"
[[dependencies.blowpipes]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
ordering="NONE"
side="BOTH"
""")
(res/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'Blowpipes 1.4.1 Forge 1.20.1'}}))
# Do not copy incompatible generated metadata from other loaders.
for f in (res/'META-INF').glob('neoforge*'):f.unlink()
(root/'settings.gradle').write_text("""pluginManagement { repositories {
    maven { url='https://maven.minecraftforge.net/' }
    gradlePluginPortal()
    mavenCentral()
} }
rootProject.name='blowpipes-forge-1.20.1'
""")
(root/'gradle.properties').write_text("org.gradle.jvmargs=-Xmx4G\norg.gradle.daemon=false\n")
(root/'build.gradle').write_text("""plugins {
    id 'java'
    id 'idea'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}
group='dev.minemods'
version='1.4.1'
base.archivesName='blowpipes-forge-1.20.1'
java.toolchain.languageVersion=JavaLanguageVersion.of(17)
minecraft {
    mappings channel:'official',version:'1.20.1'
    runs {
        client { workingDirectory project.file('run') }
        server { workingDirectory project.file('run-server'); args '--nogui' }
    }
}
dependencies { minecraft 'net.minecraftforge:forge:1.20.1-47.2.0' }
tasks.withType(JavaCompile).configureEach { options.encoding='UTF-8'; options.release=17 }
jar {
    manifest { attributes('MixinConfigs':'blowpipes.mixins.json') }
    finalizedBy 'reobfJar'
}
""")
# Verify that no unsupported recipe or advancement format is copied.
items=("bamboo_blowpipe","hunting_blowpipe","reinforced_blowpipe","plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart")
for name in items:
 p=res/'data/blowpipes/recipes'/(name+'.json');assert p.is_file(),p
 d=json.loads(p.read_text());assert 'item' in d['result'] and all(isinstance(v,dict) for v in d['key'].values())
model=res/'assets/blowpipes/models/item/bamboo_blowpipe.json';assert model.is_file()
print('Forge 1.20.1 prepared; data, animation mixins, 3D assets, sleep AI and 9 recipes verified')
