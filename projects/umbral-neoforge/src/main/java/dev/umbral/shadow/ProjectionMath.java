package dev.umbral.shadow;

import java.util.*;

/** Pure point-source projection, in emitter-local coordinates. Forward depth d > 0. */
public final class ProjectionMath {
    public record Box(double d0,double d1,double u0,double u1,double y0,double y1){}
    public record Span(double bottom,double top){}
    private ProjectionMath(){}
    public static List<Span> silhouette(List<Box> boxes,double d,double u){
        if(d<=0)return List.of();
        double slope=u/d;
        ArrayList<Span> spans=new ArrayList<>();
        for(Box b:boxes){
            double a=b.d0(),z=b.d1();
            if(d<=z+1e-6)continue;
            if(Math.abs(slope)<1e-9){
                if(b.u0()>0||b.u1()<0)continue;
            }else{
                double x=b.u0()/slope,y=b.u1()/slope;
                a=Math.max(a,Math.min(x,y));z=Math.min(z,Math.max(x,y));
                if(z-a<1e-7)continue;
            }
            if(a<=0||z<=0)continue;
            double lo=Math.min(b.y0()*d/a,b.y0()*d/z);
            double hi=Math.max(b.y1()*d/a,b.y1()*d/z);
            if(hi-lo>.001)spans.add(new Span(lo,hi));
        }
        spans.sort(Comparator.comparingDouble(Span::bottom));
        ArrayList<Span> merged=new ArrayList<>();
        for(Span span:spans){
            if(merged.isEmpty()){merged.add(span);continue;}
            int i=merged.size()-1;Span p=merged.get(i);
            if(span.bottom()<=p.top()+.02)merged.set(i,new Span(p.bottom(),Math.max(p.top(),span.top())));
            else merged.add(span);
        }
        return merged;
    }
    public static int mergeTile(int packed,int quadrant,int height){
        int factor=switch(quadrant){case 0->1;case 1->5;case 2->25;case 3->125;default->throw new IllegalArgumentException();};
        int old=packed/factor%5;
        return packed+(Math.max(old,height)-old)*factor;
    }
    public static int worldBlockY(int quarter){return Math.floorDiv(quarter-1,4);}
    public static int localHeight(int quarter){return Math.floorMod(quarter-1,4)+1;}
    public static void main(String[] args){
        Box flat=new Box(.5,1.5,-.5,.5,-1,0);
        List<Span> one=silhouette(List.of(flat),4.25,0);
        if(one.size()!=1||one.getFirst().top()!=0)throw new AssertionError("flat top");
        if(!silhouette(List.of(flat),4.25,6).isEmpty())throw new AssertionError("outside silhouette");
        if(!silhouette(List.of(flat),1.25,0).isEmpty())throw new AssertionError("inside caster");
        Box raised=new Box(1.5,2.5,-.5,.5,0,1);
        if(silhouette(List.of(raised),4.25,0).getFirst().top()<=1)throw new AssertionError("perspective");
        Box upper=new Box(1.5,2.5,-.5,.5,3,4);
        if(silhouette(List.of(raised,upper),4.25,0).size()!=2)throw new AssertionError("separate silhouettes");
        if(mergeTile(0,3,4)!=500||mergeTile(500,3,1)!=500)throw new AssertionError("shape mask");
        for(int q=-20;q<=20;q++)if(worldBlockY(q)*4+localHeight(q)!=q)throw new AssertionError("negative height");
        System.out.println("Projection geometry invariants OK");
    }
}
