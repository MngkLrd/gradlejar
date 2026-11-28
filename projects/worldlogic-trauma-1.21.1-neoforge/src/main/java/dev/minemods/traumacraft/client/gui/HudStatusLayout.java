package dev.minemods.traumacraft.client.gui;

/** Pure layout logic: one slot per active server-reported condition, in stable order. */
public final class HudStatusLayout {
    public static final int BLEEDING=1, FRACTURE=2, CONCUSSION=4, PAINKILLER=8;
    public static final int COLD=16, HEAT=32;
    public static final int COUNT=6, ICON_SIZE=16, STEP=18;
    private HudStatusLayout() {}
    public static int mask(boolean bleeding, boolean fracture, int concussionTicks, int painkillerTicks) {
        return (bleeding?BLEEDING:0)|(fracture?FRACTURE:0)
            |(concussionTicks>0?CONCUSSION:0)|(painkillerTicks>0?PAINKILLER:0);
    }
    /** Always show synchronized survival anatomy, including a healthy body. */
    public static boolean replacesVanillaHealth(boolean received,boolean alive,boolean creative,boolean spectator){
        return received&&alive&&!creative&&!spectator;
    }
    public static int withThermal(int mask,dev.minemods.traumacraft.integration.ThermalCondition condition){
        return (mask&15)|(condition.cold()?COLD:condition.hot()?HEAT:0);
    }
    public static int hudColumns(int mask){return Integer.bitCount(mask)>4?2:1;}
    /** Returns -1 for an inactive effect; no reserved or empty icon slots. */
    public static int row(int mask, int index) {
        if(index<0 || index>=COUNT)return -1;
        int bit=1<<index;
        return (mask&bit)==0?-1:Integer.bitCount(mask&(bit-1));
    }
}
