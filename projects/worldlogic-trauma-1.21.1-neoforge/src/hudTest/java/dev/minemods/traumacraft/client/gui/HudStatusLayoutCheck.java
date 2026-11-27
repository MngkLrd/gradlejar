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
        // Anatomy geometry: all seven hit targets are independent and every rectangle is bounded.
        var zones=AnatomyLayout.ZONES;
        require(zones.size()==7,"seven body zones");
        require(zones.stream().map(AnatomyLayout.Zone::part).distinct().count()==7,"unique limb targets");
        for(var z:zones){
            require(z.x()>=0&&z.y()>=0&&z.x()+z.width()<=AnatomyLayout.WIDTH&&z.y()+z.height()<=AnatomyLayout.HEIGHT,"atlas bounds");
            require(AnatomyLayout.hit(100+z.x()+z.width()/2,200+z.y()+z.height()/2,100,200)==z.part(),"zone hit mapping");
            for(var other:zones)if(z!=other)require(z.x()+z.width()<=other.x()||other.x()+other.width()<=z.x()||z.y()+z.height()<=other.y()||other.y()+other.height()<=z.y(),"overlapping zones");
        }
        require(AnatomyLayout.hit(24,51,0,0)==null,"hip gap");
        require(AnatomyLayout.hit(23,65,0,0)==null,"leg gap");
        require(AnatomyLayout.inventoryBodyX(0)*2+AnatomyLayout.WIDTH==AnatomyLayout.COLUMN_WIDTH,"inventory centered");
        require(AnatomyLayout.INVENTORY_BODY_Y>24&&AnatomyLayout.INVENTORY_BODY_Y+AnatomyLayout.HEIGHT<AnatomyLayout.INVENTORY_ICONS_Y,"lower body without icon overlap");
        require(AnatomyLayout.INVENTORY_ICONS_Y+HudStatusLayout.STEP+HudStatusLayout.ICON_SIZE<=155,"icons do not overlap footer");
        for(int w:new int[]{320,330,400,480,640,960})for(int mask=0;mask<16;mask++){
            int x=AnatomyLayout.hudX(w,mask),y=AnatomyLayout.hudY(w,240,mask);
            require(x>=4&&y>=4&&x+AnatomyLayout.hudWidth(mask)<=w&&y+AnatomyLayout.HEIGHT<=240,"HUD screen bounds");
            require(x+AnatomyLayout.hudWidth(mask)<=w/2-91||y+AnatomyLayout.HEIGHT<=192,"hotbar clearance");
        }
        require(AnatomyLayout.palette(1,false,false)==0,"healthy green");
        require(AnatomyLayout.palette(.99f,false,false)==1,"minor actual damage");
        require(AnatomyLayout.palette(1,true,false)==1&&AnatomyLayout.palette(1,false,true)==1,"injury flags visible at full HP");
        require(AnatomyLayout.palette(.4f,false,false)==2&&AnatomyLayout.palette(.1f,false,false)==3&&AnatomyLayout.palette(0,false,false)==4,"injury ramp");
        for(var condition:dev.minemods.traumacraft.integration.ThermalCondition.values()){
            require(dev.minemods.traumacraft.integration.ThermalCondition.fromWire(condition.wireId)==condition,"thermal wire ID");
            for(int base=0;base<16;base++){
                int mask=HudStatusLayout.withThermal(base,condition);
                require((mask&15)==base,"medical status preservation");
                require(Integer.bitCount(mask)==Integer.bitCount(base)+(condition.stressed()?1:0),"one actual thermal glyph");
                require(HudStatusLayout.visible(false,mask)==(mask!=0),"thermal HUD visibility");
                int cols=HudStatusLayout.hudColumns(mask),rows=(Integer.bitCount(mask)+cols-1)/cols;
                if(rows>0)require(HudStatusLayout.ICON_SIZE+(rows-1)*HudStatusLayout.STEP<=AnatomyLayout.HEIGHT,"thermal glyph bounds");
                for(int w:new int[]{320,330,400,480,640,960}){
                    int x=AnatomyLayout.hudX(w,mask),y=AnatomyLayout.hudY(w,240,mask);
                    require(x>=4&&x+AnatomyLayout.hudWidth(mask)<=w&&y+AnatomyLayout.HEIGHT<=240,"thermal screen bounds");
                }
            }
        }
        require(dev.minemods.traumacraft.integration.ThermalCondition.fromWire(99)==dev.minemods.traumacraft.integration.ThermalCondition.NONE,"invalid wire ID");
        for(var state:dev.minemods.traumacraft.integration.ThermalCondition.values()){
            float c=ThermalVisualProfile.target(state,true),h=ThermalVisualProfile.target(state,false);
            require(c>=0&&c<=1&&h>=0&&h<=1&&c*h==0,"exclusive bounded overlay");
            require((c+h>0)==state.stressed(),"only active stress");
            float value=0;for(int i=0;i<100;i++)value=ThermalVisualProfile.approach(value,c+h,.05f);
            require(Math.abs(value-c-h)<.01f,"fade in");
            for(int i=0;i<100;i++)value=ThermalVisualProfile.approach(value,0,.05f);
            require(value==0,"fade out");
            require(ThermalVisualProfile.alpha(c+h,66)<=66,"bounded opacity");
        }
        System.out.println("PASS: 176 medical/thermal combinations and all 11 visual states, fading and alpha");
        System.out.println("PASS: seven independent zones, centered lower inventory, 96 HUD layouts, injury palette and status bounds");
        System.out.println("PASS: 16 combinations, "+transitions+" removal transitions, timer boundaries and visibility");
    }
}
