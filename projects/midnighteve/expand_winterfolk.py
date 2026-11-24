#!/usr/bin/env python3
"""Winterfolk 0.10: winter fauna, living farm, daily market and role-specific POIs."""
import expand_frostlands as f
import json, gzip, struct, zlib, math
g,t,e,l=f.g,f.t,f.e,f.l
S,R,D,A,J=f.S,f.R,f.D,f.A,f.J
write,text,mc,mod,edit=f.write,f.text,f.mc,f.mod,f.edit

# Two anatomically different creatures. Box-UV rectangles have exact native texel dimensions.
# No resampling, gradients, random noise or shared atlas regions across different body parts.
# Hand-placed patches have material-specific silhouettes: fur tufts, plumage, horn and hoof.
class Atlas:
    def __init__(self,name,w,h,palette):
        self.name=name;self.w=w;self.h=h
        self.pal={k:g.rgba(v) for k,v in palette.items()}
        self.pixels=[[(0,0,0,0)]*w for _ in range(h)]
        self.used=set()
    def face(self,x,y,w,h,base,patches):
        assert x>=0 and y>=0 and x+w<=self.w and y+h<=self.h,(self.name,x,y,w,h)
        for yy in range(h):
            for xx in range(w):
                assert (x+xx,y+yy) not in self.used,(self.name,x+xx,y+yy)
                self.used.add((x+xx,y+yy))
                self.pixels[y+yy][x+xx]=self.pal[base]
        for color,rows in patches:
            for dy,lo,hi in rows:
                assert 0<=dy<h and 0<=lo<=hi<w,(self.name,color,w,h,dy,lo,hi)
                for xx in range(lo,hi+1):self.pixels[y+dy][x+xx]=self.pal[color]
    def cube(self,u,v,w,h,d,faces):
        for n,x,y,ww,hh in (("left",u,v+d,d,h),("front",u+d,v+d,w,h),("right",u+d+w,v+d,d,h),("back",u+2*d+w,v+d,w,h),("top",u+d,v,w,d),("bottom",u+d+w,v,w,d)):
            base,patch=faces[n];self.face(x,y,ww,hh,base,patch)
    def save(self):
        def chunk(k,v):return struct.pack(">I",len(v))+k+v+struct.pack(">I",zlib.crc32(k+v)&0xffffffff)
        p=A/"textures/entity"/(self.name+".png");p.parent.mkdir(parents=True,exist_ok=True)
        raw=b"".join(b"\0"+b"".join(bytes(px) for px in row) for row in self.pixels)
        p.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",self.w,self.h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw))+chunk(b"IEND",b""))
        print("PASS: atlas",self.name,self.w,self.h,len(self.used),"non-overlapping face texels")
def plain(c):return (c,[])
def p(c,*patch):return (c,list(patch))
def six(front,left,right,back,top,bottom):
    return dict(zip(("front","left","right","back","top","bottom"),(front,left,right,back,top,bottom)))
deer=Atlas("snowstag",64,64,{
    "f":"987d65","h":"b29a80","l":"c7b39b","s":"806650","d":"685548",
    "c":"d3c3aa","C":"e6d9c0","v":"baa58c","n":"39332d","N":"66574c",
    "a":"9e8972","A":"c5ad90","k":"6e6255","K":"8b7a67"})
# Torso: shoulder highlight, pale rump and belly. Sides have asymmetric local fur clusters.
deer.cube(0,0,8,9,14,six(
 p("f",("h",[(0,1,5),(1,0,5),(2,0,3)]),("c",[(4,3,4),(5,2,5),(6,2,5),(7,1,6),(8,1,6)])),
 p("f",("h",[(0,0,5),(1,0,7),(2,2,6),(3,3,4)]),("s",[(4,5,7),(5,4,7),(6,5,8)]),("c",[(6,10,13),(7,9,13),(8,3,13)])),
 p("f",("h",[(0,2,8),(1,3,9),(2,5,8)]),("s",[(4,2,4),(5,2,6),(6,3,5)]),("c",[(6,0,4),(7,0,6),(8,0,10)])),
 p("h",("c",[(1,2,5),(2,1,6),(3,1,6),(4,0,7),(5,1,6),(6,1,6),(7,2,5),(8,2,5)])),
 p("h",("l",[(1,1,3),(2,1,4),(3,2,4),(8,4,6),(9,3,6),(10,4,5)]),("f",[(11,0,2),(12,0,3),(13,1,3)])),
 p("v",("c",[(3,2,5),(4,1,6),(5,1,6),(6,2,5),(7,2,5),(8,3,4)]))))
# Neck, head and muzzle: pale throat and dark nose, side-facing eyes.
deer.cube(44,0,5,10,5,six(
 p("c",("C",[(0,1,3),(1,1,4),(2,1,3),(3,2,3)]),("v",[(7,0,1),(8,0,2),(9,0,2)])),
 p("f",("h",[(0,1,4),(1,0,4),(2,0,2),(3,1,2)]),("c",[(5,0,1),(6,0,2),(7,0,2),(8,0,3),(9,0,3)])),
 p("f",("h",[(0,0,3),(1,1,4),(2,2,4)]),("c",[(5,3,4),(6,2,4),(7,2,4),(8,1,4),(9,1,4)])),plain("s"),plain("h"),plain("v")))
deer.cube(0,24,6,5,8,six(
 p("h",("l",[(0,1,4),(1,1,4)]),("c",[(3,0,5),(4,1,4)])),
 p("f",("h",[(0,1,5),(1,1,4)]),("n",[(2,1,2)]),("N",[(1,2,2)]),("c",[(3,0,2),(4,0,4)])),
 p("f",("h",[(0,2,6),(1,3,6)]),("n",[(2,5,6)]),("N",[(1,5,5)]),("c",[(3,5,7),(4,3,7)])),
 plain("f"),p("h",("l",[(0,1,4),(1,1,3),(2,2,4)])),plain("v")))
deer.cube(30,24,4,3,4,six(p("c",("n",[(0,1,2),(1,1,2)])),p("c",("v",[(1,0,1),(2,0,2)])),p("c",("v",[(1,2,3),(2,1,3)])),plain("c"),p("c",("n",[(0,1,2)])),plain("v")))
for u in (48,56):
    deer.cube(u,24,2,4,2,six(p("v",("d",[(1,0,1),(2,0,1)])),plain("f"),plain("h"),plain("f"),plain("h"),plain("s")))
for u in (0,12,24,36):
    side=p("f",("h",[(0,0,1),(1,0,1),(2,0,0)]),("s",[(5,0,1),(6,0,1),(7,0,1)]),("k",[(9,0,1),(10,0,1)]))
    wide=p("f",("h",[(0,0,2),(1,0,1),(2,1,1)]),("s",[(6,0,1),(7,1,2),(8,1,2)]),("k",[(9,0,2),(10,0,2)]))
    deer.cube(u,40,2,11,3,six(side,wide,wide,side,plain("h"),plain("n")))
deer.cube(48,40,1,7,1,six(*[p("a",("A",[(0,0,0),(1,0,0),(3,0,0)])) for _ in range(4)],plain("A"),plain("k")))
deer.cube(54,40,3,1,1,six(plain("a"),plain("a"),plain("a"),plain("a"),plain("A"),plain("k")))
deer.cube(48,54,3,3,4,six(plain("f"),p("v",("c",[(1,1,3),(2,1,3)])),p("v",("c",[(1,0,2),(2,0,2)])),plain("c"),plain("h"),plain("c")))
deer.save()
bird=Atlas("snowgrouse",64,32,{"w":"c6c8be","h":"e2e2d4","s":"adb3ad","d":"909c99","b":"6b7471","n":"373b38","k":"857761","K":"b5a281","r":"a76c5c"})
bird.cube(0,0,6,6,8,six(
 p("h",("w",[(3,0,1),(4,0,2),(5,0,5)])),
 p("w",("h",[(0,0,4),(1,0,3),(2,1,3)]),("s",[(3,4,6),(4,3,5),(5,2,7)])),
 p("w",("h",[(0,3,7),(1,4,7),(2,4,6)]),("s",[(3,1,3),(4,2,4),(5,0,5)])),
 plain("s"),p("h",("w",[(1,2,4),(2,3,5),(5,0,2),(6,0,2)])),plain("s")))
bird.cube(28,0,4,4,4,six(p("h",("w",[(2,0,1),(3,0,3)])),p("h",("n",[(1,1,1)]),("r",[(0,1,2)]),("w",[(3,0,3)])),p("h",("n",[(1,2,2)]),("r",[(0,1,2)]),("w",[(3,0,3)])),plain("w"),plain("h"),plain("s")))
for u,v in ((44,0),(0,16)):
    wing=p("w",("h",[(0,0,3),(1,0,2)]),("d",[(1,4,5),(2,3,4),(3,2,3)]),("b",[(3,4,5)]))
    bird.cube(u,v,2,4,6,six(plain("w"),wing,wing,plain("s"),plain("h"),plain("d")))
bird.cube(16,16,4,2,4,six(plain("w"),plain("s"),plain("s"),p("d",("b",[(1,1,2)])),p("w",("s",[(2,1,2),(3,0,3)])),plain("s")))
bird.cube(32,16,2,2,2,six(plain("k"),plain("K"),plain("k"),plain("k"),plain("K"),plain("k")))
for u in (40,44):
    bird.cube(u,16,1,3,1,six(*[p("k",("K",[(0,0,0)])) for _ in range(4)],plain("K"),plain("k")))
bird.save()

# Separate client models and registration; renderer uses real per-species animations.
text(J/"winterfolk/FolkContent.java",'''package dev.mngklrd.midnighteve.winterfolk;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class FolkContent {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,"midnighteve");
    public static final Supplier<EntityType<Snowstag>> STAG=TYPES.register("snowstag",()->EntityType.Builder.of(Snowstag::new,MobCategory.CREATURE).sized(.85F,1.65F).clientTrackingRange(8).build("midnighteve:snowstag"));
    public static final Supplier<EntityType<Snowgrouse>> GROUSE=TYPES.register("snowgrouse",()->EntityType.Builder.of(Snowgrouse::new,MobCategory.CREATURE).sized(.45F,.65F).clientTrackingRange(8).build("midnighteve:snowgrouse"));
    public static final DeferredItem<Item> STAG_EGG=MidnightEve.ITEMS.register("snowstag_spawn_egg",()->new DeferredSpawnEggItem(STAG,0x987d65,0xd3c3aa,new Item.Properties()));
    public static final DeferredItem<Item> GROUSE_EGG=MidnightEve.ITEMS.register("snowgrouse_spawn_egg",()->new DeferredSpawnEggItem(GROUSE,0xc6c8be,0x6b7471,new Item.Properties()));
    public static final DeferredItem<Item> VENISON=MidnightEve.ITEMS.registerSimpleItem("venison",new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(.3F).build()));
    public static final DeferredItem<Item> ROAST=MidnightEve.ITEMS.registerSimpleItem("roast_venison",new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(.7F).build()));
    public static final DeferredItem<Item> STEW=MidnightEve.ITEMS.registerSimpleItem("winter_stew",new Item.Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(9).saturationModifier(.8F).usingConvertsTo(Items.BOWL).build()));
    public static final DeferredItem<Item> LEDGER=MidnightEve.ITEMS.registerSimpleItem("market_ledger",new Item.Properties().stacksTo(1));
    public static void init(IEventBus bus){
        TYPES.register(bus);bus.addListener(FolkContent::attributes);bus.addListener(FolkContent::placement);bus.addListener(FolkContent::creative);
        NeoForge.EVENT_BUS.addListener(DailyMarket::clonePlayer);
    }
    private static void attributes(EntityAttributeCreationEvent e){e.put(STAG.get(),Snowstag.attributes().build());e.put(GROUSE.get(),Snowgrouse.attributes().build());}
    private static void placement(RegisterSpawnPlacementsEvent e){
        e.register(STAG.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Snowstag::canSpawnHere,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(GROUSE.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Snowgrouse::canSpawnHere,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)){e.accept(STAG_EGG.get());e.accept(GROUSE_EGG.get());}
        if(e.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)){e.accept(VENISON.get());e.accept(ROAST.get());e.accept(STEW.get());}
        if(e.getTabKey().equals(CreativeModeTabs.INGREDIENTS))e.accept(LEDGER.get());
    }
}
''')
text(J/"winterfolk/Snowstag.java",'''package dev.mngklrd.midnighteve.winterfolk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
public final class Snowstag extends Animal {
    public Snowstag(EntityType<? extends Snowstag> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,24);}
    public static boolean canSpawnHere(EntityType<Snowstag> type,ServerLevelAccessor l,MobSpawnType reason,BlockPos p,RandomSource r){
        return l.getBiome(p).unwrapKey().map(k->k.location().getNamespace().equals("midnighteve")).orElse(false)
            && l.getBlockState(p.below()).isFaceSturdy(l,p.below(),net.minecraft.core.Direction.UP)&&l.getRawBrightness(p,0)>8;
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.55));
        goalSelector.addGoal(2,new BreedGoal(this,1.0));
        goalSelector.addGoal(3,new TemptGoal(this,1.05,Ingredient.of(Items.SWEET_BERRIES),false));
        goalSelector.addGoal(4,new FollowParentGoal(this,1.1));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.9));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,7));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    @Override public boolean isFood(ItemStack stack){return stack.is(Items.SWEET_BERRIES);}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){return FolkContent.STAG.get().create(level);}
}
''')
text(J/"winterfolk/Snowgrouse.java",'''package dev.mngklrd.midnighteve.winterfolk;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
public final class Snowgrouse extends Animal {
    private int eggTime=7200;
    public Snowgrouse(EntityType<? extends Snowgrouse> type,Level level){super(type,level);eggTime=6000+random.nextInt(6000);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,.24);}
    public static boolean canSpawnHere(EntityType<Snowgrouse> type,ServerLevelAccessor l,MobSpawnType reason,BlockPos p,RandomSource r){
        return l.getBiome(p).unwrapKey().map(k->k.location().getNamespace().equals("midnighteve")).orElse(false)
            && l.getBlockState(p.below()).isFaceSturdy(l,p.below(),net.minecraft.core.Direction.UP)&&l.getRawBrightness(p,0)>8;
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.4));goalSelector.addGoal(2,new BreedGoal(this,1.0));
        goalSelector.addGoal(3,new TemptGoal(this,1.0,Ingredient.of(Items.WHEAT_SEEDS),false));goalSelector.addGoal(4,new FollowParentGoal(this,1.1));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.9));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    @Override public boolean isFood(ItemStack s){return s.is(Items.WHEAT_SEEDS);}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){return FolkContent.GROUSE.get().create(level);}
    @Override public void aiStep(){
        super.aiStep();
        if(!level().isClientSide&&isAlive()&&!isBaby()&&--eggTime<=0){
            playSound(SoundEvents.CHICKEN_EGG,.6F,1.2F);spawnAtLocation(Items.EGG);
            eggTime=6000+random.nextInt(6000);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putInt("EggTime",eggTime);}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);if(n.contains("EggTime"))eggTime=Math.max(1,Math.min(12000,n.getInt("EggTime")));}
}
''')
text(J/"client/SnowstagModel.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.winterfolk.Snowstag;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class SnowstagModel extends HierarchicalModel<Snowstag>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("midnighteve","snowstag"),"main");
    private final ModelPart root,head,lf,rf,lb,rb,tail;
    public SnowstagModel(ModelPart r){root=r;head=r.getChild("neck").getChild("head");lf=r.getChild("lf");rf=r.getChild("rf");lb=r.getChild("lb");rb=r.getChild("rb");tail=r.getChild("tail");}
    public static LayerDefinition layer(){
        MeshDefinition m=new MeshDefinition();PartDefinition r=m.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-7,8,9,14),PartPose.offset(0,10,0));
        PartDefinition neck=r.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(44,0).addBox(-2.5F,-9,-2.5F,5,10,5),PartPose.offsetAndRotation(0,10,-5,.28F,0,0));
        PartDefinition h=neck.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,24).addBox(-3,-4,-6,6,5,8),PartPose.offsetAndRotation(0,-8,-.5F,-.28F,0,0));
        h.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(30,24).addBox(-2,-1,-4,4,3,4),PartPose.offset(0,-1,-5));
        h.addOrReplaceChild("ear_l",CubeListBuilder.create().texOffs(48,24).addBox(-1,-4,-1,2,4,2),PartPose.offsetAndRotation(2.6F,-3,0,0,0,.65F));
        h.addOrReplaceChild("ear_r",CubeListBuilder.create().texOffs(56,24).addBox(-1,-4,-1,2,4,2),PartPose.offsetAndRotation(-2.6F,-3,0,0,0,-.65F));
        for(int sign:new int[]{-1,1}){
            String n=sign<0?"antler_r":"antler_l";
            PartDefinition a=h.addOrReplaceChild(n,CubeListBuilder.create().texOffs(48,40).addBox(-.5F,-7,-.5F,1,7,1),PartPose.offsetAndRotation(sign*2,-3,1,-.2F,0,sign*.2F));
            a.addOrReplaceChild("fork1",CubeListBuilder.create().texOffs(54,40).addBox(sign>0?0:-3,-.5F,-.5F,3,1,1),PartPose.offsetAndRotation(0,-4,0,0,0,sign*-.35F));
            a.addOrReplaceChild("fork2",CubeListBuilder.create().texOffs(54,40).addBox(sign>0?0:-3,-.5F,-.5F,3,1,1),PartPose.offsetAndRotation(0,-6,0,0,0,sign*-.75F));
        }
        String[] names={"lf","rf","lb","rb"};int[] u={0,12,24,36};
        for(int i=0;i<4;i++)r.addOrReplaceChild(names[i],CubeListBuilder.create().texOffs(u[i],40).addBox(-1,0,-1.5F,2,11,3),PartPose.offset(i%2==0?2.7F:-2.7F,13,i<2?-5:5));
        r.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(48,54).addBox(-1.5F,-1,0,3,3,4),PartPose.offsetAndRotation(0,7,6,-.4F,0,0));
        return LayerDefinition.create(m,64,64);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(Snowstag mob,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=Mth.clamp(yaw,-55,55)*Mth.DEG_TO_RAD;head.xRot=-.28F+Mth.clamp(pitch,-30,30)*Mth.DEG_TO_RAD;
        float step=Mth.cos(limb*.6662F)*1.1F*amount;lf.xRot=rb.xRot=step;rf.xRot=lb.xRot=-step;
        tail.yRot=Mth.sin(age*.13F)*.12F;head.getChild("antler_l").visible=!mob.isBaby();head.getChild("antler_r").visible=!mob.isBaby();
    }
}
''')
text(J/"client/SnowgrouseModel.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.winterfolk.Snowgrouse;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class SnowgrouseModel extends HierarchicalModel<Snowgrouse>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("midnighteve","snowgrouse"),"main");
    private final ModelPart root,head,left,right,wingL,wingR;
    public SnowgrouseModel(ModelPart r){root=r;head=r.getChild("head");left=r.getChild("left");right=r.getChild("right");wingL=r.getChild("wing_l");wingR=r.getChild("wing_r");}
    public static LayerDefinition layer(){
        MeshDefinition m=new MeshDefinition();PartDefinition r=m.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-3,-4,-4,6,6,8),PartPose.offset(0,19,0));
        PartDefinition h=r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(28,0).addBox(-2,-3,-2,4,4,4),PartPose.offset(0,15,-3));
        h.addOrReplaceChild("beak",CubeListBuilder.create().texOffs(32,16).addBox(-1,-1,-2,2,2,2),PartPose.offset(0,-.5F,-2));
        r.addOrReplaceChild("wing_l",CubeListBuilder.create().texOffs(44,0).addBox(0,0,-3,2,4,6),PartPose.offset(2.5F,16,0));
        r.addOrReplaceChild("wing_r",CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-3,2,4,6),PartPose.offset(-2.5F,16,0));
        r.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(16,16).addBox(-2,-1,0,4,2,4),PartPose.offsetAndRotation(0,17,3,-.35F,0,0));
        r.addOrReplaceChild("left",CubeListBuilder.create().texOffs(40,16).addBox(-.5F,0,-.5F,1,3,1),PartPose.offset(1.5F,21,0));
        r.addOrReplaceChild("right",CubeListBuilder.create().texOffs(44,16).addBox(-.5F,0,-.5F,1,3,1),PartPose.offset(-1.5F,21,0));
        return LayerDefinition.create(m,64,32);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(Snowgrouse mob,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=yaw*Mth.DEG_TO_RAD*.75F;head.xRot=pitch*Mth.DEG_TO_RAD*.6F+Mth.cos(limb)*amount*.12F;
        left.xRot=Mth.cos(limb*.9F)*amount;right.xRot=-left.xRot;
        wingL.zRot=-.08F-Mth.sin(age*.16F)*.035F;wingR.zRot=-wingL.zRot;
    }
}
''')
for species,typ,shadow in [("snowstag","Snowstag",".4F"),("snowgrouse","Snowgrouse",".22F")]:
    text(J/"client"/(typ+"Renderer.java"),'''package dev.mngklrd.midnighteve.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.mngklrd.midnighteve.winterfolk.'''+typ+''';
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class '''+typ+'''Renderer extends MobRenderer<'''+typ+''','''+typ+'''Model>{
    private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath("midnighteve","textures/entity/'''+species+'''.png");
    public '''+typ+'''Renderer(EntityRendererProvider.Context c){super(c,new '''+typ+'''Model(c.bakeLayer('''+typ+'''Model.LAYER)),'''+shadow+''');}
    @Override public ResourceLocation getTextureLocation('''+typ+''' mob){return SKIN;}
    @Override protected void scale('''+typ+''' mob,PoseStack pose,float tick){if(mob.isBaby())pose.scale(.6F,.6F,.6F);}
}
''')
pfile=J/"client/LivingClient.java"
edit(pfile,"e.registerLayerDefinition(HearthlingModel.LAYER,HearthlingModel::layer);","e.registerLayerDefinition(HearthlingModel.LAYER,HearthlingModel::layer);e.registerLayerDefinition(SnowstagModel.LAYER,SnowstagModel::layer);e.registerLayerDefinition(SnowgrouseModel.LAYER,SnowgrouseModel::layer);")
edit(pfile,"e.registerEntityRenderer(LivingContent.HEARTHLING.get(),HearthlingRenderer::new);","e.registerEntityRenderer(LivingContent.HEARTHLING.get(),HearthlingRenderer::new);e.registerEntityRenderer(dev.mngklrd.midnighteve.winterfolk.FolkContent.STAG.get(),SnowstagRenderer::new);e.registerEntityRenderer(dev.mngklrd.midnighteve.winterfolk.FolkContent.GROUSE.get(),SnowgrouseRenderer::new);")
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.world.FrostMaterials.init(bus);","        dev.mngklrd.midnighteve.world.FrostMaterials.init(bus);\n        dev.mngklrd.midnighteve.winterfolk.FolkContent.init(bus);")
for n in ("snowstag","snowgrouse"):
    write(A/"models/item"/(n+"_spawn_egg.json"),{"parent":"minecraft:item/template_spawn_egg"})
for n in ("firlight_woods","silent_snowfields","peppermint_grove"):
    t.add_json(D/"worldgen/biome"/(n+".json"),lambda b:b["spawners"]["creature"].append({"type":mod("snowstag"),"weight":5,"minCount":2,"maxCount":4}))
for n in ("starlit_steppe","firlight_woods","silent_snowfields"):
    t.add_json(D/"worldgen/biome"/(n+".json"),lambda b:b["spawners"]["creature"].append({"type":mod("snowgrouse"),"weight":5,"minCount":2,"maxCount":3}))
def drop(item,a,b):
    return {"rolls":1,"entries":[{"type":"minecraft:item","name":mc(item),"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":a,"max":b}}]}]}
write(D/"loot_table/entities/snowstag.json",{"type":"minecraft:entity","pools":[drop("midnighteve:venison",1,3),drop("leather",0,2)]})
write(D/"loot_table/entities/snowgrouse.json",{"type":"minecraft:entity","pools":[drop("feather",0,2)]})
# Four individually authored 16px item silhouettes, with separate meat/ceramic/paper ramps.
f.sprite("venison",[".....oooo...","...oohhho...","..ohhhhho...","oohhhhmmmo..","ohhhhmmmdmo.","ohhmmmmmddo.","ohmmmddmddo.","ohmmdddddo..",".ohdddddo...","..oooooo...."],{"o":"713d37","h":"c97564","m":"b85e51","d":"984a42"})
f.sprite("roast_venison",[".....oooo...","...oohhho...","..ohhhhho...","oohhhmmmo...","ohhmmmmmdo..","ohhmmdddmo..","ohmmdddmdo..","ohmddddddo..",".ohdddddo...","..oooooo...."],{"o":"67432f","h":"c18e58","m":"a16d40","d":"825137"})
f.sprite("winter_stew",["...s...s....","...s..s.....","..oooooooo..",".ohhgghhhho.","ohhmhrrmhhoo","ohhmmrrmmhho","ohhhmmmmhhho",".ohhhhhhhho.",".owwwwwwmmo.","..owwwwmmo..","...oooooo..."],{"s":"c6cfc9","o":"6b5037","h":"bc9258","m":"966b3e","g":"79915c","r":"af6548","w":"d0ad7a"})
f.sprite("market_ledger",["..oooooooo..","..ohhhmmmo..","..ohlllmmo..","..ohlgllmo..","..ohggglmo..","..ohlgglmo..","..ohllllmo..","..ohlhllmo..","..ohllllmo..","..ohmmmmmo..","..oooooooo.."],{"o":"564638","h":"ad8b63","m":"8b6c4b","l":"d6c39a","g":"82966b"})
for n in ("venison","roast_venison","winter_stew","market_ledger"):
    write(A/"models/item"/(n+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":mod("item/"+n)}})
for typ,duration in [("smelting",200),("smoking",100),("campfire_cooking",600)]:
    write(D/"recipe"/("roast_venison_"+typ+".json"),{"type":"minecraft:"+typ,"ingredient":{"item":mod("venison")},"result":{"id":mod("roast_venison")},"experience":.35,"cookingtime":duration})
write(D/"recipe/winter_stew.json",{"type":"minecraft:crafting_shapeless","ingredients":[{"item":mod("roast_venison")},{"item":"minecraft:carrot"},{"item":"minecraft:potato"},{"item":"minecraft:bowl"}],"result":{"id":mod("winter_stew")}})
t.recipe("market_ledger",[" P "," B "," G "],{"P":"paper","B":"book","G":"gold_nugget"})

# One optional daily order per profession and player. Independent of finite story requests.
# Preview uses a normal right-click; sneaking confirms atomically from inventory.
text(J/"winterfolk/DailyMarket.java",'''package dev.mngklrd.midnighteve.winterfolk;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;
public final class DailyMarket {
    private static final String KEY="midnighteve_daily_market_v1";
    private record Order(String id,int count,int marks){}
    private static final Map<String,List<Order>> ORDERS=Map.of(
        "baker",List.of(new Order("minecraft:wheat",16,2),new Order("midnighteve:gingerbread",8,3),new Order("midnighteve:winter_stew",2,4)),
        "giftkeeper",List.of(new Order("minecraft:paper",16,2),new Order("midnighteve:wrapped_gift",2,3),new Order("midnighteve:ornament",4,3)),
        "woodworker",List.of(new Order("minecraft:spruce_log",20,2),new Order("midnighteve:froststone_bricks",16,3),new Order("midnighteve:garland",4,3)),
        "ranger",List.of(new Order("minecraft:coal",12,2),new Order("midnighteve:roast_venison",6,3),new Order("midnighteve:hot_cocoa",3,3)),
        "postmaster",List.of(new Order("minecraft:paper",24,2),new Order("midnighteve:sealed_parcel",2,3),new Order("minecraft:string",12,2)),
        "chandler",List.of(new Order("minecraft:honeycomb",8,2),new Order("midnighteve:candle_cluster",2,3),new Order("minecraft:lantern",3,3)),
        "fisher",List.of(new Order("minecraft:cod",12,2),new Order("minecraft:salmon",10,2),new Order("minecraft:cooked_cod",8,3)));
    private static CompoundTag data(ServerPlayer p){
        CompoundTag root=p.getPersistentData();if(!root.contains(KEY))root.put(KEY,new CompoundTag());return root.getCompound(KEY);
    }
    public static void clonePlayer(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    public static void interact(ServerPlayer p,String role,boolean confirm){
        List<Order> orders=ORDERS.get(role);if(orders==null)return;
        long ticks=p.serverLevel().getDayTime(),day=Math.floorDiv(ticks,24000L),time=Math.floorMod(ticks,24000L);
        if(time<1000||time>11000){p.displayClientMessage(Component.translatable("market.midnighteve.closed"),false);return;}
        CompoundTag d=data(p);
        if(d.contains(role)&&d.getLong(role)>=day){p.displayClientMessage(Component.translatable("market.midnighteve.done"),false);return;}
        Order o=orders.get(Math.floorMod(day+role.hashCode(),orders.size()));
        Item required=BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.id()));
        if(required==Items.AIR)return;
        if(!confirm){
            p.displayClientMessage(Component.translatable("market.midnighteve.offer",Component.translatable("quest.midnighteve.role."+role),o.count(),new ItemStack(required).getHoverName(),o.marks()),false);
            return;
        }
        var inv=p.getInventory();int count=0;
        // Only main storage and hotbar, not armor or offhand.
        for(int i=0;i<36;i++){ItemStack st=inv.getItem(i);if(st.is(required))count+=st.getCount();}
        if(count<o.count()&&!p.getAbilities().instabuild){
            p.displayClientMessage(Component.translatable("market.midnighteve.short",o.count()-count,new ItemStack(required).getHoverName()),false);return;
        }
        if(!p.getAbilities().instabuild){
            int left=o.count();
            for(int i=0;i<36&&left>0;i++){ItemStack st=inv.getItem(i);if(st.is(required)){int take=Math.min(left,st.getCount());st.shrink(take);left-=take;}}
            inv.setChanged();
        }
        d.putLong(role,day);
        ItemStack reward=new ItemStack(MidnightEve.WINTERMARK.get(),o.marks());if(!p.addItem(reward))p.drop(reward,false);
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,.5F,1.25F);
        p.displayClientMessage(Component.translatable("market.midnighteve.paid",o.marks()),false);
    }
}
''')
edit(J/"quest/WinterQuests.java","        if(role.isEmpty()||hand!=InteractionHand.MAIN_HAND||!player.isShiftKeyDown())return false;","""        if(role.isEmpty()||hand!=InteractionHand.MAIN_HAND)return false;
        if(player.getMainHandItem().is(dev.mngklrd.midnighteve.winterfolk.FolkContent.LEDGER.get())){
            if(player instanceof ServerPlayer p)dev.mngklrd.midnighteve.winterfolk.DailyMarket.interact(p,role,p.isShiftKeyDown());
            return true;
        }
        if(!player.isShiftKeyDown())return false;""")
# Separate winter livestock farm. Roof is a complete gable, three solid walls,
# wide double doors and open aisle connecting two protected animal pens.
farm=g.Template(47,24,39)
g.path(farm,5,3,40,6);g.path(farm,21,7,25,32)
g.cabin(farm,4,9,13,13,"Baker","spruce_planks","dark_oak","farmer")
# Barn: 15 by 19 footprint, high eaves, wooden gables, windows, hay loft and crossbeam.
x0,x1,z0,z1=29,43,9,28
farm.fill(x0,0,z0,x1,2,z1,"cobblestone")
farm.fill(x0,3,z0,x1,3,z1,"spruce_planks")
farm.fill(x0,4,z0,x1,10,z1,"air")
for x in range(x0,x1+1):
 for z in range(z0,z1+1):
    if x in (x0,x1) or z in (z0,z1):
        for y in range(4,11):
            beam=(x in (x0,x1) and z in (z0,z1)) or y==10 or (x in (x0,x1) and (z-z0)%5==0)
            farm.put(x,y,z,"stripped_spruce_log" if beam else ("froststone_bricks" if y==4 else "spruce_planks"),**({"axis":"y"} if beam else {})) if beam else farm.put(x,y,z,"midnighteve:froststone_bricks" if y==4 else "spruce_planks")
for z in (13,18,23):
 for x in (x0,x1):
    farm.put(x,7,z,"glass");farm.put(x,8,z,"glass")
for x in range(x0-1,x1+2):
    y=11+min(x-x0+1,x1+1-x)
    for z in range(z0-1,z1+2):
        if x==36:farm.put(x,y,z,"dark_oak_slab",type="bottom",waterlogged=False)
        else:farm.put(x,y,z,"dark_oak_stairs",facing="east" if x<36 else "west",half="bottom",shape="straight",waterlogged=False)
    if x0<=x<=x1:
        for z in (z0,z1):
            for yy in range(11,y):farm.put(x,yy,z,"spruce_planks")
for x,hinge in ((35,"left"),(36,"right")):
    for y,half in ((4,"lower"),(5,"upper")):farm.put(x,y,z0,"spruce_door",facing="north",half=half,hinge=hinge,open=False,powered=False)
    farm.doors.append((x,4,z0));farm.rooms.append((x,4,z0+1))
g.path(farm,34,6,37,8)
# Interior stalls have real connected fences and gates; central aisle remains clear.
def fence(x,z,n,s,e,w):
    farm.put(x,4,z,"spruce_fence",north=n,south=s,east=e,west=w,waterlogged=False)
for z in range(14,26):
    fence(33,z,z>14,z<25,False,z in (14,25))
    fence(39,z,z>14,z<25,z in (14,25),False)
for x in range(30,33):
    fence(x,14,False,False,True,x>30);fence(x,25,False,False,True,x>30)
for x in range(40,43):
    fence(x,14,False,False,x<42,True);fence(x,25,False,False,x<42,True)
for x in (33,39):
    farm.put(x,4,19,"spruce_fence_gate",facing="east",in_wall=False,open=False,powered=False)
for x,z in ((31,16),(41,16),(31,24),(41,24)):farm.put(x,4,z,"hay_block",axis="y")
for z in (13,20,26):
    farm.fill(30,10,z,42,10,z,"stripped_spruce_log",axis="x")
    farm.put(36,9,z,"lantern",hanging=True,waterlogged=False)
farm.put(30,4,12,"barrel",facing="up",open=False)
farm.put(42,4,12,"barrel",facing="up",open=False)
# Outdoor growing plot: irrigated soil, separate fencing and safe paths.
for x in range(5,18):
 for z in range(26,35):
    farm.put(x,3,z,"water" if x==11 else "farmland",**({"level":0} if x==11 else {"moisture":7}))
    if x!=11:farm.put(x,4,z,"carrots" if x<11 else "wheat",age=7)
for x,z in ((20,9),(26,9),(20,24),(26,24),(34,7),(38,7)):t.lamp(farm,x,z)
farm.put(15,4,19,"composter",level=0)
def animal(kind,x,y,z):
    farm.entities.append({"pos":g.L(6,[x+.5,float(y),z+.5]),"blockPos":g.L(3,[x,y,z]),"nbt":g.C({"id":g.Str(mod(kind)),"PersistenceRequired":g.B(1),"Age":g.I(0)})})
for x,z in ((31,20),(31,22)):animal("snowstag",x,4,z)
for x,z in ((41,19),(41,21),(40,23)):animal("snowgrouse",x,4,z)
farm.save("hollycroft_farm")
t.register("hollycroft_farm",["peppermint_grove","starlit_steppe"],43,15,712037,"Ферма Остролиста")
# Add the new native structure to discovery without resetting existing saved progress.
pfile=D/"tags/worldgen/structure/discoverable.json"
t.add_json(pfile,lambda d:d["values"].append(mod("hollycroft_farm")))
pfile=J/"quest/WinterExploration.java"
s=pfile.read_text(encoding="utf8")
import re
s,n=re.subn(r'(private static final List<String> LANDMARKS=List\.of\()([^;]+)(\);)',r'\1\2,"hollycroft_farm"\3',s,count=1)
assert n==1;s=s.replace(',"hollycroft_farm","hollycroft_farm"',',"hollycroft_farm"');pfile.write_text(s,encoding="utf8")
# Ensure generated residents have actual compatible workstation blocks nearby.
# Only insert into explicitly clear two-block-high locations against a wall,
# never at a door or in the player's main aisle. No runtime world scanning.
jobs={"minecraft:farmer":("minecraft:composter",{"level":"0"}),"minecraft:librarian":("minecraft:lectern",{"facing":"north","has_book":"false","powered":"false"}),"minecraft:toolsmith":("minecraft:smithing_table",{}),"minecraft:butcher":("minecraft:smoker",{"facing":"north","lit":"false"}),"minecraft:cleric":("minecraft:brewing_stand",{"has_bottle_0":"false","has_bottle_1":"false","has_bottle_2":"false"}),"minecraft:fisherman":("minecraft:barrel",{"facing":"up","open":"false"})}
solid={"minecraft:spruce_planks","minecraft:dark_oak_planks","minecraft:cobblestone","minecraft:stone_bricks","minecraft:white_terracotta","minecraft:red_terracotta","minecraft:stripped_spruce_log","minecraft:stripped_birch_wood","minecraft:polished_andesite","midnighteve:froststone_bricks"}
count=0;unresolved=[]
for path in sorted((D/"structure").glob("*.nbt")):
    root=f.read_nbt(gzip.decompress(path.read_bytes()));palette=root["palette"][1][1];blocks=root["blocks"][1][1];idx={tuple(b["pos"][1][1]):b for b in blocks}
    def name(pos):
        b=idx.get(pos)
        return palette[b["state"][1]]["Name"][1] if b else ""
    claimed=set()
    for entity in root["entities"][1][1]:
        nbt=entity["nbt"][1]
        if nbt["id"][1]!="minecraft:villager":continue
        job=nbt.get("VillagerData",(10,{}))[1].get("profession",(8,""))[1]
        if job not in jobs:continue
        x,y,z=entity["blockPos"][1][1];target,props=jobs[job]
        near=sorted((abs(px-x)+abs(pz-z), (px,py,pz)) for (px,py,pz) in idx if py==y and abs(px-x)+abs(pz-z)<=8 and name((px,py,pz))==target and (px,py,pz) not in claimed)
        if near:claimed.add(near[0][1]);continue
        candidates=[]
        for dx in range(-5,6):
            for dz in range(-5,6):
                if abs(dx)+abs(dz)<2 or abs(dx)+abs(dz)>5:continue
                p=(x+dx,y,z+dz);px,py,pz=p
                if p in claimed or name(p)!="minecraft:air" or name((px,py+1,pz))!="minecraft:air" or name((px,py-1,pz)) not in solid:continue
                sides=[(px+1,py,pz),(px-1,py,pz),(px,py,pz+1),(px,py,pz-1)]
                if any(name(q).endswith("_door") or name(q).endswith("_bed") for q in sides):continue
                if not any(name(q) in solid for q in sides) or sum(name(q)=="minecraft:air" for q in sides)<2:continue
                candidates.append((abs(dx)+abs(dz),p))
        if not candidates:unresolved.append((path.stem,job));continue
        _,p=min(candidates);entry={"Name":g.Str(target)}
        if props:entry["Properties"]=g.C({k:g.Str(v) for k,v in props.items()})
        if entry not in palette:palette.append(entry)
        idx[p]["state"]=g.I(palette.index(entry));claimed.add(p);count+=1
    root["blocks"]=g.L(10,blocks);path.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
print("PASS: new role-compatible workstation blocks:",count,"unresolved residents (left unchanged):",unresolved)
# English/Russian localization.
g.en.update({"entity.midnighteve.snowstag":"Snowstag","entity.midnighteve.snowgrouse":"Snow Grouse","item.midnighteve.snowstag_spawn_egg":"Snowstag Spawn Egg","item.midnighteve.snowgrouse_spawn_egg":"Snow Grouse Spawn Egg","item.midnighteve.venison":"Raw Venison","item.midnighteve.roast_venison":"Roast Venison","item.midnighteve.winter_stew":"Winter Stew","item.midnighteve.market_ledger":"Market Ledger","market.midnighteve.closed":"The winter market opens in the morning and closes at dusk.","market.midnighteve.done":"You have already completed today's order for this profession.","market.midnighteve.offer":"%s: today's order is %s × %s for %s wintermarks. Sneak + use the ledger to deliver from your inventory.","market.midnighteve.short":"Still needed: %s × %s.","market.midnighteve.paid":"Market order completed! +%s wintermarks."})
g.ru.update({"entity.midnighteve.snowstag":"Снежный олень","entity.midnighteve.snowgrouse":"Снежная куропатка","item.midnighteve.snowstag_spawn_egg":"Яйцо призыва снежного оленя","item.midnighteve.snowgrouse_spawn_egg":"Яйцо призыва снежной куропатки","item.midnighteve.venison":"Сырая оленина","item.midnighteve.roast_venison":"Жареная оленина","item.midnighteve.winter_stew":"Зимнее рагу","item.midnighteve.market_ledger":"Рыночная книга","market.midnighteve.closed":"Зимний рынок работает с утра до сумерек.","market.midnighteve.done":"Сегодняшний заказ этой профессии уже выполнен.","market.midnighteve.offer":"%s: сегодня нужно %s × %s за %s зимних марок. Shift + ПКМ с книгой отдаёт нужные предметы из инвентаря.","market.midnighteve.short":"Не хватает: %s × %s.","market.midnighteve.paid":"Рыночный заказ выполнен! +%s зимних марок."})
for n,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(n+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.9.0"','version="0.10.0"')
for pfile in R.rglob("*.json"):json.loads(pfile.read_text(encoding="utf8"))
print("PASS: Winterfolk expansion — two animated fauna species, 16 native structures and seven rotating daily markets")
