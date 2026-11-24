package com.kirill.screenshotmanager.core;
import java.awt.image.BufferedImage;
import java.util.List;
public final class Panorama{
    public static final float[] YAW={0,90,180,-90,0,0},PITCH={0,0,0,0,-90,90};
    private record V(double x,double y,double z){double dot(V a){return x*a.x+y*a.y+z*a.z;}}
    private static V forward(double yaw,double pitch){return new V(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));}
    public static BufferedImage stitch(List<BufferedImage> faces,int width){
        if(faces.size()!=6||width<128||width>4096||width%2!=0)throw new IllegalArgumentException("Invalid panorama");
        int height=width/2;var out=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        V[] f=new V[6],right=new V[6],up=new V[6];
        for(int i=0;i<6;i++){
            double y=Math.toRadians(YAW[i]),p=Math.toRadians(PITCH[i]);f[i]=forward(y,p);right[i]=new V(-Math.cos(y),0,-Math.sin(y));
            V a=right[i],b=f[i];up[i]=new V(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);
        }
        int[] row=new int[width];
        for(int y=0;y<height;y++){
            double pitch=Math.PI*((y+.5)/height-.5);
            for(int x=0;x<width;x++){
                V d=forward(2*Math.PI*((x+.5)/width-.5),pitch);
                int face=0;double best=d.dot(f[0]);
                for(int i=1;i<6;i++){double z=d.dot(f[i]);if(z>best){face=i;best=z;}}
                BufferedImage img=faces.get(face);
                int px=(int)((.5+.5*d.dot(right[face])/best)*img.getWidth()),py=(int)((.5-.5*d.dot(up[face])/best)*img.getHeight());
                row[x]=img.getRGB(Math.max(0,Math.min(img.getWidth()-1,px)),Math.max(0,Math.min(img.getHeight()-1,py)));
            }out.setRGB(0,y,width,1,row,0,width);
        }return out;
    }
}
