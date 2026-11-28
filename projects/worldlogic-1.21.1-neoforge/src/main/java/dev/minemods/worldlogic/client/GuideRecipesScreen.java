package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.GuideCatalog;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
final class GuideRecipesScreen extends ClearScreen implements GuideJeiLookup.Hovered{
 private final Screen parent;private final boolean ru;private final ItemStack focus;private final boolean uses;private int selected,stepPage;private EditBox search;
 private record Cell(String role,List<ItemStack> alternatives,int position){}
 private record Entry(ResourceLocation id,Component title,Component type,List<Cell> cells,int gridWidth,List<Component> steps){}
 private record Preview(ItemStack stack,int x,int y){}
 private List<Entry> recipes=List.of();private final List<Preview> drawn=new ArrayList<>();
 GuideRecipesScreen(Screen parent,boolean ru){this(parent,ru,ItemStack.EMPTY,false);}
 GuideRecipesScreen(Screen parent,boolean ru,ItemStack focus,boolean uses){super(Component.literal(ru?"Рецепты":"Recipes"));this.parent=parent;this.ru=ru;this.focus=focus.copy();this.uses=uses;}
 private String label(String a,String b){return ru?a:b;}
 private boolean matchesFocus(List<Cell> cells){
  return focus.isEmpty()||cells.stream().filter(c->uses?!c.role().equals("output"):c.role().equals("output")).flatMap(c->c.alternatives().stream()).anyMatch(st->st.is(focus.getItem()));
 }
 private List<Entry> matches(){String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT).strip();return recipes.stream().filter(e->(e.title().getString()+" "+e.id()+" "+e.type().getString()+" "+e.cells().stream().flatMap(c->c.alternatives().stream()).map(s->s.getHoverName().getString()).reduce("",(x,y)->x+" "+y)).toLowerCase(Locale.ROOT).contains(q)).toList();}
 private int usable(){return WorldLogicTabs.contentWidth(width);}
 @Override protected void init(){
  var mc=Minecraft.getInstance();var entries=new ArrayList<Entry>();
  if(mc.level!=null)for(var h:mc.level.getRecipeManager().getRecipes()){
   var r=h.value();if(r.getIngredients().isEmpty()||r.getIngredients().size()>12)continue;
   var out=r.getResultItem(mc.level.registryAccess());
   if(out.isEmpty()||focus.isEmpty()&&!(GuideCatalog.owns(h.id().getNamespace())||GuideCatalog.owns(BuiltInRegistries.ITEM.getKey(out.getItem()).getNamespace())))continue;
   var cells=new ArrayList<Cell>();int rw=r instanceof ShapedRecipe s?s.getWidth():r instanceof ShapelessRecipe?3:0;
   for(int i=0;i<r.getIngredients().size();i++){
    var items=r.getIngredients().get(i).getItems();if(items.length>0)cells.add(new Cell("input",List.of(items),i));
   }
   cells.add(new Cell("output",List.of(out.copy()),-1));
   if(!matchesFocus(cells))continue;
   String kind=r instanceof ShapedRecipe?label("По форме","Shaped"):r instanceof ShapelessRecipe?label("Без формы","Shapeless"):BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()).toString();
   entries.add(new Entry(h.id(),out.getHoverName(),Component.literal(kind),List.copyOf(cells),rw,List.of()));
  }
  for(var r:WorldLogicRecipes.processing()){
   var cells=new ArrayList<Cell>();int i=0;for(var slot:r.slots())cells.add(new Cell(slot.role().name().toLowerCase(Locale.ROOT),slot.alternatives(),i++));
   if(!matchesFocus(cells))continue;
   entries.add(new Entry(r.id(),Component.translatable(r.titleKey()),Component.translatable("worldlogic.jei.processing"),List.copyOf(cells),0,r.stepKeys().stream().map(k->(Component)Component.translatable(k)).toList()));
  }
  entries.sort(Comparator.comparing(e->e.id().toString()));recipes=List.copyOf(entries);
  String old=search==null?"":search.getValue();search=new EditBox(font,12,26,Math.max(30,usable()-24),20,Component.literal(label("Найти рецепт","Find recipe")));search.setMaxLength(100);search.setValue(old);search.setResponder(v->{selected=0;stepPage=0;});addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal("<"),b->{selected=Math.max(0,selected-1);stepPage=0;}).bounds(12,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{selected=Math.min(Math.max(0,matches().size()-1),selected+1);stepPage=0;}).bounds(48,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.literal(label("Этапы","Steps")),b->stepPage++).bounds(Math.max(82,usable()-158),height-26,64,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(usable()-88,height-26,76,20).build());
  TabRail.install(this,this::addRenderableWidget);
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public ItemStack hovered(double x,double y){for(var s:drawn)if(x>=s.x()&&x<s.x()+20&&y>=s.y()&&y<s.y()+20)return s.stack();return ItemStack.EMPTY;}
 @Override public boolean keyPressed(int key,int scan,int mods){if(!search.isFocused()&&GuideJeiLookup.key(this,key,scan))return true;return super.keyPressed(key,scan,mods);}
 @Override public boolean mouseClicked(double x,double y,int button){var st=hovered(x,y);if(!st.isEmpty()&&(button==0||button==1)){if(!GuideJeiLookup.show(st,button==1))Minecraft.getInstance().setScreen(new GuideRecipesScreen(this,ru,st,button==1));return true;}return super.mouseClicked(x,y,button);}
 private void slot(GuiGraphics g,Cell c,int x,int y,boolean named){
  g.fill(x,y,x+20,y+20,0xff8b8b8b);g.renderOutline(x,y,20,20,0xff555555);
  var st=c.alternatives().get((int)((System.currentTimeMillis()/1500)%c.alternatives().size()));
  g.renderItem(st,x+2,y+2);g.renderItemDecorations(font,st,x+2,y+2);drawn.add(new Preview(st,x,y));
  if(named)g.drawString(font,font.plainSubstrByWidth(Component.translatable("worldlogic.jei.role."+c.role()).getString(),38),x,y+21,VanillaPanels.TEXT,false);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  drawn.clear();VanillaPanels.backdrop(g,width,height);VanillaPanels.panel(g,6,6,usable()-6,height-6);g.drawCenteredString(font,title,usable()/2,10,VanillaPanels.TEXT);
  var list=matches();
  if(list.isEmpty())g.drawCenteredString(font,Component.literal(label("Нет зарегистрированных рецептов","No registered recipes")),usable()/2,62,VanillaPanels.TEXT);
  else{
   selected=Math.max(0,Math.min(selected,list.size()-1));var e=list.get(selected);
   g.drawCenteredString(font,font.plainSubstrByWidth(e.title().getString(),usable()-24),usable()/2,54,VanillaPanels.TEXT);
   g.drawCenteredString(font,font.plainSubstrByWidth(e.type().getString(),usable()-24),usable()/2,66,VanillaPanels.MUTED);
   int top=82,after;
   if(e.gridWidth()>0){
    int x=Math.max(12,usable()/2-55);
    for(var c:e.cells()){if(c.role().equals("output"))slot(g,c,x+82,top+20,false);else slot(g,c,x+c.position()%e.gridWidth()*20,top+c.position()/e.gridWidth()*20,false);}
    g.drawString(font,"→",x+66,top+25,VanillaPanels.TEXT,false);after=top+65;
   }else{
    int cols=Math.max(1,Math.min(6,(usable()-24)/40)),x=Math.max(12,(usable()-cols*40)/2);
    for(int i=0;i<e.cells().size();i++)slot(g,e.cells().get(i),x+(i%cols)*40,top+(i/cols)*34,true);
    after=top+((e.cells().size()+cols-1)/cols)*34;
   }
   if(!e.steps().isEmpty()){
    var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();for(var step:e.steps())lines.addAll(font.split(step,Math.max(20,usable()-32)));
    int visible=Math.max(0,(height-33-after)/10);
    if(visible>0){int max=Math.max(1,(lines.size()+visible-1)/visible);stepPage=Math.floorMod(stepPage,max);for(int i=0;i<visible&&stepPage*visible+i<lines.size();i++)g.drawString(font,lines.get(stepPage*visible+i),16,after+i*10,VanillaPanels.TEXT,false);}
   }
   g.drawCenteredString(font,(selected+1)+"/"+list.size(),usable()/2,height-20,VanillaPanels.TEXT);
  }
  super.render(g,mx,my,dt);var hover=hovered(mx,my);if(!hover.isEmpty())g.renderTooltip(font,hover,mx,my);
 }
}
