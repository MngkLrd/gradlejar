package dev.minemods.naturalbuoyancy;
import java.util.Set;

final class DensityRules {
    enum Kind { FLOAT, SLOW_SINK, SINK, VANILLA }
    private static final Set<String> LIGHT=Set.of("stick","feather","paper","book","writable_book","written_book","leather","rabbit_hide","string","bowl","charcoal","bamboo","sugar_cane","kelp","dried_kelp","apple","golden_apple","enchanted_golden_apple","carrot","potato","bread","melon_slice","pumpkin","melon","hay_block","sponge","lily_pad","vine","glow_berries","sweet_berries","wheat","wheat_seeds","beetroot_seeds","pumpkin_seeds","melon_seeds","cocoa_beans","snowball","fishing_rod","bow","crossbow","ice","packed_ice","blue_ice");
    private static final Set<String> HEAVY=Set.of("stone","cobblestone","deepslate","cobbled_deepslate","granite","diorite","andesite","tuff","calcite","obsidian","crying_obsidian","basalt","blackstone","netherrack","end_stone","bedrock","flint","diamond","emerald","lapis_lazuli","amethyst_shard","quartz","redstone","netherite_scrap","brick","nether_brick","anvil","chipped_anvil","damaged_anvil","chain","iron_bars","hopper","cauldron","water_bucket","lava_bucket","shears","flint_and_steel","rail","powered_rail","detector_rail","activator_rail","glass","sand","red_sand","gravel","clay","clay_ball","coal");
    private static final Set<String> SLOW=Set.of("dirt","coarse_dirt","rooted_dirt","mud","packed_mud","grass_block","moss_block","rotten_flesh","beef","porkchop","chicken","rabbit","mutton","cod","salmon","tropical_fish","pufferfish","bone","bone_meal","wet_sponge","bucket");
    private static boolean has(String s,String... tokens) {
        for(String t:tokens) if(s.equals(t)||s.startsWith(t+"_")||s.endsWith("_"+t)||s.contains("_"+t+"_")) return true;
        return false;
    }
    static Kind override(String tag) {
        return switch(tag) {
            case "naturalbuoyancy:floating" -> Kind.FLOAT;
            case "naturalbuoyancy:slow_sinking" -> Kind.SLOW_SINK;
            case "naturalbuoyancy:sinking" -> Kind.SINK;
            default -> Kind.VANILLA;
        };
    }
    static Kind materialTag(String tag) {
        if(!(tag.startsWith("minecraft:")||tag.startsWith("c:")||tag.startsWith("forge:"))) return Kind.VANILLA;
        String p=tag.substring(tag.indexOf(':')+1);
        if(p.equals("logs")||p.startsWith("logs/")||p.equals("planks")||p.equals("wool")||p.equals("wool_carpets")||p.equals("leaves")||p.equals("saplings")||p.startsWith("wooden_")||p.equals("wooden_rods")||p.equals("rods/wooden")) return Kind.FLOAT;
        for(String prefix:new String[]{"ores","ingots","nuggets","raw_materials","gems","stones","cobblestones"}) if(p.equals(prefix)||p.startsWith(prefix+"/")) return Kind.SINK;
        return Kind.VANILLA;
    }
    static Kind byPath(String p) {
        if(LIGHT.contains(p)) return Kind.FLOAT;
        if(HEAVY.contains(p)) return Kind.SINK;
        if(SLOW.contains(p)) return Kind.SLOW_SINK;
        if(has(p,"iron","gold","golden","copper","netherite","steel","tin","lead","silver","bronze","nickel","zinc","aluminum","aluminium","diamond","emerald","stone","cobblestone","deepslate","obsidian","basalt","blackstone","terracotta","concrete","ore","ingot","nugget","glass","brick","bricks","quartz","sandstone","prismarine","amethyst","lapis")) return Kind.SINK;
        if(has(p,"oak","spruce","birch","jungle","acacia","mangrove","cherry","bamboo","crimson","warped","pale_oak","wood","wooden","log","planks","wool","carpet","leaves","sapling","boat","raft","seeds","flower","banner","paper","feather","leather","cork")) return Kind.FLOAT;
        if(has(p,"dirt","mud","flesh","meat")) return Kind.SLOW_SINK;
        return Kind.VANILLA;
    }
}
