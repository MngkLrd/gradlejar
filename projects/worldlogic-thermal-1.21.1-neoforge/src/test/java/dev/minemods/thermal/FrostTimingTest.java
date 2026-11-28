package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class FrostTimingTest {
 private static int first(double local,double protection,double wet){
  double d=0;
  for(int s=1;s<=10000;s++){
   d=Adaptation.frostDose(d,local,protection,wet);
   if(d==Adaptation.FROST_THRESHOLD)return s;
  }
  return -1;
 }
 @Test void calibratedTimes(){
  assertEquals(1200,first(-5,0,0));assertEquals(600,first(-10,0,0));
  assertEquals(300,first(-15,0,0));assertEquals(180,first(-20,0,0));
  assertEquals(120,first(-28,0,0));assertEquals(75,first(-40,0,0));
  assertEquals(45,first(-55,0,0));assertEquals(45,first(-80,0,0));
  assertEquals(-1,first(-3,0,0));
 }
 @Test void insulationAndWetness(){
  assertEquals(-1,first(-15,1,0));assertTrue(first(-15,1,1)>300);
  assertTrue(first(-15,.4,0)>first(-15,.4,1));
  assertTrue(first(-20,0,0)>first(-28,0,0));
 }
 @Test void recoverBeforeInjuryAndMigrateOnce(){
  double d=0;for(int i=0;i<100;i++)d=Adaptation.frostDose(d,-15,0,0);
  assertEquals(40,d,1e-8);
  for(int i=0;i<20;i++)d=Adaptation.frostDose(d,10,0,0);
  assertEquals(0,d,1e-8);
  assertEquals(30,Adaptation.migrateFrostDose(120));
  assertEquals(0,Adaptation.migrateFrostDose(Double.NaN));
  assertEquals(30,Adaptation.migrateFrostDose(180));
  assertEquals(118,Adaptation.frostDose(120,20,0,0));
 }
}
