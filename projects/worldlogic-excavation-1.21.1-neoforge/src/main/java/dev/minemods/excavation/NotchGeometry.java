package dev.minemods.excavation;
import java.util.*;
/** Same 16px voxel solid drives collision and visible boundary rectangles.
 * Face numbering deliberately matches DOWN, UP, NORTH, SOUTH, WEST, EAST. */
public final class NotchGeometry {
    public record Box(int x0,int y0,int z0,int x1,int y1,int z1){}
    public record Face(int side,Box box,boolean exterior){}
    public record Geometry(List<Box> boxes,List<Face> faces,int remaining){}
    private static final int LIMIT=128;
    private static final Map<Integer,Geometry> CACHE=new LinkedHashMap<>(16,.75f,true);
    private NotchGeometry(){}
    public static synchronized Geometry get(int raw){
        int key=FractureMath.sanitize(raw);
        Geometry g=CACHE.get(key);if(g!=null)return g;
        boolean[] solid=new boolean[4096];int remaining=0;
        for(int z=0;z<16;z++)for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            boolean removed=cut(FractureMath.depth(key,0),x,z,y)||cut(FractureMath.depth(key,1),x,z,15-y)
                ||cut(FractureMath.depth(key,2),x,y,z)||cut(FractureMath.depth(key,3),x,y,15-z)
                ||cut(FractureMath.depth(key,4),z,y,x)||cut(FractureMath.depth(key,5),z,y,15-x);
            solid[index(x,y,z)]=!removed;if(!removed)remaining++;
        }
        List<Box> boxes=pack(solid);
        List<Face> faces=new ArrayList<>();
        for(int side=0;side<6;side++)for(int plane=0;plane<16;plane++){
            boolean[] mask=new boolean[256];
            for(int v=0;v<16;v++)for(int u=0;u<16;u++){
                int x=side<2?u:side<4?u:plane;
                int y=side<2?plane:v;
                int z=side<2?v:side<4?plane:u;
                if(!at(solid,x,y,z))continue;
                int nx=x+(side==4?-1:side==5?1:0),ny=y+(side==0?-1:side==1?1:0),nz=z+(side==2?-1:side==3?1:0);
                mask[v*16+u]=!at(solid,nx,ny,nz);
            }
            final int face=side,p=plane;
            for(int v=0;v<16;v++)for(int u=0;u<16;u++)if(mask[v*16+u]){
                int u1=u+1,v1=v+1;
                while(u1<16&&mask[v*16+u1])u1++;
                rows:while(v1<16){for(int a=u;a<u1;a++)if(!mask[v1*16+a])break rows;v1++;}
                for(int b=v;b<v1;b++)for(int a=u;a<u1;a++)mask[b*16+a]=false;
                int boundary=p+((face&1)==1?1:0);
                Box b=face<2?new Box(u,boundary,v,u1,boundary,v1):face<4?new Box(u,v,boundary,u1,v1,boundary):new Box(boundary,v,u,boundary,v1,u1);
                faces.add(new Face(face,b,boundary==0||boundary==16));
            }
        }
        g=new Geometry(List.copyOf(boxes),List.copyOf(faces),remaining);
        CACHE.put(key,g);if(CACHE.size()>LIMIT)CACHE.remove(CACHE.keySet().iterator().next());
        return g;
    }
    static synchronized int cacheSize(){return CACHE.size();}
    private static boolean cut(int depth,int u,int v,int distance){
        if(depth==0||distance>=depth)return false;
        int radius=Math.max(1,Math.min(6,2+depth/3)-distance/3);
        int a=Math.abs(u-7),b=Math.abs(v-8);
        return a<=radius&&b<=radius&&a+b<=radius*2-1;
    }
    private static int index(int x,int y,int z){return (z*16+y)*16+x;}
    private static boolean at(boolean[] a,int x,int y,int z){return x>=0&&y>=0&&z>=0&&x<16&&y<16&&z<16&&a[index(x,y,z)];}
    private static List<Box> pack(boolean[] solid){
        boolean[] left=solid.clone();List<Box> boxes=new ArrayList<>();
        for(int z=0;z<16;z++)for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(left[index(x,y,z)]){
            int x1=x+1,y1=y+1,z1=z+1;
            while(x1<16&&left[index(x1,y,z)])x1++;
            rows:while(y1<16){for(int a=x;a<x1;a++)if(!left[index(a,y1,z)])break rows;y1++;}
            layers:while(z1<16){for(int b=y;b<y1;b++)for(int a=x;a<x1;a++)if(!left[index(a,b,z1)])break layers;z1++;}
            for(int c=z;c<z1;c++)for(int b=y;b<y1;b++)for(int a=x;a<x1;a++)left[index(a,b,c)]=false;
            boxes.add(new Box(x,y,z,x1,y1,z1));
        }
        return boxes;
    }
}
