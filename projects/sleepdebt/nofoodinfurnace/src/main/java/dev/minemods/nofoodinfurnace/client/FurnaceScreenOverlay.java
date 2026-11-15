package dev.minemods.nofoodinfurnace.client;

import dev.minemods.nofoodinfurnace.NoFoodInFurnace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Draws the same red error cross that the vanilla anvil screen uses.
 * Client side only. The server-side furnace mixin remains authoritative.
 */
@EventBusSubscriber(modid = NoFoodInFurnace.MOD_ID, value = Dist.CLIENT)
public final class FurnaceScreenOverlay {
    private static final ResourceLocation ERROR_SPRITE =
            ResourceLocation.withDefaultNamespace("container/anvil/error");

    private FurnaceScreenOverlay() {}

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof FurnaceScreen screen)) {
            return;
        }

        ItemStack input = screen.getMenu().getSlot(0).getItem();
        if (input.isEmpty()) {
            return;
        }

        boolean forbidden = input.has(DataComponents.FOOD);
        if (!forbidden) {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var recipe = level.getRecipeManager().getRecipeFor(
                    RecipeType.SMELTING, new SingleRecipeInput(input), level);
            if (recipe.isPresent()) {
                var cooking = recipe.get().value();
                forbidden = cooking.category() == CookingBookCategory.FOOD
                        || cooking.getResultItem(level.registryAccess()).has(DataComponents.FOOD);
            }
        }

        if (forbidden) {
            // Vanilla furnace arrow occupies approximately X 79..103, Y 35..52.
            // Place the anvil's 28x21 sprite over it, without changing slot rendering.
            event.getGuiGraphics().blitSprite(
                    ERROR_SPRITE, screen.getGuiLeft() + 77, screen.getGuiTop() + 33, 28, 21);
        }
    }
}
