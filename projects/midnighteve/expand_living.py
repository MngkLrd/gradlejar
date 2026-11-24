#!/usr/bin/env python3
"""Living Winter: real entity, route story, staged physical displays and fireworks."""
import expand_exploration as e
import json, struct, zlib
g,t=e.g,e.t
S,R,D,A,J=e.S,e.R,e.D,e.A,e.J
write,text,mc,mod=t.write,t.text,t.mc,t.mod
edit=t.edit

# A model-sized UV atlas: six different face rectangles per cuboid, 1 texel per unit.
# Each region uses material-specific ramps and connected local clusters.
pal={"f":"526c48","F":"71875a","s":"3b513b","h":"8d9d70",
     "w":"9a7850","W":"bb9460","v":"76573d","V":"d3b17a",
     "a":"d5a553","A":"f1cb78","b":"aa773d","B":"8c5c37",
     "i":"302d29","c":"c6bfa7","C":"ece0c3","r":"a44c41"}
atlas=[[(0,0,0,0)]*64 for _ in range(32)]
def face(x,y,w,h,base,pattern=None):
    for yy in range(h):
        for xx in range(w):
            k=pattern[yy][xx] if pattern else base
            assert 0<=x+xx<64 and 0<=y+yy<32
            atlas[y+yy][x+xx]=g.rgba(pal[k])
def cube(u,v,w,h,d,front,side,top,bottom,back):
    face(u+d,v+d,w,h,front[0][0],front)
    face(u,v+d,d,h,side)
    face(u+d+w,v+d,d,h,side)
    face(u+2*d+w,v+d,w,h,back)
    face(u+d,v,w,d,top)
    face(u+d+w,v,w,d,bottom)
# Timber body, copper-lantern belly, knit hood and exposed little face.
cube(0,0,8,8,6,[
 "WWwwwwwv","WwBBBBwv","wwBaaBwv","wwBAaBwv",
 "wwBaaBwv","wwBbaBwv","vwBBBBwv","vvwwwwvv"],"w","W","v","w")
# Side wood grain is deliberately staggered, not uniform outlines.
for x,y in [(1,8),(16,9),(23,9),(24,12)]:
    face(x,y,2,2,"v")
cube(28,0,6,5,6,["FFFFFF","FCCCCf","Ficiif","FCcccf","sffffs"],"f","F","s","f")
face(29,7,2,2,"s");face(43,7,2,2,"s");face(49,8,2,2,"F")
for u in (0,10):
    cube(u,16,2,5,3,["FF","ff","fs","wv","vv"],"f","F","v","s")
for u in (22,34):
    cube(u,16,3,3,3,["www","wvv","vvv"],"v","w","v","v")
cube(48,16,2,2,2,["CC","cc"],"c","C","c","c")
def chunk(k,v):return struct.pack(">I",len(v))+k+v+struct.pack(">I",zlib.crc32(k+v)&0xffffffff)
raw=b"".join(b"\0"+b"".join(bytes(px) for px in row) for row in atlas)
p=A/"textures/entity/hearthling.png";p.parent.mkdir(parents=True,exist_ok=True)
p.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",64,32,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw))+chunk(b"IEND",b""))

# Separate registry, no client imports in server code.
text(J/"living/LivingContent.java",'''package dev.mngklrd.midnighteve.living;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class LivingContent {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"midnighteve");
    public static final Supplier<EntityType<Hearthling>> HEARTHLING=TYPES.register("hearthling",()->EntityType.Builder.of(Hearthling::new,MobCategory.CREATURE).sized(.625F,1.125F).clientTrackingRange(8).build("midnighteve:hearthling"));
    public static final DeferredItem<Item> EGG=MidnightEve.ITEMS.register("hearthling_spawn_egg",()->new DeferredSpawnEggItem(HEARTHLING,0x526c48,0xd5a553,new Item.Properties()));
    public static final DeferredBlock<FestivalDisplay> DISPLAY=MidnightEve.BLOCKS.register("festival_display",()->new FestivalDisplay(BlockBehaviour.Properties.of().strength(1.2F).sound(SoundType.WOOD).noOcclusion().noLootTable().lightLevel(s->s.getValue(FestivalDisplay.READY)&&s.getValue(FestivalDisplay.KIND)==2?8:0)));
    public static void init(IEventBus bus){
        TYPES.register(bus);bus.addListener(LivingContent::attributes);bus.addListener(LivingContent::placement);
        bus.addListener(LivingContent::creative);
        NeoForge.EVENT_BUS.addListener(WinterPostRoute::clonePlayer);
    }
    private static void attributes(EntityAttributeCreationEvent e){e.put(HEARTHLING.get(),Hearthling.attributes().build());}
    private static void placement(RegisterSpawnPlacementsEvent e){
        e.register(HEARTHLING.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Hearthling::canSpawnHere,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))e.accept(EGG.get());
    }
}
''')
text(J/"living/Hearthling.java",'''package dev.mngklrd.midnighteve.living;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import java.util.EnumSet;
import java.util.UUID;

public final class Hearthling extends PathfinderMob {
    private UUID companion;
    private int warmth;
    public Hearthling(EntityType<? extends Hearthling> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,14).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,20);
    }
    public static boolean canSpawnHere(EntityType<Hearthling> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        return level.getBiome(pos).unwrapKey().map(k->k.location().getNamespace().equals("midnighteve")).orElse(false)
            && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
            && level.getRawBrightness(pos,0)>8;
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.35));
        goalSelector.addGoal(2,new FollowGuest());
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,7));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
    }
    @Override protected InteractionResult mobInteract(Player p,InteractionHand hand){
        ItemStack stack=p.getItemInHand(hand);
        if(!stack.is(TalesContent.GINGERBREAD.get()))return super.mobInteract(p,hand);
        if(level() instanceof ServerLevel s){
            if(warmth>0&&companion!=null&&!companion.equals(p.getUUID())){
                p.displayClientMessage(Component.translatable("hearthling.midnighteve.busy"),true);return InteractionResult.CONSUME;
            }
            if(warmth>4800){p.displayClientMessage(Component.translatable("hearthling.midnighteve.full"),true);return InteractionResult.CONSUME;}
            if(!p.getAbilities().instabuild)stack.shrink(1);
            companion=p.getUUID();warmth=6000;heal(4);setPersistenceRequired();
            s.sendParticles(ParticleTypes.HEART,getX(),getY()+1,getZ(),4,.25,.15,.25,.02);
            s.playSound(null,blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.NEUTRAL,.6F,1.4F);
            p.displayClientMessage(Component.translatable("hearthling.midnighteve.bond"),true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private ServerPlayer guest(){
        if(!(level() instanceof ServerLevel s)||companion==null||warmth<=0)return null;
        ServerPlayer p=s.getServer().getPlayerList().getPlayer(companion);
        return p!=null&&p.serverLevel()==s&&p.isAlive()&&!p.isSpectator()?p:null;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel s)||warmth<=0)return;
        if(--warmth==0){companion=null;getNavigation().stop();return;}
        if(tickCount%20==0){
            ServerPlayer p=guest();
            if(p!=null&&distanceToSqr(p)<36){
                p.setTicksFrozen(Math.max(0,p.getTicksFrozen()-25));
                if(tickCount%100==0)s.sendParticles(ParticleTypes.END_ROD,getX(),getY()+.7,getZ(),2,.15,.2,.15,.005);
            }
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("Warmth",warmth);
        if(companion!=null)tag.putUUID("Guest",companion);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);warmth=Math.max(0,Math.min(6000,tag.getInt("Warmth")));
        companion=warmth>0&&tag.hasUUID("Guest")?tag.getUUID("Guest"):null;
    }
    private final class FollowGuest extends Goal {
        private ServerPlayer target;
        private int delay;
        FollowGuest(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){target=guest();return target!=null&&distanceToSqr(target)>9&&distanceToSqr(target)<400;}
        @Override public boolean canContinueToUse(){return target!=null&&guest()==target&&distanceToSqr(target)>4&&distanceToSqr(target)<576;}
        @Override public void start(){delay=0;}
        @Override public void stop(){getNavigation().stop();target=null;}
        @Override public void tick(){
            if(target==null)return;
            getLookControl().setLookAt(target,30,30);
            if(--delay<=0){delay=10;getNavigation().moveTo(target,1.15);}
        }
    }
}
''')
text(J/"client/HearthlingModel.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.living.Hearthling;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class HearthlingModel extends HierarchicalModel<Hearthling>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("midnighteve","hearthling"),"main");
    private final ModelPart root,head,armL,armR,legL,legR;
    public HearthlingModel(ModelPart root){this.root=root;head=root.getChild("head");armL=root.getChild("arm_l");armR=root.getChild("arm_r");legL=root.getChild("leg_l");legR=root.getChild("leg_r");}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition r=mesh.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-3,8,8,6),PartPose.offset(0,21,0));
        PartDefinition h=r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(28,0).addBox(-3,-5,-3,6,5,6),PartPose.offset(0,13,0));
        h.addOrReplaceChild("pom",CubeListBuilder.create().texOffs(48,16).addBox(-1,-2,-1,2,2,2),PartPose.offset(0,-5,0));
        r.addOrReplaceChild("arm_l",CubeListBuilder.create().texOffs(0,16).addBox(0,0,-1.5F,2,5,3),PartPose.offset(3,14,0));
        r.addOrReplaceChild("arm_r",CubeListBuilder.create().texOffs(10,16).addBox(-2,0,-1.5F,2,5,3),PartPose.offset(-3,14,0));
        r.addOrReplaceChild("leg_l",CubeListBuilder.create().texOffs(22,16).addBox(-1.5F,0,-1.5F,3,3,3),PartPose.offset(2,21,0));
        r.addOrReplaceChild("leg_r",CubeListBuilder.create().texOffs(34,16).addBox(-1.5F,0,-1.5F,3,3,3),PartPose.offset(-2,21,0));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(Hearthling mob,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        legL.xRot=Mth.cos(limb*.8F)*amount*.9F;legR.xRot=-legL.xRot;
        armL.xRot=-legL.xRot*.65F;armR.xRot=-legR.xRot*.65F;
        armL.zRot=-.07F-Mth.sin(age*.07F)*.025F;armR.zRot=.07F+Mth.sin(age*.07F)*.025F;
    }
}
''')
text(J/"client/HearthlingRenderer.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.living.Hearthling;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class HearthlingRenderer extends MobRenderer<Hearthling,HearthlingModel>{
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("midnighteve","textures/entity/hearthling.png");
    public HearthlingRenderer(EntityRendererProvider.Context ctx){super(ctx,new HearthlingModel(ctx.bakeLayer(HearthlingModel.LAYER)),.28F);}
    @Override public ResourceLocation getTextureLocation(Hearthling mob){return SKIN;}
}
''')
text(J/"client/LivingClient.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.living.LivingContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="midnighteve",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class LivingClient {
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(HearthlingModel.LAYER,HearthlingModel::layer);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(LivingContent.HEARTHLING.get(),HearthlingRenderer::new);}
}
''')
write(A/"models/item/hearthling_spawn_egg.json",{"parent":"minecraft:item/template_spawn_egg"})
write(D/"loot_table/entities/hearthling.json",{"type":"minecraft:entity","pools":[]})
for n in ("firlight_woods","peppermint_grove","starlit_steppe"):
    t.add_json(D/"worldgen/biome"/(n+".json"),lambda b:b["spawners"]["creature"].append({"type":mod("hearthling"),"weight":3,"minCount":1,"maxCount":2}))
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.content.TalesContent.init(bus);","        dev.mngklrd.midnighteve.content.TalesContent.init(bus);\n        dev.mngklrd.midnighteve.living.LivingContent.init(bus);")

# A delivery itinerary requires native structure presence, not just four NPCs in one hamlet.
text(J/"living/WinterPostRoute.java",'''package dev.mngklrd.midnighteve.living;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WinterPostRoute {
    private static final String KEY="midnighteve_post_route_v1";
    private static final String[] ROLES={"postmaster","ranger","fisher","chandler","postmaster"};
    private static final String[] SITES={"winter_post","mountain_refuge","fjord_trading_post","candle_hall","winter_post"};
    private static final String[] REGIONS={"firlight_woods","crystal_crown","mirror_fjords","starlit_steppe","firlight_woods"};
    private static CompoundTag data(ServerPlayer p){
        CompoundTag root=p.getPersistentData();if(!root.contains(KEY))root.put(KEY,new CompoundTag());return root.getCompound(KEY);
    }
    public static void clonePlayer(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    private static void give(ServerPlayer p,ItemStack stack){if(!p.addItem(stack))p.drop(stack,false);}
    public static boolean interact(ServerPlayer p,String role){
        ItemStack held=p.getMainHandItem();
        if(!held.is(TalesContent.JOURNAL.get())&&!held.is(TalesContent.PARCEL.get()))return false;
        CompoundTag d=data(p);int n=Math.max(0,Math.min(5,d.getInt("step")));
        if(n==5){if(held.is(TalesContent.PARCEL.get()))return false;p.displayClientMessage(Component.translatable("route.midnighteve.done"),false);return true;}
        if(held.is(TalesContent.PARCEL.get())&&(n!=0||!role.equals(ROLES[0])))return false;
        if(!role.equals(ROLES[n])){hint(p,n);return true;}
        var site=TagKey.create(Registries.STRUCTURE,ResourceLocation.fromNamespaceAndPath("midnighteve","post_route/"+SITES[n]));
        if(!p.serverLevel().structureManager().getStructureWithPieceAt(p.blockPosition(),site).isValid()){
            if(held.is(TalesContent.PARCEL.get()))return false;
            hint(p,n);return true;
        }
        // A bound itinerary is held in personal NBT. No tradable quest item duplication.
        if(n==0){
            if(!held.is(TalesContent.PARCEL.get())){p.displayClientMessage(Component.translatable("route.midnighteve.start"),false);return true;}
            if(!p.getAbilities().instabuild)held.shrink(1);
        }else if(!held.is(TalesContent.JOURNAL.get())){hint(p,n);return true;}
        d.putInt("step",n+1);
        p.displayClientMessage(Component.translatable("route.midnighteve.checkpoint",n+1,5),false);
        if(n==4){
            give(p,new ItemStack(MidnightEve.WINTERMARK.get(),12));give(p,new ItemStack(MidnightEve.WRAPPED_GIFT.get(),4));
            p.displayClientMessage(Component.translatable("route.midnighteve.done"),false);
        }else hint(p,n+1);
        return true;
    }
    private static void hint(ServerPlayer p,int n){
        p.displayClientMessage(Component.translatable("route.midnighteve.next",Component.translatable("quest.midnighteve.role."+ROLES[n]),Component.translatable("structure.midnighteve."+SITES[n])),false);
    }
    public static void journal(ServerPlayer p){
        int n=Math.max(0,Math.min(5,data(p).getInt("step")));
        p.sendSystemMessage(Component.translatable("route.midnighteve.progress",n,5));
        if(n==5)p.sendSystemMessage(Component.translatable("route.midnighteve.done"));else hint(p,n);
    }
}
''')
for site in ("winter_post","mountain_refuge","fjord_trading_post","candle_hall"):
    write(D/"tags/worldgen/structure/post_route"/(site+".json"),{"replace":False,"values":[mod(site)]})
edit(J/"quest/WinterQuests.java","        CompoundTag state=data(p);int n=","        if(dev.mngklrd.midnighteve.living.WinterPostRoute.interact(p,role))return true;\n        CompoundTag state=data(p);int n=")
edit(J/"item/WinterJournal.java","else WinterQuests.journal(p);","else {WinterQuests.journal(p);dev.mngklrd.midnighteve.living.WinterPostRoute.journal(p);}")

# Physical feast table, gifts and tree topper grow at fixed authored markers.
# Same block ID across all states, no block replacement outside authored positions.
text(J/"living/FestivalDisplay.java",'''package dev.mngklrd.midnighteve.living;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;
public final class FestivalDisplay extends Block {
    public static final IntegerProperty KIND=IntegerProperty.create("kind",0,2);
    public static final BooleanProperty READY=BooleanProperty.create("ready");
    public FestivalDisplay(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(KIND,0).setValue(READY,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(KIND,READY);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(1,0,1,15,s.getValue(READY)?16:7,15);}
}
''')
def faces(lo,hi,tex):
    dx,dy,dz=[hi[i]-lo[i] for i in range(3)]
    return {"from":lo,"to":hi,"faces":{f:{"texture":"#"+tex,"uv":[0,0,w,h]} for f,w,h in (("north",dx,dy),("south",dx,dy),("east",dz,dy),("west",dz,dy),("up",dx,dz),("down",dx,dz))}}
base=[faces([1,0,1],[15,2,15],"wood"),faces([2,2,2],[4,5,4],"wood"),faces([12,2,2],[14,5,4],"wood"),faces([2,2,12],[4,5,14],"wood"),faces([12,2,12],[14,5,14],"wood"),faces([1,5,1],[15,7,15],"wood")]
# Low solid plinth, filled with recognisable distinct silhouettes, not resized full block cubes.
extras=[
 [faces([3,7,3],[13,8,13],"white"),faces([4,8,4],[12,11,12],"bread"),faces([4,11,4],[12,12,12],"white"),faces([5,12,5],[7,13,7],"red"),faces([9,12,9],[11,13,11],"red")],
 [faces([2,7,2],[8,13,8],"gift"),faces([8,7,8],[14,12,14],"red"),faces([10,12,8],[12,13,14],"gold"),faces([4,13,2],[6,14,8],"gold")],
 [faces([7,7,7],[9,11,9],"wood"),faces([3,8,3],[13,10,13],"leaf"),faces([5,10,5],[11,12,11],"leaf"),faces([6,12,6],[10,14,10],"leaf"),faces([7,14,7],[9,16,9],"gold")]
]
textures={"particle":"minecraft:block/spruce_planks","wood":"minecraft:block/spruce_planks","white":"minecraft:block/white_wool","bread":"minecraft:block/hay_block_side","red":"minecraft:block/red_wool","gift":mod("block/gift_crate_side"),"gold":"minecraft:block/gold_block","leaf":"minecraft:block/moss_block"}
variants={}
for kind in range(3):
    for ready in (False,True):
        n="display_"+str(kind)+("_ready" if ready else "_empty")
        write(A/"models/block"/(n+".json"),{"parent":"minecraft:block/block","textures":textures,"elements":base+(extras[kind] if ready else [])})
        variants["kind="+str(kind)+",ready="+str(ready).lower()]={"model":mod("block/"+n)}
write(A/"blockstates/festival_display.json",{"variants":variants})
# Safe decorative fireworks above the square, only from unobstructed sky.
text(J/"living/FestivalScene.java",'''package dev.mngklrd.midnighteve.living;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
public final class FestivalScene {
    public static void refresh(ServerLevel level,BlockPos center,int stage){
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            BlockPos p=center.offset(x,0,z);
            if(!level.hasChunkAt(p))continue;
            var state=level.getBlockState(p);
            if(!state.is(LivingContent.DISPLAY.get()))continue;
            int need=switch(state.getValue(FestivalDisplay.KIND)){case 0->1;case 1->3;default->4;};
            if(stage>=need&&!state.getValue(FestivalDisplay.READY))level.setBlockAndUpdate(p,state.setValue(FestivalDisplay.READY,true));
        }
    }
    public static void fireworks(ServerLevel level,BlockPos center){
        for(int i=0;i<3;i++){
            BlockPos p=center.offset((i-1)*3,12,0);
            if(!level.hasChunkAt(p)||!level.isEmptyBlock(p)||!level.canSeeSky(p))continue;
            if(level.players().stream().anyMatch(player->player.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)<100))continue;
            ItemStack rocket=new ItemStack(Items.FIREWORK_ROCKET);
            var explosion=new FireworkExplosion(FireworkExplosion.Shape.SMALL_BALL,new IntArrayList(new int[]{0xd5a553,0xb54c41,0xf3e4c4}),new IntArrayList(new int[]{0x789469,0xf4d28b}),true,true);
            rocket.set(DataComponents.FIREWORKS,new Fireworks(1+i%2,List.of(explosion)));
            level.addFreshEntity(new FireworkRocketEntity(level,p.getX()+.5,p.getY(),p.getZ()+.5,rocket));
        }
    }
}
''')
p=J/"block/FestivalHearth.java"
edit(p,"        if(next>=2) light(server,pos,next==4?4:2);","        dev.mngklrd.midnighteve.living.FestivalScene.refresh(server,pos,next);\n        if(next>=2) light(server,pos,next==4?4:2);")
edit(p,"        light(s,p,4);","        light(s,p,4);\n        dev.mngklrd.midnighteve.living.FestivalScene.refresh(s,p,5);\n        dev.mngklrd.midnighteve.living.FestivalScene.fireworks(s,p);")
# A distinct one-time story reward, not an expensive repetitive grind.
g.en.update({"entity.midnighteve.hearthling":"Hearthling","item.midnighteve.hearthling_spawn_egg":"Hearthling Spawn Egg","block.midnighteve.festival_display":"Festival Display","hearthling.midnighteve.busy":"The hearthling is already accompanying another traveler.","hearthling.midnighteve.full":"The hearthling is still full.","hearthling.midnighteve.bond":"The hearthling will share its warmth with you for five minutes.","route.midnighteve.start":"Bring a sealed parcel to start the winter post route.","route.midnighteve.next":"Winter post: meet the %s at %s. Sneak-use your journal on them.","route.midnighteve.progress":"Winter post route: %s/%s","route.midnighteve.checkpoint":"Post route checkpoint %s/%s stamped!","route.midnighteve.done":"Winter Postmaster: your entire route is complete."})
g.ru.update({"entity.midnighteve.hearthling":"Очажник","item.midnighteve.hearthling_spawn_egg":"Яйцо призыва очажника","block.midnighteve.festival_display":"Праздничная витрина","hearthling.midnighteve.busy":"Очажник уже сопровождает другого путника.","hearthling.midnighteve.full":"Очажник пока сыт.","hearthling.midnighteve.bond":"Очажник будет делиться теплом с тобой пять минут.","route.midnighteve.start":"Принеси запечатанную посылку, чтобы начать зимний почтовый маршрут.","route.midnighteve.next":"Зимняя почта: найди жителя «%s» в структуре «%s». Shift + ПКМ с журналом по жителю.","route.midnighteve.progress":"Зимний почтовый маршрут: %s/%s","route.midnighteve.checkpoint":"Почтовая отметка %s/%s получена!","route.midnighteve.done":"Зимний почтальон: весь маршрут завершён."})
for name,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(name+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.7.0"','version="0.8.0"')
for p in R.rglob("*.json"):json.loads(p.read_text(encoding="utf8"))
print("PASS: Living Winter entity atlas, five route checkpoints and staged festival displays")
