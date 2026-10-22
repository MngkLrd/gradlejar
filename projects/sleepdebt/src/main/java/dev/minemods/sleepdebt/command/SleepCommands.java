package dev.minemods.sleepdebt.command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SleepCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  dispatcher.register(Commands.literal("sleepdebt")
   .then(Commands.literal("status").executes(ctx->{
    ServerPlayer p=ctx.getSource().getPlayerOrException();
    ctx.getSource().sendSuccess(()->Component.literal(
     "Sleep Debt "+SleepEngine.getDebt(p)+"/1000 | Fatigue "+SleepEngine.getFatigue(p)+"/1000 | Stress "+
     SleepEngine.getStress(p)+"/1000 | Caffeine "+SleepEngine.getCaffeine(p)+"/100"),false);
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
        })))))));
 }
 private SleepCommands(){}
}

