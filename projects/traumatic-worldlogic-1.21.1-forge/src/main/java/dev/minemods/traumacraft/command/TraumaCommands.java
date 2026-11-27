package dev.minemods.traumacraft.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.event.TraumaEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class TraumaCommands {
    private TraumaCommands() {}
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("trauma")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> show(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            BodyStates.get(p).reset();
                            dev.minemods.traumacraft.network.ModNetwork.sync(p);
                            p.sendSystemMessage(Component.literal("Trauma state reset"));
                            return 1;
                        }));
        for (BodyPart part : BodyPart.values()) {
            root.then(Commands.literal("hit").requires(source -> source.hasPermission(2))
                    .then(Commands.literal(part.name().toLowerCase())
                            .then(Commands.argument("points", FloatArgumentType.floatArg(1, 440))
                                    .executes(ctx -> {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        float points = FloatArgumentType.getFloat(ctx, "points");
                                        BodyState state = BodyStates.get(p);
                                        state.damage(part, points);
                                        p.sendSystemMessage(Component.literal("Applied " + points + " HP to " + part.label));
                                        return show(p);
                                    }))));
            root.then(Commands.literal("bleed").requires(source -> source.hasPermission(2))
                    .then(Commands.literal(part.name().toLowerCase())
                            .then(Commands.argument("severity", IntegerArgumentType.integer(1, 2))
                                    .executes(ctx -> {
                                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                                        BodyStates.get(p).setBleeding(part, IntegerArgumentType.getInteger(ctx, "severity"));
                                        return show(p);
                                    }))));
            root.then(Commands.literal("fracture").requires(source -> source.hasPermission(2))
                    .then(Commands.literal(part.name().toLowerCase())
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                BodyStates.get(p).setFractured(part, true);
                                return show(p);
                            })));
        }
        event.getDispatcher().register(root);
    }
    private static int show(ServerPlayer player) {
        BodyState state = BodyStates.get(player);
        dev.minemods.traumacraft.network.ModNetwork.sync(player);
        player.sendSystemMessage(Component.literal("=== TraumaCraft: local HP ==="));
        for (BodyPart part : BodyPart.values()) {
            String flags = (state.bleeding(part) > 0 ? " [BLEED " + state.bleeding(part) + "]" : "")
                    + (state.fractured(part) ? " [FRACTURE]" : "")
                    + (state.health(part) <= 0 ? " [DESTROYED]" : "");
            player.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,
                    "%s: %.0f/%d%s", part.label, state.health(part), part.maximum, flags)));
        }
        player.sendSystemMessage(Component.literal("Bandage: stops bleeding | Splint: stabilizes fracture | Medkit: restores 25 local HP"));
        return 1;
    }
}
