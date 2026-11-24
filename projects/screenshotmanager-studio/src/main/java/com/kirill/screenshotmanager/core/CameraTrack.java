package com.kirill.screenshotmanager.core;
import java.util.List;

public final class CameraTrack {
    public record Pose(double x,double y,double z,float yaw,float pitch){}
    private final List<Pose> poses;
    public CameraTrack(List<Pose> poses){
        if(poses.size()<2||poses.size()>12)throw new IllegalArgumentException("2-12 camera points");
        this.poses=List.copyOf(poses);
    }
    private static double curve(double a,double b,double c,double d,double t){
        double v=.5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
        return Math.max(Math.min(b,c),Math.min(Math.max(b,c),v));
    }
    private static double angle(double from,double to){double v=(to-from)%360;return from+(v>180?v-360:v<-180?v+360:v);}
    public Pose sample(double fraction){
        double f=Math.max(0,Math.min(1,fraction));
        // Ease into and out of the path, then interpolate each segment.
        f=f*f*(3-2*f);
        double u=f*(poses.size()-1);
        int i=Math.min(poses.size()-2,(int)u);double t=u-i;
        Pose a=poses.get(Math.max(0,i-1)),b=poses.get(i),c=poses.get(i+1),d=poses.get(Math.min(poses.size()-1,i+2));
        double ya=angle(b.yaw,a.yaw),yc=angle(b.yaw,c.yaw),yd=angle(yc,d.yaw);
        return new Pose(curve(a.x,b.x,c.x,d.x,t),curve(a.y,b.y,c.y,d.y,t),curve(a.z,b.z,c.z,d.z,t),
                (float)curve(ya,b.yaw,yc,yd,t),(float)curve(a.pitch,b.pitch,c.pitch,d.pitch,t));
    }
}
