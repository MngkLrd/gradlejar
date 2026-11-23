package dev.minemods.riverweir.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.minemods.riverweir.RiverWeirMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Checks what the live TFG server actually loaded (JEI / EMI index loaded recipes). */
@Mod.EventBusSubscriber(modid = RiverWeirMod.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeirCommands {
    private static final String[] IDS = {
        "net_twine", "net_roll", "weir_post", "bronze_net_roll", "iron_net_roll"
    };
    private WeirCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("riverweir")
            .then(Commands.literal("recipes").executes(ctx -> {
                MinecraftServer server = ctx.getSource().getServer();
                int loaded = 0;
                for (String recipe : IDS) {
                    ResourceLocation id = new ResourceLocation(RiverWeirMod.ID, recipe);
                    boolean present = server.getRecipeManager().byKey(id).isPresent();
                    if (present) loaded++;
                    Component message = Component.literal("[River Weir] " + id + ": " +
                        (present ? "LOADED" : "NOT LOADED (check TFG datapacks/recipes)"));
                    ctx.getSource().sendSuccess(() -> message, false);
                }
                return loaded;
            })));
    }
}
