package dev.fractal.cell;
public final class LatticeMath {
 public record Properties(int reinforcement,int conductivity,int light){}
 public static Properties evaluate(int[] types){
  int r=0,c=0,l=0;for(int n:types){if(n==1)r++;else if(n==2)c++;else if(n==3)l++;}
  return new Properties(Math.min(3,r),Math.min(3,c),Math.min(3,l));
 }
 public static int signal(int raw,int conductivity){return Math.min(Math.max(0,raw),Math.min(15,Math.max(0,conductivity)*5));}
 public static float miningFactor(int reinforcement){return 1f/(1f+Math.max(0,reinforcement)*.75f);}
 public static void main(String[] args){
  var p=evaluate(new int[]{1,1,2,2,2,3});
  if(p.reinforcement()!=2||p.conductivity()!=3||p.light()!=1)throw new AssertionError();
  if(signal(15,0)!=0||signal(15,1)!=5||signal(15,2)!=10||signal(15,3)!=15)throw new AssertionError();
  if(miningFactor(3)>=miningFactor(1)||miningFactor(0)!=1)throw new AssertionError();
  if(evaluate(new int[]{3,3,3,3,3,3}).light()!=3)throw new AssertionError();
  System.out.println("Lattice properties and signal invariants OK");
 }
}
