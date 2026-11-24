package dev.ataflore.huss;

import dev.ataflore.huss.training.Training;
import dev.ataflore.huss.item.DisciplineWhistle;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(HussMod.ID)
public final class HussMod {
    public static final String ID="husshusshuss";
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,ID);
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,ID);
    private static final DeferredHolder<MobEffect,MobEffect> TRAINING=EFFECTS.register("training",()->new MobEffect(MobEffectCategory.NEUTRAL,0xffa33a){});
    public static Holder<MobEffect> trainingEffect(){return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TRAINING.get());}
    public static final DeferredHolder<Item,Item> WHISTLE=ITEMS.register("discipline_whistle",()->new DisciplineWhistle(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<SoundEvent,SoundEvent> HUSS=SOUNDS.register("huss",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ID,"huss")));
    public HussMod(IEventBus bus) {
        ITEMS.register(bus); SOUNDS.register(bus); EFFECTS.register(bus);
        bus.addListener(this::creative);
        NeoForge.EVENT_BUS.addListener(Training::onTick);
        NeoForge.EVENT_BUS.addListener(Training::onInteract);
    }
    private void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(WHISTLE.get());
    }
}
