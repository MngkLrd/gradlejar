package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.TabLayout;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
/** Core-owned vanilla-style navigation. No added PNGs or server-side state. */
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class TabRail {
 private TabRail(){}
 private static final ResourceLocation INVENTORY=id("inventory"),GUIDE=id("guide"),ANALYTICS=id("analytics");
 private static final Map<Screen,ResourceLocation> OWNED=new WeakHashMap<>();
 private static final Map<Screen,Screen> PARENTS=new WeakHashMap<>();
 private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("worldlogic",path);}
 static {
  WorldLogicTabs.register(INVENTORY,0,Component.translatable("worldlogic.tabs.inventory"),()->new ItemStack(Items.CHEST),parent->parent);
  WorldLogicTabs.register(GUIDE,10,Component.translatable("worldlogic.tabs.guide"),()->new ItemStack(Items.BOOK),GuideScreen::new);
  WorldLogicTabs.register(ANALYTICS,90,Component.translatable("worldlogic.analytics.title"),()->new ItemStack(Items.COMPASS),AnalyticsScreen::new);
 }
 private static ResourceLocation current(Screen screen){
  if(screen instanceof InventoryScreen)return INVENTORY;
  if(screen instanceof GuideScreen)return GUIDE;
  if(screen instanceof AnalyticsScreen)return ANALYTICS;
  return OWNED.get(screen);
 }
 private static void open(Screen from,WorldLogicTabs.Tab tab){
  var mc=Minecraft.getInstance();if(mc.player==null||tab.id().equals(current(from)))return;
  Screen parent=from instanceof InventoryScreen?from:PARENTS.get(from);
  if(parent==null)parent=new InventoryScreen(mc.player);
  if(tab.id().equals(INVENTORY)){mc.setScreen(parent);return;}
  Screen next=tab.factory().apply(parent);
  if(next==null||next==from||next==parent)return;
  OWNED.put(next,tab.id());PARENTS.put(next,parent);mc.setScreen(next);
 }
 private static final class IconButton extends Button {
  final Supplier<ItemStack> icon;final Component caption;final boolean selected;
  IconButton(Component caption,Supplier<ItemStack> icon,boolean selected,OnPress press){
   super(0,0,TabLayout.SIZE,TabLayout.SIZE,Component.empty(),press,DEFAULT_NARRATION);
   this.icon=icon;this.caption=caption;this.selected=selected;setTooltip(Tooltip.create(caption));
  }
  @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
   super.renderWidget(g,mx,my,delta);
   if(selected)g.renderOutline(getX()+1,getY()+1,TabLayout.SIZE-2,TabLayout.SIZE-2,0xffd6b36c);
   if(icon!=null){ItemStack stack=icon.get();if(stack!=null&&!stack.isEmpty())g.renderItem(stack,getX()+4,getY()+4);}
   else g.drawCenteredString(Minecraft.getInstance().font,caption,getX()+TabLayout.SIZE/2,getY()+8,0xffffffff);
  }
 }
 private static final class Rail {
  int page;final List<IconButton> tabs=new ArrayList<>();IconButton prev,next;
 }
 private static final Map<Screen,Rail> RAILS=new WeakHashMap<>();
 // The rail values must not hold strong references back to their weak screen keys.
 private static java.lang.ref.WeakReference<Screen> weak(Screen screen){return new java.lang.ref.WeakReference<>(screen);}
 static void install(Screen screen,Consumer<AbstractWidget> add){
  if(current(screen)==null)return;
  var ref=weak(screen);var rail=new Rail();
  for(var tab:WorldLogicTabs.available()){
   var b=new IconButton(tab.title(),tab.icon(),tab.id().equals(current(screen)),clicked->{Screen s=ref.get();if(s!=null)open(s,tab);});
   b.active=!b.selected;rail.tabs.add(b);add.accept(b);
  }
  rail.prev=new IconButton(Component.literal("^"),null,false,clicked->{rail.page--;Screen s=ref.get();if(s!=null)layout(s,rail);});
  rail.prev.setTooltip(Tooltip.create(Component.translatable("worldlogic.tabs.previous")));add.accept(rail.prev);
  rail.next=new IconButton(Component.literal("v"),null,false,clicked->{rail.page++;Screen s=ref.get();if(s!=null)layout(s,rail);});
  rail.next.setTooltip(Tooltip.create(Component.translatable("worldlogic.tabs.next")));add.accept(rail.next);
  RAILS.put(screen,rail);layout(screen,rail);
 }
 private static void layout(Screen screen,Rail rail){
  int x=screen.width-TabLayout.GUTTER+4,y=8,available=screen.height-16;
  boolean safe=true;
  if(screen instanceof InventoryScreen inv){
   int right=inv.getGuiLeft()+inv.getXSize();x=right+4;y=inv.getGuiTop();available=Math.min(inv.getYSize(),screen.height-y);
   var player=Minecraft.getInstance().player;
   if(player!=null&&!player.getActiveEffects().isEmpty()){
    int remaining=screen.width-right;
    x=right+(remaining>=124?124:36);
   }
   safe=inv.getMenu().getCarried().isEmpty()&&x+TabLayout.SIZE<=screen.width;
  }
  int rows=safe?TabLayout.capacity(available):0,size=TabLayout.pageSize(rail.tabs.size(),rows);
  rail.page=TabLayout.clampPage(rail.page,rail.tabs.size(),size);
  boolean paged=size>0&&rail.tabs.size()>size;
  for(int i=0;i<rail.tabs.size();i++){
   var b=rail.tabs.get(i);int slot=i-rail.page*size;
   b.visible=size>0&&slot>=0&&slot<size;
   if(b.visible){b.setX(x);b.setY(y+slot*TabLayout.STEP);}
  }
  rail.prev.visible=paged;rail.next.visible=paged;
  rail.prev.active=rail.page>0;rail.next.active=rail.page+1<TabLayout.pages(rail.tabs.size(),size);
  if(paged){rail.prev.setX(x);rail.prev.setY(y+size*TabLayout.STEP);rail.next.setX(x);rail.next.setY(y+(size+1)*TabLayout.STEP);}
 }
 @SubscribeEvent public static void init(ScreenEvent.Init.Post e){
  Screen s=e.getScreen();
  // Built-in screens install during init too, including rebuildWidgets.
  if(!(s instanceof GuideScreen)&&!(s instanceof AnalyticsScreen)&&current(s)!=null)install(s,e::addListener);
 }
 @SubscribeEvent public static void render(ScreenEvent.Render.Pre e){
  var rail=RAILS.get(e.getScreen());if(rail!=null)layout(e.getScreen(),rail);
 }
}
