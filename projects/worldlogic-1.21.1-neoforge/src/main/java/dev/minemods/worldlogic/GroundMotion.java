package dev.minemods.worldlogic;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.minemods.worldlogic.api.WorldLogicApi;
/** Vanilla travel uses retention (higher means lower braking), not the SI friction coefficient. */
public final class GroundMotion{
 private GroundMotion(){}
 public static boolean wet(Level level,BlockPos pos){return level.isRainingAt(pos.above());}
 public static float retention(BlockState s,float vanilla,boolean wet,boolean inertia,boolean slippery,double movement,double weather){
  if(!Float.isFinite(vanilla))return vanilla;
  // Preserve unusual custom friction, including slime, honey and existing ice.
  if(vanilla<.50f||vanilla>.91f)return vanilla;
  float target=vanilla;
  if(inertia){
   if(s.is(BlockTags.SAND)||s.is(Blocks.GRAVEL)||s.is(Blocks.SOUL_SAND)||s.is(Blocks.SNOW_BLOCK))target=.57f;
   else if(s.is(Blocks.MUD)||s.is(Blocks.CLAY)||s.is(Blocks.PACKED_MUD))target=wet?.66f:.59f;
   else if(s.is(BlockTags.DIRT)||Material.block(s,"rough_terrain"))target=.67f;
   else if(Material.block(s,"wood"))target=.73f;
   else if(Material.block(s,"ricochet_stone")||Material.block(s,"ricochet_metal")||Material.block(s,"glass"))target=.76f;
   else target=.69f;
   target=(float)(vanilla+(target-vanilla)*Math.min(1.5,Math.max(0,movement)));
  }
  if(wet&&slippery&&(Material.block(s,"slippery")||s.is(Blocks.MUD)||s.is(Blocks.CLAY)||s.is(Blocks.PACKED_MUD)))
   target=Math.max(target,(float)Math.min(.94,vanilla+.25*Math.max(0,weather)));
  return Math.max(.50f,Math.min(.94f,target));
 }
 public static float effective(Level level,BlockPos pos,BlockState s,Entity entity,float vanilla){
  if(!(entity instanceof Player player)||player.isCreative()||player.isSpectator()||player.getAbilities().flying||player.isInWater()||player.onClimbable())return vanilla;
  boolean client=level.isClientSide;
  return retention(s,vanilla,wet(level,pos),RuleSync.inertia(client),RuleSync.friction(client),RuleSync.movement(client),RuleSync.weather(client));
 }
 public static Optional<WorldLogicApi.GroundTraction> sample(ServerLevel l,BlockPos pos,Entity entity){
  if(l.isOutsideBuildHeight(pos)||!l.hasChunkAt(pos)||entity.level()!=l)return Optional.empty();
  var state=l.getBlockState(pos);
  if(state.isAir()||!state.getFluidState().isEmpty())return Optional.empty();
  return Optional.of(new WorldLogicApi.GroundTraction(effective(l,pos,state,entity,state.getFriction(l,pos,entity)),wet(l,pos)));
 }
 static double downhillRisk(double speed,double drop,double run,boolean slick,boolean injured,double fatigue){
  if(speed<.24||drop<.48||run<.35)return 0;
  double grade=drop/run;if(grade<.32)return 0;
  return Math.min(1.4,(grade-.32)*1.8+(speed-.24)*3.3+(slick?.23:0)+(injured?.24:0)+Math.max(0,fatigue-55)*.005);
 }
 static void verify(){
  float rock=retention(Blocks.STONE.defaultBlockState(),.6f,false,true,true,1,1);
  float sand=retention(Blocks.SAND.defaultBlockState(),.6f,false,true,true,1,1);
  float wet=retention(Blocks.STONE.defaultBlockState(),.6f,true,true,true,1,1);
  if(rock<=sand||wet<rock||retention(Blocks.ICE.defaultBlockState(),.98f,true,true,true,1,1)!=.98f||retention(Blocks.STONE.defaultBlockState(),.6f,false,false,false,1,1)!=.6f)throw new IllegalStateException("ground traction controls");
  if(downhillRisk(.18,1,1,true,true,90)!=0||downhillRisk(.35,0,1,true,true,90)!=0||downhillRisk(.33,1.1,1.4,false,false,0)<.8)throw new IllegalStateException("downhill risk controls");
 }
}
