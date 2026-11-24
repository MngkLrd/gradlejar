package dev.minemods.sleepdebt.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
/** Inert migration ID: existing saved entities are discarded. */
public final class SleeplessEntity extends Monster {
 public SleeplessEntity(EntityType<? extends Monster> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder createAttributes(){return Monster.createMonsterAttributes();}
 @Override protected void registerGoals(){}
 @Override public void tick(){if(!level().isClientSide)discard();}
}
