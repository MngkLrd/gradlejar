package dev.minemods.treechopper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Eight 2-pixel stages; only reachable combinations of four cuts are registered. */
public final class CutGeometry {
    static final int MAX_SUM=10;
    private static final List<int[]> PROFILES=new ArrayList<>();
    private static final int[] INDEX=new int[9*9*9*9];
    static{
        Arrays.fill(INDEX,-1);
        for(int n=0;n<=8;n++)for(int s=0;s<=8;s++)for(int w=0;w<=8;w++)for(int e=0;e<=8;e++){
            if(n+s+w+e>MAX_SUM)continue;
            INDEX[key(n,s,w,e)]=PROFILES.size();
            PROFILES.add(new int[]{n,s,w,e});
        }
    }
    private CutGeometry(){}
    private static int key(int n,int s,int w,int e){return ((n*9+s)*9+w)*9+e;}
    static int count(){return PROFILES.size();}
    static int[] depths(int profile){return PROFILES.get(profile).clone();}
    static int profile(int[] work){
        int[] d=Arrays.stream(work).map(ChopEffort::depth).toArray();
        // Non-severed work fits naturally. A final protected/held cut may overshoot;
        // trim only its visual profile, never the persisted work or felling calculation.
        while(Arrays.stream(d).sum()>MAX_SUM){
            int largest=0;for(int i=1;i<4;i++)if(d[i]>d[largest])largest=i;
            d[largest]--;
        }
        return INDEX[key(d[0],d[1],d[2],d[3])];
    }
    public static int shallow(int depth){return Math.max(0,depth-1);}
    public static boolean falls(int n,int s,int w,int e){return ChopEffort.severed(new int[]{125*n,125*s,125*w,125*e});}
    public static int remaining(int n,int s,int w,int e){return Math.max(0,8-n-s)*Math.max(0,8-w-e);}
}
