package dev.minemods.treechopper;
final class ChopEffort {
    private ChopEffort(){}
    private static double clamp(double x){return Double.isFinite(x)?Math.max(0,Math.min(1,x)):1;}
    static double power(double stamina,double primary,double support){
        double main=clamp(primary), other=clamp(support);
        if(main<=.05)return 0;
        return (.4+.6*clamp(stamina))*main*(.7+.3*other);
    }
    static int cooldown(double power){return (int)Math.ceil(7+10*(1-clamp(power)));}
    static int fellingWear(int upperLogs,boolean finalStrike){return Math.max(0,upperLogs)+(finalStrike?1:0);}
    static int depthIncrement(int before,double speed,double resistance,double power,double roll){
        if(power<=0)return 0;
        if(before==0)return 1;
        double quality=clamp(power);
        // Each effective strike visibly deepens the notch; strength changes the chance of a double increment.
        double work=Math.max(1,Math.min(3,.45+.25*Math.max(0,speed)*quality/Math.max(.1,resistance)));
        int whole=(int)Math.floor(work);
        return whole+(roll>1-(work-whole)?1:0);
    }
}
