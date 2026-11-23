package dev.minemods.riverweir.logic;

import java.util.Arrays;

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
        check(WeirMath.salvageRolls(8,0),8,"unused net refund");
        check(WeirMath.salvageRolls(8,10),6,"mild wear recovery");
        check(WeirMath.salvageRolls(8,70),4,"used net recovery");
        check(WeirMath.salvageRolls(8,100),0,"torn net no recovery");
        check(WeirMath.salvageRolls(0,0),0,"old installation compatibility");
        System.out.println("River Weir geometry regression checks passed.");
    }
}
