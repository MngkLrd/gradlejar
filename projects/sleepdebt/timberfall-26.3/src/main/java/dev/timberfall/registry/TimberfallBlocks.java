package dev.timberfall.registry;
import dev.timberfall.Timberfall;
import dev.timberfall.block.NotchedLogBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;
public final class TimberfallBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(Timberfall.MOD_ID);
    private static final Map<String,DeferredBlock<NotchedLogBlock>> NOTCHES = new LinkedHashMap<>();
    static {
        for (String wood : new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","poplar","crimson","warped"}) {
            var holder = REGISTRY.registerBlock("notched_"+wood+"_log",NotchedLogBlock::new,
                props -> props.strength(2.0f).sound(SoundType.WOOD).noOcclusion());
            NOTCHES.put(wood,holder);
        }
    }
    public static Block notch(String wood) {
        var b=NOTCHES.get(wood);
        return b==null?null:b.get();
    }
    public static String woodType(Block block) {
        for (var e:NOTCHES.entrySet()) if (e.getValue().get()==block) return e.getKey();
        return null;
    }
}
