from pathlib import Path
import json, zipfile, hashlib

ROOT=Path('projects/breakableframes-bundle')
ROOT.mkdir(parents=True,exist_ok=True)
MOD='breakableframes'
VERSIONS={
 '1.20.1':('forge','neoforge','fabric'),
 '1.21.1':('neoforge','fabric'),
 '26.1.2':('neoforge','fabric'),
 '26.2':('neoforge','fabric'),
 '26.3':('neoforge','fabric')
}
JDK={ '1.20.1':17, '1.21.1':21,'26.1.2':25,'26.2':25,'26.3':25}
PACK={'1.20.1':15,'1.21.1':34,'26.1.2':84,'26.2':88,'26.3':97}
NEO={'1.21.1':'21.1.244','26.1.2':'26.1.2.114','26.2':'26.2.0.88','26.3':'26.3.0.101'}
FABRIC_API={'1.20.1':'0.92.12+1.20.1','1.21.1':'0.116.17+1.21.1','26.1.2':'0.155.3+26.1.2','26.2':'0.161.0+26.2','26.3':'0.162.0+26.3'}

def put(root,p,content):
 f=root/p
 f.parent.mkdir(parents=True,exist_ok=True)
 if isinstance(content,(dict,list)):content=json.dumps(content,indent=2,ensure_ascii=False)+'\n'
 f.write_text(content,encoding='utf-8')

COMMON='''package dev.minemods.breakableframes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Vanilla frame block; vanilla assets, no block/item registration. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class FrameStateMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void breakableframes$hardness(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> result) {
        if (getBlock() == Blocks.END_PORTAL_FRAME) result.setReturnValue(5.0F);
    }

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void breakableframes$drops(LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> result) {
        if (getBlock() != Blocks.END_PORTAL_FRAME) return;
        BlockState state = (BlockState)(Object)this;
        List<ItemStack> drops = new ArrayList<>(result.getReturnValue());
        drops.add(new ItemStack(Items.END_PORTAL_FRAME));
        if (state.getValue(EndPortalFrameBlock.HAS_EYE)) drops.add(new ItemStack(Items.ENDER_EYE));
        result.setReturnValue(drops);
    }

    // REMOVAL_METHOD
    // REMOVAL_IMPL

    private static void clearPortal(Level level, BlockPos pos) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos scan = pos.offset(dx, 0, dz);
                if (level.getBlockState(scan).is(Blocks.END_PORTAL)) {
                    level.removeBlock(scan, false);
                }
            }
        }
    }
}
'''

# 26.x uses ServerLevel in its post-removal hook, while 1.20/1.21 expose onRemove.
def mixin(mc):
 src=COMMON
 if mc.startswith('26.'):
  src=src.replace('import net.minecraft.world.level.Level;','import net.minecraft.world.level.Level;\nimport net.minecraft.server.level.ServerLevel;')
  src=src.replace('    // REMOVAL_METHOD\n    // REMOVAL_IMPL','''    @Inject(method = "affectNeighborsAfterRemoval", at = @At("TAIL"))
    private void breakableframes$removed(ServerLevel world, BlockPos pos, boolean moved, CallbackInfo info) {
        if (getBlock() == Blocks.END_PORTAL_FRAME) clearPortal(world, pos);
    }''')
 else:
  src=src.replace('    // REMOVAL_METHOD\n    // REMOVAL_IMPL','''    @Inject(method = "onRemove", at = @At("TAIL"))
    private void breakableframes$removed(Level world, BlockPos pos, BlockState next, boolean moved, CallbackInfo info) {
        if (getBlock() == Blocks.END_PORTAL_FRAME && !next.is(Blocks.END_PORTAL_FRAME) && !world.isClientSide) {
            clearPortal(world, pos);
        }
    }''')
 return src


def build_gradle(mc,loader):
 jdk=JDK[mc]
 if loader=='fabric':
  # Loom remap on 1.20.1/1.21.1, native for 26.x.
  modern=mc.startswith('26.')
  plugin='net.fabricmc.fabric-loom' if modern else 'fabric-loom'
  loom_version='1.18.2' if modern else '1.7.4' if mc=='1.20.1' else '1.10-SNAPSHOT'
  dep='implementation' if modern else 'modImplementation'
  mappings='' if modern else '    mappings loom.officialMojangMappings()\n'
  return f'''plugins {{ id '{plugin}' version '{loom_version}' }}
version = '1.0.0'
group = 'dev.minemods'
base.archivesName = 'breakableframes-{mc}-{loader}'
repositories {{ mavenCentral() }}
dependencies {{
    minecraft 'com.mojang:minecraft:{mc}'
{mappings}    {dep} 'net.fabricmc:fabric-loader:0.19.5'
    {dep} 'net.fabricmc.fabric-api:fabric-api:{FABRIC_API[mc]}'
}}
java.toolchain.languageVersion = JavaLanguageVersion.of({max(21,jdk)})
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''
 if mc=='1.20.1':
  dep=('net.minecraftforge:forge:1.20.1-47.4.10' if loader=='forge' else 'net.neoforged:forge:1.20.1-47.1.106')
  return f'''plugins {{
    id 'java'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
    id 'org.spongepowered.mixin' version '0.7.+'
}}
version = '1.0.0'
group = 'dev.minemods'
base.archivesName = 'breakableframes-{mc}-{loader}'
java.toolchain.languageVersion = JavaLanguageVersion.of(17)
minecraft {{ mappings channel: 'official', version: '{mc}' }}
repositories {{
    maven {{ url='https://repo.spongepowered.org/repository/maven-public/' }}
    maven {{ url='https://maven.neoforged.net/releases' }}
}}
dependencies {{
    minecraft '{dep}'
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}}
mixin {{ add sourceSets.main, 'breakableframes.refmap.json'; config 'breakableframes.mixins.json' }}
jar {{ manifest {{ attributes('MixinConfigs': 'breakableframes.mixins.json') }} }}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release=17 }}
'''
 return f'''plugins {{ id 'java-library'; id 'net.neoforged.moddev' version '2.0.148'; id 'idea' }}
version='1.0.0'
group='dev.minemods'
base.archivesName='breakableframes-{mc}-{loader}'
java.toolchain.languageVersion=JavaLanguageVersion.of({jdk})
neoForge {{
    version='{NEO[mc]}'
    runs {{ client {{ client() }}; server {{ server() }} }}
    mods {{ breakableframes {{ sourceSet(sourceSets.main) }} }}
}}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''

def meta(mc,loader):
 if loader=='fabric':
  return {
   'schemaVersion':1,'id':'breakableframes','version':'1.0.0',
   'name':'Breakable End Portal Frames',
   'description':'Mine and reclaim vanilla End Portal Frames. Eyes drop separately and existing portals close when a frame is broken.',
   'authors':['MineMods'],'license':'MIT',
   'environment':'*',
   'entrypoints':{'main':['dev.minemods.breakableframes.BreakableFrames']},
   'mixins':['breakableframes.mixins.json'],
   'depends':{'fabricloader':'>=0.15.0','minecraft':mc,'java':'>='+str(JDK[mc])}
  }
 if mc=='1.20.1':
  return f'''modLoader="javafml"
loaderVersion="[47,)"
license="MIT"
[[mods]]
modId="breakableframes"
version="1.0.0"
displayName="Breakable End Portal Frames"
description="Mine and reclaim End Portal Frames; eyes are returned and the portal closes."
[[dependencies.breakableframes]]
modId="minecraft"
mandatory=true
versionRange="[{mc},{mc}]"
ordering="NONE"
side="BOTH"
'''
 return f'''modLoader="javafml"
loaderVersion="[1,)"
license="MIT"
[[mods]]
modId="breakableframes"
version="1.0.0"
displayName="Breakable End Portal Frames"
description="Mine and reclaim End Portal Frames; eyes are returned and the portal closes."
[[mixins]]
config="breakableframes.mixins.json"
[[dependencies.breakableframes]]
modId="neoforge"
type="required"
versionRange="[{NEO[mc]},)"
ordering="NONE"
side="BOTH"
[[dependencies.breakableframes]]
modId="minecraft"
type="required"
versionRange="[{mc},{mc}]"
ordering="NONE"
side="BOTH"
'''

def gen(mc,loader):
 root=ROOT/f'breakableframes-{mc}-{loader}'
 put(root,'settings.gradle',"pluginManagement { repositories { maven { url 'https://maven.minecraftforge.net' }; maven { url 'https://maven.neoforged.net/releases' }; maven { url 'https://maven.fabricmc.net/' }; maven { url 'https://repo.spongepowered.org/repository/maven-public/' }; gradlePluginPortal() } }\nrootProject.name = 'breakableframes'\n")
 put(root,'build.gradle',build_gradle(mc,loader))
 put(root,'gradle.properties','org.gradle.jvmargs=-Xmx2G\norg.gradle.daemon=false\n')
 gradle='8.8' if mc=='1.20.1' and loader=='fabric' else '8.14.5' if mc=='1.20.1' or mc=='1.21.1' and loader=='fabric' else '9.7.1' if loader=='fabric' else '9.2.1'
 gradle_java=21 if mc=='1.20.1' and loader=='fabric' else JDK[mc]
 put(root,'build-profile.json',{'slug':'breakableframes','version':'1.0.0','minecraft':mc,'loader':loader,'java':str(gradle_java),'gradle':gradle,'jar_glob':f'breakableframes-{mc}-{loader}-1.0.0.jar'})
 put(root,'src/main/java/dev/minemods/breakableframes/mixin/FrameStateMixin.java',mixin(mc))
 if loader=='fabric':
  put(root,'src/main/java/dev/minemods/breakableframes/BreakableFrames.java','''package dev.minemods.breakableframes;
import net.fabricmc.api.ModInitializer;
public final class BreakableFrames implements ModInitializer {
    @Override public void onInitialize() { }
}
''')
 else:
  imp='net.minecraftforge.fml.common.Mod' if mc=='1.20.1' else 'net.neoforged.fml.common.Mod'
  put(root,'src/main/java/dev/minemods/breakableframes/BreakableFrames.java',f'''package dev.minemods.breakableframes;
import {imp};
@Mod("breakableframes")
public final class BreakableFrames {{
    public BreakableFrames() {{ }}
}}
''')
 put(root,'src/main/resources/breakableframes.mixins.json',{'required':True,'minVersion':'0.8','package':'dev.minemods.breakableframes.mixin','compatibilityLevel':'JAVA_'+str(JDK[mc]),'mixins':['FrameStateMixin'],'injectors':{'defaultRequire':1}})
 if loader=='fabric':put(root,'src/main/resources/fabric.mod.json',meta(mc,loader))
 else:put(root, 'src/main/resources/META-INF/'+('mods.toml' if mc=='1.20.1' else 'neoforge.mods.toml'),meta(mc,loader))
 put(root,'src/main/resources/pack.mcmeta',{'pack':{'pack_format':PACK[mc],'description':'Breakable End Portal Frames - native mining tags'}})
 put(root,'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['minecraft:end_portal_frame']})
 put(root,'README.md',f'''# Breakable End Portal Frames 1.0.0 (Minecraft {mc} {loader})
Mine vanilla End Portal Frames at hardness 5.0, drop the End Portal Frame block and (when present) one Eye of Ender. Upon removing a frame, nearby active End Portal blocks are cleared. No recipes or textures, no custom item.

## Build
Requires Java {JDK[mc]} for Minecraft ({gradle_java} for build tools), Gradle {gradle}.
`gradle --no-daemon clean build` -> `build/libs/breakableframes-{mc}-{loader}-1.0.0.jar`.

## Tests
* Launch client and dedicated server; mod loads and no Mixin errors.
* Mine an End Portal Frame with a diamond pickaxe, verify frame drop.
* Mine a frame with an eye, verify separate Eye of Ender.
* Activate stronghold portal, break one frame, verify 3x3 active blocks vanish.
* Re-place frames, fill eyes, confirm portal can be reactivated.
* Creative breaking generates no drops.

## Caveat
Client/server side should match for correct breaking progress, and loaders are not interchangeable. Unverified in actual game until CI/in-game test.
''')
 return root

for mc,loaders in VERSIONS.items():
 for loader in loaders:gen(mc,loader)
put(ROOT,'README.md','# Breakable End Portal Frames - 11 independently buildable ports\n\nMinecraft 1.20.1 Forge/NeoForge/Fabric, 1.21.1 NeoForge/Fabric, 26.1.2 NeoForge/Fabric, 26.2 NeoForge/Fabric, 26.3 NeoForge/Fabric.\n\nEach folder is self-contained and compatible with the MngkLrd/gradlejar Build Hub. Download a compiled JAR from successful GitHub Actions releases after CI builds. Source ZIP alone is not a compiled mod.\n')
put(ROOT,'build-manifest.json',{'mod':'breakableframes','version':'1.0.0','targets':[{'mc':mc,'loader':loader}for mc,ls in VERSIONS.items() for loader in ls]})
zipname=ROOT.parent/'BreakableEndPortalFrames-1.0.0-all-11-source.zip'
with zipfile.ZipFile(zipname,'w',zipfile.ZIP_DEFLATED,compresslevel=8) as z:
 for f in ROOT.rglob('*'):
  if f.is_file():z.write(f,f.relative_to(ROOT.parent))
print(f'Created {zipname} ({zipname.stat().st_size:,} bytes); files={len(list(ROOT.rglob("*")))}')