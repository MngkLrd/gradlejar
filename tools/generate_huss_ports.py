from pathlib import Path
import json, re, shutil, sys
ROOT=Path(__file__).resolve().parents[1]
P=ROOT/'projects'
BASE=P/'husshusshuss-neoforge-1.21.1'
PKG=Path('src/main/java/dev/ataflore/huss')
RES=Path('src/main/resources')
VERSION='0.3.0-beta'
loader,mc=sys.argv[1:3]
if loader=='neoforge' and mc=='1.21.1':
    print(BASE.relative_to(ROOT)); sys.exit(0)
legacy=mc=='1.20.1'
modern=mc.startswith('26.')
fabric=loader=='fabric'
forge=legacy and not fabric
dest=P/f'husshusshuss-{loader}-{mc}'
template=P/f'campfirebatch-{loader}-{mc}'
def read(p):return p.read_text(encoding='utf-8')
def write(p,s):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(s,encoding='utf-8')
def js(p,o):write(p,json.dumps(o,indent=2)+'\n')
def java(name,s):write(dest/PKG/(name+'.java'),s)
def src(name):return read(BASE/PKG/(name+'.java'))
if dest.exists(): shutil.rmtree(dest)
shutil.copytree(BASE,dest,ignore=shutil.ignore_patterns('build','.gradle','run'))
gradle=read(template/'build.gradle').replace('dev.minemods.campfirebatch','dev.ataflore.huss').replace('campfirebatch','husshusshuss').replace("'1.0.1'",repr(VERSION))
gradle+= '\n'+read(BASE/'build.gradle')[read(BASE/'build.gradle').index("tasks.register('generateAssets'"):]
write(dest/'build.gradle',gradle)
write(dest/'settings.gradle',read(template/'settings.gradle').replace('campfirebatch','husshusshuss'))
shutil.copyfile(template/'gradle.properties',dest/'gradle.properties')
profile=json.loads(read(template/'build-profile.json'))
profile.update(slug='husshusshuss',version=VERSION,jar_glob=f'husshusshuss-{loader}-mc{mc}-{VERSION}.jar')
for key in ('minimum_loader_version','compile_loader_version'):profile.pop(key,None)
js(dest/'build-profile.json',profile)
shutil.copyfile(template/RES/'pack.mcmeta',dest/RES/'pack.mcmeta')
(dest/RES/'META-INF/neoforge.mods.toml').unlink()
if fabric:
    meta=json.loads(read(template/RES/'fabric.mod.json'))
    meta.update(id='husshusshuss',version=VERSION,name='Huss Huss Huss',description='Everyone goes out partying. Except the villagers who grind.',authors=['ataflore'],entrypoints={'main':['dev.ataflore.huss.HussMod']})
    meta.pop('mixins',None)
    if modern:meta['mixins']=['husshusshuss.mixins.json']
    else:meta['entrypoints']['client']=['dev.ataflore.huss.client.ClientEvents']
    js(dest/RES/'fabric.mod.json',meta)
else:
    metapath='META-INF/mods.toml' if legacy else 'META-INF/neoforge.mods.toml'
    meta=read(template/RES/metapath).replace('campfirebatch','husshusshuss').replace('1.0.1',VERSION)
    meta=re.sub(r'displayName\s*=\s*"[^"]*"','displayName="Huss Huss Huss"',meta)
    if modern:meta+='\n[[mixins]]\nconfig="husshusshuss.mixins.json"\n'
    write(dest/RES/metapath,meta)
recipe=dest/RES/'data/husshusshuss/recipe/discipline_whistle.json'
data=json.loads(read(recipe))
adv=dest/RES/'data/husshusshuss/advancement/recipes/tools/discipline_whistle.json'
if legacy:
    data['result']['item']=data['result'].pop('id')
    recipe.unlink()
    js(recipe.parent.parent/'recipes'/recipe.name,data)
    if adv.exists():
        a=json.loads(read(adv))
        # 1.20.1 item predicate expects an array of identifiers.
        for c in a.get('criteria',{}).values():
            for i in c.get('conditions',{}).get('items',[]):
                if isinstance(i.get('items'),str):i['items']=[i['items']]
        adv.unlink()
        js(adv.parents[3]/'advancements/recipes/tools'/adv.name,a)
if modern:
    data['key']={k:(v['item'] if isinstance(v,dict) else v) for k,v in data['key'].items()}
    js(recipe,data)
    js(dest/RES/'assets/husshusshuss/items/discipline_whistle.json',{'model':{'type':'minecraft:model','model':'husshusshuss:item/discipline_whistle'}})
    # Recipe discovery remains automatic via the vanilla recipe manager; no stale predicate schema.
    if adv.exists():adv.unlink()
# Original shared audio is not regenerated or fetched from the internet.
# All loaders use precisely the same checked OGG asset.
mod=src('HussMod')
training=src('training/Training')
item=src('item/DisciplineWhistle')
model=src('client/TrainingModel')
renderer=src('client/TrainingRenderer')
client=src('client/ClientEvents')
if forge:
    mod=mod.replace('net.neoforged.bus.api','net.minecraftforge.eventbus.api').replace('net.neoforged.fml.common.Mod','net.minecraftforge.fml.common.Mod').replace('net.neoforged.neoforge.common.NeoForge','net.minecraftforge.common.MinecraftForge').replace('net.neoforged.neoforge.event.','net.minecraftforge.event.').replace('net.neoforged.neoforge.registries.DeferredRegister','net.minecraftforge.registries.DeferredRegister').replace('net.neoforged.neoforge.registries.DeferredHolder','net.minecraftforge.registries.RegistryObject')
    mod=mod.replace('DeferredHolder<MobEffect,MobEffect>','RegistryObject<MobEffect>').replace('DeferredHolder<Item,Item>','RegistryObject<Item>').replace('DeferredHolder<SoundEvent,SoundEvent>','RegistryObject<SoundEvent>').replace('Holder<MobEffect> trainingEffect(){return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TRAINING.get());}','MobEffect trainingEffect(){return TRAINING.get();}')
    mod=mod.replace('DeferredRegister.create(Registries.ITEM,ID)','DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS,ID)').replace('DeferredRegister.create(Registries.MOB_EFFECT,ID)','DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS,ID)').replace('DeferredRegister.create(Registries.SOUND_EVENT,ID)','DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS,ID)')
    mod=mod.replace('public HussMod(IEventBus bus) {','public HussMod() {\n        IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();').replace('NeoForge.EVENT_BUS','MinecraftForge.EVENT_BUS').replace('ResourceLocation.fromNamespaceAndPath(ID,"huss")','new ResourceLocation(ID,"huss")')
    training=training.replace('net.neoforged.neoforge.event.tick.EntityTickEvent','net.minecraftforge.event.entity.living.LivingEvent').replace('net.neoforged.neoforge.event.entity.player','net.minecraftforge.event.entity.player').replace('EntityTickEvent.Post','LivingEvent.LivingTickEvent')
    client=client.replace('net.neoforged.api.distmarker','net.minecraftforge.api.distmarker').replace('net.neoforged.neoforge.client.event.','net.minecraftforge.client.event.').replace('net.neoforged.neoforge.common.util.TriState','net.minecraftforge.eventbus.api.Event').replace('net.neoforged.bus.api.SubscribeEvent','net.minecraftforge.eventbus.api.SubscribeEvent').replace('import net.neoforged.fml.common.EventBusSubscriber;','import net.minecraftforge.fml.common.Mod;').replace('@EventBusSubscriber','@Mod.EventBusSubscriber').replace('EventBusSubscriber.Bus.MOD','Mod.EventBusSubscriber.Bus.MOD').replace('EventBusSubscriber.Bus.GAME','Mod.EventBusSubscriber.Bus.FORGE').replace('e.setCanRender(TriState.TRUE)','e.setResult(Event.Result.ALLOW)')
if legacy:
    training=training.replace('new ClientboundUpdateMobEffectPacket(v.getId(),marker,false)','new ClientboundUpdateMobEffectPacket(v.getId(),marker)')
    renderer=renderer.replace('ResourceLocation.fromNamespaceAndPath(HussMod.ID,"training_villager")','new ResourceLocation(HussMod.ID,"training_villager")')
if fabric:
    idtype='Identifier' if modern else 'ResourceLocation'
    idexpr=('new ResourceLocation(ID,path)' if legacy else f'{idtype}.fromNamespaceAndPath(ID,path)')
    effecttype='MobEffect' if legacy else 'Holder<MobEffect>'
    effectexpr='TRAINING' if legacy else 'BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TRAINING)'
    props='new Item.Properties().stacksTo(1)' if not modern else 'new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id("discipline_whistle"))).stacksTo(1)'
    tab='ItemGroupEvents.modifyEntriesEvent' if not modern else 'CreativeModeTabEvents.modifyOutputEvent'
    tabimport='net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents' if not modern else 'net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents'
    accept='e.accept(WHISTLE.get())'
    mod=f'''package dev.ataflore.huss;
import dev.ataflore.huss.training.Training;
import dev.ataflore.huss.item.DisciplineWhistle;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import {tabimport};
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.{idtype};
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.npc.Villager;
import java.util.function.Supplier;
public final class HussMod implements ModInitializer {{
 public static final String ID="husshusshuss";
 private static {idtype} id(String path){{return {idexpr};}}
 public static final Supplier<Item> WHISTLE=()->HussMod.whistle;
 public static final Supplier<SoundEvent> HUSS=()->HussMod.huss;
 private static Item whistle;
 private static SoundEvent huss;
 private static MobEffect TRAINING;
 public static {effecttype} trainingEffect(){{return {effectexpr};}}
 public void onInitialize(){{
  whistle=Registry.register(BuiltInRegistries.ITEM,id("discipline_whistle"),new DisciplineWhistle({props}));
  huss=Registry.register(BuiltInRegistries.SOUND_EVENT,id("huss"),SoundEvent.createVariableRangeEvent(id("huss")));
  TRAINING=Registry.register(BuiltInRegistries.MOB_EFFECT,id("training"),new MobEffect(MobEffectCategory.NEUTRAL,0xffa33a){{}});
  {tab}(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e->{{{accept};}});
  UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{{
   if(!player.getItemInHand(hand).is(WHISTLE.get()) || !(entity instanceof Villager v) || v.isBaby())return InteractionResult.PASS;
   if(world instanceof ServerLevel level) Training.whistle(level,player,v);
   return InteractionResult.SUCCESS;
  }});
  ServerTickEvents.END_WORLD_TICK.register(Training::tickWorld);
 }}
}}
'''
    if legacy:mod=mod.replace('e.accept(WHISTLE.get())','e.accept(WHISTLE.get())')
    training=re.sub(r'import net\.(?:neoforged|minecraftforge)\.[^\n]+;\n','',training)
    training=re.sub(r'    public static void onInteract\([\s\S]*?(?=    public static void whistle)','',training)
    training=training.replace('public static void onTick(EntityTickEvent.Post event) {\n        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level)) return;', '''public static void tickWorld(ServerLevel world) {
        for (Villager v : new java.util.ArrayList<>(ACTIVE.keySet())) {
            if (v==null || v.level()!=world) continue;
            if (v.isRemoved()) { ACTIVE.remove(v); continue; }
            tick(v,world);
        }
    }
    private static void tick(Villager v,ServerLevel level) {''')
    if not modern:
        client='''package dev.ataflore.huss.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.EntityType;
public final class ClientEvents implements ClientModInitializer {
 public void onInitializeClient(){
  EntityModelLayerRegistry.registerModelLayer(TrainingRenderer.LAYER,TrainingModel::layer);
  EntityRendererRegistry.register(EntityType.VILLAGER,TrainingRenderer::new);
 }
}
'''
        renderer=renderer.replace('public TrainingRenderer(EntityRendererProvider.Context context) {','private final net.minecraft.client.model.VillagerModel<net.minecraft.world.entity.npc.Villager> vanilla,training;\n    public TrainingRenderer(EntityRendererProvider.Context context) {').replace('this.model=new TrainingModel(context.bakeLayer(LAYER));','this.vanilla=this.model;\n        this.training=new TrainingModel(context.bakeLayer(LAYER));').replace('\n}\n','''
    @Override public void render(net.minecraft.world.entity.npc.Villager v,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
        this.model=!v.isBaby() && v.hasEffect(HussMod.trainingEffect())?training:vanilla;
        super.render(v,yaw,partial,pose,buffers,light);
    }
}
''')
if modern:
    if fabric:mod=mod.replace('ServerTickEvents.END_WORLD_TICK.register(Training::tickWorld);','ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerLevel world:server.getAllLevels()) Training.tickWorld(world);});')
    training=re.sub(r'player\.displayClientMessage\((Component\.translatable\([^\n]+?\)),true\)',r'notifyPlayer(player,\1)',training)
    training=training.replace('    private Training() {}','    private Training() {}\n    private static void notifyPlayer(Player p, Component text) {\n        if (p instanceof net.minecraft.server.level.ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(text));\n    }')
    mod=mod.replace('ResourceLocation','Identifier').replace('net.minecraft.world.entity.npc.Villager','net.minecraft.world.entity.npc.villager.Villager')
    if not fabric:mod=mod.replace('new Item.Properties().stacksTo(1)','new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,"discipline_whistle"))).stacksTo(1)')
    training=training.replace('net.minecraft.world.entity.npc.Villager','net.minecraft.world.entity.npc.villager.Villager')
    training=training.replace('InteractionResult.sidedSuccess(event.getLevel().isClientSide)','InteractionResult.SUCCESS').replace('player.getCooldowns().isOnCooldown(HussMod.WHISTLE.get())','player.getCooldowns().isOnCooldown(new net.minecraft.world.item.ItemStack(HussMod.WHISTLE.get()))').replace('player.getCooldowns().addCooldown(HussMod.WHISTLE.get(),40)','player.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(HussMod.WHISTLE.get()),40)').replace('.broadcast(v,new Clientbound','.sendToTrackingPlayers(v,new Clientbound').replace('MobEffects.MOVEMENT_SPEED','MobEffects.SPEED').replace('MobEffects.DAMAGE_RESISTANCE','MobEffects.RESISTANCE').replace('v.hasImpulse=true;','')
    item=item.replace('import net.minecraft.world.InteractionResultHolder;','import net.minecraft.world.InteractionResult;').replace('InteractionResultHolder<ItemStack> use','InteractionResult use').replace('InteractionResultHolder.sidedSuccess(stack, level.isClientSide)','InteractionResult.SUCCESS')
    # Preserve vanilla renderer; carry synchronized training time in the extracted render state.
    # No state is shared between villagers. Model animation is applied at submit time.
    java('client/HussRenderState','''package dev.ataflore.huss.client;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import java.util.Map;
import java.util.WeakHashMap;
public final class HussRenderState {
 public static final Map<VillagerRenderState,Float> TIME=new WeakHashMap<>();
 private HussRenderState(){}
}
''')
    java('client/mixin/VillagerRendererMixin','''package dev.ataflore.huss.client.mixin;
import dev.ataflore.huss.HussMod;
import dev.ataflore.huss.training.Training;
import dev.ataflore.huss.client.HussRenderState;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",at=@At("TAIL"))
 private void huss$extract(Villager v,VillagerRenderState s,float partial,CallbackInfo ci){
  var effect=v.getEffect(HussMod.trainingEffect());
  if(effect==null||v.isBaby())HussRenderState.TIME.remove(s);
  else HussRenderState.TIME.put(s,Math.max(0,Training.DURATION+2-effect.getDuration()+partial));
 }
}
''')
    # Reuse the authored geometry and all three exercise pose curves from 0.3.0.
    geom=model[model.index('        PartDefinition right='):model.index('        return LayerDefinition.create')]
    poses=model[model.index('        float t=elapsed%'):model.rindex('\n    }\n}')]
    poses=poses.replace('root()','huss$root')
    helper=model[model.index('    private static float hit('):model.index('    @Override public void setupAnim')]
    java('client/mixin/VillagerModelMixin','''package dev.ataflore.huss.client.mixin;
import dev.ataflore.huss.client.HussRenderState;
import dev.ataflore.huss.training.Training;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(VillagerModel.class)
public abstract class VillagerModelMixin {
 @Unique private ModelPart huss$root;
 @Inject(method="createBodyModel",at=@At("RETURN"))
 private static void huss$mesh(CallbackInfoReturnable<MeshDefinition> cir){
  PartDefinition root=cir.getReturnValue().getRoot();
'''+geom+'''
 }
 @Inject(method="<init>",at=@At("RETURN"))
 private void huss$init(ModelPart root,CallbackInfo ci){
  huss$root=root;
  if(root.hasChild("huss_left_arm"))root.getChild("huss_left_arm").visible=false;
  if(root.hasChild("huss_right_arm"))root.getChild("huss_right_arm").visible=false;
 }
'''+helper+'''
 @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V",at=@At("TAIL"))
 private void huss$pose(VillagerRenderState state,CallbackInfo ci){
  if(huss$root==null||!huss$root.hasChild("huss_left_arm"))return;
  ModelPart leftArm=huss$root.getChild("huss_left_arm"),rightArm=huss$root.getChild("huss_right_arm");
  Float elapsed=HussRenderState.TIME.get(state);
  leftArm.visible=rightArm.visible=elapsed!=null;
  huss$root.getChild("arms").visible=elapsed==null;
  if(elapsed==null)return;
  ModelPart head=huss$root.getChild("head"),body=huss$root.getChild("body"),leftLeg=huss$root.getChild("left_leg"),rightLeg=huss$root.getChild("right_leg");
  ModelPart leftFore=leftArm.getChild("forearm"),rightFore=rightArm.getChild("forearm");
'''+poses+'''
 }
}
''')
    js(dest/RES/'husshusshuss.mixins.json',{'required':True,'package':'dev.ataflore.huss.client.mixin','compatibilityLevel':'JAVA_21','client':['VillagerRendererMixin','VillagerModelMixin'],'injectors':{'defaultRequire':1}})
    for p in ['client/TrainingModel','client/TrainingRenderer','client/ClientEvents']:(dest/PKG/(p+'.java')).unlink()
else:
    java('client/TrainingModel',model)
    java('client/TrainingRenderer',renderer)
    java('client/ClientEvents',client)
java('HussMod',mod)
java('training/Training',training)
java('item/DisciplineWhistle',item)
verify=read(BASE/'tools/verify_release.py')
if modern:
    verify=verify.replace('dev/ataflore/huss/client/TrainingModel.class','dev/ataflore/huss/client/mixin/VillagerModelMixin.class').replace('dev/ataflore/huss/client/TrainingRenderer.class','dev/ataflore/huss/client/mixin/VillagerRendererMixin.class').replace('dev/ataflore/huss/client/ClientEvents.class','dev/ataflore/huss/client/HussRenderState.class')
if legacy:verify=verify.replace('data/husshusshuss/recipe/','data/husshusshuss/recipes/')
verify+=r"""
with zipfile.ZipFile(sys.argv[1]) as z:
    from pathlib import Path
    profile=json.loads((Path(__file__).resolve().parents[1]/'build-profile.json').read_text())
    mc=profile['minecraft']
    modern=mc.startswith('26.')
    legacy=mc=='1.20.1'
    folder='recipes' if legacy else 'recipe'
    recipe=json.loads(z.read(f'data/husshusshuss/{folder}/discipline_whistle.json'))
    assert recipe['type']=='minecraft:crafting_shaped'
    assert recipe['category']=='equipment'
    assert recipe['pattern']==[' IG',' HI']
    ingredients={'I':'minecraft:iron_ingot','G':'minecraft:gold_nugget','H':'minecraft:goat_horn'}
    expected=ingredients if modern else {k:{'item':v} for k,v in ingredients.items()}
    assert recipe['key']==expected,recipe
    assert recipe['result']=={('item' if legacy else 'id'):'husshusshuss:discipline_whistle','count':1},recipe
    if modern:
        model=json.loads(z.read('assets/husshusshuss/items/discipline_whistle.json'))
        assert model=={'model':{'type':'minecraft:model','model':'husshusshuss:item/discipline_whistle'}}
        assert not any(n.startswith('data/husshusshuss/advancement/') for n in z.namelist())
    header=z.read('dev/ataflore/huss/HussMod.class')
    assert int.from_bytes(header[6:8],'big')==(69 if modern else 61 if legacy else 65)
    print('PASS: version-specific recipe JSON, item model, class bytecode target',mc)
"""
write(dest/'tools/verify_release.py',verify)
print(dest.relative_to(ROOT))
