package dev.mngklrd.hearthbound;
public final class QualityContractTest {
    private static void expect(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
    public static void main(String[] args){
        expect(QualityMath.finish(90,-1),90); // all inputs raw
        expect(QualityMath.finish(90,80),90); // raw inputs excluded upstream
        expect(QualityMath.finish(90,0),70); // 0 is a real previous quality
        expect(QualityMath.finish(100,-1),100);
        expect(QualityMath.heating(199,200),-1);
        expect(QualityMath.heating(200,200),100);
        expect(QualityMath.heating(Float.NaN,200),-1);
        expect(QualityMath.merged(80,4,100,4),90);
        expect(QualityMath.merged(-1,4,100,1),75);
        expect(QualityMath.batches(6,6000,new int[]{6,4,5,0,0}),4);
        expect(QualityMath.batches(6,2000,new int[]{6,4,5}),2);
        expect(QualityMath.batches(6,999,new int[]{6,4,5}),0);
        expect(QualityMath.batches(3,3000,new int[]{0,0,0}),0);
        expect(QualityMath.batches(3,3000,new int[]{8,8,8}),3);
        float perTick=20;for(int i=0;i<20;i++)perTick=QualityMath.advanceHeat(perTick,500,.012f,1);
        if(Math.abs(perTick-QualityMath.advanceHeat(20,500,.012f,20))>.01f)throw new AssertionError("analytic heat");
        if(!Float.isFinite(QualityMath.advanceHeat(Float.NaN,400,.012f,20)))throw new AssertionError("finite heat");
        expect(QualityMath.quantize(-100),0);
        expect(QualityMath.quantize(110),100);
        if(Math.abs(QualityMath.saturationMultiplier(70)-1f)>1e-6)throw new AssertionError("neutral");
        expect(QualityMath.stars(95),5);
        expect(QualityMath.stars(80),4);
        expect(QualityMath.stars(59),2);
        expect(QualityMath.stars(19),0);
        expect(Math.round(QualityMath.soupUnder(600,1200)),40);
        expect(Math.round(QualityMath.soupUnder(1200,1200)),0);
        expect(Math.round(QualityMath.soupOver(1500,1200)),0);
        expect(Math.round(QualityMath.soupOver(2100,1200)),28);
        expect(Math.round(QualityMath.soupHeat(600,1200)),8);
        System.out.println("Hearthbound quality contract: passed");
    }
}
