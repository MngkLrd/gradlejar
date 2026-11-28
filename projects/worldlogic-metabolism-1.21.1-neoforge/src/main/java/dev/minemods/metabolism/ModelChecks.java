package dev.minemods.metabolism;
public final class ModelChecks {
 private static void require(boolean value,String what){if(!value)throw new AssertionError(what);}
 public static void main(String[] args){run();}
 public static void verifyCore(net.minecraft.server.MinecraftServer server){
  var p=new net.minecraft.server.level.ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MetabolismCoreQA"),net.minecraft.server.level.ClientInformation.createDefault());
  var fatigue=dev.minemods.worldlogic.api.WorldLogicApi.fatigue(p);
  require(Double.isFinite(fatigue.fatiguePercent())&&fatigue.fatiguePercent()>=0&&fatigue.fatiguePercent()<=100,"actual Core fatigue API");
  dev.minemods.worldlogic.api.WorldLogicApi.addExertion(p,0);
  require(fatigue.enabled(),"Core exertion enabled in QA");
  for(int i=0;i<5;i++)dev.minemods.worldlogic.api.WorldLogicApi.addExertion(p,100);
  double before=dev.minemods.worldlogic.api.WorldLogicApi.fatigue(p).fatiguePercent();
  var meal=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD)).orElseThrow();
  var eaten=Metabolism.acceptMeal(p,meal.meal(),meal.known());
  require(eaten.staminaPoints()>2&&dev.minemods.worldlogic.api.WorldLogicApi.fatigue(p).fatiguePercent()<before,"actual Core recovery from food");
  var water=dev.minemods.metabolism.api.MetabolismApi.reportWaterIntake(p,250,0,0);
  require(Math.abs(water-2)<1e-5&&dev.minemods.metabolism.api.MetabolismApi.reportWaterIntake(p,250,0,0)==0,"actual water boost and duplicate/cooldown");
  require(Metabolism.state(p).drinkCooldown==15,"drink persistence");
  p.getFoodData().setFoodLevel(20);require(p.canEat(false),"full hunger allows room in metabolic stomach");
  var filled=Metabolism.state(p);filled.stomach=MetabolicModel.STOMACH_MAX;Metabolism.store(p,filled);require(!p.canEat(false),"full stomach does not override vanilla");
  require(FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET)).orElseThrow().meal().kcal()>0,"milk calories");
  var generic=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
  generic.set(net.minecraft.core.component.DataComponents.FOOD,new net.minecraft.world.food.FoodProperties.Builder().nutrition(5).saturationModifier(.6f).build());
  require(FoodProfiles.profile(generic).orElseThrow().meal().kcal()>0&&!FoodProfiles.profile(generic).orElseThrow().known(),"generic FOOD component");
  var event=new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE),0,net.minecraft.world.item.ItemStack.EMPTY);
  Metabolism.store(p,new MetabolicModel.State());net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
  require(Metabolism.state(p).stomach>0,"registered food finish handler");
  var level=p.serverLevel();var pos=level.getSharedSpawnPos().above(5);var original=level.getBlockState(pos);
  Metabolism.store(p,new MetabolicModel.State());p.getFoodData().setFoodLevel(20);
  try{
   var cake=net.minecraft.world.level.block.Blocks.CAKE.defaultBlockState();level.setBlock(pos,cake,3);
   var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
   var result=level.getBlockState(pos).useWithoutItem(level,p,hit);
   require(result.consumesAction()&&level.getBlockState(pos).getValue(net.minecraft.world.level.block.CakeBlock.BITES)==1&&Metabolism.state(p).stomach==150,"actual cake bite and metabolic hook");
   var full=Metabolism.state(p);full.stomach=MetabolicModel.STOMACH_MAX;Metabolism.store(p,full);
   p.getFoodData().setFoodLevel(20);
   require(!level.getBlockState(pos).useWithoutItem(level,p,hit).consumesAction()&&level.getBlockState(pos).getValue(net.minecraft.world.level.block.CakeBlock.BITES)==1,"full stomach cake negative");
  }finally{level.setBlock(pos,original,3);}
  Metabolism.store(p,new MetabolicModel.State());var bottle=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,bottle,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
  require(Metabolism.state(p).drinkCooldown==15&&Metabolism.state(p).stomach==0,"water finish no calories");
  Metabolism.store(p,new MetabolicModel.State());
  var medicine=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,medicine,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
  require(Metabolism.state(p).drinkCooldown==0&&Metabolism.state(p).stomach==0,"non-food medicine not free energy");
  var manual=dev.minemods.metabolism.api.MetabolismApi.reportFoodIntake(p,100,java.util.Map.of("carb",20d),0,.9);
  require(manual.acceptedKcal()==100&&dev.minemods.metabolism.api.MetabolismApi.energy(p).stomachFraction()>0,"nonstandard meal public path");
  try{dev.minemods.metabolism.api.MetabolismApi.reportFoodIntake(p,100,java.util.Map.of("carb",Double.NaN),0,1);throw new AssertionError("invalid input accepted");}catch(IllegalArgumentException expected){} 
 }
 public static void run(){
  var rest=new MetabolicModel.State();var active=new MetabolicModel.State();
  MetabolicModel.tick(rest,0);MetabolicModel.tick(active,1);require(active.fast<rest.fast,"activity costs more");
  var fed=new MetabolicModel.State();double[] n=new double[MetabolicModel.N];n[0]=65;
  MetabolicModel.ingest(fed,new MetabolicModel.Meal(300,n,0,1),true);
  require(fed.stomach==300&&fed.fast==1350&&fed.pending[0]>0,"queued, not immediate");
  MetabolicModel.tick(fed,0);require(fed.stomach<300&&fed.stores[0]>1&&fed.fast>1350,"digestion");
  var toxin=new MetabolicModel.State();MetabolicModel.ingest(toxin,new MetabolicModel.Meal(100,n,.5,1),true);
  require(toxin.recovery()<new MetabolicModel.State().recovery(),"toxin penalizes recovery");
  double before=toxin.toxin;MetabolicModel.tick(toxin,0);require(toxin.toxin<before,"clearance");
  var lacking=new MetabolicModel.State();lacking.stores[0]=0;require(lacking.quality()<.7,"single limiting nutrient");
  for(int i=0;i<10000;i++)MetabolicModel.tick(lacking,1);require(lacking.fast>=0&&lacking.reserve>=0&&lacking.stores[0]>=0,"bounded fasting");
  var depleted=new MetabolicModel.State();for(int i=0;i<1200;i++)MetabolicModel.tick(depleted,0);
  require(depleted.stores[0]<depleted.stores[6]&&depleted.trend(0)<0&&depleted.deficient()>0,"separate nutrient reserve periods and trend");
  var full=new MetabolicModel.State();MetabolicModel.ingest(full,new MetabolicModel.Meal(10000,n,0,1),true);
  require(full.stomach==MetabolicModel.STOMACH_MAX&&full.queuedEnergy==full.stomach,"meal capacity");
  var cleared=new MetabolicModel.State();MetabolicModel.ingest(cleared,new MetabolicModel.Meal(10,n,0,.5),true);
  for(int i=0;i<10;i++)MetabolicModel.tick(cleared,0);require(cleared.stomach==0&&cleared.absorption==1&&cleared.pending[0]==0,"empty stomach clears pending profile");
  var reserved=new MetabolicModel.State();double prior=reserved.fast+reserved.reserve;
  MetabolicModel.tick(reserved,0);require(reserved.reserve<21000&&reserved.fast+reserved.reserve<prior,"resting fat oxidation conserves energy");
  require(reserved.totalHours()>reserved.fastHours(),"reserve forecast");
  var old=Metabolism.encode(fed);old.putInt("schema",1);var migrated=Metabolism.decode(old);require(migrated.baseline[0]==migrated.stores[0]&&migrated.observedSeconds==0,"schema 1 migration");
  var low=new MetabolicModel.State();low.stores[0]=0;low.stores[5]=0;low.stores[7]=0;low.stores[8]=0;
  require(low.tissueFactor()<new MetabolicModel.State().tissueFactor()&&low.boneFactor()<.5&&low.bloodFactor()<.5&&low.fractureThresholdFactor()<1,"clinical factors");
  var dry=new MetabolicModel.State();dry.water=20;dry.salts=90;dry.bodyC=39;require(dry.digestionRate()<new MetabolicModel.State().digestionRate()&&dry.thermalCost()>1,"optional physiology");
  var hist=new MetabolicModel.State();MetabolicModel.ingest(hist,new MetabolicModel.Meal(200,n,0,1),true);for(int i=0;i<1200;i++)MetabolicModel.tick(hist,0);
  require(hist.historyCount==1&&hist.historyIntake[0]==200&&hist.historyBurn[0]>0&&hist.daySeconds==0,"history rollover");
  var historyRestored=Metabolism.decode(Metabolism.encode(hist));require(historyRestored.historyCount==1&&historyRestored.historyIntake[0]==200,"history persistence");
  var quick=new MetabolicModel.State();quick.fast=0;quick.reserve=0;double[] carb=new double[MetabolicModel.N];carb[2]=45;
  var q=MetabolicModel.consume(quick,new MetabolicModel.Meal(180,carb,0,1),true);
  require(q.requestedPoints()>2&&quick.fast>0&&quick.queuedEnergy<180&&quick.fast+quick.queuedEnergy<=180.00001,"quick energy conservation");
  double absorbed=MetabolicModel.tick(quick,0);
  require(absorbed>0&&MetabolicModel.digestionPoints(quick,absorbed,true)>MetabolicModel.digestionPoints(quick,absorbed,false),"digestion and resting");
  var over=new MetabolicModel.State();over.stomach=1700;over.queuedEnergy=1700;
  var overflow=MetabolicModel.consume(over,new MetabolicModel.Meal(300,carb,0,1),true);
  require(overflow.acceptedKcal()==100&&over.overload>0&&over.mealComfort()<.65,"overeating reduces uptake and caps accepted energy");
  require(MetabolicModel.consume(over,new MetabolicModel.Meal(300,carb,0,1),true).requestedPoints()==0,"no benefit when full");
  var drink=new MetabolicModel.State();double fuel=drink.fast;require(MetabolicModel.waterBoost(drink,250,0,0)==2&&drink.fast==fuel&&MetabolicModel.waterBoost(drink,250,0,0)==0,"water no calories and cooldown");
  for(int i=0;i<15;i++)MetabolicModel.tick(drink,0);require(MetabolicModel.waterBoost(drink,250,.8,35)<.2,"dirty salty water low bonus");
  var round=Metabolism.decode(Metabolism.encode(over));require(round.overload==over.overload,"overeating persistence");
  var restored=Metabolism.decode(Metabolism.encode(fed));require(restored.fast==fed.fast&&restored.pending[0]==fed.pending[0]&&restored.stomach==fed.stomach&&restored.baseline[0]==fed.baseline[0]&&restored.observedSeconds==fed.observedSeconds,"NBT roundtrip");
 }
}
