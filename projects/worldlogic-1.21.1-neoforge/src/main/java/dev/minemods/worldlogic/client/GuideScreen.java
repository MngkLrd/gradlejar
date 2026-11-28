package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.GuideBook;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
/** Local encyclopedia. Models are rendered through vanilla's real baked item renderer. */
public final class GuideScreen extends Screen{
 private final Screen parent;private List<GuideBook.Chapter> chapters=List.of();
 private int chapter,page,chapterOffset,scroll;private boolean russian;
 private int left,top,panelW,panelH,navW,contentX,contentW,textY,visibleLines;
 private List<FormattedCharSequence> lines=List.of();
 private final List<Preview> previews=new ArrayList<>();private List<net.minecraft.resources.ResourceLocation> icons=List.of();
 private record Preview(ItemStack stack,int x,int y,int size){}
 public GuideScreen(Screen parent){super(Component.literal("WorldLogic"));this.parent=parent;}
 private String label(String ru,String en){return russian?ru:en;}
 private Component lit(String s){return Component.literal(s);}
 private void button(String text,int x,int y,int w,Runnable action){addRenderableWidget(Button.builder(lit(font.plainSubstrByWidth(text,Math.max(1,w-8))),b->action.run()).bounds(x,y,w,20).build());}
 private void load(){
  var mc=Minecraft.getInstance();var loaded=new TreeMap<String,GuideBook.Chapter>();
  for(var entry:mc.getResourceManager().listResources("worldlogic_guide",id->id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()){
   try(var reader=entry.getValue().openAsReader()){
    var c=GuideBook.read(reader);String key=c.id();
    // Built-in chapters have priority over duplicate IDs from a different namespace.
    if(!loaded.containsKey(key)||entry.getKey().getNamespace().equals("worldlogic"))loaded.put(key,c);
   }catch(Exception e){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Invalid guide resource {}",entry.getKey(),e);}
  }
  chapters=GuideBook.catalog(loaded.values(),ModList.get()::isLoaded);
 }
 private GuideBook.Chapter current(){return chapters.get(chapter);}
 private GuideBook.Page article(){return current().pages().get(page);}
 void select(int c,int p){chapter=c;page=p;chapterOffset=c;scroll=0;Minecraft.getInstance().setScreen(this);}
 private void changeChapter(int value){chapter=value;page=0;scroll=0;rebuildWidgets();}
 private void changePage(int delta){if(chapters.isEmpty())return;page=Math.max(0,Math.min(current().pages().size()-1,page+delta));scroll=0;rebuildWidgets();}
 @Override protected void init(){
  russian=Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
  load();chapter=Math.min(chapter,Math.max(0,chapters.size()-1));
  int usable=dev.minemods.worldlogic.api.client.WorldLogicTabs.contentWidth(width);
  panelW=Math.min(620,usable-8);panelH=Math.min(360,height-8);left=(usable-panelW)/2;top=(height-panelH)/2;
  navW=Math.max(78,Math.min(140,panelW/3));contentX=left+navW+12;contentW=panelW-navW-20;
  int bottom=top+panelH;int navRows=Math.max(1,(panelH-122)/22);
  chapterOffset=Math.max(0,Math.min(chapterOffset,Math.max(0,chapters.size()-navRows)));
  for(int i=0;i<navRows&&chapterOffset+i<chapters.size();i++){int k=chapterOffset+i;button((k==chapter?"> ":"")+(ModList.get().isLoaded(chapters.get(k).requires())?"":"* ")+chapters.get(k).title().get(russian),left+5,top+30+i*22,navW-9,()->changeChapter(k));}
  button(label("Указатель","Index"),left+5,bottom-88,navW-9,()->Minecraft.getInstance().setScreen(new GuideIndexScreen(this,chapters,russian)));
  button(label("Рецепты","Recipes"),left+5,bottom-66,navW-9,()->Minecraft.getInstance().setScreen(new GuideRecipesScreen(this,russian)));
  int half=(navW-11)/2;button("^",left+5,bottom-44,half,()->{chapterOffset=Math.max(0,chapterOffset-navRows);rebuildWidgets();});button("v",left+7+half,bottom-44,half,()->{chapterOffset=Math.min(Math.max(0,chapters.size()-navRows),chapterOffset+navRows);rebuildWidgets();});
  button(label("Назад","Back"),left+5,bottom-22,navW-9,this::onClose);
  TabRail.install(this,this::addRenderableWidget);
  if(chapters.isEmpty())return;
  page=Math.min(page,current().pages().size()-1);
  int ctrl=Math.max(22,Math.min(48,(contentW-10)/4));
  button("<",contentX,bottom-22,ctrl,()->changePage(-1));button(">",contentX+ctrl+2,bottom-22,ctrl,()->changePage(1));
  button("^",contentX+contentW-2*ctrl-2,bottom-22,ctrl,()->scroll=Math.max(0,scroll-visibleLines));
  button("v",contentX+contentW-ctrl,bottom-22,ctrl,()->scroll=Math.min(Math.max(0,lines.size()-visibleLines),scroll+visibleLines));
  previews.clear();var models=article().models().stream().filter(BuiltInRegistries.ITEM::containsKey).limit(5).toList();
  int size=Math.min(panelH<175?23:34,Math.max(16,(contentW-4)/Math.max(1,models.size())-4));
  int gap=4, total=models.size()*(size+gap)-gap,px=contentX+Math.max(0,(contentW-total)/2);
  int py=top+42;
  for(var id:models){var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));if(!stack.isEmpty())previews.add(new Preview(stack,px,py,size));px+=size+gap;}
  icons=article().icons().stream().filter(id->Minecraft.getInstance().getResourceManager().getResource(id).isPresent()).limit(Math.max(0,contentW/20)).toList();
  textY=py+(previews.isEmpty()?0:size+10)+(icons.isEmpty()?0:22);
  var list=new ArrayList<FormattedCharSequence>();
  for(String paragraph:article().body().get(russian)){list.addAll(font.split(lit(paragraph),Math.max(20,contentW)));list.add(FormattedCharSequence.EMPTY);}
  lines=List.copyOf(list);visibleLines=Math.max(1,(bottom-32-textY)/11);scroll=Math.min(scroll,Math.max(0,lines.size()-visibleLines));
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xb0101010);
  int right=left+panelW,bottom=top+panelH;
  g.fill(left,top,right,bottom,0xff46362b);g.renderOutline(left,top,panelW,panelH,0xff2b211b);
  g.fill(left+navW,top+4,right-4,bottom-4,0xffe6d3ac);
  g.drawString(font,label("Полевой справочник","Field guide"),left+6,top+10,0xffeadbb7,false);
  if(!chapters.isEmpty()){
   g.drawString(font,font.plainSubstrByWidth(article().title().get(russian),contentW),contentX,top+10,0xff33271e,false);
   String source=ModList.get().isLoaded(current().requires())?current().source():label("Требуется аддон: ","Requires addon: ")+current().requires();
   g.drawString(font,font.plainSubstrByWidth(source,contentW),contentX,top+24,0xff75624c,false);
   for(var p:previews){
    g.fill(p.x,p.y,p.x+p.size,p.y+p.size,0xffc7ad82);g.renderOutline(p.x,p.y,p.size,p.size,0xffa38b67);
    g.pose().pushPose();g.pose().translate(p.x+2,p.y+2,0);float scale=(p.size-4)/16f;g.pose().scale(scale,scale,1);g.renderItem(p.stack,0,0);g.pose().popPose();
   }
   int iconX=contentX;for(var id:icons){g.blit(id,iconX,textY-20,0,0,16,16,16,16);iconX+=20;}
   for(int i=0;i<visibleLines&&scroll+i<lines.size();i++)g.drawString(font,lines.get(scroll+i),contentX,textY+i*11,0xff372a20,false);
   String position=(page+1)+"/"+current().pages().size();
   g.drawCenteredString(font,position,contentX+contentW/2,bottom-35,0xff6d563e);
   if(lines.size()>visibleLines){int x=right-8,available=Math.max(4,bottom-33-textY);g.fill(x,textY,x+2,textY+available,0xffb49a76);int h=Math.max(3,available*visibleLines/lines.size());int y=textY+(available-h)*scroll/Math.max(1,lines.size()-visibleLines);g.fill(x,y,x+2,y+h,0xff795b3e);}
  }else g.drawString(font,label("Нет доступных глав","No chapters available"),contentX,top+34,0xff33271e,false);
  super.render(g,mx,my,delta);
  for(var p:previews)if(mx>=p.x&&mx<p.x+p.size&&my>=p.y&&my<p.y+p.size){g.renderTooltip(font,p.stack,mx,my);break;}
 }
}
