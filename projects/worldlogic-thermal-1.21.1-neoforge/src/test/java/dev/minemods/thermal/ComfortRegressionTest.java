package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class ComfortRegressionTest{
 @Test void survivalAtZero(){
  var start=ThermalModel.initial();
  var bare=new ThermalModel.Exposure(0,0,0,0,0,0,false,0,0);
  assertTrue(ThermalModel.tick(start,bare,1).targetC()>35);
  var fire=new ThermalModel.Exposure(0,ClimateComfort.capRadiant(0,ClimateComfort.heat(60,2)),.3,0,0,0,false,0,0);
  assertTrue(ThermalModel.tick(start,fire,1).targetC()>=36.6);
  var cold=new ThermalModel.State(34,0,34,ThermalModel.Stage.HYPOTHERMIA,0);
  assertTrue(ThermalModel.tick(cold,fire,1).bodyC()>34);
  var grass=new ThermalModel.Exposure(0,0,.2,0,1,0,false,0,0);
  assertTrue(ThermalModel.tick(start,grass,1).targetC()>36.6);
 }
 @Test void coolingAndLimits(){
  assertEquals(0,ClimateComfort.cooling(0,10,1,true,true));assertEquals(0,ClimateComfort.cooling(40,10,1,true,false));
  assertTrue(ClimateComfort.cooling(40,20,1,true,true)>ClimateComfort.cooling(40,90,1,true,true));
  assertTrue(ClimateComfort.cooling(40,20,1,true,true)>ClimateComfort.cooling(40,20,1,false,true));
  var start=ThermalModel.initial();
  var bare=new ThermalModel.Exposure(38,0,0,0,0,0,false,0,0);
  var dressed=new ThermalModel.Exposure(38,0,0,0,.04,0,false,0,0,7,.9);
  assertTrue(ThermalModel.tick(start,dressed,1).targetC()<ThermalModel.tick(start,bare,1).targetC());
 }
}
