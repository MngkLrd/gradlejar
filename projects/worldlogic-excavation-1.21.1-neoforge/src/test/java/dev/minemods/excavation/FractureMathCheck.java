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
        check(NotchGeometry.cacheSize()<=128,"bounded cache");
        System.out.println("EXCAVATION_MATH_PASS: 1024 mixed-tool sequences, 90 directional geometries, conserved reserve and bounded cache");
    }
}
