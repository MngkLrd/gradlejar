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
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
                            dev.minemods.traumacraft.disease.WaterIllness.clear(p);
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
        event.getDispatcher().register(Commands.literal("traumadeath").executes(ctx->dev.minemods.traumacraft.event.DeathReports.openLast(ctx.getSource().getPlayerOrException())));
        event.getDispatcher().register(Commands.literal("traumaphysiology").executes(ctx->{
            ServerPlayer p=ctx.getSource().getPlayerOrException();
            var snapshot=dev.minemods.traumacraft.api.TraumaApi.physiology(p);
            if(snapshot.isEmpty())return 0;
            var f=snapshot.get();
            p.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,"Oxygen %.0f%% | Perfusion %.0f%% | Delivery %.0f%% | Internal blood loss %.1f mL/s",f.oxygenFraction()*100,f.perfusion()*100,f.oxygenDelivery()*100,f.internalBleedingMlPerSecond())));
            for(var organ:f.organs())p.sendSystemMessage(Component.literal(String.format(java.util.Locale.ROOT,"%s: %.0f%% | internal loss %.1f mL/s",organ.id(),organ.integrity()*100,organ.internalBleedingMlPerSecond())));
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("traumaillness").executes(ctx->{
            ServerPlayer p=ctx.getSource().getPlayerOrException();
            p.sendSystemMessage(dev.minemods.traumacraft.disease.WaterIllness.diagnosis(dev.minemods.traumacraft.disease.WaterIllness.load(p)));
            return 1;
        }));
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
