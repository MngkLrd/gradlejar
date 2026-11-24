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
        expect(QualityMath.merged(-1,4,100,1),100);
        expect(QualityMath.quantize(-100),0);
        expect(QualityMath.quantize(110),100);
        if(Math.abs(QualityMath.saturationMultiplier(70)-1f)>1e-6)throw new AssertionError("neutral");
        System.out.println("Hearthbound quality contract: passed");
    }
}
