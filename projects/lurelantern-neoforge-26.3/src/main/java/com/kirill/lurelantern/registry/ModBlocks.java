package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.item.LureLanternBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModBlocks {
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("lurelantern");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("lurelantern");
 public static final DeferredBlock<LureLanternBlock> BASIC=BLOCKS.registerBlock("lure_lantern",LureLanternBlock::new,
   p->p.strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:12));
 public static final DeferredBlock<LureLanternBlock> ADVANCED=BLOCKS.registerBlock("advanced_lure_lantern",LureLanternBlock::new,
   p->p.strength(.8f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:13));
 public static final DeferredItem<LureLanternBlockItem> BASIC_ITEM=ITEMS.registerItem("lure_lantern",
   p->new LureLanternBlockItem(BASIC.get(),p,false),p->p.stacksTo(1));
 public static final DeferredItem<LureLanternBlockItem> ADVANCED_ITEM=ITEMS.registerItem("advanced_lure_lantern",
   p->new LureLanternBlockItem(ADVANCED.get(),p,true),p->p.stacksTo(1));
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
}
