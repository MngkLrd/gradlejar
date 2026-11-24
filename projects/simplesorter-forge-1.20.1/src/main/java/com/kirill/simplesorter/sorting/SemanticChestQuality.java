package com.kirill.simplesorter.sorting;

import java.util.*;

/** One truthful label/quality model for both the renderer and the sort report. */
public final class SemanticChestQuality {
    private SemanticChestQuality(){}
    public record Result(String label,int score,int representative,int occupied){
        public boolean mixed(){return "mixed".equals(label);}
    }
    public static Result evaluate(List<SemanticItemProfile> profiles){
        int n=profiles.size();if(n==0)return new Result("",100,-1,0);
        SemanticItemProfile first=profiles.get(0);
        boolean sameGroup=true,sameMaterial=first.hasMaterial(),sameFamily=true,sameId=true,samePurpose=true;
        long sum=0;long[] affinity=new long[n];
        for(int i=0;i<n;i++){
            var p=profiles.get(i);
            sameGroup&=first.group().equals(p.group());sameFamily&=first.legacyFamily().equals(p.legacyFamily());
            sameMaterial&=p.hasMaterial()&&first.material().equals(p.material());
            sameId&=first.itemId().equals(p.itemId());samePurpose&=first.purpose().equals(p.purpose());
            for(int k=0;k<i;k++){
                int a=p.affinity(profiles.get(k));
                // Protected items are not semantically unrelated to themselves.
                if(!p.moveAllowed()&&!profiles.get(k).moveAllowed()&&p.itemId().equals(profiles.get(k).itemId()))a=100;
                sum+=a;affinity[i]+=a;affinity[k]+=a;
            }
        }
        int score=n==1?100:(int)Math.round(2.0*sum/(n*(double)(n-1)));
        String label;
        if(!sameGroup)label="mixed";
        else if("unknown".equals(first.group()))label=sameId?"item/"+first.itemId():"mixed";
        else if("equipment".equals(first.group())&&!samePurpose)label="equipment";
        else if(sameMaterial)label="material/"+first.material();
        else if(sameFamily)label=first.legacyFamily();
        else label="group/"+first.group();
        // Keep the existing label packet and NBT length contract.
        if(label.length()>48)label=sameFamily?first.legacyFamily():"group/"+first.group();
        if(label.length()>48)label="misc";
        int rep=0;
        for(int i=1;i<n;i++){
            if(affinity[i]>affinity[rep]||(affinity[i]==affinity[rep]
                    &&profiles.get(i).itemId().compareTo(profiles.get(rep).itemId())<0))rep=i;
        }
        return new Result(label,score,"mixed".equals(label)?-1:rep,n);
    }
}
