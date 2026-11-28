package dev.minemods.worldlogic.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.core.registries.BuiltInRegistries;
final class GuideRecipesScreen extends ClearScreen{
 private static final Set<String> NS=Set.of("worldlogic","hydration","traumacraft","worldlogic_thermal","worldlogicterrain","wl_treechopper","worldlogic_metabolism","metabolism","worldlogic_trauma","worldlogic_aqua","worldlogic_aquaculture");
 private final Screen parent;private final boolean ru;private final ItemStack focus;private final boolean uses;private int selected;private EditBox search;
 private record Entry(RecipeHolder<?> holder,ItemStack output){}
 private List<Entry> recipes=List.of();
 GuideRecipesScreen(Screen parent,boolean ru){this(parent,ru,ItemStack.EMPTY,false);}
 GuideRecipesScreen(Screen parent,boolean ru,ItemStack focus,boolean uses){super(Component.literal(ru?"Рецепты":"Recipes"));this.parent=parent;this.ru=ru;this.focus=focus.copy();this.uses=uses;}
 private List<Entry> matches(){String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT);return recipes.stream().filter(e->(e.output().getHoverName().getString()+" "+e.holder().id()).toLowerCase(Locale.ROOT).contains(q)).toList();}
 private int usable(){return dev.minemods.worldlogic.api.client.WorldLogicTabs.contentWidth(width);}
 @Override protected void init(){
  var mc=Minecraft.getInstance();var entries=new ArrayList<Entry>();
  if(mc.level!=null)for(var h:mc.level.getRecipeManager().getRecipes()){
   var r=h.value();if(r.getIngredients().isEmpty()||r.getIngredients().size()>9)continue;
   var out=r.getResultItem(mc.level.registryAccess());if(out.isEmpty()||!(NS.contains(h.id().getNamespace())||NS.contains(BuiltInRegistries.ITEM.getKey(out.getItem()).getNamespace())))continue;
   if(!focus.isEmpty()&&(uses?r.getIngredients().stream().noneMatch(i->i.test(focus)):!out.is(focus.getItem())))continue;
   entries.add(new Entry(h,out.copy()));
  }
  entries.sort(Comparator.comparing(e->e.holder().id().toString()));recipes=List.copyOf(entries);
  String old=search==null?"":search.getValue();search=new EditBox(font,12,26,Math.max(30,usable()-24),20,Component.literal(ru?"Найти рецепт":"Find recipe"));search.setMaxLength(100);search.setValue(old);search.setResponder(v->selected=0);addRenderableWidget(search);
  addRenderableWidget(Button.builder(Component.literal("<"),b->selected=Math.max(0,selected-1)).bounds(12,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->selected=Math.min(Math.max(0,matches().size()-1),selected+1)).bounds(48,height-26,32,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(usable()-88,height-26,76,20).build());
  TabRail.install(this,this::addRenderableWidget);
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 private void slot(GuiGraphics g,int x,int y){g.fill(x,y,x+20,y+20,0xff938c80);g.renderOutline(x,y,20,20,0xff665d50);}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  g.fill(0,0,width,height,0xff46362b);g.drawCenteredString(font,title,usable()/2,10,0xfff0dfc0);
  var list=matches();ItemStack hover=ItemStack.EMPTY;
  if(list.isEmpty())g.drawCenteredString(font,Component.literal(ru?"Нет подходящих рецептов":"No matching recipes"),usable()/2,62,0xfff0dfc0);
  else{
   selected=Math.max(0,Math.min(selected,list.size()-1));var e=list.get(selected);var r=e.holder().value();
   int x=Math.max(12,usable()/2-55),y=82;g.drawCenteredString(font,font.plainSubstrByWidth(e.output().getHoverName().getString(),usable()-24),usable()/2,54,0xfff0dfc0);
   g.drawCenteredString(font,Component.literal(r instanceof ShapedRecipe?(ru?"По форме":"Shaped"):r instanceof ShapelessRecipe?(ru?"Без формы":"Shapeless"):BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()).toString()),usable()/2,66,0xffdbc29a);
   int rw=r instanceof ShapedRecipe s?s.getWidth():3;
   var ingredients=r.getIngredients();for(int k=0;k<9;k++){int sx=x+k%3*20,sy=y+k/3*20;slot(g,sx,sy);int i=k%3<rw?k/3*rw+k%3:-1;if(i<0||i>=ingredients.size())continue;var alternatives=ingredients.get(i).getItems();if(alternatives.length==0)continue;var st=alternatives[(int)((System.currentTimeMillis()/1500)%alternatives.length)];g.renderItem(st,sx+2,sy+2);if(mx>=sx&&mx<sx+20&&my>=sy&&my<sy+20)hover=st;}
   g.drawString(font,"→",x+66,y+25,0xfff0dfc0,false);int ox=x+82,oy=y+20;slot(g,ox,oy);g.renderItem(e.output(),ox+2,oy+2);g.renderItemDecorations(font,e.output(),ox+2,oy+2);if(mx>=ox&&mx<ox+20&&my>=oy&&my<oy+20)hover=e.output();
   g.drawCenteredString(font,(selected+1)+"/"+list.size(),usable()/2,height-20,0xfff0dfc0);
   if(height>=190)g.drawCenteredString(font,font.plainSubstrByWidth(e.holder().id().toString(),usable()-24),usable()/2,150,0xffdbc29a);
  }
  super.render(g,mx,my,dt);if(!hover.isEmpty())g.renderTooltip(font,hover,mx,my);
 }
}
