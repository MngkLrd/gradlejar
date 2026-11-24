package dev.minemods.riverweir.logic;

/** Deterministic low-cost motion, independent of game classes. */
public final class WeirStruggle {
    private WeirStruggle(){}
    public static int cycle(int seed){return 30+Math.floorMod(seed,15);}
    public static double strength(int age,int seed){
        if(age<0)return 0;
        int cycle=cycle(seed),segment=age/cycle,local=age%cycle;
        int hash=seed^(segment*0x9e3779b9);
        int burst=10+Math.floorMod(hash,9);
        if(local>=burst)return 0;
        double amplitude=.60+Math.floorMod(hash>>>8,41)*.01;
        return amplitude*Math.sin(Math.PI*(local+1)/(burst+1));
    }
    public static float escapeChance(int tier){
        return tier==0?.05f:tier==1?.025f:.01f;
    }
    /** One roll per entanglement, never per tick. If selected, use a late active burst. */
    public static int escapeTick(int tier,float roll,int duration,int seed){
        if(duration<40||roll>=escapeChance(tier))return -1;
        int from=duration*55/100,to=duration-8;
        int best=from;
        double max=-1;
        for(int i=from;i<=to;i++){
            double s=strength(i,seed);
            if(s>max){max=s;best=i;}
        }
        return best;
    }
}
