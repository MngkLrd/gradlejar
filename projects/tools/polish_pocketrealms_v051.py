"""Finish Pocket Realms 0.5.1 atmosphere and use vanilla End Portal rendering."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/"projects/pocketrealms"
java=root/"src/main/java/com/kirill/pocketrealms"
assets=root/"src/main/resources/assets/pocketrealms"
def put(path,text):
    p=java/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf8")
def replace(path,before,after):
    p=java/path;s=p.read_text(encoding="utf8")
    if s.count(before)!=1:raise RuntimeError(f"{path} unexpected source: {before[:75]}")
    p.write_text(s.replace(before,after,1),encoding="utf8")

put("registry/ModBlockEntities.java",'''package com.kirill.pocketrealms.registry;
import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.blockentity.PocketDoorPortalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public final class ModBlockEntities {
    private ModBlockEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PocketRealms.MOD_ID);
    public static final Supplier<BlockEntityType<PocketDoorPortalBlockEntity>> DOOR_PORTAL =
        BLOCK_ENTITIES.register("door_portal", () -> BlockEntityType.Builder.of(
            PocketDoorPortalBlockEntity::new, ModBlocks.POCKET_DOOR.get()).build(null));
}
''')
put("blockentity/PocketDoorPortalBlockEntity.java",'''package com.kirill.pocketrealms.blockentity;
import com.kirill.pocketrealms.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class PocketDoorPortalBlockEntity extends BlockEntity {
    public PocketDoorPortalBlockEntity(BlockPos pos,BlockState state){
        super(ModBlockEntities.DOOR_PORTAL.get(),pos,state);
    }
}
''')
put("client/PocketDoorPortalRenderer.java",'''package com.kirill.pocketrealms.client;
import com.kirill.pocketrealms.blockentity.PocketDoorPortalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.joml.Matrix4f;
/** Real vanilla End Portal shader, aligned with the CLOSED door's plane, not the swung leaf. */
public class PocketDoorPortalRenderer implements BlockEntityRenderer<PocketDoorPortalBlockEntity> {
    public PocketDoorPortalRenderer(BlockEntityRendererProvider.Context ctx){}
    @Override public void render(PocketDoorPortalBlockEntity entity,float tick,PoseStack pose,
        MultiBufferSource source,int light,int overlay) {
        var state=entity.getBlockState();
        if(!state.getValue(DoorBlock.OPEN)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER)return;
        Direction facing=state.getValue(DoorBlock.FACING);
        VertexConsumer v=source.getBuffer(RenderType.endPortal());
        Matrix4f m=pose.last().pose();
        float a=.06f,b=.94f,y0=.02f,y1=1.98f;
        if(facing==Direction.EAST||facing==Direction.WEST){
            float x=facing==Direction.EAST?.24f:.76f;
            quad(v,m,x,y0,a,x,y0,b,x,y1,b,x,y1,a);
            quad(v,m,x,y0,b,x,y0,a,x,y1,a,x,y1,b);
        }else{
            float z=facing==Direction.SOUTH?.24f:.76f;
            quad(v,m,a,y0,z,b,y0,z,b,y1,z,a,y1,z);
            quad(v,m,b,y0,z,a,y0,z,a,y1,z,b,y1,z);
        }
    }
    private static void quad(VertexConsumer v,Matrix4f m,
        float x0,float y0,float z0,float x1,float y1,float z1,
        float x2,float y2,float z2,float x3,float y3,float z3){
        v.addVertex(m,x0,y0,z0);v.addVertex(m,x1,y1,z1);
        v.addVertex(m,x2,y2,z2);v.addVertex(m,x3,y3,z3);
    }
}
''')
replace("PocketRealms.java","import com.kirill.pocketrealms.registry.ModSounds;",
    "import com.kirill.pocketrealms.registry.ModSounds;\nimport com.kirill.pocketrealms.registry.ModBlockEntities;")
replace("PocketRealms.java","        ModBlocks.BLOCKS.register(modEventBus);",
    "        ModBlocks.BLOCKS.register(modEventBus);\n        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);")
replace("block/PocketDoorBlock.java","import com.kirill.pocketrealms.world.PocketTeleporter;",
    """import com.kirill.pocketrealms.world.PocketTeleporter;
import com.kirill.pocketrealms.blockentity.PocketDoorPortalBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;""")
replace("block/PocketDoorBlock.java","extends DoorBlock {","extends DoorBlock implements EntityBlock {")
replace("block/PocketDoorBlock.java","    private InteractionResult open(",
    """    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?new PocketDoorPortalBlockEntity(pos,state):null;
    }
    private InteractionResult open(""")
replace("block/PocketDoorBlock.java","if (!level.isClientSide()) setOpen(player, level, state, pos, true);",
    """if (!level.isClientSide()) {
                BlockPos bottom=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
                var dir=state.getValue(FACING);
                double dx=player.getX()-bottom.getX()-.5, dz=player.getZ()-bottom.getZ()-.5;
                boolean inside=dx*dir.getStepX()+dz*dir.getStepZ()>0;
                var hinge=inside?DoorHingeSide.RIGHT:DoorHingeSide.LEFT;
                if(state.getValue(HINGE)!=hinge){
                    level.setBlock(bottom,level.getBlockState(bottom).setValue(HINGE,hinge),2);
                    level.setBlock(bottom.above(),level.getBlockState(bottom.above()).setValue(HINGE,hinge),2);
                    state=level.getBlockState(pos);
                }
                setOpen(player,level,state,pos,true);
            }""")
replace("client/PocketClientEvents.java","import com.kirill.pocketrealms.registry.ModEntities;",
    "import com.kirill.pocketrealms.registry.ModEntities;\nimport com.kirill.pocketrealms.registry.ModBlockEntities;")
replace("client/PocketClientEvents.java",
    "        event.registerEntityRenderer(ModEntities.VOID_PURSUER.get(), VoidPursuerRenderer::new);",
    """        event.registerEntityRenderer(ModEntities.VOID_PURSUER.get(), VoidPursuerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DOOR_PORTAL.get(),PocketDoorPortalRenderer::new);""")
replace("client/PocketClientEvents.java",
    "float pulse = (float) (0.32D + 0.10D * Math.sin(System.nanoTime() / 600_000_000.0D));",
    """double t=System.nanoTime()/1_000_000_000.0D;
                    float pulse=(float)(0.50D+0.125D*Math.sin(t*1.3D)+0.035D*Math.sin(t*3.1D));""")
replace("world/PocketGenerator.java","        markCriticalRoute(level, entry, open, distances, centerX, centerZ);",
    "        // No guide lamps: the maze does not reveal its route.")
replace("world/PocketGenerator.java","if (random.nextFloat() < 0.08F)","if (random.nextFloat() < 0.035F)")
replace("world/PocketGenerator.java","if (random.nextFloat() < 0.05F)","if (random.nextFloat() < 0.023F)")
replace("world/PocketGenerator.java","        entry.nextPursuerAt = level.getGameTime() + 20 * 65;",
    "        entry.nextPursuerAt = level.getGameTime() + 20 * 65;\n        entry.nextDarknessAt=level.getGameTime()+20L*(30+random.nextInt(26));")
replace("world/PocketRunEntry.java","    public long nextPursuerAt;",
    "    public long nextPursuerAt;\n    public long nextDarknessAt;")
replace("world/PocketRunEntry.java",'        tag.putLong("NextPursuerAt", nextPursuerAt);',
    '        tag.putLong("NextPursuerAt", nextPursuerAt);\n        tag.putLong("NextDarknessAt",nextDarknessAt);')
replace("world/PocketRunEntry.java",'        entry.nextPursuerAt = tag.getLong("NextPursuerAt");',
    '        entry.nextPursuerAt = tag.getLong("NextPursuerAt");\n        entry.nextDarknessAt=tag.getLong("NextDarknessAt");')
put("world/PocketDarknessDirector.java",'''package com.kirill.pocketrealms.world;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
/** Infrequent Warden-style pulses; disabled in the final reward chamber. */
public final class PocketDarknessDirector {
    private PocketDarknessDirector(){}
    public static void tick(MinecraftServer server) {
        var realm=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if(realm==null||realm.getGameTime()%20!=0)return;
        var data=PocketRunData.get(server);
        long now=realm.getGameTime();
        for(var run:data.allRuns()){
            if(!run.generated||run.completed||run.retiring||run.nextDarknessAt<=0||now<run.nextDarknessAt)continue;
            var player=server.getPlayerList().getPlayer(run.owner);
            if(player==null||player.serverLevel()!=realm||!player.isAlive()||!run.contains(player.blockPosition()))continue;
            if(player.distanceToSqr(run.exitPos.getX()+.5,run.exitPos.getY()+1,run.exitPos.getZ()+.5)>90)
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,72,0,true,false,false));
            run.nextDarknessAt=now+20L*(34+realm.random.nextInt(29));
            data.setDirty();
        }
    }
}
''')
replace("event/PocketServerEvents.java","import com.kirill.pocketrealms.world.PocketAtmosphere;",
    "import com.kirill.pocketrealms.world.PocketAtmosphere;\nimport com.kirill.pocketrealms.world.PocketDarknessDirector;")
replace("event/PocketServerEvents.java","        PocketAtmosphere.tick(event.getServer());",
    "        PocketAtmosphere.tick(event.getServer());\n        PocketDarknessDirector.tick(event.getServer());")
s=(java/"event/PocketServerEvents.java").read_text(encoding="utf8")
first=s.index("                    data.findRunAt(player.blockPosition()).ifPresent(run -> {")
last=s.index("                    // Falling below the maze",first)
s=s[:first]+"                    // No automatic exit directions or objective hints.\n"+s[last:]
(java/"event/PocketServerEvents.java").write_text(s,encoding="utf8")
replace("world/PocketTeleporter.java",
    '        player.displayClientMessage(Component.translatable("message.pocketrealms.exit_hint"), true);',
    '        // No explicit wayfinding instructions: the last chamber is discovered organically.')
block=assets/"blockstates/pocket_door.json"
o=json.loads(block.read_text(encoding="utf8"))
o["multipart"]=[x for x in o["multipart"] if "portal_veil" not in x.get("apply",{}).get("model","")]
block.write_text(json.dumps(o,indent=2)+"\n",encoding="utf8")
for lang,desc in [("en_us","The exit is somewhere beyond the darkness."),("ru_ru","Выход где-то по ту сторону тьмы.")]:
    file=assets/"lang"/(lang+".json");s=json.loads(file.read_text(encoding="utf8"))
    s["message.pocketrealms.exit_hint"]=desc
    file.write_text(json.dumps(s,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
(root/"README.md").open("a",encoding="utf8").write(
    "\n## 0.5.1 atmosphere revision\nRemoved guide lamps and repeated exit hints. "
    "Lower light frequency, stronger breathing vignette, rare short Darkness effects. "
    "Door portal uses vanilla RenderType.endPortal via client-only block entity renderer, "
    "parallel to the shut door rather than attached to its swung leaf.\n")
# Low-pitched, spatial, layered voice remains frightening even without an audio encoder.
snd=java/"world/PocketSoundDirector.java"
voice=snd.read_text(encoding="utf8")
for a,b in [("SoundSource.HOSTILE, 1.1F, 1.0F","SoundSource.HOSTILE, 1.1F, .78F"),
            ("SoundSource.HOSTILE, .9F, 1.0F","SoundSource.HOSTILE, .9F, .72F")]:
    if voice.count(a)!=1:raise RuntimeError("Pursuer sound pitch patch needs original "+a)
    voice=voice.replace(a,b,1)
snd.write_text(voice,encoding="utf8")
print("Pocket Realms visual and terror polish applied.")
