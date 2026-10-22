"""Pocket Realms 0.6.1: gameplay-ready architectural blocks and deterministic assets.
Run AFTER unpack_pocketrealms_v060.py and fix_pocketrealms_v060_height.py.
No pip dependencies; resources are generated with Python's standard library.
"""
from pathlib import Path
import json
import random
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / "projects" / "pocketrealms"
JAVA = ROOT / "src/main/java/com/kirill/pocketrealms"
RES = ROOT / "src/main/resources/assets/pocketrealms"


def put(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_one(path, old, new):
    content = path.read_text(encoding="utf-8")
    found = content.count(old)
    if found != 1:
        raise SystemExit(f"Expected exactly one integration anchor in {path}: {old!r}; got {found}")
    put(path, content.replace(old, new, 1))


put(JAVA / "block/ArchitecturalBlock.java", """package com.kirill.pocketrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Fixed sculptural silhouettes; collision uses exactly the visible cuboids. */
public class ArchitecturalBlock extends Block {
    public static final VoxelShape BONE_WALL = Shapes.or(
            Block.box(0, 0, 5, 16, 3, 11),
            Block.box(0, 10, 6, 16, 13, 10),
            Block.box(1, 3, 6, 4, 16, 10),
            Block.box(12, 3, 6, 15, 16, 10));
    public static final VoxelShape RIFT_GRATE = Shapes.or(
            Block.box(7, 0, 0, 9, 16, 16),
            Block.box(0, 0, 7, 16, 16, 9));
    public static final VoxelShape BROKEN_PILLAR = Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14),
            Block.box(4, 3, 4, 12, 12, 12),
            Block.box(3, 12, 3, 11, 14, 11));
    private final VoxelShape outline;

    public ArchitecturalBlock(Properties properties, VoxelShape outline) {
        super(properties);
        this.outline = outline;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return outline;
    }
}
""")

put(JAVA / "block/CrumblingStoneBlock.java", """package com.kirill.pocketrealms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A delayed floor trap: only weight on the block starts the countdown. */
public final class CrumblingStoneBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public CrumblingStoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 10);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage < 3) {
            level.setBlock(pos, state.setValue(STAGE, stage + 1), 3);
            level.scheduleTick(pos, this, 10);
        } else {
            level.levelEvent(2001, pos, Block.getId(state));
            level.removeBlock(pos, false);
        }
    }
}
""")

blocks = JAVA / "registry/ModBlocks.java"
replace_one(blocks, "import com.kirill.pocketrealms.block.ReturnPortalBlock;",
"""import com.kirill.pocketrealms.block.ReturnPortalBlock;
import com.kirill.pocketrealms.block.ArchitecturalBlock;
import com.kirill.pocketrealms.block.CrumblingStoneBlock;
import net.minecraft.world.level.block.SlabBlock;""")

registration = """    public static final DeferredBlock<Block> CRUMBLING_STONE = BLOCKS.register("crumbling_stone",
            () -> new CrumblingStoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_TILES)
                    .strength(8.0F, 1200.0F)));
    public static final DeferredBlock<Block> RITUAL_SLAB = BLOCKS.register("ritual_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
                    .strength(10.0F, 1200.0F)));
    public static final DeferredBlock<Block> BONE_WALL = BLOCKS.register("bone_wall",
            () -> new ArchitecturalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK)
                    .strength(10.0F, 1200.0F).noOcclusion(), ArchitecturalBlock.BONE_WALL));
    public static final DeferredBlock<Block> RIFT_GRATE = BLOCKS.register("rift_grate",
            () -> new ArchitecturalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)
                    .strength(10.0F, 1200.0F).noOcclusion(), ArchitecturalBlock.RIFT_GRATE));
    public static final DeferredBlock<Block> CRACKED_VOID_BRICKS = BLOCKS.register("cracked_void_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
                    .strength(10.0F, 1200.0F)));
    public static final DeferredBlock<Block> BROKEN_PILLAR = BLOCKS.register("broken_pillar",
            () -> new ArchitecturalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE)
                    .strength(10.0F, 1200.0F).noOcclusion(), ArchitecturalBlock.BROKEN_PILLAR));
"""
replace_one(blocks,
    "    public static final DeferredBlock<Block> CORRUPTION_NEST = BLOCKS.register(",
    registration + "    public static final DeferredBlock<Block> CORRUPTION_NEST = BLOCKS.register(")

items = JAVA / "registry/ModItems.java"
itemreg = "\n".join(
    '    public static final DeferredItem<net.minecraft.world.item.BlockItem> ' + name.upper() +
    ' = ITEMS.registerSimpleBlockItem("' + name + '", ModBlocks.' + name.upper() + ');'
    for name in ("crumbling_stone", "ritual_slab", "bone_wall", "rift_grate",
                 "cracked_void_bricks", "broken_pillar")
)
source = items.read_text(encoding="utf-8")
assert source.rstrip().endswith("}")
put(items, source.rstrip()[:-1] + "\n" + itemreg + "\n}\n")

generator = JAVA / "world/PocketGenerator.java"
replace_one(generator,
    "        weatherExitNeighborhood(level, open, centerX, centerZ, exitCell, random);",
    "        decorate061(level, open, rooms, centerX, centerZ, random, startCell, exitCell);\n"
    "        weatherExitNeighborhood(level, open, centerX, centerZ, exitCell, random);")
replace_one(generator,
    "        if (roll < 0.48F) return ModBlocks.VOID_BRICKS.get().defaultBlockState();",
    "        if (roll < 0.27F) return ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState();\n"
    "        if (roll < 0.48F) return ModBlocks.VOID_BRICKS.get().defaultBlockState();")

architecture = """
    /**
     * Dungeon architecture v0.6.1.
     * All props are placed only in dead ends, and NEVER inside an enclosed room,
     * the guaranteed start, or the final portal's 5x5 clearance. Full passage
     * connectivity is preserved; traps are optional danger, not forced deaths.
     */
    private static void decorate061(ServerLevel level, boolean[][] open, PocketRooms rooms,
                                    int cx, int cz, RandomSource random,
                                    BlockPos spawn, BlockPos exit) {
        for (int gx = 2; gx < GRID - 2; gx++) {
            for (int gz = 2; gz < GRID - 2; gz++) {
                if (!open[gx][gz] || rooms.inRoom(gx, gz) || rooms.nearRoom(gx, gz)) continue;
                int neighbors = (open[gx-1][gz] ? 1 : 0) + (open[gx+1][gz] ? 1 : 0)
                        + (open[gx][gz-1] ? 1 : 0) + (open[gx][gz+1] ? 1 : 0);
                if (neighbors != 1) continue;
                BlockPos pos = gridToWorld(cx, cz, gx, gz);
                if (pos.distSqr(spawn) < 64 || pos.distSqr(exit) < 100) continue;
                float pick = random.nextFloat();
                if (pick < 0.17F) {
                    level.setBlock(pos, ModBlocks.CRUMBLING_STONE.get().defaultBlockState(), 2);
                } else if (pick < 0.29F) {
                    level.setBlock(pos.above(), ModBlocks.RITUAL_SLAB.get().defaultBlockState(), 2);
                } else if (pick < 0.41F) {
                    level.setBlock(pos.above(), ModBlocks.BONE_WALL.get().defaultBlockState(), 2);
                } else if (pick < 0.51F) {
                    level.setBlock(pos.above(), ModBlocks.RIFT_GRATE.get().defaultBlockState(), 2);
                } else if (pick < 0.65F) {
                    level.setBlock(pos.above(), ModBlocks.BROKEN_PILLAR.get().defaultBlockState(), 2);
                }
            }
        }
    }

"""
replace_one(generator,
    "    private static BlockState floorBlock(int type, RandomSource random) {",
    architecture + "    private static BlockState floorBlock(int type, RandomSource random) {")

properties = ROOT / "gradle.properties"
replace_one(properties, "mod_version=0.6.0", "mod_version=0.6.1")


# Standalone 32x32 nearest-neighbour pixel art with crisp, non-AA clusters.
def png_bytes(pixels):
    h = len(pixels)
    w = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes((*c, 255)) for c in row) for row in pixels)
    def chunk(name, payload):
        return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload) & 0xffffffff)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


PALETTES = {
    "crumbling_stone": [(48, 50, 54), (58, 59, 64), (69, 68, 72), (78, 76, 79)],
    "ritual_slab": [(44, 48, 54), (52, 58, 65), (63, 65, 73), (79, 76, 81)],
    "bone_wall": [(80, 74, 67), (105, 95, 82), (126, 114, 96), (150, 133, 110)],
    "rift_grate": [(34, 38, 45), (49, 54, 61), (67, 70, 77), (87, 88, 94)],
    "cracked_void_bricks": [(35, 33, 43), (44, 42, 51), (58, 53, 62), (71, 64, 75)],
    "broken_pillar": [(42, 41, 49), (55, 53, 61), (68, 65, 71), (82, 77, 81)],
}


def build_texture(name, stage=0):
    seed = sum((i + 1) * ord(c) for i, c in enumerate(name)) + stage * 137
    rnd = random.Random(seed)
    colors = PALETTES[name]
    pix = [[colors[1] for _ in range(32)] for _ in range(32)]
    for ty in range(0, 32, 2):
        for tx in range(0, 32, 2):
            c = colors[rnd.choices(range(4), weights=[7, 33, 21, 4])[0]]
            for dy in range(2):
                for dx in range(2):
                    pix[ty+dy][tx+dx] = c

    def draw(x1, y1, x2, y2, color):
        for yy in range(max(0,y1), min(32,y2)):
            for xx in range(max(0,x1), min(32,x2)):
                pix[yy][xx] = color

    if name in ("cracked_void_bricks", "crumbling_stone"):
        for y in (0, 16, 31):
            draw(0, y, 32, y + 1, colors[0])
        for y, offsets in ((0, (12, 27)), (16, (6, 21))):
            for x in offsets:
                draw(x, y, x+1, min(32, y+16), colors[0])
                draw(x+1, y+1, x+2, min(32,y+14), colors[2])
    if name == "ritual_slab":
        for inset in (3, 7, 12):
            draw(inset, inset, 32-inset, inset+1, colors[3])
            draw(inset, 31-inset, 32-inset, 32-inset, colors[0])
        draw(14, 9, 18, 23, colors[2])
        draw(9, 14, 23, 18, colors[2])
        draw(14, 14, 18, 18, colors[0])
    if name == "bone_wall":
        for x in (3, 14, 25):
            draw(x, 2, x+3, 30, colors[3])
            draw(x+1, 4, x+2, 28, colors[2])
        for y in (4, 25):
            draw(0, y, 32, y+3, colors[2])
    if name == "rift_grate":
        for x in (3, 11, 19, 27):
            draw(x, 0, x+3, 32, colors[2])
            draw(x+2, 0, x+3, 32, colors[0])
        for y in (6, 24):
            draw(0, y, 32, y+3, colors[3])
            draw(0, y+2, 32, y+3, colors[0])
    if name == "broken_pillar":
        for x in (3, 8, 22, 27):
            draw(x, 0, x+2, 32, colors[0])
        draw(4, 3, 28, 6, colors[3])
        draw(3, 24, 29, 27, colors[0])
        draw(22, 0, 32, 11, colors[0])
    if name in ("cracked_void_bricks", "crumbling_stone"):
        cracks = 2 + stage * 4 if name == "crumbling_stone" else 8
        for _ in range(cracks):
            x = rnd.randrange(3, 28)
            y = rnd.randrange(3, 27)
            for step in range(rnd.randrange(2, 6)):
                if 0 <= x < 32 and 0 <= y < 32:
                    pix[y][x] = colors[0]
                x += rnd.choice((-1, 0, 1))
                y += 1
    return png_bytes(pix)


def save_json(relative, data):
    put(RES / relative, json.dumps(data, ensure_ascii=False, indent=2) + "\n")


def block_texture(name, stage=None):
    suffix = name if stage is None else f"{name}_{stage}"
    out = RES / f"textures/block/{suffix}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(build_texture(name, 0 if stage is None else stage))


def cube(name):
    save_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"pocketrealms:block/{name}"}}})
    save_json(f"models/block/{name}.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"pocketrealms:block/{name}"}
    })
    save_json(f"models/item/{name}.json", {"parent": f"pocketrealms:block/{name}"})
    block_texture(name)


cube("cracked_void_bricks")
for stage in range(4):
    block_texture("crumbling_stone", stage)
    save_json(f"models/block/crumbling_stone_{stage}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"pocketrealms:block/crumbling_stone_{stage}"}
    })
save_json("blockstates/crumbling_stone.json", {"variants": {
    f"stage={i}": {"model": f"pocketrealms:block/crumbling_stone_{i}"} for i in range(4)
}})
save_json("models/item/crumbling_stone.json", {"parent": "pocketrealms:block/crumbling_stone_0"})

block_texture("ritual_slab")
save_json("blockstates/ritual_slab.json", {"variants": {
    "type=bottom": {"model": "pocketrealms:block/ritual_slab"},
    "type=top": {"model": "pocketrealms:block/ritual_slab_top"},
    "type=double": {"model": "pocketrealms:block/ritual_slab_double"}
}})
for suffix, parent in (("", "slab"), ("_top", "slab_top"), ("_double", "cube_all")):
    save_json(f"models/block/ritual_slab{suffix}.json", {
        "parent": f"minecraft:block/{parent}",
        "textures": {"bottom": "pocketrealms:block/ritual_slab",
                     "top": "pocketrealms:block/ritual_slab",
                     "side": "pocketrealms:block/ritual_slab",
                     "all": "pocketrealms:block/ritual_slab"}
    })
save_json("models/item/ritual_slab.json", {"parent": "pocketrealms:block/ritual_slab"})


SHAPES = {
    "bone_wall": [(0,0,5,16,3,11), (0,10,6,16,13,10), (1,3,6,4,16,10), (12,3,6,15,16,10)],
    "rift_grate": [(7,0,0,9,16,16), (0,0,7,16,16,9)],
    "broken_pillar": [(2,0,2,14,3,14), (4,3,4,12,12,12), (3,12,3,11,14,11)]
}
for name, boxes in SHAPES.items():
    block_texture(name)
    save_json(f"blockstates/{name}.json", {"variants": {"": {"model": f"pocketrealms:block/{name}"}}})
    save_json(f"models/block/{name}.json", {
        "ambientocclusion": False,
        "textures": {"material": f"pocketrealms:block/{name}", "particle": f"pocketrealms:block/{name}"},
        "elements": [
            {"from": list(box[:3]), "to": list(box[3:]),
             "faces": {face: {"texture": "#material"} for face in
                       ("north", "south", "east", "west", "up", "down")}}
            for box in boxes
        ]
    })
    save_json(f"models/item/{name}.json", {"parent": f"pocketrealms:block/{name}"})

for locale, translations in {
    "en_us": ["Crumbling Stone", "Ritual Slab", "Bone Wall", "Rift Grate", "Cracked Void Bricks", "Broken Pillar"],
    "ru_ru": ["Осыпающийся камень", "Ритуальная плита", "Костяная стена", "Решётка разлома",
              "Треснувшие кирпичи Бездны", "Разрушенная колонна"]
}.items():
    lang = RES / "lang" / (locale + ".json")
    data = json.loads(lang.read_text(encoding="utf-8"))
    for key, value in zip(PALETTES.keys(), translations):
        data[f"block.pocketrealms.{key}"] = value
    # JSON insertion order is not relied on; use explicit item / block names.
    keys = ("crumbling_stone", "ritual_slab", "bone_wall", "rift_grate",
            "cracked_void_bricks", "broken_pillar")
    for key, value in zip(keys, translations):
        data[f"block.pocketrealms.{key}"] = value
    put(lang, json.dumps(data, ensure_ascii=False, indent=2) + "\n")

readme = ROOT / "README.md"
with readme.open("a", encoding="utf-8") as fh:
    fh.write("""
\n## Pocket Realms 0.6.1 - Structural Horror
- Six registered world blocks with block items and dedicated 32x32 pixel textures.
- Crumbling Stone cracks over 40 ticks after a living creature steps on it, then collapses.
- Ritual Slab uses vanilla slab states, top/bottom/double.
- Bone Wall, Rift Grate, and Broken Pillar have collision outlines matching their custom 3D meshes.
- Procedural decoration only affects noncritical dead-end cells; no exit route can be sealed.
- Cracked Void Bricks replace part of generic maze masonry.
- All mechanics are server-side. No new mandatory runtime library.
- Future: authored room-theme placement and custom mesh texture import.
""")
print("Pocket Realms 0.6.1: six blocks, interactive trap, procedural dungeon placement and resources prepared.")
