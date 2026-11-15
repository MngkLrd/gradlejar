"""Restore Pocket Realms 0.5.0 assets and apply registries/event hooks."""
from pathlib import Path
import base64,hashlib,io,lzma,tarfile,json

root = Path(__file__).resolve().parents[1]/"projects"/"pocketrealms"
parts=[root/f"upgrade-v050-{n:02d}.b64" for n in range(3)]
data=base64.b64decode("".join(p.read_text("ascii") for p in parts),validate=True)
sha=hashlib.sha256(data).hexdigest()
if sha!="0372b7bc961cd53c02d3b0cb04ce47f77234881cd90d2431c17cd03e1eb3b15e":
    raise SystemExit("Pocket Realms 0.5.0 source transfer corrupted: "+sha)
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(data)),mode="r:") as archive:
    for member in archive:
        part=Path(member.name)
        if not member.isfile() or part.is_absolute() or ".." in part.parts:
            raise SystemExit("Unsafe source member "+member.name)
        dest=root/part
        dest.parent.mkdir(parents=True,exist_ok=True)
        with archive.extractfile(member) as fh:
            dest.write_bytes(fh.read())

src=root/"src/main/java/com/kirill/pocketrealms"
def edit(relative, changes):
    path=src/relative
    content=path.read_text(encoding="utf8")
    for old,new in changes:
        if content.count(old)!=1:raise SystemExit(relative+" has unexpected count for "+repr(old))
        content=content.replace(old,new,1)
    path.write_text(content,encoding="utf8")

edit("registry/ModEntities.java", [
("import com.kirill.pocketrealms.entity.CorruptedZombie;",
"import com.kirill.pocketrealms.entity.CorruptedZombie;\nimport com.kirill.pocketrealms.entity.WallGrasper;\nimport com.kirill.pocketrealms.entity.VoidPursuer;"),
('    public static final Supplier<EntityType<CorruptedSkeleton>> CORRUPTED_SKELETON = ENTITIES.register(',
 '    public static final Supplier<EntityType<CorruptedSkeleton>> CORRUPTED_SKELETON = ENTITIES.register('),
('                    .sized(0.6F, 1.99F).clientTrackingRange(8).build("corrupted_skeleton"));',
 '''                    .sized(0.6F, 1.99F).clientTrackingRange(8).build("corrupted_skeleton"));

    public static final Supplier<EntityType<WallGrasper>> WALL_GRASPER = ENTITIES.register(
            "wall_grasper", () -> EntityType.Builder.of(WallGrasper::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.45F).clientTrackingRange(8).build("wall_grasper"));

    public static final Supplier<EntityType<VoidPursuer>> VOID_PURSUER = ENTITIES.register(
            "void_pursuer", () -> EntityType.Builder.of(VoidPursuer::new, MobCategory.MONSTER)
                    .sized(.7F, 2.28F).clientTrackingRange(10).build("void_pursuer"));''')
])
edit("registry/ModEntityAttributes.java",[
("import net.minecraft.world.entity.monster.Zombie;",
"import net.minecraft.world.entity.monster.Zombie;\nimport com.kirill.pocketrealms.entity.WallGrasper;\nimport com.kirill.pocketrealms.entity.VoidPursuer;"),
('                .add(Attributes.MOVEMENT_SPEED, 0.285D).build());',
 '''                .add(Attributes.MOVEMENT_SPEED, 0.285D).build());
        event.put(ModEntities.WALL_GRASPER.get(), WallGrasper.createAttributes().build());
        event.put(ModEntities.VOID_PURSUER.get(), VoidPursuer.createAttributes().build());''')])
edit("client/PocketClientEvents.java",[
('event.registerEntityRenderer(ModEntities.CORRUPTED_SKELETON.get(), CorruptedSkeletonRenderer::new);',
 '''event.registerEntityRenderer(ModEntities.CORRUPTED_SKELETON.get(), CorruptedSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.WALL_GRASPER.get(), WallGrasperRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_PURSUER.get(), VoidPursuerRenderer::new);'''),
('    @SubscribeEvent\n    public static void onGuiLayers', '''    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GraspingHandsModel.LAYER, GraspingHandsModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onGuiLayers''')])
edit("event/PocketServerEvents.java",[
('import com.kirill.pocketrealms.world.CollapseController;', 'import com.kirill.pocketrealms.world.CollapseController;\nimport com.kirill.pocketrealms.world.RiftThreatDirector;'),
('        CorruptionSpawner.tick(event.getServer());', '        CorruptionSpawner.tick(event.getServer());\n        RiftThreatDirector.tick(event.getServer());')])
edit("world/PocketRunEntry.java",[
('    public long nextShift;', '    public long nextShift;\n    public long nextPursuerAt;'),
('tag.putLong("NextShift", nextShift);','tag.putLong("NextShift", nextShift);\n        tag.putLong("NextPursuerAt", nextPursuerAt);'),
('entry.nextShift = tag.getLong("NextShift");','entry.nextShift = tag.getLong("NextShift");\n        entry.nextPursuerAt = tag.getLong("NextPursuerAt");')])
edit("world/PocketGenerator.java",[
('entry.nextShift = level.getGameTime() + 20 * 15;', 'entry.nextShift = level.getGameTime() + 20 * 15;\n        entry.nextPursuerAt = level.getGameTime() + 20 * 65;')])
for filename,changes in [
("en_us.json",{"entity.pocketrealms.wall_grasper":"Grasping Hands","entity.pocketrealms.void_pursuer":"The Pursuer","message.pocketrealms.pursuer":"Something has found your trail.","message.pocketrealms.grabbed":"The wall has you! Strike the hands to break free!"}),
("ru_ru.json",{"entity.pocketrealms.wall_grasper":"Руки из стены","entity.pocketrealms.void_pursuer":"Преследователь","message.pocketrealms.pursuer":"Кто-то вышел на твой след.","message.pocketrealms.grabbed":"Стена схватила тебя! Ударь по рукам, чтобы вырваться!"})]:
    file=root/"src/main/resources/assets/pocketrealms/lang"/filename
    payload=json.loads(file.read_text(encoding="utf8"));payload.update(changes)
    file.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
p=root/"gradle.properties";content=p.read_text(encoding="utf8")
if content.count("mod_version=0.4.1")!=1:raise SystemExit("Version property missing")
p.write_text(content.replace("mod_version=0.4.1","mod_version=0.5.0"),encoding="utf8")
print("Pocket Realms 0.5.0 gameplay, renders, textures and translation sources restored.")
