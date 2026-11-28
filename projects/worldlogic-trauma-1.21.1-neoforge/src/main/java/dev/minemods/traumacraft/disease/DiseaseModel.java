package dev.minemods.traumacraft.disease;

/** Game-calibrated syndromes, not pathogen identification or real-world clinical advice. All times are active player seconds. */
public final class DiseaseModel {
    public enum Kind { NONE, GASTROENTERITIS, ENTEROCOLITIS }
    public enum Stage { HEALTHY, INCUBATING, MILD, MODERATE, SEVERE }
    public record Physiology(double hydration,double electrolytes,String dehydration) {
        public static final Physiology UNKNOWN=new Physiology(100,50,"UNKNOWN");
        public double recoveryFactor(){
            double water=.45+.55*clamp(hydration/100,0,1);
            double salts=1-.5*clamp(Math.abs(electrolytes-50)/50,0,1);
            double stage=switch(dehydration==null?"UNKNOWN":dehydration){
                case "MILD"->.9;case "MODERATE"->.75;case "SEVERE"->.55;case "CRITICAL"->.4;default->1;
            };
            return Math.max(.18,water*salts*stage);
        }
    }
    public record State(Kind kind,double dose,double severity,int incubation,int elapsed,int support,int vomiting){
        public static final State HEALTHY=new State(Kind.NONE,0,0,0,0,0,0);
        public State{
            if(kind==null)kind=Kind.NONE;
            dose=clamp(dose,0,4);severity=clamp(severity,0,1);
            incubation=Math.max(0,Math.min(240,incubation));elapsed=Math.max(0,Math.min(1000000,elapsed));
            support=Math.max(0,Math.min(150,support));vomiting=Math.max(0,Math.min(5,vomiting));
            if(kind==Kind.NONE){severity=0;incubation=0;elapsed=0;support=0;vomiting=0;}
            else if(incubation>0){severity=0;elapsed=0;support=0;vomiting=0;}
        }
        public Stage stage(){
            if(kind==Kind.NONE)return Stage.HEALTHY;
            if(incubation>0)return Stage.INCUBATING;
            if(severity>=.8)return Stage.SEVERE;
            if(severity>=.5)return Stage.MODERATE;
            return Stage.MILD;
        }
        public boolean symptomatic(){return kind!=Kind.NONE&&incubation==0&&severity>0;}
    }
    public record Step(State state,boolean diarrhea,boolean vomit){}
    private DiseaseModel(){}
    static double clamp(double v,double lo,double hi){return Double.isFinite(v)?Math.max(lo,Math.min(hi,v)):lo;}
    public static State intake(State s,int volumeMb,double contaminationRisk,double salinityGkg){
        if(volumeMb<=0||!Double.isFinite(contaminationRisk)||contaminationRisk<0||contaminationRisk>1||!Double.isFinite(salinityGkg)||salinityGkg<0)return s;
        // Clean saline water does not itself create an infection. Hydration owns direct salt/water balance.
        double load=Math.min(1000,volumeMb)/250.0*Math.max(0,contaminationRisk-.10)
                *(1+.15*clamp(salinityGkg/35,0,1));
        if(load<=0)return s;
        double dose=clamp(s.dose()+load,0,4);
        Kind kind=dose>=1.55||s.kind()==Kind.ENTEROCOLITIS?Kind.ENTEROCOLITIS:Kind.GASTROENTERITIS;
        if(s.symptomatic())return new State(kind,dose,clamp(s.severity()+load*.18,0,1),0,s.elapsed(),s.support(),s.vomiting());
        if(s.incubation()>0)return new State(kind,dose,0,s.incubation(),0,0,0);
        if(dose<.35)return new State(Kind.NONE,dose,0,0,0,0,0);
        return new State(kind,dose,0,(int)Math.round(180-90*clamp(dose/2,0,1)),0,0,0);
    }
    public static double loss(State s){
        if(!s.symptomatic())return 0;
        return s.severity()*(s.kind()==Kind.ENTEROCOLITIS?.36:.24)+(s.vomiting()>0?.55:0);
    }
    public static boolean canTreat(State s){return s.symptomatic()&&s.support()==0;}
    public static State treat(State s){
        if(!canTreat(s))return s;
        return new State(s.kind(),s.dose(),s.severity(),0,s.elapsed(),150,s.vomiting());
    }
    public static Step second(State s,Physiology physiology,boolean resting){return second(s,physiology,resting,1);}
    public static Step second(State s,Physiology physiology,boolean resting,double resistance){
        if(s.kind()==Kind.NONE)return new Step(new State(Kind.NONE,Math.max(0,s.dose()-.003),0,0,0,0,0),false,false);
        if(s.incubation()>1)return new Step(new State(s.kind(),s.dose(),0,s.incubation()-1,0,0,0),false,false);
        if(s.incubation()==1){
            double severity=clamp(.24+s.dose()*.34,.25,1);
            boolean vomit=severity>=.5;
            return new Step(new State(s.kind(),s.dose(),severity,0,0,0,vomit?5:0),true,vomit);
        }
        if(!s.symptomatic())return new Step(State.HEALTHY,false,false);
        double speed=(s.kind()==Kind.ENTEROCOLITIS?.00145:.0022)
                *physiology.recoveryFactor()*clamp(resistance,.1,1)*(resting?1.25:1)*(s.support()>0?2.5:1);
        double severity=Math.max(0,s.severity()-speed);
        if(severity<.10)return new Step(State.HEALTHY,false,false);
        int elapsed=s.elapsed()+1;
        int diarrheaInterval=severity>=.8?22:severity>=.5?38:65;
        int vomitInterval=s.kind()==Kind.ENTEROCOLITIS?45:65;
        boolean diarrhea=elapsed%diarrheaInterval==0;
        boolean vomit=severity>=.5&&elapsed%vomitInterval==0;
        return new Step(new State(s.kind(),Math.max(0,s.dose()-.002),severity,0,elapsed,Math.max(0,s.support()-1),vomit?5:Math.max(0,s.vomiting()-1)),diarrhea,vomit);
    }
}
