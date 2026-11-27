package dev.minemods.metabolism;
public final class ModelChecks {
 private static void require(boolean value,String what){if(!value)throw new AssertionError(what);}
 public static void main(String[] args){run();}
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
  var restored=Metabolism.decode(Metabolism.encode(fed));require(restored.fast==fed.fast&&restored.pending[0]==fed.pending[0]&&restored.stomach==fed.stomach&&restored.baseline[0]==fed.baseline[0]&&restored.observedSeconds==fed.observedSeconds,"NBT roundtrip");
 }
}
