package dev.minemods.traumacraft.client.gui;

/** Standalone JDK regression; no Minecraft runtime or graphical-client claim. */
public final class HudStatusLayoutCheck {
    private static void require(boolean value,String detail){if(!value)throw new AssertionError(detail);}
    public static void main(String[] args) {
        int transitions=0;
        for(int mask=0;mask<16;mask++){
            int actual=HudStatusLayout.mask((mask&1)!=0,(mask&2)!=0,(mask&4)!=0?1:0,(mask&8)!=0?1:0);
            require(actual==mask,"mask "+mask);
            require(HudStatusLayout.visible(false,actual)==(mask!=0),"healthy visibility "+mask);
            require(HudStatusLayout.visible(true,actual),"injury visibility "+mask);
            int next=0;
            for(int i=0;i<4;i++){
                int row=HudStatusLayout.row(actual,i);
                require(row==(((mask&(1<<i))!=0)?next++:-1),"dense rows "+mask+":"+i);
            }
            for(int removed=0;removed<4;removed++){
                if((mask&(1<<removed))==0)continue;
                int after=mask&~(1<<removed), expected=0;
                require(HudStatusLayout.row(after,removed)==-1,"removed status");
                for(int i=0;i<4;i++)require(HudStatusLayout.row(after,i)==(((after&(1<<i))!=0)?expected++:-1),"repacked rows");
                transitions++;
            }
        }
        require(HudStatusLayout.mask(false,false,-1,-1)==0,"expired timers");
        require(HudStatusLayout.mask(false,false,1,1)==12,"last active timer tick");
        require(HudStatusLayout.row(15,-1)==-1&&HudStatusLayout.row(15,4)==-1,"invalid index");
        System.out.println("PASS: 16 combinations, "+transitions+" removal transitions, timer boundaries and visibility");
    }
}
