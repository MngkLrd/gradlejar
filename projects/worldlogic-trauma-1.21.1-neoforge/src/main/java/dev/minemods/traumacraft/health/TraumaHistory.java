package dev.minemods.traumacraft.health;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Recent observed medical changes, not a reconstruction of unknown causes. Server game ticks. */
public final class TraumaHistory {
    public static final int LIMIT=12, WINDOW=2400;
    public enum Kind { IMPACT, INTERNAL_DECLINE, EXTERNAL_BLEED, SHOCK, UNCONSCIOUS, WAKING }
    public record Entry(int ageSeconds,Kind kind,DeathReport.Origin origin,int regions,int organs,float hpLoss,float bloodLoss,int shock){}
    public record Snapshot(float[] hp,float[] organs,float blood,int shock,boolean unconscious){
        public static Snapshot of(BodyState b){
            float[] h=new float[BodyPart.values().length],o=new float[OrganState.Organ.values().length];
            for(var p:BodyPart.values())h[p.ordinal()]=b.health(p);
            for(var organ:OrganState.Organ.values())o[organ.ordinal()]=b.organs().integrity(organ);
            return new Snapshot(h,o,b.bloodMl(),b.shockStage(),b.organs().unconscious());
        }
    }
    private record Timed(int tick,Entry entry){}
    private final ArrayDeque<Timed> events=new ArrayDeque<>();
    private void prune(int tick){while(!events.isEmpty()&&(tick<events.peekFirst().tick()||(long)tick-events.peekFirst().tick()>WINDOW))events.removeFirst();}
    private void add(int tick,Kind kind,DeathReport.Origin origin,int regions,int organs,float hp,float blood,int shock){
        prune(tick);
        if(events.size()==LIMIT)events.removeFirst();
        events.addLast(new Timed(tick,new Entry(0,kind,origin,regions,organs,hp,blood,shock)));
    }
    public void observe(int tick,Kind kind,DeathReport.Origin origin,Snapshot before,BodyState b){
        var after=Snapshot.of(b);int regions=0,organs=0;float hp=0;
        for(var p:BodyPart.values()){
            int i=p.ordinal();float delta=before.hp()[i]-after.hp()[i];if(delta>.0001f){regions|=1<<i;hp+=delta;}
        }
        for(var o:OrganState.Organ.values()){
            int i=o.ordinal();if(before.organs()[i]-after.organs()[i]>.0001f)organs|=1<<i;
        }
        float blood=Math.max(0,before.blood()-after.blood());
        boolean bloodBand=(int)(before.blood()/500)!=(int)(after.blood()/500);
        boolean shockChanged=before.shock()!=after.shock();
        boolean consciousChanged=before.unconscious()!=after.unconscious();
        if(kind==Kind.IMPACT?(regions!=0||organs!=0||blood>0):
            bloodBand||b.vitalDestroyed()||b.fatalBloodLoss()||(!consciousChanged&&!shockChanged&&organs!=0&&(tick%100==0))){
            add(tick,kind,origin,regions,organs,hp,blood,after.shock());
        }
        if(shockChanged)add(tick,Kind.SHOCK,DeathReport.Origin.SYSTEMIC,0,0,0,0,after.shock());
        if(consciousChanged)add(tick,after.unconscious()?Kind.UNCONSCIOUS:Kind.WAKING,DeathReport.Origin.SYSTEMIC,0,0,0,0,after.shock());
    }
    public List<Entry> snapshot(int tick){
        prune(tick);List<Entry> result=new ArrayList<>(events.size());
        for(var t:events){var e=t.entry();result.add(new Entry((tick-t.tick())/20,e.kind(),e.origin(),e.regions(),e.organs(),e.hpLoss(),e.bloodLoss(),e.shock()));}
        return List.copyOf(result);
    }
}
