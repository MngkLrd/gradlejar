package dev.minemods.excavation;
/** Discrete game work and conserved mineral units, not an SI fracture threshold. */
public final class FractureMath {
    public static final int COMPLETE=180, ORE_UNITS=9, MAX_DEPTH=15;
    private FractureMath(){}
    public record State(int work,int extracted,int key){
        public State{
            if(work<0||work>=COMPLETE||extracted<0||extracted>=ORE_UNITS||key!=sanitize(key))
                throw new IllegalArgumentException("Invalid fracture state");
        }
    }
    public record Plan(int work,int extracted,int key,int units,boolean complete){}
    public static int hits(float speed,float hardness,boolean ore){
        if(!Float.isFinite(speed)||speed<=0||!Float.isFinite(hardness)||hardness<0)throw new IllegalArgumentException("Invalid tool or hardness");
        int base=speed>=8?3:speed>=6?5:speed>=4?7:9;
        int extra=(int)Math.min(9,Math.ceil(Math.max(0,hardness-(ore?3f:1.5f))/2));
        return Math.max(2,Math.min(9,base-(ore?0:2)+extra));
    }
    /**
     * Budget measured in elapsed game ticks between first and last accepted strike.
     * Vanilla progress is sampled from the actual source block and the player.
     * 1.5x for soil/rock, 2x for ore; floor at the tick boundary.
     * A 4-tick minimum normal manual cadence is about five clicks/sec.
     * Very fast vanilla blocks use fewer clicks, including single-strike mining.
     */
    public record Timing(int vanillaTicks,int targetTicks,int hits){
        public Timing{
            if(vanillaTicks<1||targetTicks<1||hits<1||hits>9)throw new IllegalArgumentException("Invalid timing");
        }
        public int gapAfter(int acceptedHit){
            if(hits<=1||acceptedHit>=hits)return 1;
            int k=Math.max(1,acceptedHit),den=hits-1;
            return (int)(((long)targetTicks*k)/den-((long)targetTicks*(k-1))/den);
        }
    }
    public static Timing timing(float progress,float toolSpeed,float hardness,boolean ore,boolean soil){
        if(!Float.isFinite(progress)||progress<=0||!Float.isFinite(toolSpeed)||toolSpeed<=0)
            throw new IllegalArgumentException("Invalid vanilla progress");
        int vanilla=(int)Math.max(1,Math.min(Integer.MAX_VALUE/2,Math.ceil(1.0/progress-1e-6)));
        int budget=(int)((long)vanilla*(ore?4:3)/2);
        int preferred=soil?(toolSpeed>=6?2:toolSpeed>=4?3:4):hits(toolSpeed,hardness,ore);
        int count=Math.max(1,Math.min(preferred,1+budget/4));
        return new Timing(vanilla,budget,count);
    }
    public static int acceptedIndex(State old,Timing t){
        int step=(COMPLETE+t.hits()-1)/t.hits();
        return Math.min(t.hits(),(old.work()+step-1)/step+1);
    }
    public static int depth(int key,int side){return (key>>>(side*4))&15;}
    public static int sanitize(int key){return key&0xFFFFFF;}
    public static Plan strike(State old,int hits,int face,boolean ore){
        if(hits<1||hits>9||face<0||face>5||(!ore&&old.extracted()!=0))
            throw new IllegalArgumentException("Invalid strike");
        int step=(COMPLETE+hits-1)/hits;
        int work=Math.min(COMPLETE,old.work()+step), remaining=ORE_UNITS-old.extracted();
        boolean done=work==COMPLETE||(ore&&remaining==1);
        int units=0;
        if(ore){
            int future=(COMPLETE-work+step-1)/step;
            units=done?remaining:((hits<=5&&remaining-4>=Math.max(1,future))?4:1);
            if(units>=remaining){units=remaining;done=true;}
        }
        if(done)work=COMPLETE;
        int increment=step>=60?3:step>=36?2:1;
        int before=depth(old.key(),face);
        int after=Math.min(MAX_DEPTH,before+increment);
        int key=(old.key()&~(15<<(face*4)))|(after<<(face*4));
        return new Plan(work,old.extracted()+units,key,units,done);
    }
}
