# No Food in Furnace

Minecraft 1.21.1 / NeoForge 21.1.x / Java 21.

Stops all food cooking in the **ordinary vanilla furnace**. Food is detected automatically for vanilla and modded recipes by checking:

- the recipe's `CookingBookCategory.FOOD`,
- the actual input stack's `DataComponents.FOOD`,
- the result stack's `DataComponents.FOOD`.

Ore smelting and other non-food furnace recipes work normally. Smokers, campfires, blast furnaces, and modded cooking machines are unaffected.

The check is done in `AbstractFurnaceBlockEntity.canBurn` using a targeted common-side Mixin; this does not delete recipes or modify other machines.

Build: Java 21, `gradle --no-daemon clean build`.

Output: `build/libs/nofoodinfurnace-1.21.1-1.0.0.jar`.

Test: raw beef/raw chicken/potato/kelp in furnace (blocked); iron ore (works); food in smoker/campfire (works); food added by another mod (blocked if recipe is categorized FOOD or either stack has the FOOD component). Existing fuel can keep burning until exhausted if it was burning before a food item is inserted.
