package dev.voidchisel;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

/**
 * Regular pickaxe behavior and vanilla durability display.
 * The durability bar appears only after the first damage point.
 */
public final class VoidPickaxeItem extends PickaxeItem {
    public VoidPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }
}
