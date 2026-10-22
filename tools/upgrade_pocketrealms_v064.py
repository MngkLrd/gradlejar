"""Pocket Realms 0.6.4: autonomous nightmare mobs, relics, collapse and spawn guarantees.
Installs AFTER 0.6.3, without changing legacy map formats or client/server separation.
"""
from pathlib import Path
import json
import random
import struct
import zlib

repo=Path(__file__).resolve().parents[1]
root=repo/"projects/pocketrealms"
java=root/"src/main/java/com/kirill/pocketrealms"
res=root/"src/main/resources"
assets=res/"assets/pocketrealms"
staged=repo/"tools/pocketrealms_v064"

def patch(path,old,new):
    content=path.read_text(encoding="utf8")
    if content.count(old)!=1:
        raise SystemExit(f"0.6.4 expected exactly one patch anchor in {path}: {old!r} got {content.count(old)}")
    path.write_text(content.replace(old,new,1),encoding="utf8")

for f in sorted(staged.rglob("*.java")):
    target=java/f.relative_to(staged)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(f.read_bytes())
    print("v064",f.relative_to(staged))

# Registries: both predators share the previously registered common entity bus.
registry=java/"registry/ModEntities.java"
patch(registry,
    'import com.kirill.pocketrealms.entity.VoidPursuer;',
    'import com.kirill.pocketrealms.entity.VoidPursuer;\nimport com.kirill.pocketrealms.entity.VeilHound;\nimport com.kirill.pocketrealms.entity.VeilGhost;')
patch(registry,
    '    public static final Supplier<EntityType<VoidPursuer>> VOID_PURSUER = ENTITIES.register(',
    '''    public static final Supplier<EntityType<VeilHound>> VEIL_HOUND = ENTITIES.register(
            "veil_hound", () -> EntityType.Builder.of(VeilHound::new, MobCategory.MONSTER)
                    .sized(0.7F,0.86F).clientTrackingRange(12).build("veil_hound"));
    public static final Supplier<EntityType<VeilGhost>> VEIL_GHOST = ENTITIES.register(
            "veil_ghost", () -> EntityType.Builder.of(VeilGhost::new, MobCategory.MONSTER)
                    .sized(0.6F,1.99F).clientTrackingRange(12).build("veil_ghost"));
    public static final Supplier<EntityType<VoidPursuer>> VOID_PURSUER = ENTITIES.register(''')

attributes=java/"registry/ModEntityAttributes.java"
patch(attributes,'import com.kirill.pocketrealms.entity.VoidPursuer;',
    'import com.kirill.pocketrealms.entity.VoidPursuer;\nimport com.kirill.pocketrealms.entity.VeilHound;\nimport com.kirill.pocketrealms.entity.VeilGhost;')
patch(attributes,
    '        event.put(ModEntities.WALL_GRASPER.get(), WallGrasper.createAttributes().build());',
    '''        event.put(ModEntities.VEIL_HOUND.get(), VeilHound.createAttributes().build());
        event.put(ModEntities.VEIL_GHOST.get(), AbstractSkeleton.createAttributes().build());
        event.put(ModEntities.WALL_GRASPER.get(), WallGrasper.createAttributes().build());''')

items=java/"registry/ModItems.java"
patch(items,'import net.minecraft.world.item.Tiers;',
    '''import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import com.kirill.pocketrealms.item.RealmTotemItem;''')
patch(items,
    '    public static final DeferredItem<Item> CRUMBLING_STONE = ITEMS.registerItem("crumbling_stone",',
    '''    // The veil set uses iron-class protection, so it is not stronger than endgame gear.
    // Its full-set benefit is reduced Veil Hound rear-attack damage.
    public static final DeferredItem<Item> VEIL_HOOD = ITEMS.registerItem("veil_hood",
            p -> new ArmorItem(ArmorMaterials.IRON,ArmorItem.Type.HELMET,p.durability(220)));
    public static final DeferredItem<Item> VEIL_COAT = ITEMS.registerItem("veil_coat",
            p -> new ArmorItem(ArmorMaterials.IRON,ArmorItem.Type.CHESTPLATE,p.durability(320)));
    public static final DeferredItem<Item> VEIL_LEGGINGS = ITEMS.registerItem("veil_leggings",
            p -> new ArmorItem(ArmorMaterials.IRON,ArmorItem.Type.LEGGINGS,p.durability(290)));
    public static final DeferredItem<Item> VEIL_BOOTS = ITEMS.registerItem("veil_boots",
            p -> new ArmorItem(ArmorMaterials.IRON,ArmorItem.Type.BOOTS,p.durability(260)));
    public static final DeferredItem<Item> REALM_TOTEM = ITEMS.registerItem("realm_totem",
            p -> new RealmTotemItem(p.stacksTo(16)));
    public static final DeferredItem<Item> CRUMBLING_STONE = ITEMS.registerItem("crumbling_stone",''')

tabs=java/"registry/ModCreativeTabs.java"
patch(tabs,'                output.accept(ModItems.POCKET_DOOR.get());',
    '''                output.accept(ModItems.POCKET_DOOR.get());
                output.accept(ModItems.REALM_TOTEM.get());
                output.accept(ModItems.VEIL_HOOD.get());
                output.accept(ModItems.VEIL_COAT.get());
                output.accept(ModItems.VEIL_LEGGINGS.get());
                output.accept(ModItems.VEIL_BOOTS.get());''')

for loc,translations in {
    "en_us":{"item.pocketrealms.realm_totem":"Realm Totem",
             "item.pocketrealms.veil_hood":"Veil Hood",
             "item.pocketrealms.veil_coat":"Veil Coat",
             "item.pocketrealms.veil_leggings":"Veil Leggings",
             "item.pocketrealms.veil_boots":"Veil Boots",
             "entity.pocketrealms.veil_hound":"Veil Hound",
             "entity.pocketrealms.veil_ghost":"The Witness"},
    "ru_ru":{"item.pocketrealms.realm_totem":"Тотем кармана",
             "item.pocketrealms.veil_hood":"Капюшон Завесы",
             "item.pocketrealms.veil_coat":"Нагрудник Завесы",
             "item.pocketrealms.veil_leggings":"Поножи Завесы",
             "item.pocketrealms.veil_boots":"Сапоги Завесы",
             "entity.pocketrealms.veil_hound":"Призрачная гончая",
             "entity.pocketrealms.veil_ghost":"Наблюдатель"}
}.items():
    path=assets/"lang"/f"{loc}.json"
    j=json.loads(path.read_text("utf8"));j.update(translations)
    path.write_text(json.dumps(j,indent=2,ensure_ascii=False)+"\n",encoding="utf8")

def png(pixels):
    h=len(pixels);w=len(pixels[0])
    raw=b"".join(b"\0"+b"".join(bytes(rgba) for rgba in row) for row in pixels)
    def ch(k,d):
        return struct.pack(">I",len(d))+k+d+struct.pack(">I",zlib.crc32(k+d)&0xffffffff)
    return b"\x89PNG\r\n\x1a\n"+ch(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))+ch(b"IDAT",zlib.compress(raw,9))+ch(b"IEND",b"")

# 64x64 layered MC entity UV palettes. All silhouettes dark and matte, strongly readable.
for name,seed,base,bright,alpha in [
        ("veil_hound",640,(19,20,29),(66,49,81),145),
        ("veil_ghost",641,(55,57,72),(169,177,198),115)]:
    rand=random.Random(seed)
    pixels=[]
    for y in range(64):
        row=[]
        for x in range(64):
            cluster=((x//4*3+y//4*5+rand.randrange(2))%8)
            tone=(bright if cluster==0 else base)
            a=max(55,min(210,alpha+(36 if cluster==0 else -10)))
            row.append((tone[0],tone[1],tone[2],a))
        pixels.append(row)
    # Long, glowing violet/cold-white eyes on the standard head face.
    color=(202,119,230,225) if name=="veil_hound" else (220,235,255,200)
    for y in range(10,12):
        for x in (9,10,13,14):pixels[y][x]=color
    for y in (12,13):
        for x in range(9,15):pixels[y][x]=(6,7,12,205)
    if name=="veil_hound":
        for x in range(18,42):
            for y in range(17,20): pixels[y][x]=(37,29,52,170)
    dest=assets/"textures/entity"/f"{name}.png"
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(png(pixels))

# Compact icon sprites (16x16, opaque blocks of pixel clusters, no AA).
palette={
    "veil_hood":("hood",(26,25,36),(74,65,86),(184,105,186)),
    "veil_coat":("coat",(24,22,30),(80,66,90),(176,95,180)),
    "veil_leggings":("legs",(24,25,32),(79,72,91),(174,105,182)),
    "veil_boots":("boots",(24,25,31),(65,63,81),(170,92,176)),
    "realm_totem":("totem",(28,25,37),(104,80,125),(208,132,219))}
for item,(kind,dark,body,accent) in palette.items():
    image=[[(0,0,0,0) for _ in range(16)] for _ in range(16)]
    def rect(x0,y0,x1,y1,color):
        for y in range(max(0,y0),min(16,y1)):
            for x in range(max(0,x0),min(16,x1)):image[y][x]=(*color,255)
    if kind=="hood":
        rect(3,2,13,13,dark);rect(4,3,12,12,body);rect(5,5,11,11,dark);rect(6,7,10,8,accent)
    elif kind=="coat":
        rect(3,2,13,7,dark);rect(1,4,5,11,body);rect(11,4,15,11,body)
        rect(4,4,12,15,body);rect(7,3,9,14,dark);rect(7,7,9,12,accent)
    elif kind=="legs":
        rect(4,2,12,7,body);rect(4,6,7,15,dark);rect(9,6,12,15,dark)
        rect(5,8,7,12,body);rect(10,8,12,12,body);rect(4,6,12,7,accent)
    elif kind=="boots":
        rect(3,3,7,11,body);rect(9,3,13,11,body)
        rect(2,10,8,14,dark);rect(8,10,14,14,dark)
        rect(2,11,8,12,accent);rect(9,11,14,12,accent)
    elif kind=="totem":
        rect(6,1,10,14,dark);rect(5,4,11,12,body);rect(3,6,13,10,dark)
        rect(6,5,10,10,accent);rect(7,6,9,9,(230,185,228))
        rect(6,13,10,15,accent)
    dest=assets/"textures/item"/f"{item}.png"
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(png(image))
    model=assets/"models/item"/f"{item}.json"
    model.parent.mkdir(parents=True,exist_ok=True)
    model.write_text(json.dumps({"parent":"minecraft:item/generated",
                                "textures":{"layer0":f"pocketrealms:item/{item}"}},indent=2)+"\n",encoding="utf8")

recipes={
"realm_totem":(["OEO","ESE","OEO"],{"O":"minecraft:obsidian","E":"minecraft:ender_pearl","S":"minecraft:echo_shard"}),
"veil_hood":(["E E","IPI"," I "],{"E":"minecraft:echo_shard","I":"minecraft:iron_ingot","P":"minecraft:phantom_membrane"}),
"veil_coat":(["P P","IEI","III"],{"P":"minecraft:phantom_membrane","I":"minecraft:iron_ingot","E":"minecraft:echo_shard"}),
"veil_leggings":(["III","P P","I I"],{"P":"minecraft:phantom_membrane","I":"minecraft:iron_ingot"}),
"veil_boots":(["P P","I I"],{"P":"minecraft:phantom_membrane","I":"minecraft:iron_ingot"})}
for item,(pattern,key) in recipes.items():
    path=res/"data/pocketrealms/recipe"/f"{item}.json"
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps({"type":"minecraft:crafting_shaped","category":"equipment",
        "pattern":pattern,"key":{k:{"item":v} for k,v in key.items()},
        "result":{"id":f"pocketrealms:{item}","count":1}},indent=2)+"\n",encoding="utf8")

properties=root/"gradle.properties"
patch(properties,"mod_version=0.6.3","mod_version=0.6.4")

with (root/"README.md").open("a",encoding="utf8") as f:
    f.write("""
\n## Pocket Realms 0.6.4 — Behind You
- **Final collapse is fatal:** no path preservation after the deadline.
  Sanctuary persists, but corridors and floor outside may disappear completely.
- **Veil Hound:** translucent wolf; moves incrementally THROUGH walls, never
  teleports; runs away when directly watched; rear-only bite and recoverable combat.
- **The Witness:** separate ephemeral, wall-phasing apparition that dissolves after
  a sustained gaze. Hounds and ghosts are capped to one of each per active run.
- **Endermen:** at most two per pocket; prevent spawning on unsupported floors;
  clear excess/void-bound entities each second.
- **Veil armor:** iron-class four-piece set with special rear-bite mitigation.
  Item art is bespoke; equipped armor currently uses vanilla iron's render layers.
- **Realm Totem:** one-use ritual crafted from obsidian, pearl and echo shard.
  Right click in the Overworld for a FRESH run; item is consumed only after
  successful teleport; no Pocket Door or surrounding blocks are destroyed.
- **Torch at spawn:** explicitly placed on the start floor AFTER maze generation.
- Standard 1.21.1 recipe JSONs, no required third-party mod dependency.
""")
print("PASS Pocket Realms 0.6.4 overlay applied.")
