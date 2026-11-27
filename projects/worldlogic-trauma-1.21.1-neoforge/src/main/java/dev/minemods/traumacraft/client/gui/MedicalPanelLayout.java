package dev.minemods.traumacraft.client.gui;

/** Integer GUI geometry shared with the layout regression. */
public record MedicalPanelLayout(int width,int height,int rowHeight,int bodyScale,int columnWidth) {
    public static final int FIRST_ROW=27, FOOTER=36;
    public static MedicalPanelLayout of(int w,int h){
        int pw=Math.max(148,Math.min(300,w-182)),ph=Math.min(232,h-12);
        int row=Math.min(23,(ph-FIRST_ROW-FOOTER-3)/7);
        int scale=pw>=238&&ph>=226?2:1;
        return new MedicalPanelLayout(pw,ph,row,scale,scale==2?108:64);
    }
    public boolean available(int w,int h){return w>=330&&h>=202;}
    public int rowBottom(){return FIRST_ROW+7*rowHeight;}
    public int bodyBottom(){return FIRST_ROW+AnatomyLayout.HEIGHT*bodyScale;}
    public int footerTop(){return height-FOOTER;}
    public int bodyX(){return (columnWidth-AnatomyLayout.WIDTH*bodyScale)/2;}
}
