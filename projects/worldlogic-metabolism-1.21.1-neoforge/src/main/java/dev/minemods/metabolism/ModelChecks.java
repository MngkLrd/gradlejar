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
  require(server.getCommands().getDispatcher().getRoot().getChild("feed")!=null&&server.getCommands().getDispatcher().getRoot().getChild("worldlogic").getChild("feed")!=null,"operator feed aliases");
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
  var ids=net.minecraft.core.registries.BuiltInRegistries.ITEM;
  int audited=0;
  for(var item:ids){if(!ids.getKey(item).getNamespace().equals("minecraft"))continue;var stack=new net.minecraft.world.item.ItemStack(item);var prof=FoodProfiles.profile(stack,p);if(prof.isEmpty())continue;
   var e=prof.get();require(e.known(),"all vanilla food is explicitly profiled: "+ids.getKey(item));
   var mm=e.meal();var n=mm.nutrients();require(Math.abs(mm.kcal()-(4*n[0]+9*n[1]+4*n[2]+2*n[3]))<1e-6,"macro energy consistency "+ids.getKey(item));audited++;
  }
  require(audited>=35,"vanilla food coverage");
  if(net.neoforged.fml.ModList.get().isLoaded("worldlogic_aqua"))for(var item:ids){String id=ids.getKey(item).toString();if(id.startsWith("aquaculture:raw_")||id.startsWith("aquaculture:cooked_"))require(FoodProfiles.profile(new net.minecraft.world.item.ItemStack(item),p).isEmpty(),"blank whole/portion must not grant nutrition: "+id);}
  require(FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OMINOUS_BOTTLE),p).isEmpty(),"ominous bottle is not nutrition");
  var cod=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_COD)).orElseThrow().meal();
  var salmon=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_SALMON)).orElseThrow().meal();
  var chicken=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN)).orElseThrow().meal();
  var pork=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_PORKCHOP)).orElseThrow().meal();
  var honey=FoodProfiles.profile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HONEY_BOTTLE)).orElseThrow().meal();
  require(salmon.nutrients()[1]>cod.nutrients()[1]*5&&salmon.kcal()>cod.kcal()&&chicken.nutrients()[0]>pork.nutrients()[0]&&pork.nutrients()[1]>chicken.nutrients()[1]*2,"species-specific macros");
  require(honey.nutrients()[0]==0&&honey.nutrients()[2]>40,"honey carbohydrate");
  var profile=dev.minemods.metabolism.api.MetabolismApi.foodProfile(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_SALMON),p).orElseThrow();
  require(profile.known()&&profile.fatG()==salmon.nutrients()[1]&&profile.kcal()==salmon.kcal(),"public food profile");
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
   require(result.consumesAction()&&level.getBlockState(pos).getValue(net.minecraft.world.level.block.CakeBlock.BITES)==1&&Math.abs(Metabolism.state(p).stomach-FoodProfiles.cakeSlice().meal().kcal())<1e-6,"actual cake bite and metabolic hook");
   var full=Metabolism.state(p);full.stomach=MetabolicModel.STOMACH_MAX;Metabolism.store(p,full);
   p.getFoodData().setFoodLevel(20);
   require(!level.getBlockState(pos).useWithoutItem(level,p,hit).consumesAction()&&level.getBlockState(pos).getValue(net.minecraft.world.level.block.CakeBlock.BITES)==1,"full stomach cake negative");
  }finally{level.setBlock(pos,original,3);}
  Metabolism.store(p,new MetabolicModel.State());var bottle=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,bottle,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
  require(Metabolism.state(p).drinkCooldown==15&&Metabolism.state(p).stomach==0,"water finish no calories");
  require(!WaterIntakeBridge.useItemFallback(true,true)&&WaterIntakeBridge.useItemFallback(false,true)&&WaterIntakeBridge.useItemFallback(true,false),"Hydration callback owns water, legacy standalone fallback remains");
  var organs=dev.minemods.metabolism.api.MetabolismApi.organInputs(p);
  if(!net.neoforged.fml.ModList.get().isLoaded("worldlogic_trauma"))require(!organs.available()&&organs.digestiveFunction()==1&&organs.hepaticFunction()==1,"missing Trauma neutral public snapshot");
  if(dev.minemods.worldlogic.api.WorldLogicApi.class.getDeclaredMethods().length>0){try{var handshake=dev.minemods.worldlogic.api.WorldLogicApi.class.getMethod("usesMetabolicRestRecovery",net.minecraft.server.level.ServerPlayer.class);require(Boolean.TRUE.equals(handshake.invoke(null,p))&&RestRecovery.coreOwns(p),"native Core policy handshake");}catch(NoSuchMethodException oldCore){require(!RestRecovery.coreOwns(p),"legacy Core fallback");}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}}
  Metabolism.store(p,new MetabolicModel.State());
  var medicine=net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,net.minecraft.world.item.alchemy.Potions.HEALING);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(p,medicine,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE)));
  require(Metabolism.state(p).drinkCooldown==0&&Metabolism.state(p).stomach==0,"non-food medicine not free energy");
  require(dev.minemods.metabolism.api.MetabolismApi.restoreNutrition(p,true)&&p.getFoodData().getFoodLevel()==20&&Metabolism.state(p).fast==MetabolicModel.FAST_MAX&&dev.minemods.metabolism.api.MetabolismApi.restingRecoveryPointsPerSecond(p)>33,"actual nutrition reset and rest API");
  var manual=dev.minemods.metabolism.api.MetabolismApi.reportFoodIntake(p,100,java.util.Map.of("carb",20d),0,.9);
  require(manual.acceptedKcal()==100&&dev.minemods.metabolism.api.MetabolismApi.energy(p).stomachFraction()>0,"nonstandard meal public path");
  try{dev.minemods.metabolism.api.MetabolismApi.reportFoodIntake(p,100,java.util.Map.of("carb",Double.NaN),0,1);throw new AssertionError("invalid input accepted");}catch(IllegalArgumentException expected){} 
 }
 public static void run(){
  var expectedFish=java.util.Set.of("pike","perch","crucian_carp","carp","roach","bream","trout","grayling","char","taimen","whitefish","burbot","catfish","tench","loach","rotan","rudd","bowfin","piranha","arapaima","tilapia","pacu","snakehead","electric_eel","atlantic_cod","herring","mackerel","halibut","haddock","flounder","tuna","barracuda","grouper","snapper","mahi_mahi","parrotfish");
  require(FishNutrition.species().equals(expectedFish),"all 36 actual Aqua species profiled");
  for(String id:expectedFish){
   var raw=FishNutrition.portion(id,100,false).orElseThrow().meal();var small=FishNutrition.portion(id,17,false).orElseThrow().meal();
   var cooked=FishNutrition.portion(id,80,true).orElseThrow().meal();
   for(int i=0;i<MetabolicModel.N;i++)require(Math.abs(small.nutrients()[i]-raw.nutrients()[i]*.17)<1e-8,"mass scaling "+id);
   require(Math.abs(small.kcal()-raw.kcal()*.17)<1e-8&&cooked.kcal()<=raw.kcal()&&cooked.kcal()>raw.kcal()*.89,"cooking yield and retention "+id);
   require(raw.nutrients()[2]==0&&raw.toxin()>cooked.toxin()&&raw.absorption()<cooked.absorption(),"raw cooked differences "+id);
  }
  require(FishNutrition.portion("mackerel",100,false).orElseThrow().meal().nutrients()[1]>10*FishNutrition.portion("atlantic_cod",100,false).orElseThrow().meal().nutrients()[1],"lean vs fatty fish");
  require(FishNutrition.portion("halibut",17,false).orElseThrow().meal().kcal()>0&&FishNutrition.portion("halibut",100,true).isEmpty()&&FishNutrition.portion("halibut",0,false).isEmpty()&&FishNutrition.portion("unknown",100,false).isEmpty(),"fish invalid and halibut");
  var rest=new MetabolicModel.State();var active=new MetabolicModel.State();
  int[] seconds={3,5,7,10,12};int[] hunger={20,16,12,6,0};
  for(int i=0;i<seconds.length;i++){
   var s=new MetabolicModel.State();s.fast=MetabolicModel.FAST_MAX;require(s.restSeconds(hunger[i])==seconds[i],"rest tier "+seconds[i]);
   for(boolean core:new boolean[]{false,true}){
    var clock=new RestRecovery.Clock();double fatigue=100;int elapsed=0;
    for(int tick=1;tick<=seconds[i]*20;tick++){
     if(core&&tick%20==0)fatigue=Math.max(0,fatigue-4);
     double requested=clock.request(fatigue,true,s.restRate(hunger[i])),after=Math.max(0,fatigue-requested);
     clock.settled(fatigue,after);fatigue=after;
     if(fatigue<=1e-7&&elapsed==0)elapsed=tick;
    }
    require(fatigue<1e-6&&elapsed>0&&elapsed>=seconds[i]*20-10&&elapsed<=seconds[i]*20,"full rest recovery duration "+seconds[i]+": "+elapsed);
   }
  }
  var clock=new RestRecovery.Clock();require(clock.request(100,false,100d/3)==0,"no resting bonus while active");
  double req=clock.request(90,true,100d/3);clock.settled(90,90-req);require(clock.request(95,true,100d/3)==0,"new exertion interrupts rest");
  var sick=new MetabolicModel.State();sick.toxin=.8;sick.fast=0;sick.reserve=10000;sick.stores[0]=0;
  require(sick.restSeconds(20)>=10,"poor metabolism slows rest");
  MetabolicModel.restoreNutrition(sick,false);require(sick.fast==MetabolicModel.FAST_MAX&&sick.quality()==1&&sick.toxin==.8&&sick.reserve==10000,"feed does not cure poison or increase fat");
  MetabolicModel.restoreNutrition(sick,true);require(sick.toxin==0&&sick.restSeconds(20)==3,"full nutritional reset");
  MetabolicModel.tick(rest,0);MetabolicModel.tick(active,1);require(active.fast<rest.fast,"activity costs more");
  var fed=new MetabolicModel.State();double[] n=new double[MetabolicModel.N];n[0]=65;
  MetabolicModel.ingest(fed,new MetabolicModel.Meal(300,n,0,1),true);
  require(fed.stomach==300&&fed.fast==1350&&fed.pending[0]>0,"queued, not immediate");
  MetabolicModel.tick(fed,0);require(fed.stomach<300&&fed.stores[0]>1&&fed.fast>1350,"digestion");
  var healthyGut=new MetabolicModel.State();var damagedGut=new MetabolicModel.State();
  MetabolicModel.ingest(healthyGut,new MetabolicModel.Meal(300,n,0,1),true);MetabolicModel.ingest(damagedGut,new MetabolicModel.Meal(300,n,0,1),true);
  damagedGut.organInputs(true,.4,.25);healthyGut.toxin=.4;damagedGut.toxin=.4;
  double hAbs=MetabolicModel.tick(healthyGut,0),dAbs=MetabolicModel.tick(damagedGut,0);
  require(Math.abs(dAbs/hAbs-.4)<1e-6&&Math.abs((.4-damagedGut.toxin)/(.4-healthyGut.toxin)-.25)<1e-6,"organ factors applied once to throughput and clearance");
  var stopped=new MetabolicModel.State();stopped.organInputs(true,0,0);stopped.toxin=.3;var blocked=MetabolicModel.consume(stopped,new MetabolicModel.Meal(100,n,0,1),true);
  require(blocked.quickKcal()==0&&blocked.requestedPoints()==0&&MetabolicModel.tick(stopped,0)==0&&stopped.toxin==.3,"zero digestive/hepatic function blocks own pathways");
  stopped.organInputs(false,0,0);require(stopped.digestionRate()>0&&stopped.digestiveFunction==1&&stopped.hepaticFunction==1,"missing provider restores neutral");
  stopped.organInputs(true,Double.NaN,.5);require(!stopped.organInputsAvailable&&stopped.digestiveFunction==1,"invalid organ input neutral");
  var transientOrgans=Metabolism.decode(Metabolism.encode(damagedGut));require(!transientOrgans.organInputsAvailable&&transientOrgans.digestiveFunction==1&&transientOrgans.hepaticFunction==1,"organ state not copied to save");
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
