package dev.minemods.treechopper;
final class ChopEffort {
    static final int COMPLETE=1000;
    private ChopEffort(){}
    private static double clamp(double x){return Double.isFinite(x)?Math.max(0,Math.min(1,x)):1;}
    static double power(double stamina,double primary,double support){
        double main=clamp(primary), other=clamp(support);
        if(main<=.05)return 0;
        return (.4+.6*clamp(stamina))*main*(.7+.3*other);
    }
    static int cooldown(double power){return (int)Math.ceil(15+10*(1-clamp(power)));}
    static int fellingWear(int upperLogs,boolean finalStrike){return Math.max(0,upperLogs)+(finalStrike?1:0);}
    static int workPerHit(double speed,double resistance,double power,double logs,double roll){
        if(power<=0)return 0;
        double mass=Math.sqrt(Math.max(4,logs)/4.0);
        return Math.max(1,(int)Math.round((100+25*Math.max(0,speed))*clamp(power)*(.9+.2*clamp(roll))/(mass*Math.max(.1,resistance))));
    }
    static int depth(int work){return work<=0?0:Math.min(4,Math.max(1,work/250));}
    /** Every face contributes to one trunk. Counter-cuts gain up to 25% efficiency. */
    static int progress(int[] w){
        int sum=w[0]+w[1]+w[2]+w[3];
        int opposed=Math.min(w[0],w[1])+Math.min(w[2],w[3]);
        return Math.min(COMPLETE,sum+Math.min(250,2*opposed));
    }
    static boolean severed(int[] w){return progress(w)>=COMPLETE;}
}
