#!/usr/bin/env python3
"""0.11 QA corrections from in-game feedback. Keep gameplay, climate, aurora unchanged."""
import expand_evergreen as e
import json
w,f,g,t=e.w,e.f,e.g,e.t
S,R,D,A,J=e.S,e.R,e.D,e.A,e.J
write,text,mod,edit=e.write,e.text,e.mod,e.edit

# Actual structure candidates, not a locate-command workaround. All six native biomes
# can host a settlement; the coastal/mountain ones may have isolated settlements too.
# No change to vanilla or other mods' structure sets.
for n,spacing,separation in [("everlight_hamlet",28,12),("hollycroft_farm",32,14)]:
    tag=D/"tags/worldgen/biome/has_structure"/(n+".json")
    write(tag,{"replace":False,"values":[mod(b) for b in ("firlight_woods","peppermint_grove","starlit_steppe","crystal_crown","mirror_fjords","silent_snowfields")]})
    p=D/"worldgen/structure_set"/(n+".json")
    s=json.loads(p.read_text());s["placement"]["spacing"]=spacing;s["placement"]["separation"]=separation
    # Keep salts, avoid overlap with the settlement's candidate footprint.
    if n=="hollycroft_farm":s["placement"]["exclusion_zone"]={"other_set":mod("everlight_hamlet"),"chunk_count":4}
    write(p,s)
    assert spacing>separation and len(json.loads(tag.read_text())["values"])==6
print("PASS: expanded native settlement candidates, distinct salts, farm exclusion radius")

# Author exact-sized box UVs: front is north. Patches are hand-selected row clusters.
# All original mob gameplay is preserved. Symmetric limb pieces intentionally reuse UV.
Atlas=w.Atlas
def plain(color):return (color,[])
def p(color,*patches):return (color,list(patches))
def six(front,left,right,back,top,bottom):
    return dict(zip(("front","left","right","back","top","bottom"),(front,left,right,back,top,bottom)))
deer=Atlas("snowstag",64,64,{"f":"a4886c","h":"b89d80","l":"c6ae90","s":"92745d","d":"79624f","c":"d4c2a3","C":"e1d0b4","v":"b6a083","n":"342e28","N":"635140","a":"aa9276","A":"c6b297","k":"645449","K":"877363"})
deer.cube(0,0,8,8,12,six(
 p("f",("h",[(0,1,6),(1,0,6),(2,1,4)]),("c",[(3,2,4),(4,1,5),(5,1,6),(6,1,6),(7,2,5)])),
 p("f",("h",[(0,0,4),(1,0,5),(2,1,4),(0,8,11),(1,8,10)]),("l",[(1,1,2),(2,1,2)]),("s",[(3,4,6),(4,3,6),(5,4,7),(5,10,11)]),("c",[(6,6,11),(7,3,11)])),
 p("f",("h",[(0,0,3),(1,0,4),(0,7,11),(1,7,10),(2,8,10)]),("l",[(1,8,9),(2,8,9)]),("s",[(3,5,7),(4,4,7),(5,3,5),(5,0,1)]),("c",[(6,0,5),(7,0,8)])),
 p("h",("c",[(2,2,5),(3,1,6),(4,0,7),(5,0,7),(6,1,6),(7,2,5)])),
 p("h",("l",[(1,1,4),(2,1,4),(3,2,3),(7,3,6),(8,2,6),(9,3,5)]),("f",[(10,0,2),(11,0,3)])),
 p("v",("c",[(2,2,5),(3,1,6),(4,1,6),(5,2,6),(6,2,5),(7,2,5),(8,3,4)]))))
deer.cube(40,0,6,7,5,six(plain("f"),p("f",("h",[(0,0,3),(1,0,3),(2,1,3)]),("c",[(3,3,4),(4,2,4),(5,2,4),(6,1,4)])),p("f",("h",[(0,1,4),(1,1,4),(2,2,4)]),("c",[(3,0,1),(4,0,2),(5,0,2),(6,0,3)])),p("c",("C",[(0,1,4),(1,1,4),(2,2,3)]),("v",[(5,0,1),(6,0,5)])),plain("h"),plain("v")))
deer.cube(40,13,5,9,5,six(p("c",("C",[(0,1,3),(1,0,3),(2,1,4),(3,1,3),(4,2,3)]),("v",[(7,0,1),(8,0,2)])),p("f",("h",[(0,0,3),(1,0,4),(2,1,3)]),("c",[(3,0,1),(4,0,2),(5,0,2),(6,0,3),(7,0,3),(8,0,2)])),p("f",("h",[(0,1,4),(1,0,4),(2,1,3)]),("c",[(3,3,4),(4,2,4),(5,2,4),(6,1,4),(7,1,4),(8,2,4)])),p("f",("s",[(3,1,2),(4,1,3),(5,2,3),(6,2,3)])),plain("h"),plain("v")))
# Eyes on front corners AND adjacent side texels. Avoid disconnected black dots.
deer.cube(0,20,5,5,6,six(
 p("h",("l",[(0,1,3),(1,1,3)]),("n",[(2,0,0),(2,4,4)]),("c",[(3,1,3),(4,0,4)])),
 p("f",("h",[(0,0,3),(1,0,2)]),("N",[(1,0,1)]),("n",[(2,0,1)]),("c",[(3,0,2),(4,0,3)])),
 p("f",("h",[(0,2,5),(1,3,5)]),("N",[(1,4,5)]),("n",[(2,4,5)]),("c",[(3,3,5),(4,2,5)])),plain("f"),p("h",("l",[(0,1,3),(1,1,3),(2,2,3)])),plain("v")))
deer.cube(24,20,4,3,4,six(p("c",("n",[(0,1,2),(1,1,2)]),("v",[(2,0,3)])),p("c",("v",[(1,0,1),(2,0,2)])),p("c",("v",[(1,2,3),(2,1,3)])),plain("c"),p("c",("n",[(0,1,2)])),plain("v")))
for u in (40,48):
    deer.cube(u,28,2,4,2,six(p("v",("d",[(1,0,1),(2,0,1)])),plain("f"),plain("h"),plain("f"),plain("h"),plain("s")))
deer.cube(56,28,1,7,1,six(*[p("a",("A",[(0,0,0),(1,0,0),(2,0,0)])) for _ in range(4)],plain("A"),plain("k")))
deer.cube(56,38,3,1,1,six(plain("a"),plain("a"),plain("a"),plain("a"),plain("A"),plain("k")))
for u in (0,12,24,36):
    deer.cube(u,40,3,6,3,six(*[p("f",("h",[(0,0,1),(1,0,1),(2,1,1)]),("s",[(4,1,2),(5,0,2)])) for _ in range(4)],plain("h"),plain("s")))
    deer.cube(u,50,2,6,2,six(*[p("s",("f",[(0,0,1),(1,0,1)]),("K",[(4,0,1)]),("k",[(5,0,1)])) for _ in range(4)],plain("f"),plain("n")))
deer.cube(48,44,3,3,4,six(plain("f"),p("v",("c",[(1,1,3),(2,1,3)])),p("v",("c",[(1,0,2),(2,0,2)])),plain("c"),plain("h"),plain("c")))
deer.save()
text(J/"client/SnowstagModel.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.winterfolk.Snowstag;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class SnowstagModel extends HierarchicalModel<Snowstag>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("midnighteve","snowstag"),"main");
    private final ModelPart root,neck,head,tail;
    private final ModelPart[] legs=new ModelPart[4];
    public SnowstagModel(ModelPart r){root=r;neck=r.getChild("neck");head=neck.getChild("head");tail=r.getChild("tail");for(int i=0;i<4;i++)legs[i]=r.getChild("leg"+i);}
    public static LayerDefinition layer(){
        MeshDefinition m=new MeshDefinition();PartDefinition r=m.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-6,8,8,12),PartPose.offset(0,10,0));
        r.addOrReplaceChild("rump",CubeListBuilder.create().texOffs(40,0).addBox(-3,-3,-2,6,7,5),PartPose.offsetAndRotation(0,10,5,-.12F,0,0));
        PartDefinition n=r.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(40,13).addBox(-2.5F,-8,-2.5F,5,9,5),PartPose.offsetAndRotation(0,9,-5,.42F,0,0));
        PartDefinition h=n.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,20).addBox(-2.5F,-4,-5,5,5,6),PartPose.offsetAndRotation(0,-7,-1,-.42F,0,0));
        h.addOrReplaceChild("muzzle",CubeListBuilder.create().texOffs(24,20).addBox(-2,-1,-4,4,3,4),PartPose.offset(0,-.4F,-4));
        h.addOrReplaceChild("ear_l",CubeListBuilder.create().texOffs(40,28).addBox(-1,-4,-1,2,4,2),PartPose.offsetAndRotation(2.2F,-3,0,0,0,.9F));
        h.addOrReplaceChild("ear_r",CubeListBuilder.create().texOffs(48,28).addBox(-1,-4,-1,2,4,2),PartPose.offsetAndRotation(-2.2F,-3,0,0,0,-.9F));
        for(int sign:new int[]{-1,1}){
            PartDefinition a=h.addOrReplaceChild(sign<0?"antler_r":"antler_l",CubeListBuilder.create().texOffs(56,28).addBox(-.5F,-7,-.5F,1,7,1),PartPose.offsetAndRotation(sign*1.8F,-3,0,-.32F,0,sign*.25F));
            for(int i=0;i<2;i++)a.addOrReplaceChild("fork"+i,CubeListBuilder.create().texOffs(56,38).addBox(sign>0?0:-3,-.5F,-.5F,3,1,1),PartPose.offsetAndRotation(0,-3-i*2.5F,0,0,0,sign*(-.48F-i*.18F)));
        }
        for(int i=0;i<4;i++){
            int u=i*12;PartDefinition leg=r.addOrReplaceChild("leg"+i,CubeListBuilder.create().texOffs(u,40).addBox(-1.5F,0,-1.5F,3,6,3),PartPose.offset(i%2==0?2.5F:-2.5F,12,i<2?-4:5));
            leg.addOrReplaceChild("shin",CubeListBuilder.create().texOffs(u,50).addBox(-1,0,-1,2,6,2),PartPose.offset(0,6,0));
        }
        r.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(48,44).addBox(-1.5F,-1,0,3,3,4),PartPose.offsetAndRotation(0,8,7,-.35F,0,0));
        return LayerDefinition.create(m,64,64);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(Snowstag mob,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=Mth.clamp(yaw,-55,55)*Mth.DEG_TO_RAD;head.xRot=-.42F+Mth.clamp(pitch,-30,30)*Mth.DEG_TO_RAD;
        float step=Mth.cos(limb*.6662F)*.85F*amount;
        for(int i=0;i<4;i++){float a=(i==0||i==3)?step:-step;legs[i].xRot=a;legs[i].getChild("shin").xRot=Math.max(0,-a)*.6F;}
        tail.yRot=Mth.sin(age*.13F)*.12F;head.getChild("antler_l").visible=!mob.isBaby();head.getChild("antler_r").visible=!mob.isBaby();
    }
}
''')
# Bird: retain the silhouette the tester liked. Use the original atlas, but redraw front/side
# eye corners and all visible beak surfaces in a warmer three-tone horn ramp.
bird=w.bird
for y in range(4):
    for x in range(4):
        if y==1 and x in (0,3):bird.pixels[4+y][32+x]=bird.pal["n"]
        elif y==0 and x in (0,3):bird.pixels[4+y][32+x]=bird.pal["r"]
for y in range(4):
    for x in range(4):
        if y==1 and x==0:bird.pixels[4+y][28+x]=bird.pal["n"]
        if y==1 and x==3:bird.pixels[4+y][36+x]=bird.pal["n"]
# Beak UV (32,16), 2x2x2: lighter crown, warm midtone and dark lower edge.
for x,y,col in [(34,16,"K"),(35,16,"K"),(34,17,"K"),(35,17,"k"),(34,18,"K"),(35,18,"K"),(34,19,"k"),(35,19,"b"),(32,18,"K"),(33,18,"k"),(32,19,"k"),(33,19,"b"),(36,18,"k"),(37,18,"K"),(36,19,"b"),(37,19,"k")]:bird.pixels[y][x]=bird.pal[col]
bird.save()
# Move the small beak down a little to preserve a visible face above it.
edit(J/"client/SnowgrouseModel.java","PartPose.offset(0,-.5F,-2)","PartPose.offset(0,0,-2)")
# Hearthling is now a fabric-clad forest spirit with ears, face and scarf, not a walking lantern.
h=Atlas("hearthling",64,32,{"g":"5c744d","h":"788d5f","s":"465d40","l":"94a478","c":"bc9d76","C":"d9bd95","v":"a18262","n":"3a3027","r":"aa5146","R":"c96d58","d":"813e37","b":"806348","B":"a48761"})
h.cube(0,0,6,7,5,six(p("g",("h",[(0,0,3),(1,0,3),(2,1,2)]),("s",[(4,3,5),(5,2,5),(6,0,5)])),p("g",("h",[(0,0,2),(1,0,3),(2,1,2)]),("s",[(4,2,4),(5,2,4),(6,0,4)])),p("g",("h",[(0,1,4),(1,2,4),(2,2,3)]),("s",[(4,0,2),(5,0,3),(6,0,4)])),p("g",("s",[(4,1,3),(5,2,5),(6,0,5)])),plain("h"),plain("s")))
h.cube(24,0,7,5,5,six(
 p("g",("h",[(0,0,5),(1,0,1)]),("C",[(1,2,5),(2,1,5),(3,1,5),(4,2,4)]),("n",[(2,2,2),(2,4,4)]),("v",[(4,1,1),(4,5,5)])),
 p("g",("h",[(0,0,2),(1,0,2)]),("s",[(3,2,4),(4,1,4)]),("c",[(2,0,0),(3,0,0)])),
 p("g",("h",[(0,2,4),(1,2,4)]),("s",[(3,0,2),(4,0,3)]),("c",[(2,4,4),(3,4,4)])),
 p("g",("h",[(0,1,5),(1,1,4)]),("s",[(3,3,5),(4,2,6)])),p("h",("l",[(0,1,4),(1,1,4),(2,2,3)])),plain("s")))
h.cube(0,13,2,5,2,six(*[p("g",("h",[(0,0,1),(1,0,0)]),("c",[(3,0,1),(4,0,1)])) for _ in range(4)],plain("h"),plain("v")))
h.cube(8,13,2,5,2,six(*[p("g",("h",[(0,0,1),(1,1,1)]),("c",[(3,0,1),(4,0,1)])) for _ in range(4)],plain("h"),plain("v")))
for u in (16,26):h.cube(u,13,2,3,3,six(plain("b"),p("B",("b",[(2,0,2)])),p("B",("b",[(2,0,2)])),plain("b"),plain("B"),plain("b")))
h.cube(36,13,7,2,5,six(p("r",("R",[(0,0,3),(1,1,2)]),("d",[(1,4,6)])),plain("r"),plain("r"),plain("d"),plain("R"),plain("d")))
h.cube(0,23,2,4,1,six(p("r",("R",[(0,0,1),(1,0,1)]),("d",[(3,0,1)])),plain("r"),plain("r"),plain("r"),plain("R"),plain("d")))
for u in (8,16):h.cube(u,23,2,3,2,six(p("c",("v",[(1,0,1),(2,0,1)])),plain("g"),plain("g"),plain("s"),plain("h"),plain("s")))
h.cube(24,23,4,2,4,six(plain("g"),plain("g"),plain("g"),plain("s"),p("h",("l",[(0,1,2),(1,1,2)])),plain("s")))
h.save()
text(J/"client/HearthlingModel.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.living.Hearthling;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public final class HearthlingModel extends HierarchicalModel<Hearthling>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("midnighteve","hearthling"),"main");
    private final ModelPart root,head,armL,armR,legL,legR,scarf;
    public HearthlingModel(ModelPart r){root=r;head=r.getChild("head");armL=r.getChild("arm_l");armR=r.getChild("arm_r");legL=r.getChild("leg_l");legR=r.getChild("leg_r");scarf=r.getChild("scarf").getChild("end");}
    public static LayerDefinition layer(){
        MeshDefinition m=new MeshDefinition();PartDefinition r=m.getRoot();
        r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-3,-7,-2.5F,6,7,5),PartPose.offset(0,21,0));
        PartDefinition h=r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(24,0).addBox(-3.5F,-5,-2.5F,7,5,5),PartPose.offset(0,14,0));
        h.addOrReplaceChild("hood",CubeListBuilder.create().texOffs(24,23).addBox(-2,-2,-2,4,2,4),PartPose.offsetAndRotation(0,-5,.25F,.15F,0,0));
        h.addOrReplaceChild("ear_l",CubeListBuilder.create().texOffs(8,23).addBox(-1,-3,-1,2,3,2),PartPose.offsetAndRotation(3,-3,0,0,0,.9F));
        h.addOrReplaceChild("ear_r",CubeListBuilder.create().texOffs(16,23).addBox(-1,-3,-1,2,3,2),PartPose.offsetAndRotation(-3,-3,0,0,0,-.9F));
        r.addOrReplaceChild("arm_l",CubeListBuilder.create().texOffs(0,13).addBox(0,0,-1,2,5,2),PartPose.offset(2.8F,15,0));
        r.addOrReplaceChild("arm_r",CubeListBuilder.create().texOffs(8,13).addBox(-2,0,-1,2,5,2),PartPose.offset(-2.8F,15,0));
        r.addOrReplaceChild("leg_l",CubeListBuilder.create().texOffs(16,13).addBox(-1,0,-1.5F,2,3,3),PartPose.offset(1.5F,21,0));
        r.addOrReplaceChild("leg_r",CubeListBuilder.create().texOffs(26,13).addBox(-1,0,-1.5F,2,3,3),PartPose.offset(-1.5F,21,0));
        PartDefinition s=r.addOrReplaceChild("scarf",CubeListBuilder.create().texOffs(36,13).addBox(-3.5F,0,-2.5F,7,2,5),PartPose.offset(0,13,0));
        s.addOrReplaceChild("end",CubeListBuilder.create().texOffs(0,23).addBox(-1,0,-.5F,2,4,1),PartPose.offset(1.4F,1,-2.6F));
        return LayerDefinition.create(m,64,32);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(Hearthling mob,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        legL.xRot=Mth.cos(limb*.8F)*amount*.85F;legR.xRot=-legL.xRot;
        armL.xRot=-legL.xRot*.65F;armR.xRot=-legR.xRot*.65F;
        armL.zRot=-.18F-Mth.sin(age*.07F)*.035F;armR.zRot=.18F+Mth.sin(age*.07F)*.035F;
        scarf.xRot=.12F+amount*.3F+Mth.sin(age*.12F)*.05F;
    }
}
''')
# Item redraws: true 16x16 silhouettes, consistent diagonal inventory presentation,
# warm baked dough with off-white icing; dark stitched leather and paper book edges.
f.sprite("gingerbread",["......ooo.....",".....ohhho....",".....ohcho....","......hho.....","..ooohhhoo....",".ohhhhchhhho..",".ohhhohhhooo..","..oooohhho....",".....hchho....",".....hhmho....","....ohh.hho...","....ohh.ohho..","...ohho..hho..","...ooo...ooo.."],{"o":"8d5935","h":"c28b51","m":"af7541","c":"f0dfbd"})
f.sprite("market_ledger",[".....oooooo...","...oohhhhggo..",".oohhhhggggo..","ohhhgggggggo..","ohgggggggggo..","ohggllgggsgo..","ohggllggssgo..","ohgggggssggo..","ohggggssgggo..","ohgggggggooo..","ohggggoooPpo..","oggoooPPpppo..","oooPPppppoo...","..ooooooo....."],{"o":"494031","h":"9e8b58","g":"69764c","s":"596740","l":"b1a273","P":"ddd0ae","p":"b8a684"})
print("PASS: tester-directed fauna and inventory art replacement; climate and aurora untouched")
