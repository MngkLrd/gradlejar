#!/usr/bin/env python3
"""Halo Haven 2.2 hotfix for addon bundle. Deterministic source patch before Gradle settings extraction."""
from __future__ import annotations
from pathlib import Path
import tarfile, tempfile, re, json, math, random, colorsys, wave, subprocess, shutil, io, hashlib

PROJECT = Path("projects/halohaven-alpha21")
BUNDLE = PROJECT / "bundle.tar.xz"
assert BUNDLE.is_file(), BUNDLE
tmp = tempfile.TemporaryDirectory()
root = Path(tmp.name)
with tarfile.open(BUNDLE, "r:xz") as tar:
    for member in tar:
        if not member.isfile(): continue
        relative = Path(member.name)
        assert relative.parts[0] == "src" and ".." not in relative.parts
        dest = root/relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(tar.extractfile(member).read())

SRC = root/"src/main/java/dev/halohaven/alpha2"
RES = root/"src/main/resources"
ASSET = RES/"assets/halohavenalpha2"

def update(path, before, after):
    text = path.read_text(encoding="utf-8")
    assert before in text, (path, before[:100])
    path.write_text(text.replace(before, after), encoding="utf-8")

# Real two-high, interactive timber doors (preserve old registry IDs for existing saves).
main = SRC/"HaloHavenAlpha2.java"
update(main, "import net.minecraft.world.level.block.state.BlockBehaviour;",
       "import net.minecraft.world.level.block.state.BlockBehaviour;\nimport net.minecraft.world.level.block.state.properties.BlockSetType;")
for wood in ("dawnwood","heartwood","veilwood"):
    for suffix,klass in (("door_face","DoorBlock"),("trapdoor_face","TrapDoorBlock")):
        name = wood+"_"+suffix
        old = 'BLOCKS.register("'+name+'", () -> new Block(BlockBehaviour.Properties.of().strength(1.6F,6.0F).requiresCorrectToolForDrops()))'
        new = 'BLOCKS.register("'+name+'", () -> new '+klass+'(BlockSetType.OAK,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).strength(3.0F).noOcclusion()))'
        if suffix=="trapdoor_face":
            new = 'BLOCKS.register("'+name+'", () -> new '+klass+'(BlockSetType.OAK,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).strength(3.0F).noOcclusion()))'
        update(main,old,new)
update(main,"        HavenEntities.register(bus);","        HavenEntities.register(bus);\n        HavenSounds.register(bus);")

# Full vanilla-style door combinations. Rotation follows the 1.21.1 vanilla door blockstates.
rot_closed = {"east":0,"north":270,"south":90,"west":180}
rot_left_open = {"east":90,"north":0,"south":180,"west":270}
rot_right_open = {"east":270,"north":180,"south":0,"west":90}
for wood in ("dawnwood","heartwood","veilwood"):
    name=wood+"_door_face"
    models=ASSET/"models/block"
    states=ASSET/"blockstates"
    states.mkdir(parents=True,exist_ok=True)
    models.mkdir(parents=True,exist_ok=True)
    for half in ("bottom","top"):
        for hinge in ("left","right"):
            for opened in ("","_open"):
                model=name+"_"+half+"_"+hinge+opened
                (models/(model+".json")).write_text(json.dumps({
                    "parent":"minecraft:block/door_"+half+"_"+hinge+opened,
                    "textures":{"bottom":"halohavenalpha2:block/"+wood+"_door_face",
                                "top":"halohavenalpha2:block/"+wood+"_door_face"}},separators=(",",":")))
    variants={}
    for facing in rot_closed:
        for half in ("lower","upper"):
            for hinge in ("left","right"):
                for opened in (False,True):
                    y=(rot_left_open[facing] if hinge=="left" else rot_right_open[facing]) if opened else rot_closed[facing]
                    ref=name+"_"+("bottom" if half=="lower" else "top")+"_"+hinge+("_open" if opened else "")
                    variants["facing="+facing+",half="+half+",hinge="+hinge+",open="+str(opened).lower()]={
                        "model":"halohavenalpha2:block/"+ref,**({"y":y} if y else {})}
    (states/(name+".json")).write_text(json.dumps({"variants":variants},indent=2))
    (ASSET/"models/item"/(name+".json")).write_text(json.dumps({
        "parent":"minecraft:item/generated",
        "textures":{"layer0":"halohavenalpha2:block/"+name}},indent=2))
    # Real trapdoors with a horizontal upper/lower state and hinged open panel.
    tn=wood+"_trapdoor_face"
    for model, parent in (("bottom","template_orientable_trapdoor_bottom"),
                          ("top","template_orientable_trapdoor_top"),
                          ("open","template_orientable_trapdoor_open")):
        (models/(tn+"_"+model+".json")).write_text(json.dumps({
            "parent":"minecraft:block/"+parent,
            "textures":{"texture":"halohavenalpha2:block/"+tn}},indent=2))
    v={}
    for facing,y in (("north",0),("east",90),("south",180),("west",270)):
        for half in ("bottom","top"):
            for opened in (False,True):
                label="facing="+facing+",half="+half+",open="+str(opened).lower()
                v[label]={"model":"halohavenalpha2:block/"+tn+"_"+("open" if opened else half),"y":y}
    (states/(tn+".json")).write_text(json.dumps({"variants":v},indent=2))
    (ASSET/"models/item"/(tn+".json")).write_text(json.dumps({
        "parent":"halohavenalpha2:block/"+tn+"_bottom"},indent=2))

# Replace generic cube UV layout with dedicated face regions that do not overlap.
model = SRC/"client/HavenClient.java"
update(model, "CubeListBuilder.create().texOffs(0,18).addBox(-bw/2f", "CubeListBuilder.create().texOffs(0,32).addBox(-bw/2f")
update(model, "CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-6,8,8,7)",
       "CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-5,6,6,6)")
update(model, "CubeListBuilder.create().texOffs(64,0)", "CubeListBuilder.create().texOffs(0,72)")
update(model, "CubeListBuilder.create().texOffs(64,20)", "CubeListBuilder.create().texOffs(38,72)")
update(model, "CubeListBuilder.create().texOffs(64,40)", "CubeListBuilder.create().texOffs(0,100)")
update(model, "CubeListBuilder.create().texOffs(96,42)", "CubeListBuilder.create().texOffs(100,0)")
update(model, "CubeListBuilder.create().texOffs(110,42)", "CubeListBuilder.create().texOffs(100,24)")
update(model, 'add(body,"shell",CubeListBuilder.create().texOffs(0,18)',
       'add(body,"shell",CubeListBuilder.create().texOffs(0,52)')
update(model, "wingLeft.visible=species.flight || species.form==HavenSpecies.Form.FOX;",
       "wingLeft.visible=species.flight || species.id.equals(\"sunpuff\");")
update(model, "frontLeft.visible=frontRight.visible=backLeft.visible=backRight.visible=legs;",
       """frontLeft.visible=frontRight.visible=legs;
            boolean biped=species.form==HavenSpecies.Form.ROBED || species.form==HavenSpecies.Form.GOLEM ||
                          species.form==HavenSpecies.Form.STALKER;
            backLeft.visible=backRight.visible=legs && !biped;
            if(biped){frontLeft.z=0;frontRight.z=0;}""")
update(model, "body.y=species.flight?13+Mth.sin(ageInTicks*.13f)*.6f:13;",
       """body.y=species.flight?13+Mth.sin(ageInTicks*.13f)*.6f:
                ((species.form==HavenSpecies.Form.ROBED || species.form==HavenSpecies.Form.GOLEM ||
                  species.form==HavenSpecies.Form.STALKER)?10:13);
            head.visible=species.form!=HavenSpecies.Form.MANTA && species.form!=HavenSpecies.Form.WHALE;
            tail.visible=species.form==HavenSpecies.Form.DEER || species.form==HavenSpecies.Form.FOX ||
                         species.form==HavenSpecies.Form.CAT || species.form==HavenSpecies.Form.BEAST ||
                         species.form==HavenSpecies.Form.MANTA || species.form==HavenSpecies.Form.WHALE;""")

# Each face is designed in pixel-clusters per material. Not random isolated pixels.
from PIL import Image, ImageDraw
palette={
 "sunpuff":"#b6bfa0","dawnstag":"#c6ac8b","pollenwing":"#e2c4a0",
 "chalkshell":"#b4bdba","palewing":"#d2c9cf","rubblelurker":"#918c89",
 "crownfox":"#6f9a7a","mossback":"#5d7c60","barkstalker":"#4d725c",
 "chimewing":"#8dbabc","resonant":"#537980","cliffprowler":"#788493",
 "littlechime":"#d4c9ae","silentchorister":"#9e929d","choir_sentinel":"#b4ada8",
 "veilmanta":"#697b92","shadowstalker":"#434a69","memory_wisp":"#75a9b2",
 "cloud_wanderer":"#9bb5c0","sky_swallow":"#b6d2c7"
}
hard=set("chalkshell rubblelurker resonant choir_sentinel".split())
feather=set("pollenwing palewing chimewing sky_swallow".split())
spectral=set("littlechime silentchorister memory_wisp veilmanta".split())
def color(hexcode, factor):
    rgb=tuple(int(hexcode[i:i+2],16) for i in (1,3,5))
    return tuple(max(0,min(255,int(x*factor))) for x in rgb)+(255,)
def image_for(name):
    base=palette[name]
    pal=[color(base,f) for f in (0.56,0.72,0.85,1,1.09,1.18)]
    im=Image.new("RGBA",(128,128),(0,0,0,0))
    rng=random.Random(233+sum((i+1)*ord(x) for i,x in enumerate(name)))
    def region(x0,y0,w,h,kind):
        d=ImageDraw.Draw(im)
        d.rectangle((x0,y0,x0+w-1,y0+h-1),fill=pal[2])
        # Top-lit face + quiet large clusters at constrained centers.
        d.line((x0,y0,x0+w-1,y0),fill=pal[4],width=1)
        d.line((x0+w-1,y0,x0+w-1,y0+h-1),fill=pal[1],width=1)
        patches=max(2,(w*h)//24)
        for i in range(patches):
            x=x0+rng.randrange(max(1,w-3))
            y=y0+rng.randrange(max(1,h-3))
            if kind=="fur":
                dx,dy=(2,1) if i%3 else (1,2)
            elif kind=="stone":
                dx,dy=(3,1) if i%3 else (1,2)
            elif kind=="feather":
                dx,dy=(3,1)
            else:
                dx,dy=(2,2)
            d.rectangle((x,y,min(x+dx,x0+w-1),min(y+dy,y0+h-1)),
                        fill=pal[3+i%3] if i%4 else pal[1])
        if kind=="stone":
            for x in range(x0+3,x0+w-1,7):
                y=y0+3+(x*3)%max(2,h-4)
                if y<y0+h-1:d.line((x,y,x+2,y),fill=pal[0])
        if kind=="feather":
            for y in range(y0+2,y0+h-1,4):
                d.line((x0+2,y,x0+w-3,y),fill=pal[4])
    kind="stone" if name in hard else "feather" if name in feather else "arcane" if name in spectral else "fur"
    # Full cube UV spans, with independently shaded head, trunk, legs, wing membrane and tail.
    for args in [(0,0,25,14),(0,32,72,20),(0,52,72,17),(0,72,18,18),
                 (38,72,60,18),(0,100,32,18),(100,0,27,19),(100,24,27,15)]:
        region(*args,kind)
    # Distinct 6x6 head, UV front face lies within (6..11, 6..11).
    d=ImageDraw.Draw(im)
    eyes=pal[0] if name not in spectral else (224,239,210,255)
    iris=(252,231,168,255) if name not in spectral else (126,231,237,255)
    for x in (7,10):
        d.rectangle((x,8,x+1,9),fill=eyes)
        im.putpixel((x,8),iris)
    im.putpixel((9,11),pal[1])
    # Body identity: shoulder patches, mineral veins, spirals or vertebral ribs.
    if name in hard:
        for x,y in ((11,42),(16,45),(26,39),(31,44),(46,41)):
            d.line((x,y,x+2,y-2),fill=pal[5],width=2)
    elif kind=="fur":
        for x,y in ((13,43),(19,48),(37,42),(46,46)):
            d.line((x,y,x+4,y),fill=pal[1],width=2)
    elif kind=="arcane":
        for x,y in ((13,43),(25,44),(39,42)):
            d.rectangle((x,y,x+1,y+2),fill=iris)
    if name in ("dawnstag","crownfox","cliffprowler","sunpuff"):
        for x in (8,14,20):
            d.line((x,2,x+1,2),fill=pal[5])
    if name=="veilmanta":
        for x in range(42,86,9):
            d.line((x,76,x+4,78),fill=(125,222,213,255))
    if name=="sky_swallow":
        for x in range(42,88,10):
            d.line((x,76,x+6,76),fill=pal[5])
    return im

sprites=ASSET/"textures/entity"
for species in palette:
    target=sprites/(species+".png")
    assert target.exists(),target
    image_for(species).save(target)
    assert Image.open(target).size==(128,128)
# Original procedural sound design, no third-party sound recordings.
sonic=ASSET/"sounds/mob"
sonic.mkdir(parents=True,exist_ok=True)
snd_json={}
sample_rate=22050
for i,name in enumerate(list(palette)+["skyfolk"]):
    seed=3123+i*33
    rng=random.Random(seed)
    def clip(t):
        form=(
            "bell" if name in ("skyfolk","littlechime","resonant","choir_sentinel")
            else "wind" if name in ("veilmanta","cloud_wanderer","memory_wisp","silentchorister")
            else "chirp" if name in ("pollenwing","chimewing","sky_swallow","palewing")
            else "growl" if name in ("shadowstalker","barkstalker","rubblelurker","cliffprowler")
            else "animal"
        )
        duration={"bell":1.1,"wind":1.25,"chirp":.46,"growl":.72,"animal":.62}[form]
        if t>=duration:return 0
        fadein=min(1,t/.07)
        fadeout=min(1,(duration-t)/(.24 if form in ("bell","wind") else .18))
        env=max(0,fadein*fadeout)
        f=(180+((i*37)%240))
        if form=="bell":
            val=.40*math.sin(2*math.pi*f*t)*math.exp(-1.8*t)+.17*math.sin(2*math.pi*f*2.4*t)*math.exp(-3*t)
        elif form=="wind":
            val=.24*math.sin(2*math.pi*(f*.7+15*math.sin(t*3))*t)+.13*math.sin(2*math.pi*f*1.18*t)
        elif form=="chirp":
            pitch=f*(1.1+1.1*t/duration)
            val=.25*math.sin(2*math.pi*pitch*t)+.05*math.sin(2*math.pi*2*pitch*t)
        elif form=="growl":
            val=.22*math.sin(2*math.pi*(75+f*.2)*t)+.08*math.sin(2*math.pi*(150+f*.2)*t)
            val+=.04*(rng.random()-.5)
        else:
            val=.17*math.sin(2*math.pi*(f+20*math.sin(t*9))*t)+.08*math.sin(2*math.pi*(f*.6)*t)
        return max(-.8,min(.8,val*env))
    import struct
    duration=1.25 if name in ("veilmanta","cloud_wanderer","memory_wisp","silentchorister") else 1.1 if name in ("skyfolk","littlechime","resonant","choir_sentinel") else .72 if name in ("shadowstalker","barkstalker","rubblelurker","cliffprowler") else .46 if name in ("pollenwing","chimewing","sky_swallow","palewing") else .62
    wav=sonic/(name+".wav")
    with wave.open(str(wav),"wb") as w:
        w.setframerate(sample_rate);w.setnchannels(1);w.setsampwidth(2)
        frames=b"".join(struct.pack("<h",int(clip(n/sample_rate)*32767)) for n in range(int(sample_rate*duration)))
        w.writeframes(frames)
    ogg=sonic/(name+".ogg")
    subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",str(wav),"-c:a","libvorbis","-qscale:a","3",str(ogg)],check=True)
    wav.unlink()
    snd_json["mob."+name]={"sounds":[{"name":"halohavenalpha2:mob/"+name,"volume":.75 if name!="skyfolk" else .6}]}
(ASSET/"sounds.json").write_text(json.dumps(snd_json,indent=2),encoding="utf-8")

(SRC/"HavenSounds.java").write_text("""package dev.halohaven.alpha2;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class HavenSounds {
    public static final DeferredRegister<SoundEvent> REG=DeferredRegister.create(Registries.SOUND_EVENT,HaloHavenAlpha2.MODID);
    public static final Map<String,DeferredHolder<SoundEvent,SoundEvent>> AMBIENT=new LinkedHashMap<>();
    static {
        for (HavenSpecies species:HavenSpecies.values()){
            String name="mob_"+species.id;
            AMBIENT.put(species.id,REG.register(name,()->SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HaloHavenAlpha2.MODID,"mob."+species.id))));
        }
        REG.register("mob_skyfolk",()->SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(HaloHavenAlpha2.MODID,"mob.skyfolk")));
    }
    public static void register(IEventBus bus){REG.register(bus);}
}
""",encoding="utf-8")

creature=SRC/"HavenCreature.java"
text=creature.read_text(encoding="utf-8")
start=text.index("    @Override protected SoundEvent getAmbientSound(){")
end=text.index("    @Override protected SoundEvent getHurtSound",start)
text=text[:start]+"""    @Override protected SoundEvent getAmbientSound(){
        return HavenSounds.AMBIENT.get(species().id).get();
    }
"""+text[end:]
creature.write_text(text,encoding="utf-8")

# The original NBT village houses have clear two-block doors but generic birch material.
# Edit their palette only; do not change coordinates/room volumes.
try:
    import nbtlib
    for biome in ("dawn","canopy","veil","chalk","gorge","choir"):
        f=RES/"data/halohavenalpha2/structure"/("settlement_"+biome+".nbt")
        if not f.exists():continue
        nbt=nbtlib.load(f,gzipped=True)
        wood="heartwood" if biome=="canopy" else "veilwood" if biome=="veil" else "dawnwood"
        for state in nbt.get("palette",[]):
            if str(state.get("Name",""))=="minecraft:birch_door":
                state["Name"]=nbtlib.String("halohavenalpha2:"+wood+"_door_face")
        nbt.save(f,gzipped=True)
except ImportError:
    print("nbtlib absent: retaining vanilla working template doors")

# Keep the release self-documenting and explicitly credit original audio composition.
(RES/"halo_audio_credits.txt").write_text(
    "Halo Haven 2.2 sound recordings: originally synthesized for this mod in Python; "
    "no external recordings, performers, or third-party samples were used.\n",encoding="utf-8")
with tarfile.open(BUNDLE,"w:xz",preset=6) as tar:
    for src in sorted((root/"src").rglob("*")):
        if src.is_file():
            tar.add(src,arcname=src.relative_to(root).as_posix())
print("Patched Halo Haven bundle:",BUNDLE.stat().st_size,"bytes, 20 UV skins, 21 original OGG voices, 3 native doors")
tmp.cleanup()
