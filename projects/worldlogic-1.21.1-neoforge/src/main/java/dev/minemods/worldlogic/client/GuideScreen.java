package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.BookLayout;
import dev.minemods.worldlogic.GuideBook;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
/** Two-page field guide: table of contents, chapter contents and complete article spreads. */
public final class GuideScreen extends ClearScreen implements GuideJeiLookup.Hovered{
 private final Screen parent;private List<GuideBook.Chapter> chapters=List.of();
 private enum View{CONTENTS,CHAPTER,ARTICLE}
 private View view=View.CONTENTS;private int chapter,page,spread,listOffset;private boolean russian;
 private BookLayout b;private int firstRows,firstCapacity,spreadCount,previewY,iconY;
 private List<FormattedCharSequence> lines=List.of();
 private final List<Preview> previews=new ArrayList<>();private List<ResourceLocation> icons=List.of();
 private record Preview(ItemStack stack,int x,int y){}
 public GuideScreen(Screen parent){super(Component.literal("WorldLogic"));this.parent=parent;}
 private String label(String ru,String en){return russian?ru:en;}
 private Component lit(String s){return Component.literal(s);}
 private void button(String text,int x,int y,int w,Runnable action){
  if(w<12)return;
  var button=Button.builder(lit(font.plainSubstrByWidth(text,Math.max(1,w-8))),v->action.run()).bounds(x,y,w,18).build();
  button.setTooltip(Tooltip.create(lit(text)));addRenderableWidget(button);
 }
 private void link(String text,int x,int y,int w,Runnable action){
  var widget=new Button(x,y,w,15,lit(text),v->action.run(),v->v.get()){
   @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
    if(isHoveredOrFocused())g.fill(getX(),getY(),getX()+getWidth(),getY()+getHeight(),0xffd5b786);
    g.drawString(font,font.plainSubstrByWidth(getMessage().getString(),Math.max(1,getWidth()-6)),getX()+3,getY()+3,0xff48311e,false);
   }
  };
  widget.setTooltip(Tooltip.create(lit(text)));addRenderableWidget(widget);
 }
 private void load(){
  var mc=Minecraft.getInstance();var loaded=new TreeMap<String,GuideBook.Chapter>();
  for(var entry:mc.getResourceManager().listResources("worldlogic_guide",id->id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()){
   try(var reader=entry.getValue().openAsReader()){
    var c=GuideBook.read(reader);String key=c.id();
    if(!loaded.containsKey(key)||entry.getKey().getNamespace().equals("worldlogic"))loaded.put(key,c);
   }catch(Exception e){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Invalid guide resource {}",entry.getKey(),e);}
  }
  chapters=GuideBook.catalog(loaded.values(),ModList.get()::isLoaded);
 }
 private GuideBook.Chapter current(){return chapters.get(chapter);}
 private GuideBook.Page article(){return current().pages().get(page);}
 void select(int c,int p){chapter=c;page=p;view=View.ARTICLE;spread=0;listOffset=0;Minecraft.getInstance().setScreen(this);}
 @Override public ItemStack hovered(double x,double y){if(view==View.ARTICLE&&spread==0)for(var p:previews)if(x>=p.x&&x<p.x+16&&y>=p.y&&y<p.y+16)return p.stack;return ItemStack.EMPTY;}
 @Override public boolean keyPressed(int key,int scan,int mods){return GuideJeiLookup.key(this,key,scan)||super.keyPressed(key,scan,mods);}
 @Override public boolean mouseClicked(double x,double y,int button){var st=hovered(x,y);if(!st.isEmpty()&&(button==0||button==1)){if(!GuideJeiLookup.show(st,button==1))Minecraft.getInstance().setScreen(new GuideRecipesScreen(this,russian,st,button==1));return true;}return super.mouseClicked(x,y,button);}
 private void contents(){view=View.CONTENTS;listOffset=0;rebuildWidgets();}
 private void chapter(int value){chapter=value;page=0;view=View.CHAPTER;listOffset=0;rebuildWidgets();}
 private void openArticle(int value){page=value;spread=0;view=View.ARTICLE;rebuildWidgets();}
 private void step(int d){
  if(view!=View.ARTICLE)return;
  if(spread+d>=0&&spread+d<spreadCount){spread+=d;rebuildWidgets();return;}
  if(page+d>=0&&page+d<current().pages().size()){page+=d;spread=d<0?Integer.MAX_VALUE:0;rebuildWidgets();}
 }
 private int listRows(){return Math.max(1,(b.pageH()-68)/17);}
 @Override protected void init(){
  russian=Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru");
  load();chapter=Math.min(chapter,Math.max(0,chapters.size()-1));b=BookLayout.of(width,height);
  previews.clear();icons=List.of();lines=List.of();
  TabRail.install(this,this::addRenderableWidget);
  if(!b.readable()){button(label("Назад","Back"),4,Math.max(4,height-22),Math.max(12,width-40),this::onClose);return;}
  int y=b.bottom()-28, half=Math.max(18,(b.textW()-4)/2);
  button(label("Оглавление","Contents"),b.leftText(),b.textTop()+3,b.textW(),this::contents);
  button(label("Каталог","Catalog"),b.rightText(),b.textTop()+3,half,()->Minecraft.getInstance().setScreen(new GuideCatalogScreen(this,russian)));
  button(label("Рецепты","Recipes"),b.rightText()+half+4,b.textTop()+3,half,()->Minecraft.getInstance().setScreen(new GuideRecipesScreen(this,russian)));
  if(view==View.CONTENTS){int w=Math.max(12,(b.textW()-32)/2);button(label("Поиск","Search"),b.rightText(),y,w,()->Minecraft.getInstance().setScreen(new GuideIndexScreen(this,chapters,russian)));if(ModList.get().isLoaded("patchouli"))button("Patchouli",b.rightText()+w+4,y,w,()->GuideOpener.openPatchouli(this));}
  if(chapters.isEmpty()){button(label("Назад","Back"),b.leftText(),y,b.textW(),this::onClose);return;}
  if(view==View.ARTICLE){
   page=Math.min(page,current().pages().size()-1);
   prepareArticle();
   spread=Math.max(0,Math.min(spread,spreadCount-1));
   button("<",b.leftText(),y,24,()->step(-1));
   button(">",b.rightText()+b.textW()-24,y,24,()->step(1));
   button(label("Глава","Chapter"),b.leftText()+28,y,Math.max(12,b.textW()-28),()->{view=View.CHAPTER;listOffset=(page/(2*listRows()))*2*listRows();rebuildWidgets();});
  }else{
   int count=view==View.CONTENTS?chapters.size():current().pages().size(),rows=listRows(),capacity=rows*2;
   listOffset=Math.max(0,Math.min(listOffset,Math.max(0,(count-1)/capacity*capacity)));
   for(int i=0;i<capacity&&listOffset+i<count;i++){
    int k=listOffset+i,x=i<rows?b.leftText():b.rightText(),yy=b.textTop()+43+(i%rows)*17;
    String title=view==View.CONTENTS?((ModList.get().isLoaded(chapters.get(k).requires())?"":"* ")+chapters.get(k).title().get(russian)):current().pages().get(k).title().get(russian);
    link((k+1)+". "+title,x,yy,b.textW(),()->{if(view==View.CONTENTS)chapter(k);else openArticle(k);});
   }
   button("<",b.leftText(),y,24,()->{listOffset=Math.max(0,listOffset-capacity);rebuildWidgets();});
   button(">",b.rightText()+b.textW()-24,y,24,()->{listOffset=Math.min(Math.max(0,(count-1)/capacity*capacity),listOffset+capacity);rebuildWidgets();});
   button(label("Назад","Back"),b.leftText()+28,y,Math.max(12,b.textW()-28),view==View.CONTENTS?this::onClose:this::contents);
  }
 }
 private void prepareArticle(){
  var list=new ArrayList<FormattedCharSequence>();
  for(String paragraph:article().body().get(russian)){list.addAll(font.split(lit(paragraph),b.textW()));list.add(FormattedCharSequence.EMPTY);}
  lines=List.copyOf(list);
  var models=article().models().stream().filter(BuiltInRegistries.ITEM::containsKey).limit(Math.max(0,b.textW()/20)).toList();
  previewY=b.textTop()+43;
  // Short screens keep the text readable, rather than overlapping the footer.
  if(b.rows()>=5){
   int px=b.leftText();
   for(var id:models){var st=new ItemStack(BuiltInRegistries.ITEM.get(id));if(!st.isEmpty()){previews.add(new Preview(st,px,previewY));px+=20;}}
   icons=article().icons().stream().filter(id->Minecraft.getInstance().getResourceManager().getResource(id).isPresent()).limit(Math.max(0,b.textW()/20)).toList();
  }
  iconY=previewY+(previews.isEmpty()?0:22);
  int extra=(previews.isEmpty()?0:2)+(icons.isEmpty()?0:2);
  firstRows=Math.max(1,b.rows()-extra);firstCapacity=firstRows+b.rows();spreadCount=b.spreads(lines.size(),firstCapacity);
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){
  if(view==View.ARTICLE&&dy!=0){step(dy<0?1:-1);return true;}
  return super.mouseScrolled(x,y,dx,dy);
 }
 private void drawBook(GuiGraphics g){
  int x=b.x(),y=b.y(),w=b.w(),h=b.h(),mid=b.right(),bottom=b.bottom();
  g.fill(x+3,y+4,x+w+3,bottom+4,0x88000000);
  // Stepped leather cover, page edges, two facing sheets and a central binding.
  g.fill(x+2,y,x+w-2,bottom,0xff513520);g.fill(x,y+3,x+w,bottom-3,0xff65432a);
  g.fill(x+3,y+3,x+w-3,bottom-3,0xff936b40);g.fill(x+5,y+5,x+w-5,bottom-5,0xff513520);
  g.fill(b.left()-3,y+8,mid,bottom-7,0xffa99066);g.fill(mid,y+8,b.right()+b.pageW()+3,bottom-7,0xffa99066);
  for(int yy=bottom-13;yy<bottom-7;yy+=2)g.fill(x+10,yy,x+w-10,yy+1,0xffc7ad7d);
  g.fill(b.left()-1,y+7,mid,bottom-13,0xffebd8af);g.fill(mid,y+7,b.right()+b.pageW()+1,bottom-13,0xffefdfba);
  g.fill(mid-5,y+8,mid-3,bottom-13,0xffddc59b);g.fill(mid-3,y+8,mid-1,bottom-13,0xffcbb085);
  g.fill(mid-1,y+8,mid+1,bottom-13,0xff9f805a);g.fill(mid+1,y+8,mid+3,bottom-13,0xffcbb085);
  g.fill(mid+3,y+8,mid+5,bottom-13,0xffddc59b);
  g.fill(b.left(),y+7,mid-5,y+8,0xfff7e7c7);g.fill(mid+5,y+7,b.right()+b.pageW(),y+8,0xfff7e7c7);
 }
 private void heading(GuiGraphics g,String text,int x){
  g.drawString(font,font.plainSubstrByWidth(text,b.textW()),x,b.textTop()+25,0xff4b3420,false);
  g.fill(x,b.textTop()+37,x+b.textW(),b.textTop()+38,0xffb79a72);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xb0101010);
  if(b.readable()){
   drawBook(g);
   if(chapters.isEmpty())heading(g,label("Нет глав","No chapters"),b.leftText());
   else if(view!=View.ARTICLE){
    heading(g,view==View.CONTENTS?label("Полевой справочник","Field guide"):current().title().get(russian),b.leftText());
    heading(g,view==View.CONTENTS?label("Разделы","Chapters"):label("Статьи","Articles"),b.rightText());
    g.drawCenteredString(font,Integer.toString(listOffset/(2*listRows())+1),b.rightText()+b.textW()/2,b.bottom()-24,0xff6d563e);
   }else{
    heading(g,article().title().get(russian),b.leftText());
    heading(g,spread==0?current().title().get(russian):label("Продолжение","Continued"),b.rightText());
    int leftRows=spread==0?firstRows:b.rows(),start=b.offset(spread,firstCapacity),top=b.textTop()+43;
    if(spread==0){
     for(var p:previews){g.renderItem(p.stack,p.x,p.y);}
     int ix=b.leftText();for(var id:icons){g.blit(id,ix,iconY,0,0,16,16,16,16);ix+=20;}
     top+=(b.rows()-firstRows)*11;
    }
    for(int i=0;i<leftRows&&start+i<lines.size();i++)g.drawString(font,lines.get(start+i),b.leftText(),top+i*11,0xff372a20,false);
    for(int i=0;i<b.rows()&&start+leftRows+i<lines.size();i++)g.drawString(font,lines.get(start+leftRows+i),b.rightText(),b.textTop()+43+i*11,0xff372a20,false);
    g.drawCenteredString(font,(page+1)+"/"+current().pages().size()+" · "+(spread+1)+"/"+spreadCount,b.rightText()+b.textW()/2,b.bottom()-24,0xff6d563e);
   }
  }else g.drawCenteredString(font,label("Уменьшите масштаб интерфейса","Reduce GUI scale"),Math.max(1,width/2),Math.max(0,height/2-12),0xffeeeeee);
  super.render(g,mx,my,delta);
  if(view==View.ARTICLE&&spread==0)for(var p:previews)if(mx>=p.x&&mx<p.x+16&&my>=p.y&&my<p.y+16){g.renderTooltip(font,p.stack,mx,my);break;}
 }
}
