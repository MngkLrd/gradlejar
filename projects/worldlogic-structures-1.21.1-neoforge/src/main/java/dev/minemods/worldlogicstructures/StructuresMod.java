package dev.minemods.worldlogicstructures;

import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import dev.minemods.worldlogicstructures.api.StructuresApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.function.Supplier;

@Mod(StructuresApi.MOD_ID)
public final class StructuresMod {
 private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,StructuresApi.MOD_ID);
 private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,StructuresApi.MOD_ID);
 static final Supplier<StructureType<RegionalStructure>> REGIONAL=TYPES.register("regional",()->()->RegionalStructure.CODEC);
 static final Supplier<StructurePieceType> ARCHITECTURE=PIECES.register("architecture",()->(context,tag)->new ArchitecturePiece(tag));
 public StructuresMod(IEventBus bus){
  TYPES.register(bus); PIECES.register(bus);
  WorldLogicCapabilities.register(StructuresApi.KEY,0,StructuresApi.MOD_ID,StructuresApi.CATALOGUE);
  NeoForge.EVENT_BUS.addListener(this::commands);
 }
 private void commands(RegisterCommandsEvent event){
  var dispatcher=event.getDispatcher();
  var search=Commands.literal("structure_find").executes(c->dispatcher.execute("locate structure #worldlogic_structures:abandoned",c.getSource()));
  for(var family:StructuresApi.Family.values())search.then(Commands.literal(family.getSerializedName()).executes(c->dispatcher.execute("locate structure "+family.locateId(),c.getSource())));
  dispatcher.register(Commands.literal("worldlogic").requires(s->s.hasPermission(2)).then(search));
  dispatcher.register(Commands.literal("worldlogic_structures").requires(s->s.hasPermission(2))
   .then(Commands.literal("selftest").executes(c->{
    try{StructureChecks.run(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("STRUCTURES_CHECKS_PASSED designs=27 families=8"),false);return 1;}
    catch(RuntimeException e){c.getSource().sendFailure(Component.literal("STRUCTURES_CHECKS_FAILED "+e.getMessage()));return 0;}
   })));
 }
}
