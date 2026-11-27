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
  var restored=Metabolism.decode(Metabolism.encode(fed));require(restored.fast==fed.fast&&restored.pending[0]==fed.pending[0]&&restored.stomach==fed.stomach,"NBT roundtrip");
 }
}
