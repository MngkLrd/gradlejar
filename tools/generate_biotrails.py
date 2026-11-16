from pathlib import Path
from PIL import Image
import json, textwrap, os, zipfile, hashlib, shutil

ROOT=Path(__file__).resolve().parents[1]
ROOT.mkdir(parents=True,exist_ok=True)
VERSION='0.1.0'
SOURCES={'lava':('minecraft:lava','Lava','Лава'), 'slime':('minecraft:slime_block','Slime','Слизь'), 'lichen':('minecraft:glow_lichen','Glow Lichen','Светящийся лишайник')}
# Vanilla-style deliberately clustered 16x16 art. Restricted ramps, no AA/noise.
PALETTES={
 'lava':['#7c422a','#9e5832','#b36d3e','#c5814d'],
 'slime':['#3b5f31','#537840','#638c4a','#7ba159'],
 'lichen':['#3e756e','#548e80','#65a294','#7bb3a2']
}
# Each mark consists of the ball and heel, with near-neighbor hue steps.
# Explicit 4-connected clusters. No random pixel sprinkling.
LEFT=[(3,2),(4,2),(5,2),(3,3),(4,3),(5,3),(3,4),(4,4),(5,4), (4,5),(4,6),(4,7), (3,9),(4,9),(5,9),(3,10),(4,10),(5,10)]
RIGHT=[(10,5),(11,5),(12,5),(10,6),(11,6),(12,6),(10,7),(11,7),(12,7), (11,8),(11,9),(11,10),(10,12),(11,12),(12,12),(10,13),(11,13),(12,13)]


def put(root,path,txt):
 p=root/path; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(txt,encoding='utf-8')
def dump(root,path,obj):put(root,path,json.dumps(obj,ensure_ascii=False,indent=2)+'\n')

JAVA_TRAIL_BLOCK='''package dev.biotrails.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A replaceable-by-time, non-colliding thin decal; scheduled server ticks survive world reloads. */
public final class TrailBlock extends Block {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int STAGE_TICKS = 160; // 4 stages = 32 seconds

    public TrailBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No outline, no cursor interception.
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getBlock() != this) return;
        int age = state.getValue(AGE);
        if (age >= 3) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
            level.scheduleTick(pos, this, STAGE_TICKS);
        }
    }
}
'''

JAVA_ENGINE='''package dev.biotrails.gameplay;

import dev.biotrails.block.TrailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RESOURCE_ID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.function.Supplier;

/** All actions are server-only: no client-only classes and no per-player network packets. */
public final class TrailEngine {
    private static final int ACTIVE_TICKS = 2400; // 2 minutes
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private static final TagKey<Block> LAVA = TagKey.create(Registries.BLOCK, RESOURCE_ID.fromNamespaceAndPath("biotrails", "lava_sources"));
    private static final TagKey<Block> SLIME = TagKey.create(Registries.BLOCK, RESOURCE_ID.fromNamespaceAndPath("biotrails", "slime_sources"));
    private static final TagKey<Block> LICHEN = TagKey.create(Registries.BLOCK, RESOURCE_ID.fromNamespaceAndPath("biotrails", "lichen_sources"));

    private final Supplier<? extends Block> lava;
    private final Supplier<? extends Block> slime;
    private final Supplier<? extends Block> lichen;

    public TrailEngine(Supplier<? extends Block> lava, Supplier<? extends Block> slime, Supplier<? extends Block> lichen) {
        this.lava = lava;
        this.slime = slime;
        this.lichen = lichen;
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickOne(player);
        }
        // If somebody disconnects, do not keep a permanent entry in our session map.
        if (server.getTickCount() % 1200 == 0) {
            ACTIVE.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        }
    }

    private void tickOne(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.isSpectator() || !player.isAlive()) { ACTIVE.remove(player.getUUID()); return; }
        long now = level.getGameTime();
        BlockPos feet = player.blockPosition();
        BlockState atFeet = level.getBlockState(feet);
        BlockState beneath = level.getBlockState(feet.below());
        BlockState contact = level.getBlockState(BlockPos.containing(player.getX(), player.getY() - 0.10, player.getZ()));
        Supplier<? extends Block> source = null;
        if (player.isInLava() || atFeet.is(LAVA) || contact.is(LAVA)) source = lava;
        else if (atFeet.is(SLIME) || beneath.is(SLIME) || contact.is(SLIME)) source = slime;
        else if (atFeet.is(LICHEN) || beneath.is(LICHEN) || contact.is(LICHEN)) source = lichen;

        Session session = ACTIVE.get(player.getUUID());
        if (source != null) {
            if (session == null) { session = new Session(); ACTIVE.put(player.getUUID(), session); }
            session.trail = source;
            session.expiresAt = now + ACTIVE_TICKS;
        }
        if (session == null) return;
        if (now > session.expiresAt) { ACTIVE.remove(player.getUUID()); return; }
        if (!player.onGround() || player.isPassenger() || player.isInWaterOrBubble() || now - session.lastAttempt < 5) return;
        double x = player.getX(), y = player.getY(), z = player.getZ();
        if (session.hasPrevious && ((x-session.lastX)*(x-session.lastX) + (z-session.lastZ)*(z-session.lastZ)) < 0.35) return;
        session.hasPrevious = true;
        session.lastX = x; session.lastZ = z; session.lastAttempt = now;
        BlockPos footprint = BlockPos.containing(x, y + 0.03, z);
        if (!level.getBlockState(footprint).isAir()) return; // Never destroy plants, liquids, carpet or other blocks.
        BlockPos floor = footprint.below();
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.isFaceSturdy(level, floor, Direction.UP)) return; // No floating footprints.
        Block block = session.trail.get();
        Direction dir = Direction.fromYRot(player.getYRot());
        BlockState track = block.defaultBlockState().setValue(TrailBlock.FACING, dir);
        if (level.setBlock(footprint, track, 3)) {
            level.scheduleTick(footprint, block, TrailBlock.STAGE_TICKS);
        }
    }

    private static final class Session {
        private Supplier<? extends Block> trail;
        private long expiresAt;
        private long lastAttempt = -100;
        private boolean hasPrevious;
        private double lastX, lastZ;
    }
}
'''

# 26.x onGround() method? modern might be onGround(), same.
# Valid server tick event families separated by loader and MC generation.

def forge_main(mc,neo=False):
 modern=mc!='1.20.1'; new=mc.startswith('26.')
 if neo and mc=='1.20.1':
  return forge_main(mc, False)
 if False:
  return '''package dev.biotrails;
import dev.biotrails.block.TrailBlock;
import dev.biotrails.gameplay.TrailEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.TickEvent;

@Mod(BioTrails.MODID)
public final class BioTrails {
 public static final String MODID = "biotrails";
 private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
 public static final RegistryObject<Block> LAVA = register("lava_trail");
 public static final RegistryObject<Block> SLIME = register("slime_trail");
 public static final RegistryObject<Block> LICHEN = register("lichen_trail");
 private final TrailEngine engine = new TrailEngine(LAVA,SLIME,LICHEN);
 private static RegistryObject<Block> register(String name){return BLOCKS.register(name, ()->new TrailBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)));}
 public BioTrails(){ BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus()); NeoForge.EVENT_BUS.addListener(this::tick); }
 private void tick(TickEvent.ServerTickEvent event){ if(event.phase == TickEvent.Phase.END) engine.tick(event.getServer()); }
}
'''
 if neo:
  imports='''import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
'''
  register='''private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
 public static final DeferredBlock<TrailBlock> LAVA = BLOCKS.registerBlock("lava_trail",TrailBlock::new, BioTrails::props);
 public static final DeferredBlock<TrailBlock> SLIME = BLOCKS.registerBlock("slime_trail",TrailBlock::new, BioTrails::props);
 public static final DeferredBlock<TrailBlock> LICHEN = BLOCKS.registerBlock("lichen_trail",TrailBlock::new, BioTrails::props);
 private static BlockBehaviour.Properties props(){return BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3);}
'''
  cons='''public BioTrails(IEventBus modBus){BLOCKS.register(modBus); NeoForge.EVENT_BUS.addListener(this::tick);}
 private void tick(ServerTickEvent.Post event){engine.tick(event.getServer());}'''
  if not new: # On NeoForge 1.21.1 the third argument is Properties, not Supplier.
   register=register.replace('BioTrails::props', 'props()')
 else:
  imports='''import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.TickEvent;
'''
  if mc=='1.20.1':
   register='''private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
 public static final RegistryObject<Block> LAVA = register("lava_trail");
 public static final RegistryObject<Block> SLIME = register("slime_trail");
 public static final RegistryObject<Block> LICHEN = register("lichen_trail");
 private static RegistryObject<Block> register(String name){return BLOCKS.register(name, ()->new TrailBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)));}
'''
   cons='''public BioTrails(){BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus()); MinecraftForge.EVENT_BUS.addListener(this::tick);}
 private void tick(TickEvent.ServerTickEvent event){if(event.phase == TickEvent.Phase.END) engine.tick(event.getServer());}'''
  elif mc=='1.21.1':
   register='''private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
 public static final RegistryObject<Block> LAVA = register("lava_trail");
 public static final RegistryObject<Block> SLIME = register("slime_trail");
 public static final RegistryObject<Block> LICHEN = register("lichen_trail");
 private static RegistryObject<Block> register(String name){return BLOCKS.register(name, ()->new TrailBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)));}
'''
   cons='''public BioTrails(){BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus()); MinecraftForge.EVENT_BUS.addListener(this::tick);}
 private void tick(TickEvent.ServerTickEvent event){if(event.phase == TickEvent.Phase.END) engine.tick(event.getServer());}'''
  else:
   register='''private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
 public static final RegistryObject<Block> LAVA = register("lava_trail");
 public static final RegistryObject<Block> SLIME = register("slime_trail");
 public static final RegistryObject<Block> LICHEN = register("lichen_trail");
 private static RegistryObject<Block> register(String name){return BLOCKS.register(name, ()->new TrailBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)));}
'''
   cons='''public BioTrails(){BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus()); MinecraftForge.EVENT_BUS.addListener(this::tick);}
 private void tick(TickEvent.ServerTickEvent.Post event){engine.tick(event.getServer());}'''
 return f'''package dev.biotrails;
import dev.biotrails.block.TrailBlock;
import dev.biotrails.gameplay.TrailEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
{imports}
@Mod(BioTrails.MODID)
public final class BioTrails {{
 public static final String MODID = "biotrails";
 {register}
 private final TrailEngine engine = new TrailEngine(LAVA,SLIME,LICHEN);
 {cons}
}}
'''

def fabric_main(mc):
 new=mc.startswith('26.'); ident='Identifier' if new else 'ResourceLocation'
 factory=f'{ident}.fromNamespaceAndPath(MODID,name)' if mc!='1.20.1' else f'new {ident}(MODID,name)'
 key=f'BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)).noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)' if new else 'BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().instabreak().lightLevel(s->s.getValue(TrailBlock.AGE)>1?1:3)'
 # For 1.21.1: create 1.21 vanilla resource keys? leave at first
 return f'''package dev.biotrails;
import dev.biotrails.block.TrailBlock;
import dev.biotrails.gameplay.TrailEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.{ident};
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.Registry;

public final class BioTrails implements ModInitializer {{
 public static final String MODID = "biotrails";
 public static final Block LAVA = register("lava_trail");
 public static final Block SLIME = register("slime_trail");
 public static final Block LICHEN = register("lichen_trail");
 private static Block register(String name) {{
   {ident} id={factory};
   return Registry.register(BuiltInRegistries.BLOCK,id,new TrailBlock({key}));
 }}
 @Override public void onInitialize() {{
   TrailEngine engine=new TrailEngine(()->LAVA,()->SLIME,()->LICHEN);
   ServerTickEvents.END_SERVER_TICK.register(engine::tick);
 }}
}}
'''

def fabric_client(mc):
 if mc.startswith('26.'):
  return None
 return '''package dev.biotrails.client;
import dev.biotrails.BioTrails;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
public final class BioTrailsClient implements ClientModInitializer {
 @Override public void onInitializeClient(){
   BlockRenderLayerMap.INSTANCE.putBlock(BioTrails.LAVA,RenderType.cutout());
   BlockRenderLayerMap.INSTANCE.putBlock(BioTrails.SLIME,RenderType.cutout());
   BlockRenderLayerMap.INSTANCE.putBlock(BioTrails.LICHEN,RenderType.cutout());
 }
}
'''
# 1.20.1 Fabric RenderType is named RenderLayer? Official Mojang names use RenderType, okay.

def neo_mod_toml(mc):
 start={'1.20.1':'47.1.106','1.21.1':'21.1.244','26.2':'26.2.0.88','26.3':'26.3.0.76'}[mc]
 return f'''modLoader="javafml"
loaderVersion="[1,)"
license="MIT"
[[mods]]
modId="biotrails"
version="{VERSION}"
displayName="Bio-Trails"
authors="BioTrails Contributors"
description="Vanilla-style glowing footprints after walking on lava, slime or glow lichen."
[[dependencies.biotrails]]
modId="neoforge"
type="required"
versionRange="[{start},)"
ordering="NONE"
side="BOTH"
[[dependencies.biotrails]]
modId="minecraft"
type="required"
versionRange="[{mc},{'1.20.2' if mc=='1.20.1' else '1.21.2' if mc=='1.21.1' else '26.3' if mc=='26.2' else '26.4'})"
ordering="NONE"
side="BOTH"
'''

def forge_mod_toml(mc):
 minforge={'1.20.1':'47.3.0','1.21.1':'52.1.0','26.2':'65.1.5','26.3':'66.1.0'}[mc]
 return f'''modLoader="javafml"
loaderVersion="[{minforge},)"
license="MIT"
[[mods]]
modId="biotrails"
version="{VERSION}"
displayName="Bio-Trails"
authors="BioTrails Contributors"
description="Vanilla-style glowing footprints after walking on lava, slime or glow lichen."
[[dependencies.biotrails]]
modId="forge"
mandatory=true
versionRange="[{minforge},)"
ordering="NONE"
side="BOTH"
[[dependencies.biotrails]]
modId="minecraft"
mandatory=true
versionRange="[{mc},{'1.20.2' if mc=='1.20.1' else '1.21.2' if mc=='1.21.1' else '26.3' if mc=='26.2' else '26.4'})"
ordering="NONE"
side="BOTH"
'''


def gradle_build(mc,loader):
 jdk=17 if mc=='1.20.1' else 21 if mc=='1.21.1' else 25
 if loader=='fabric':
  new=mc.startswith('26.')
  loom='net.fabricmc.fabric-loom' if new else 'fabric-loom'
  d='implementation' if new else 'modImplementation'
  mapping='' if new else 'mappings loom.officialMojangMappings()\n'
  return f'''plugins {{ id '{loom}' version '{'1.18-SNAPSHOT' if mc=='26.3' else '1.17.21' if mc=='26.2' else '1.11.8' if mc=='1.21.1' else '1.7-SNAPSHOT'}' }}
version = '{VERSION}'
group = 'dev.biotrails'
base.archivesName = 'biotrails-{mc}-{loader}'
repositories {{ mavenCentral() }}
dependencies {{
 minecraft 'com.mojang:minecraft:{mc}'
 {mapping} {d} 'net.fabricmc:fabric-loader:{'0.19.5' if new else '0.17.3' if mc=='1.21.1' else '0.15.11'}'
 {d} 'net.fabricmc.fabric-api:fabric-api:{'0.164.0+26.3' if mc=='26.3' else '0.161.0+26.2' if mc=='26.2' else '0.116.17+1.21.1' if mc=='1.21.1' else '0.92.2+1.20.1'}'
}}
java.toolchain.languageVersion = JavaLanguageVersion.of({jdk})
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''
 if loader=='neoforge' and mc!='1.20.1':
  neo={'1.21.1':'21.1.244','26.2':'26.2.0.88','26.3':'26.3.0.76'}[mc]
  return f'''plugins {{ id 'java-library'; id 'net.neoforged.moddev' version '2.0.148'; id 'idea' }}
version='{VERSION}'
group='dev.biotrails'
base.archivesName='biotrails-{mc}-{loader}'
java.toolchain.languageVersion=JavaLanguageVersion.of({jdk})
neoForge {{
 version='{neo}'
 runs {{ client {{ client() }}; server {{ server() }} }}
 mods {{ biotrails {{ sourceSet(sourceSets.main) }} }}
}}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''
 if loader=='neoforge' and mc=='1.20.1':
  return f'''plugins {{ id 'java'; id 'net.minecraftforge.gradle' version '[6.0,6.2)' }}
version='{VERSION}'
group='dev.biotrails'
base.archivesName='biotrails-{mc}-{loader}'
java.toolchain.languageVersion=JavaLanguageVersion.of({jdk})
minecraft {{ mappings channel: 'official', version: '{mc}' }}
repositories {{ maven {{ url='https://maven.neoforged.net/releases' }} }}
dependencies {{ minecraft 'net.neoforged:forge:1.20.1-47.1.106' }}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''
 # Forge 26.x ForgeGradle 7, legacy 6
 if mc.startswith('26.'):
  forgever={'26.2':'65.1.5','26.3':'66.1.0'}[mc]
  gradlev='7.0.40'
  deps=f"implementation minecraft.dependency('net.minecraftforge:forge:{mc}-{forgever}')\n annotationProcessor 'net.minecraftforge:eventbus-validator:7.0.5'"
 else:
  forgever={'1.20.1':'47.3.0','1.21.1':'52.1.20'}[mc]
  gradlev='[6.0,6.2)'
  deps=f"minecraft 'net.minecraftforge:forge:{mc}-{forgever}'"
 runs = "; runs { register('client'); register('server') }" if mc.startswith('26.') else ''
 return f'''plugins {{ id 'java'; id 'net.minecraftforge.gradle' version '{gradlev}' }}
version='{VERSION}'
group='dev.biotrails'
base.archivesName='biotrails-{mc}-{loader}'
java.toolchain.languageVersion=JavaLanguageVersion.of({jdk})
minecraft {{ mappings channel: 'official', version: '{mc}'{runs} }}
repositories {{ {'minecraft.mavenizer(it); maven fg.forgeMaven; maven fg.minecraftLibsMaven; mavenCentral()' if mc.startswith('26.') else "maven { url='https://maven.minecraftforge.net' }"} }}
dependencies {{ {deps} }}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={jdk} }}
'''

def gen_project(mc,loader):
 jdk=17 if mc=='1.20.1' else 21 if mc=='1.21.1' else 25
 gradle='8.9' if jdk==17 else '9.2.1' if jdk==21 else '9.6.0'
 # Gradle 9 with ForgeGradle 6 likely not compatible -> select Gradle 8.9 for FG6.
 if loader=='forge' and jdk!=25: gradle='8.9'
 if loader=='neoforge' and mc=='1.20.1':gradle='8.9'
 # Fabric 1.20.1 requires Gradle 8.9 for old loom; Fabric 1.21.1 uses Gradle 8.9.
 if loader=='fabric' and jdk!=25:gradle='8.9'
 root=ROOT/'projects'/f'biotrails-{mc}-{loader}'
 model='Identifier' if mc.startswith('26.') else 'ResourceLocation'
 put(root,'build.gradle',gradle_build(mc,loader))
 put(root,'settings.gradle',f'''pluginManagement {{ repositories {{ maven {{ url='https://maven.fabricmc.net/' }}; maven {{ url='https://maven.neoforged.net/releases' }}; maven {{ url='https://maven.minecraftforge.net' }}; gradlePluginPortal(); mavenCentral() }} }}
rootProject.name='biotrails-{mc}-{loader}'
''')
 put(root,'gradle.properties',f'org.gradle.jvmargs=-Xmx2G\norg.gradle.daemon=false\n')
 dump(root,'build-profile.json',{'slug':'biotrails','version':VERSION,'minecraft':mc,'loader':loader,'java':str(jdk),'gradle':gradle,'jar_glob':f'biotrails-{mc}-{loader}-{VERSION}.jar'})
 prefix=Path('src/main/java/dev/biotrails')
 put(root,prefix/'block/TrailBlock.java',JAVA_TRAIL_BLOCK)
 engine_java=JAVA_ENGINE.replace('RESOURCE_ID',model)
 if mc=='1.20.1': engine_java=engine_java.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
 put(root,prefix/'gameplay/TrailEngine.java',engine_java)
 main_java=fabric_main(mc) if loader=='fabric' else forge_main(mc,loader=='neoforge')
 if loader=='forge' and mc.startswith('26.'):
  main_java=main_java.replace('import net.minecraft.world.level.block.Block;', 'import net.minecraft.world.level.block.Block;\nimport net.minecraft.resources.Identifier;\nimport net.minecraft.resources.ResourceKey;\nimport net.minecraft.core.registries.Registries;')
  main_java=main_java.replace('new TrailBlock(BlockBehaviour.Properties.of().', 'new TrailBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID,name))).')
 put(root,prefix/'BioTrails.java',main_java)
 if loader=='fabric':
  client=fabric_client(mc)
  if client: put(root,prefix/'client/BioTrailsClient.java',client)
  dump(root,'src/main/resources/fabric.mod.json',{'schemaVersion':1,'id':'biotrails','version':VERSION,'name':'Bio-Trails','description':'Vanilla-style temporary glowing footprints.','environment':'*','entrypoints':{'main':['dev.biotrails.BioTrails'],**({'client':['dev.biotrails.client.BioTrailsClient']} if client else {})},'depends':{'fabricloader':'>=0.15.0' if jdk==17 else '>=0.16.0' if jdk==21 else '>=0.19.0','minecraft':mc,'java':'>='+str(jdk),'fabric-api':'*'}})
 else:
  put(root,'src/main/resources/META-INF/'+('neoforge.mods.toml' if loader=='neoforge' and mc!='1.20.1' else 'mods.toml'),neo_mod_toml(mc) if loader=='neoforge' and mc!='1.20.1' else (forge_mod_toml(mc).replace('[47.3.0,)', '[47.1.106,)') if loader=='neoforge' and mc=='1.20.1' else forge_mod_toml(mc)))
 pack=15 if mc=='1.20.1' else 34 if mc=='1.21.1' else 88 if mc=='26.2' else 97
 dump(root,'src/main/resources/pack.mcmeta', {'pack':{'description':'Bio-Trails resources','min_format':([97,1] if mc=='26.3' else 88), 'max_format':([97,1] if mc=='26.3' else 88)}} if mc.startswith('26.') else {'pack':{'pack_format':pack,'description':'Bio-Trails resources'}})
 resources=Path('src/main/resources'); assets=resources/'assets/biotrails'
 # On versions before 26.1, model's render_type hint ensures cutout where supported.
 for source in SOURCES:
  states={}
  for age in range(4):
   for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
    states[f'age={age},facing={facing}']={'model':f'biotrails:block/{source}_{age}','y':angle}
  dump(root,assets/f'blockstates/{source}_trail.json',{'variants':states})
  for age in range(4):
   model_obj={'ambientocclusion':False,'render_type':'minecraft:cutout','textures':{'trace':f'biotrails:block/{source}_{age}','particle':f'biotrails:block/{source}_{age}'},'elements':[{'from':[0,0.03125,0],'to':[16,0.03125,16],'faces':{'up':{'texture':'#trace','uv':[0,0,16,16]}}}]}
   dump(root,assets/f'models/block/{source}_{age}.json',model_obj)
   im=Image.new('RGBA',(16,16),(0,0,0,0))
   pts=LEFT+RIGHT; colors=PALETTES[source]
   for ix,(x,y) in enumerate(pts):
    # fade removes coherent material chunks rather than checkerboard noise
    if age == 1 and y in (2,5,10,13) and x in (3,10):continue
    if age == 2 and (y<4 or y>11):continue
    if age == 3 and (y<6 or y>9):continue
    if x in (3,10):k=0
    elif x in (5,12):k=2
    elif y in (2,5):k=3
    else:k=1
    c=colors[k].lstrip('#');rgb=tuple(int(c[i:i+2],16) for i in (0,2,4))
    im.putpixel((x,y),rgb+(255,))
   p=root/assets/f'textures/block/{source}_{age}.png'; p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
  tagpath=resources/f'data/biotrails/tags/{"blocks" if mc=="1.20.1" else "block"}/{source}_sources.json'
  dump(root,tagpath,{'replace':False,'values':[SOURCES[source][0]]})
 # English/RU: this only applies to internal non-item blocks, but useful for diagnostics.
 for lang,trans in [('en_us',{f'block.biotrails.{s}_trail':SOURCES[s][1]+' Footprint' for s in SOURCES}),('ru_ru',{f'block.biotrails.{s}_trail':SOURCES[s][2]+' (след)' for s in SOURCES})]:
  dump(root,assets/f'lang/{lang}.json',trans)
 put(root,'README.md',f'''# Bio-Trails {VERSION} / Minecraft {mc} / {loader}
\nRequires **Java {jdk}** and **Gradle {gradle}**. Build with `gradle --no-daemon clean build` in this directory.\nResult: `build/libs/biotrails-{mc}-{loader}-{VERSION}.jar`. Install on **server and client**. Fabric needs Fabric API.\n\n## Gameplay\nWalk on lava, slime blocks or glow lichen to charge your soles for 120 seconds. Footprint pairs appear on supported solid surfaces every ~0.6 block traveled. Each trace casts block-light level 1-3 and fades over four 8-second phases (32 seconds).\n\nSource blocks are controlled by datapack block tags in `data/biotrails/tags/`. No items, recipes, loot or new dimensions.\n\nLimitations: traces do not appear on half-height slabs, in water or where a block/plant already occupies their space. There is no in-game status HUD. Light recalculation has a cost: do not increase the frequency without testing servers. This source has not been runtime-tested in the actual game.\n''')
 return root

for mc in ('1.20.1','1.21.1','26.2','26.3'):
 for loader in ('forge','neoforge','fabric'):
  gen_project(mc,loader)
