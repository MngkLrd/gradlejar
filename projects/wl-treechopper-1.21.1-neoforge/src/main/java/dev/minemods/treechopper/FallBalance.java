package dev.minemods.treechopper;
import net.minecraft.core.Direction;

/** Pure, deterministic felling direction. Face order is the persisted CutProgress order. */
final class FallBalance {
    static final Direction[] FACES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private FallBalance(){}
    // A guide is recognized only when an actual strike starts a shallow counter-cut
    // opposite an already deep cut. Merely switching sides is not enough.
    static int guideForStrike(int[] before,int struck,int previous){
        int opposite=struck^1;
        if(validGuide(before,previous))return previous;
        if(before[opposite]>=500&&before[opposite]<1000&&before[struck]<=250
            &&before[struck]<before[opposite]
            &&before[struck^2]<=250&&before[struck^3]<=250)return opposite;
        return -1;
    }
    static boolean validGuide(int[] work,int face){
        return face>=0&&face<4&&work[face]>=500&&work[face^1]>0&&work[face^1]<=250
            &&work[face^2]<=250&&work[face^3]<=250;
    }
    static int counterWork(int[] work,int struck,int guide,int proposed){
        if(guide<0||struck!=(guide^1))return proposed;
        int target=Math.max(work[struck]+1,750-work[guide]);
        return Math.min(proposed,Math.min(250,target));
    }
    static double[] cutVector(int[] work,int guide){
        if(validGuide(work,guide)){
            Direction d=FACES[guide];return new double[]{2.8*d.getStepX(),2.8*d.getStepZ()};
        }
        double total=0;for(int w:work)total+=w;
        if(total<=0)return new double[]{0,0};
        return new double[]{(work[3]-work[2])/total,(work[1]-work[0])/total};
    }
    static Direction choose(double cutX,double cutZ,double centerX,double centerZ,double leanX,double leanZ,Direction fallback){
        // Horizontal center-of-mass offset is torque per unit mass (gravity cancels);
        // upper vertical logs additionally estimate the pre-existing inclination.
        double x=cutX+.95*centerX+.30*leanX,z=cutZ+.95*centerZ+.30*leanZ;
        Direction best=fallback;
        double max=x*best.getStepX()+z*best.getStepZ();
        for(Direction face:FACES){
            double score=x*face.getStepX()+z*face.getStepZ();
            if(score>max+1e-8){max=score;best=face;}
        }
        return best;
    }
    static double mass(boolean log,Direction.Axis axis){
        return log?(axis==Direction.Axis.Y?1.0:.9):.12;
    }
}
