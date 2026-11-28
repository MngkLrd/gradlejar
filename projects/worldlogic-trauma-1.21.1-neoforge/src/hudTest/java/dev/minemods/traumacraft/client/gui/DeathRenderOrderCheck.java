package dev.minemods.traumacraft.client.gui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.spi.ToolProvider;

/** Structural regression of compiled client methods, without claiming GPU/client rendering. */
public final class DeathRenderOrderCheck {
    private static void require(boolean value,String detail){if(!value)throw new AssertionError(detail);}
    private static String disassemble(String name){
        var out=new StringWriter();
        var writer=new PrintWriter(out);
        int result=ToolProvider.findFirst("javap").orElseThrow().run(writer,writer,
            "-c","-p","-classpath",System.getProperty("java.class.path"),
            "dev.minemods.traumacraft.client.gui."+name);
        require(result==0,"javap failed: "+out);
        return out.toString();
    }
    private static String method(String code,String signature){
        int start=code.indexOf(signature);
        require(start>=0,"Missing compiled method: "+signature);
        int end=code.indexOf("\n\n",start);
        return code.substring(start,end<0?code.length():end);
    }
    private static int count(String text,String needle){int total=0,pos=0;while((pos=text.indexOf(needle,pos))>=0){total++;pos+=needle.length();}return total;}
    public static void verify(){
        String death=disassemble("TraumaDeathScreen");
        String render=method(death,"public void render(");
        String history=method(death,"private void renderHistory(");
        String buttons=method(death,"private void renderReportButtons(");
        require(count(death,"Method renderBackground:")==1,"one explicit death background in entire screen");
        require(!death.contains("Screen.render:(Lnet/minecraft/client/gui/GuiGraphics;"),"no inherited redraw/blur after report");
        int bg=render.indexOf("Method renderBackground:"),panel=render.indexOf("GuiGraphics.fill:");
        require(bg>=0&&panel>bg,"background before panel");
        require(render.contains("Method renderReportButtons:")&&history.contains("Method renderReportButtons:"),"both report pages render foreground buttons");
        require(count(buttons,"Button.render:")==2&&!buttons.contains("renderBackground"),"two foreground-only buttons");
        require(!history.contains("renderBackground"),"history never redraws background");
        String medical=method(disassemble("TraumaInventoryScreen"),"public void render(");
        require(count(medical,"Method renderBackground:")==1,"one custom medical background");
        require(medical.indexOf("InventoryScreen.render:")>=0&&medical.indexOf("InventoryScreen.render:")<medical.indexOf("Method renderBackground:"),"vanilla inventory branch separate from custom dashboard");
        System.out.println("PASS: compiled death/anatomy/history render order; no second background pass");
    }
}
