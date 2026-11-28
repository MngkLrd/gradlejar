package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.SeasonKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class FishLife {
 static final long BIO_YEAR=72000L,MAX_CATCHUP=24000L;
 private FishLife(){}
 static void juvenile(Fish f,Species s,long now){
  var n=Aquaculture.traits(f,s);n.putInt("wl_fish_age_years",0);
  n.putLong("wl_life_age",0);n.putLong("wl_life_last",now);n.putBoolean("wl_juvenile",true);
  n.putDouble("wl_fish_length_m",Math.max(.05,s.adultLength()*.12));n.putDouble("wl_fish_mass_kg",Math.max(.001,s.maxKg()*.001728));
  n.putLong("wl_fish_fed_until",now+FishFeeding.DIGEST_TICKS/2);
  f.syncVisualLength(s,n);
 }
 static void age(Fish f,Species s,long now){
  var n=Aquaculture.traits(f,s);
  long previous=n.contains("wl_life_last")?n.getLong("wl_life_last"):now;
  long elapsed=Math.max(0,Math.min(MAX_CATCHUP,now-previous));n.putLong("wl_life_last",now);
  long age=n.contains("wl_life_age")?n.getLong("wl_life_age"):n.getInt("wl_fish_age_years")*BIO_YEAR;
  age+=elapsed;n.putLong("wl_life_age",age);n.putInt("wl_fish_age_years",(int)Math.min(100,age/BIO_YEAR));
  if(n.getBoolean("wl_juvenile")){
   double target=s.adultLength()*Math.min(.85,.12+.73*age/(BIO_YEAR*(double)s.maturityYears()));
   double old=n.getDouble("wl_fish_length_m");
   if(FishFeeding.satiety(f,now)>0&&target>old){
    double length=Math.min(target,old+s.adultLength()*elapsed/(BIO_YEAR*(double)s.maturityYears()));
    n.putDouble("wl_fish_length_m",length);n.putDouble("wl_fish_mass_kg",Math.max(.001,s.maxKg()*Math.pow(length/s.adultLength(),3)));
    f.syncVisualLength(s,n);
   }
   if(n.getInt("wl_fish_age_years")>=s.maturityYears()&&n.getDouble("wl_fish_length_m")>=s.adultLength()*.65)n.putBoolean("wl_juvenile",false);
  }
  if(age>=(s.maturityYears()*3L+8)*BIO_YEAR&&f.isAlive())f.hurt(f.damageSources().generic(),Float.MAX_VALUE);
 }
 static boolean mature(Fish f,Species s){return !f.getPersistentData().getBoolean("wl_juvenile")&&FishFeeding.mature(f,s);}
 static boolean seasonAllows(Species s,SeasonKind season){
  if(s.region().equals("tropical")||s.region().equals("marine_warm"))return true;
  if(s.region().equals("cold")||s.region().equals("marine_cold"))return season==SeasonKind.SPRING||season==SeasonKind.AUTUMN;
  return season==SeasonKind.SPRING||season==SeasonKind.SUMMER;
 }
 static boolean ready(Fish f,Species s,long now){
  var n=f.getPersistentData();
  return f.isAlive()&&mature(f,s)&&FishWater.contact(f)&&!f.ecologyPanic()&&FishFeeding.satiety(f,now)>=30
   &&n.getDouble("wl_fish_stress")<.3&&now>=n.getLong("wl_roe_next");
 }
 static boolean nursery(ServerLevel l,BlockPos p,Species s){
  var h=Aquaculture.habitat(l,p);
  if(h==null||!s.suitable(h.kind(),h.temp(),h.salt(),h.risk(),h.depth())||!l.getBlockState(p).getCollisionShape(l,p).isEmpty())return false;
  var below=l.getBlockState(p.below());
  return switch(s.nursery()){
   case "vegetation"->FishEcology.hasPlants(l,p);
   case "gravel"->below.is(Blocks.GRAVEL)||below.is(Blocks.SAND)||below.is(Blocks.STONE)||below.is(Blocks.COBBLESTONE);
   case "open_water"->Aquaculture.water(l,p.above())&&Aquaculture.water(l,p.below())&&h.depth()>=3;
   default->below.isCollisionShapeFullBlock(l,p.below());
  };
 }
 static boolean courtship(ServerLevel l,Fish female,Species s){
  long now=l.getGameTime();var n=Aquaculture.traits(female,s);
  if(!n.getBoolean("wl_fish_female")||!ready(female,s,now))return false;
  var season=WorldLogicApi.season(l,female.blockPosition());
  if(season.isEmpty()||!seasonAllows(s,season.get().kind()))return false;
  Fish partner=null;double nearest=36;
  for(Fish other:l.getEntitiesOfClass(Fish.class,female.getBoundingBox().inflate(6))){
   if(other==female||Aquaculture.species(other)!=s||Aquaculture.traits(other,s).getBoolean("wl_fish_female")||!ready(other,s,now))continue;
   double d=female.distanceToSqr(other);
   if(d<nearest&&FishEcology.clearWaterPath(l,female.position(),other.position())){nearest=d;partner=other;}
  }
  if(partner==null)return false;
  if(nearest>2.25){FishEcology.steer(l,female,partner.position(),.11);return true;}
  for(int i=0;i<18;i++){
   BlockPos p=i==0?female.blockPosition():female.blockPosition().offset(l.random.nextInt(7)-3,l.random.nextInt(5)-2,l.random.nextInt(7)-3);
   if(!nursery(l,p,s)||!FishEcology.clearWaterPath(l,female.position(),Vec3.atCenterOf(p)))continue;
   var h=Aquaculture.habitat(l,p);var area=PopulationData.area(p,h.kind());var pop=PopulationData.get(l);
   if(!PopulationData.area(female.blockPosition(),h.kind()).equals(area))continue;
   int eggs=Math.min(pop.room(area,s),s.hatchMin()+l.random.nextInt(s.hatchMax()-s.hatchMin()+1));
   if(eggs<1)continue;
   RoeClutch clutch=Aquaculture.ROE.get().create(l);if(clutch==null)return false;
   clutch.moveTo(p.getX()+.5,p.getY()+(s.nursery().equals("open_water")?.5:.03),p.getZ()+.5,0,0);
   clutch.initialize(s,h.kind(),eggs);
   if(!pop.reserve(clutch.getUUID(),area,s,eggs)){clutch.discard();continue;}
   if(!l.addFreshEntity(clutch)){pop.remove(clutch.getUUID());clutch.discard();continue;}
   long cooldown=now+Math.max(1,s.reproductionCooldownDays())*24000L;
   n.putLong("wl_roe_next",cooldown);Aquaculture.traits(partner,s).putLong("wl_roe_next",cooldown);
   n.putLong("wl_fish_fed_until",now+Math.max(0,(n.getLong("wl_fish_fed_until")-now)/2));
   var pn=partner.getPersistentData();pn.putLong("wl_fish_fed_until",now+Math.max(0,(pn.getLong("wl_fish_fed_until")-now)/2));
   return true;
  }
  return false;
 }
}
