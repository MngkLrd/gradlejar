package dev.minemods.thermal;
import static dev.minemods.thermal.ThermalModel.*;
public final class ThermalModelTest {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; System.out.println("PASS " + label); }
    static Exposure air(double c,double heat,double wind,double wet,double clothes,double run) { return new Exposure(c,heat,wind,wet,clothes,run,false,c,0); }
    static State run(State s, Exposure e, int seconds) { for(int i=0;i<seconds;i++) s=tick(s,e,1); return s; }
    public static void main(String[] args) {
        State normal=run(initial(),air(22,0,0,0,0,0),600);
        check(normal.bodyC()==37,"comfort stable");
        State cold=run(initial(),air(-15,0,0,0,0,0),300);
        check(cold.stage()==Stage.HYPOTHERMIA,"cold exposure causes hypothermia");
        State dry=run(initial(),air(0,0,0,0,.8,0),180);
        State wet=run(initial(),air(0,0,.8,1,.8,0),180);
        check(wet.bodyC()<dry.bodyC(),"wet wind worsens cooling");
        check(dry.bodyC()>run(initial(),air(0,0,0,0,0,0),180).bodyC(),"dry insulation protects");
        check(run(cold,air(0,25,0,0,.5,0),180).bodyC()>cold.bodyC(),"radiant heat recovers cold body");
        State hot=run(initial(),air(45,0,0,0,0,0),400);
        check(hot.stage()==Stage.HYPERTHERMIA,"extreme heat causes hyperthermia");
        check(run(initial(),air(35,0,0,0,1,1),180).bodyC()>run(initial(),air(35,0,0,0,0,0),180).bodyC(),"exertion and insulation worsen heat");
        check(run(initial(),new Exposure(25,0,0,1,0,0,true,5,0),60).bodyC()<run(initial(),air(5,0,0,0,0,0),60).bodyC(),"cold immersion cools faster");
        check(run(hot,air(22,0,0,0,0,0),180).bodyC()<hot.bodyC(),"shade/comfort recovers hot body");
        check(tick(initial(),air(-30,0,0,0,0,0),0).bodyC()==37,"zero elapsed time");
        check(tick(initial(),air(-30,0,0,0,0,0),10000).bodyC()==tick(initial(),air(-30,0,0,0,0,0),5).bodyC(),"offline gap bounded");
        check(stage(35)==Stage.VERY_COLD && stage(39.5)==Stage.HYPERTHERMIA,"stage boundaries");
        check(run(initial(),new Exposure(8,0,0,0,0,0,false,8,1),60).bodyC()>run(initial(),air(8,0,0,0,0,0),60).bodyC(),"model hot meal reduces cooling (not wired to core)");
        check(tick(initial(),air(-20,0,0,0,0,0),1).trendCPerMinute()<0,"cooling trend");
        check(ClimateModel.geographicAir(-.8,0)<ClimateModel.geographicAir(.8,0),"legacy geographic helper gradient");
        check(ClimateModel.geographicAir(.3,100)<ClimateModel.geographicAir(.3,0),"legacy altitude helper");
        check(ClimateModel.biomeAir(.2,150,63)<ClimateModel.biomeAir(.2,63,63),"legacy biome altitude helper");
        check(ClimateModel.dayOffset(8000,false,false)>ClimateModel.dayOffset(20000,false,false),"legacy day helper");
        check(Math.abs(ClimateModel.dayOffset(20000,true,false))<Math.abs(ClimateModel.dayOffset(20000,false,false)),"legacy roof helper");
        check(ClimateModel.dayOffset(8000,false,true)<ClimateModel.dayOffset(8000,false,false),"legacy storm helper");
        check(ClimateModel.dayOffset(8000,true,true)==ClimateModel.dayOffset(8000,true,false),"legacy roof/storm helper");
        check(ClimateModel.water(-30,-30)==1 && ClimateModel.water(50,50)==32,"water temperature bounds");
        check(ClimateModel.skinWetness(0,true,false,0)==1,"immersion wets skin");
        check(ClimateModel.skinWetness(0,false,true,0)>.0,"rain wets skin");
        check(ClimateModel.skinWetness(1,false,false,20)<ClimateModel.skinWetness(1,false,false,0),"radiant warmth dries skin");
        check(ClimateModel.skinWetness(0,false,false,50)==0,"drying bounded at zero");
        check(ClimateModel.heatContribution(30,1)>ClimateModel.heatContribution(30,3),"radiant heat decays with distance");
        System.out.println("PASS total="+checks);
    }
}
