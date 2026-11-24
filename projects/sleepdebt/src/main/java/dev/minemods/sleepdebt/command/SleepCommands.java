package dev.minemods.sleepdebt.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.registry.ModEntities;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.SleepSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator testing presets change Sleep Debt's own state, not the whole world's TPS.
 * They take effect immediately and remain safe on dedicated servers.
 */
public final class SleepCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  dispatcher.register(Commands.literal("sleepdebt")
   .then(Commands.literal("status").executes(ctx->{
    ServerPlayer p=ctx.getSource().getPlayerOrException();
    ctx.getSource().sendSuccess(()->Component.literal(
     "Sleep Debt "+SleepEngine.getDebt(p)+"/1000 | Fatigue "+SleepEngine.getFatigue(p)+"/1000 | Stress "+
     SleepEngine.getStress(p)+"/1000 | Caffeine "+SleepEngine.getCaffeine(p)+"/100"
     +" | Sleep quality here "+SleepEngine.getCurrentQuality(p)+"%"),false);
    return 1;
   }))
   .then(Commands.literal("showcase").requires(source->source.hasPermission(2)).executes(ctx->{
    boolean good=ShowcaseBuilder.create(ctx.getSource().getPlayerOrException());
    ctx.getSource().sendSuccess(()->Component.literal(good?
      "Sleep Debt showcase placed: campsite, coffee maker and dreamcatcher.":
      "Cannot place the showcase: choose level ground with empty space 4 blocks away."),false);
    return good?1:0;
   }))
   .then(Commands.literal("debug").requires(source->source.hasPermission(2))
    .then(Commands.literal("set")
     .then(Commands.argument("debt",IntegerArgumentType.integer(0,1000))
      .then(Commands.argument("fatigue",IntegerArgumentType.integer(0,1000))
       .then(Commands.argument("stress",IntegerArgumentType.integer(0,1000))
        .executes(ctx->{
         ServerPlayer p=ctx.getSource().getPlayerOrException();
         SleepEngine.setNeeds(p,
          IntegerArgumentType.getInteger(ctx,"debt"),
          IntegerArgumentType.getInteger(ctx,"fatigue"),
          IntegerArgumentType.getInteger(ctx,"stress"));
         ctx.getSource().sendSuccess(()->Component.literal("Sleep Debt test values applied"),true);
         return 1;
        })))))
    .then(Commands.literal("preset")
     .then(Commands.literal("rested").executes(ctx->preset(ctx.getSource(),0,0,0,"rested")))
     .then(Commands.literal("tired").executes(ctx->preset(ctx.getSource(),450,350,200,"tired")))
     .then(Commands.literal("exhausted").executes(ctx->preset(ctx.getSource(),780,700,650,"exhausted")))
     .then(Commands.literal("critical").executes(ctx->preset(ctx.getSource(),960,900,950,"critical")))
    )
   ));
 }
 private static int preset(CommandSourceStack source,int debt,int fatigue,int stress,String name)
     throws com.mojang.brigadier.exceptions.CommandSyntaxException {
  ServerPlayer player=source.getPlayerOrException();
  SleepEngine.setNeeds(player,debt,fatigue,stress);
  source.sendSuccess(()->Component.literal("Sleep Debt preset '"+name+"': "
    +debt+" debt, "+fatigue+" fatigue, "+stress+" stress"),true);
  return 1;
 }
 private SleepCommands(){}
}
