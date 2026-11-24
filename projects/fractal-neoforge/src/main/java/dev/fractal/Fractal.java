package dev.fractal;
import dev.fractal.block.*;
import dev.fractal.cell.*;
import dev.fractal.item.Probe;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

@Mod("fractal")
public final class Fractal {
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("fractal");
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("fractal");
 public static final DeferredBlock<HostBlock> STONE=BLOCKS.register("stone_cell",()->new HostBlock(Material.STONE,hostProps(1.5f,6).requiresCorrectToolForDrops()));
 public static final DeferredBlock<HostBlock> COPPER=BLOCKS.register("copper_cell",()->new HostBlock(Material.COPPER,hostProps(3,6).requiresCorrectToolForDrops()));
 public static final DeferredBlock<HostBlock> WOOD=BLOCKS.register("wood_cell",()->new HostBlock(Material.WOOD,hostProps(2,3)));
 public static final DeferredBlock<Block> STONE_MATRIX=BLOCKS.register("stone_matrix",()->new Block(shell()));
 public static final DeferredBlock<Block> COPPER_MATRIX=BLOCKS.register("copper_matrix",()->new Block(shell()));
 public static final DeferredBlock<Block> WOOD_MATRIX=BLOCKS.register("wood_matrix",()->new Block(shell()));
 public static final DeferredBlock<Block> CRYSTAL=BLOCKS.register("crystal_matrix",()->new Block(shell().lightLevel(s->12)));
 public static final DeferredBlock<SocketBlock> SOCKET=BLOCKS.register("lattice_socket",()->new SocketBlock(shell().lightLevel(s->s.getValue(SocketBlock.TYPE)==3?12:0)));
 public static final DeferredBlock<InputBlock> INPUT=BLOCKS.register("input_terminal",()->new InputBlock(shell()));
 public static final DeferredBlock<Block> OUTPUT=BLOCKS.register("output_terminal",()->new Block(shell()));
 public static final DeferredBlock<Block> EXIT=BLOCKS.register("ascension_node",()->new Block(shell().lightLevel(s->10)));
 public static final DeferredItem<Probe> PROBE=ITEMS.register("matter_probe",()->new Probe(new Item.Properties().stacksTo(1)));
 private static BlockBehaviour.Properties shell(){return BlockBehaviour.Properties.of().strength(-1,3600000).noLootTable().pushReaction(PushReaction.BLOCK);}
 private static BlockBehaviour.Properties hostProps(float hardness,float resist){return BlockBehaviour.Properties.of().strength(hardness,resist).pushReaction(PushReaction.BLOCK).lightLevel(s->s.getValue(HostBlock.LIGHT)*5);}
 public Fractal(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);bus.addListener(this::tab);NeoForge.EVENT_BUS.addListener(CellNetwork::tick);NeoForge.EVENT_BUS.addListener(CellNetwork::commands);NeoForge.EVENT_BUS.addListener(CellNetwork::onBreak);}
 private void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(PROBE.get());}
}
