package dev.minemods.treechopper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared immutable geometry, cached only for encountered depth tuples, never registered as states. */
public final class CutGeometry {
    static final int CACHE_LIMIT=512;
    private static final Map<Integer,Geometry> CACHE=new LinkedHashMap<>(64,.75f,true);
    record Box(int x0,int y0,int z0,int x1,int y1,int z1){}
    record Geometry(List<Box> boxes,VoxelShape shape){}
    private CutGeometry(){}
    static int key(int[] work){
        return ChopEffort.depth(work[0])|(ChopEffort.depth(work[1])<<4)|(ChopEffort.depth(work[2])<<8)|(ChopEffort.depth(work[3])<<12);
    }
    static int sanitize(int key){
        int result=0;
        for(int i=0;i<4;i++)result|=Math.min(8,(key>>>(4*i))&15)<<(4*i);
        return result;
    }
    static int depth(int key,int face){return (key>>>(4*face))&15;}
    static synchronized Geometry geometry(int raw){
        int key=sanitize(raw);
        Geometry g=CACHE.get(key);
        if(g==null){
            List<Box> boxes=new ArrayList<>(5);
            int n=depth(key,0),s=depth(key,1),w=depth(key,2),e=depth(key,3);
            boxes.add(new Box(0,0,0,16,4,16));
            band(boxes,4,6,shallow(n),shallow(s),shallow(w),shallow(e));
            band(boxes,6,10,n,s,w,e);
            band(boxes,10,12,shallow(n),shallow(s),shallow(w),shallow(e));
            boxes.add(new Box(0,12,0,16,16,16));
            VoxelShape shape=Shapes.empty();
            for(Box b:boxes)shape=Shapes.or(shape,Block.box(b.x0,b.y0,b.z0,b.x1,b.y1,b.z1));
            g=new Geometry(List.copyOf(boxes),shape.optimize());
            CACHE.put(key,g);
            if(CACHE.size()>CACHE_LIMIT)CACHE.remove(CACHE.keySet().iterator().next());
        }
        return g;
    }
    static synchronized int cacheSize(){return CACHE.size();}
    private static void band(List<Box> out,int bottom,int top,int n,int s,int w,int e){
        if(w+e<8&&n+s<8)out.add(new Box(w*2,bottom,n*2,16-e*2,top,16-s*2));
    }
    public static int shallow(int depth){return Math.max(0,depth-1);}
    public static boolean falls(int n,int s,int w,int e){return ChopEffort.severed(new int[]{125*n,125*s,125*w,125*e});}
    public static int remaining(int n,int s,int w,int e){return Math.max(0,8-n-s)*Math.max(0,8-w-e);}
}
