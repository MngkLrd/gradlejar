package dev.minemods.worldlogicterrain.mobility;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class TerrainMobility {
    private static final ResourceLocation SPEED=ResourceLocation.fromNamespaceAndPath("worldlogicterrain","surface_speed");
    private record Sample(ServerLevel level,long time,boolean wet) {}
    private static final Map<ServerPlayer,Sample> ENV=new WeakHashMap<>();
    private TerrainMobility() {}
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.isCreative() || p.isSpectator() || p.getAbilities().flying || p.isPassenger() || p.isInWater() || p.isInLava() || p.isFallFlying() || p.onClimbable()) {
            apply(p,1); ENV.remove(p); return;
        }
        // Keep the last ground modifier through ordinary jumps, rather than rewarding bunny hopping.
        if (!p.onGround()) return;
        ServerLevel level=p.serverLevel();
        BlockPos feet=p.blockPosition(), floor=p.getOnPos();
        if (!level.hasChunkAt(feet)||!level.hasChunkAt(floor)) { apply(p,1); return; }
        Sample old=ENV.get(p);
        long now=level.getGameTime();
        if (old==null||old.level()!=level||now-old.time()>=20||now<old.time()) {
            old=new Sample(level,now,WorldLogicApi.environment(level,feet).map(WorldLogicApi.Environment::rainingAt).orElse(false));
            ENV.put(p,old);
        }
        BlockState ground=level.getBlockState(floor), plant=level.getBlockState(feet);
        apply(p,factor(ground,plant,old.wet()));
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p){apply(p,1);ENV.remove(p);}
    }
    private static void apply(ServerPlayer p,double factor) {
        var attr=p.getAttribute(Attributes.MOVEMENT_SPEED);
        if(attr==null)return;
        var old=attr.getModifier(SPEED);
        double amount=factor-1;
        if(old!=null&&Math.abs(old.amount()-amount)<.0001)return;
        if(old!=null)attr.removeModifier(SPEED);
        if(Math.abs(amount)>.0001)attr.addTransientModifier(new AttributeModifier(SPEED,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    static double factor(BlockState floor,BlockState feet,boolean wet) {
        double base=surface(floor), vegetation=vegetation(feet);
        if(feet.is(Blocks.SNOW))base=snow(feet.getValue(SnowLayerBlock.LAYERS));
        if(wet&&floor.is(Blocks.FARMLAND))base*=.95;
        double strength=WorldLogicApi.ruleStrength("movement");
        return Math.max(.45,Math.min(1.20,1+(base*vegetation-1)*strength));
    }
    private static double snow(int layers){return layers<=2?.90:Math.max(.55,.90-.055*(layers-2));}
    private static double surface(BlockState b) {
        if(b.is(Blocks.DIRT_PATH))return 1.05;
        if(b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.DIRT)||b.is(Blocks.COARSE_DIRT)||b.is(Blocks.ROOTED_DIRT))return 1;
        if(b.is(Blocks.STONE_BRICKS)||b.is(Blocks.POLISHED_ANDESITE)||b.is(Blocks.POLISHED_GRANITE)||b.is(Blocks.POLISHED_DIORITE))return 1.10;
        if(b.is(Blocks.GRAVEL)||b.is(Blocks.CLAY))return .90;
        if(b.is(Blocks.FARMLAND))return .85;
        if(b.is(BlockTags.SAND))return .80;
        if(b.is(Blocks.MUD))return .75;
        if(b.is(Blocks.SNOW_BLOCK))return .70;
        if(b.is(Blocks.SNOW))return snow(b.getValue(SnowLayerBlock.LAYERS));
        if(b.is(BlockTags.ICE))return .85;
        if(b.is(Blocks.MOSS_BLOCK)||b.is(Blocks.PODZOL))return .95;
        // Core material family is the shared source for modded soft and hard material categories.
        // Unknown material stays neutral; no name guessing.
        return switch(WorldLogicApi.materialFamily(b)) {
            case "soft_landing" -> .95;
            case "wood","ricochet_stone","ricochet_metal","glass" -> 1;
            default -> 1;
        };
    }
    private static double vegetation(BlockState b) {
        if(b.is(Blocks.SHORT_GRASS))return .97;
        if(b.is(Blocks.TALL_GRASS))return .88;
        if(b.is(Blocks.FERN))return .95;
        if(b.is(Blocks.LARGE_FERN))return .85;
        if(b.is(BlockTags.FLOWERS))return .98;
        var id=BuiltInRegistries.BLOCK.getKey(b.getBlock());
        if(!id.getNamespace().equals("worldlogicterrain"))return 1;
        return switch(id.getPath()) {
            case "meadow_grass","dry_grass","forest_litter" -> .97;
            case "fallen_leaves","wild_chamomile","wild_cornflower","wild_clover" -> .98;
            case "low_fern" -> .95;
            case "dense_meadow_grass","tall_dry_grass","wild_heather","dwarf_shrub","desert_shrub" -> .90;
            case "sedge" -> .85;
            case "forest_shrub","cattail" -> .80;
            case "dense_underbrush","reed" -> .75;
            default -> 1;
        };
    }
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("inspect").then(Commands.argument("feet",BlockPosArgument.blockPos()).executes(c->{
            ServerLevel l=c.getSource().getLevel();
            BlockPos pos=BlockPosArgument.getLoadedBlockPos(c,"feet");
            var env=WorldLogicApi.environment(l,pos);
            if(env.isEmpty())return 0;
            double f=factor(l.getBlockState(pos.below()),l.getBlockState(pos),env.get().rainingAt());
            c.getSource().sendSuccess(()->Component.literal(String.format(Locale.ROOT,"Terrain factor=%.3f API=%d",f,WorldLogicApi.API_VERSION)),false);
            return 1;
          }))));
    }
}
