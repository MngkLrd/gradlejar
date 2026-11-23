package dev.minemods.riverweir.logic;

/** Pure, deterministic net-area arithmetic; no game classes needed. */
public final class WeirMath {
    private WeirMath() {}
    public static int requiredRolls(int[] depths,int panelWidth,int panelHeight){
        if(depths==null||depths.length==0||panelWidth<=0||panelHeight<=0)
            throw new IllegalArgumentException("Invalid net geometry");
        int rolls=0;
        for(int column=0;column<depths.length;column+=panelWidth){
            int deepest=0;
            for(int col=column;col<Math.min(depths.length,column+panelWidth);col++){
                if(depths[col]<1)throw new IllegalArgumentException("Invalid column depth");
                deepest=Math.max(deepest,depths[col]);
            }
            rolls+=(deepest+panelHeight-1)/panelHeight;
        }
        return rolls;
    }
    public static int salvageRolls(int installed,int wear){
        if(installed<=0)return 0; // historical v0.1/0.2 installations have no recorded material cost
        if(wear<=0)return installed;
        if(wear>=80)return 0;
        if(wear<40)return Math.max(1,(installed*3)/4);
        return installed/2;
    }
}
