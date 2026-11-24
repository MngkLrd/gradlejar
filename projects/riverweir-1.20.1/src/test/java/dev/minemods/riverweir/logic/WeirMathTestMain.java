package dev.minemods.riverweir.logic;

import java.util.Arrays;
import dev.minemods.riverweir.menu.WeirLayout;

/** Called from Gradle check without external test dependencies. */
public final class WeirMathTestMain {
    private static void check(int got,int expected,String message){
        if(got!=expected)throw new AssertionError(message+" expected "+expected+" got "+got);
    }
    public static void main(String[] args){
        check(WeirMath.requiredRolls(new int[]{3,3,3,3},4,3),1,"4x3");
        check(WeirMath.requiredRolls(new int[]{3,3,3,3,6,6,6,6},4,3),3,"two depth groups");
        check(WeirMath.requiredRolls(new int[]{1,2},4,3),1,"small stream");
        check(WeirMath.requiredRolls(new int[]{1,3,7,1,2},4,3),4,"irregular riverbed");
        check(WeirMath.requiredRolls(new int[]{12,12,12,12},4,3),4,"maximum depth");
        check(WeirMath.requiredRolls(new int[]{5,5,5,5},4,5),1,"bronze 4x5");
        check(WeirMath.requiredRolls(new int[]{10,5,5,5,7},4,5),4,"bronze irregular depth");
        check(WeirMath.requiredRolls(new int[]{8,8,8,8},4,8),1,"iron 4x8");
        check(WeirMath.requiredRolls(new int[]{16,15,8,8,8},4,8),3,"iron deep floor");
        check(WeirMath.salvageRolls(8,0),8,"unused net refund");
        check(WeirMath.salvageRolls(8,10),6,"mild wear recovery");
        check(WeirMath.salvageRolls(8,70),4,"used net recovery");
        check(WeirMath.salvageRolls(8,100),0,"torn net no recovery");
        check(WeirMath.salvageRolls(0,0),0,"old installation compatibility");
        check(WeirLayout.rows(2),1,"jute rows");
        check(WeirLayout.rows(18),2,"bronze rows");
        check(WeirLayout.rows(27),3,"iron rows");
        check(WeirLayout.playerInventoryY(2),67,"jute player inventory offset");
        check(WeirLayout.playerInventoryY(18),85,"bronze player inventory offset");
        check(WeirLayout.playerInventoryY(27),103,"iron player inventory offset");
        check(WeirLayout.playerHotbarY(2),125,"jute hotbar offset");
        check(WeirLayout.playerHotbarY(18),143,"bronze hotbar offset");
        check(WeirLayout.playerHotbarY(27),161,"iron hotbar offset");
        check(WeirLayout.imageHeight(2),149,"jute GUI height");
        check(WeirLayout.imageHeight(18),167,"bronze GUI height");
        check(WeirLayout.imageHeight(27),185,"iron GUI height");
        check(WeirStruggle.escapeTick(0,.051f,140,71),-1,"jute does not escape above 5 percent");
        check(WeirStruggle.escapeTick(1,.026f,140,71),-1,"bronze does not escape above 2.5 percent");
        check(WeirStruggle.escapeTick(2,.011f,140,71),-1,"iron does not escape above 1 percent");
        for(int seed:new int[]{0,71,-12,Integer.MIN_VALUE,Integer.MAX_VALUE}){
            int cycle=WeirStruggle.cycle(seed);
            boolean burst=false,rest=false;
            for(int i=0;i<cycle*4;i++){
                double strength=WeirStruggle.strength(i,seed);
                if(strength<0||strength>1.001)throw new AssertionError("strength outside bounds");
                burst|=strength>.5;rest|=strength==0;
            }
            if(!burst||!rest)throw new AssertionError("missing motion or rest");
            for(int tier=0;tier<3;tier++){
                int escape=WeirStruggle.escapeTick(tier,0,140,seed);
                if(escape<77||escape>132||WeirStruggle.strength(escape,seed)<=0)
                    throw new AssertionError("escape outside late active burst");
            }
        }
        WeirTextureTest.run();
        System.out.println("River Weir geometry, GUI, struggle and texture regression checks passed.");
    }
}
