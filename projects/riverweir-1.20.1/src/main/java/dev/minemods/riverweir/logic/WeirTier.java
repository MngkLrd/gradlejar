package dev.minemods.riverweir.logic;

import dev.minemods.riverweir.RiverWeirMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Three distinct physical net materials, deliberately capped for river use. */
public enum WeirTier {
    JUTE(0,"net_roll",16,6,3,100,2,0.26f,180,1,3),
    BRONZE(1,"bronze_net_roll",24,10,5,220,18,0.40f,120,1,2),
    IRON(2,"iron_net_roll",32,16,8,360,27,0.55f,80,1,1);

    public final int id, maxSpan, maxDepth, panelHeight, endurance, storage, cooldownTicks, catchWear, clogPerCatch;
    public final float catchChance;
    public final String rollId;
    WeirTier(int id,String rollId,int maxSpan,int maxDepth,int panelHeight,int endurance,int storage,
             float catchChance,int cooldownTicks,int catchWear,int clogPerCatch) {
        this.id=id;this.rollId=rollId;this.maxSpan=maxSpan;this.maxDepth=maxDepth;
        this.panelHeight=panelHeight;this.endurance=endurance;this.storage=storage;
        this.catchChance=catchChance;this.cooldownTicks=cooldownTicks;
        this.catchWear=catchWear;this.clogPerCatch=clogPerCatch;
    }
    public static WeirTier byId(int value){
        for(WeirTier tier:values())if(tier.id==value)return tier;
        return JUTE;
    }
    public boolean isRoll(ItemStack stack){
        return !stack.isEmpty()&&new ResourceLocation(RiverWeirMod.ID,rollId).equals(
            ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
