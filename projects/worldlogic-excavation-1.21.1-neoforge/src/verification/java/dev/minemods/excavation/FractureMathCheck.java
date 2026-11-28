package dev.minemods.excavation;
public final class FractureMathCheck {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static void run(int[] hits,boolean ore){
        var state=new FractureMath.State(0,0,0);int total=0;
        for(int i=0;i<9;i++){
            var plan=FractureMath.strike(state,hits[i%hits.length],i%6,ore);
            check(plan.work()>state.work(),"work");
            check(!ore||plan.units()>0,"each strike yields");
            check(plan.extracted()==total+plan.units(),"accounting");total+=plan.units();
            if(plan.complete()){check(total==(ore?9:0),"conservation");return;}
            check(NotchGeometry.get(plan.key()).remaining()<NotchGeometry.get(state.key()).remaining(),"geometry");
            state=new FractureMath.State(plan.work(),plan.extracted(),plan.key());
        }
        throw new AssertionError("bounded completion");
    }
    public static void main(String[] args){
        check(FractureMath.hits(6,3,true)==5,"iron");
        check(FractureMath.hits(8,3,true)==3,"diamond");
        check(FractureMath.hits(6,1.5f,false)<5,"stone");
        for(int a=2;a<=9;a++)for(int b=2;b<=9;b++)for(int c=2;c<=9;c++){
            run(new int[]{a,b,c},true);run(new int[]{a,b,c},false);
        }
        for(int side=0;side<6;side++)for(int depth=1;depth<=15;depth++){
            var g=NotchGeometry.get(depth<<(side*4));
            int volume=g.boxes().stream().mapToInt(x->(x.x1()-x.x0())*(x.y1()-x.y0())*(x.z1()-x.z0())).sum();
            check(volume==g.remaining()&&volume>0&&volume<4096,"box volume");
            check(!g.faces().isEmpty(),"surface mesh");
        }
        // Every vanilla duration across representative tiers/categories; exact integer schedule and hard cap.
        int timingCases=0;
        for(int ticks=1;ticks<=600;ticks++)for(float speed:new float[]{1,2,4,6,8,9,12,25,100})for(int kind=0;kind<3;kind++){
            boolean ore=kind==1,soil=kind==2;
            var timing=FractureMath.timing(1f/ticks,speed,ore?3f:soil?.5f:1.5f,ore,soil);
            check(timing.targetTicks()==(long)timing.vanillaTicks()*(ore?4:3)/2,"category target");
            check(timing.targetTicks()<=2L*timing.vanillaTicks(),"hard cap");
            int elapsed=0;
            for(int n=1;n<timing.hits();n++){
                int gap=timing.gapAfter(n);check(gap>=4,"manual cadence");elapsed+=gap;
            }
            check(timing.hits()==1||elapsed==timing.targetTicks(),"exact schedule");
            if(timing.hits()==1){
                var plan=FractureMath.strike(new FractureMath.State(0,0,0),1,2,ore);
                check(plan.complete()&&plan.units()==(ore?9:0),"fast single strike");
            }
            timingCases++;
        }
        System.out.println("EXCAVATION_TIMING_PASS: "+timingCases+" duration/tier/category cases");
        check(NotchGeometry.cacheSize()<=128,"bounded cache");
        System.out.println("EXCAVATION_MATH_PASS: 1024 mixed-tool sequences, 90 directional geometries, conserved reserve and bounded cache");
    }
}
