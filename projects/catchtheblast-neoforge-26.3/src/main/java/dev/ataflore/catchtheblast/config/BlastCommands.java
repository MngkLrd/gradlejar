package dev.ataflore.catchtheblast.config;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class BlastCommands {
    private BlastCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("catchtheblast")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("terrain")
                .executes(ctx -> report(ctx.getSource(), false))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(ctx -> {
                        BlastConfig.TERRAIN_DAMAGE.set(BoolArgumentType.getBool(ctx, "enabled"));
                        BlastConfig.SPEC.save();
                        return report(ctx.getSource(), true);
                    }))));
    }
    private static int report(CommandSourceStack source, boolean broadcast) {
        boolean enabled = BlastConfig.TERRAIN_DAMAGE.get();
        source.sendSuccess(() -> Component.translatable(enabled ? "command.catchtheblast.terrain.enabled" : "command.catchtheblast.terrain.disabled"), broadcast);
        return enabled ? 1 : 0;
    }
}
