package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.TabLayout;
import dev.minemods.worldlogic.client.compat.NativePeerViews;
import net.neoforged.bus.api.EventPriority;
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
  dashboard("metabolism",30,"worldlogic.stats.metabolism",Items.COOKED_BEEF);
  dashboard("worldlogic_trauma",40,"worldlogic.stats.trauma",Items.REDSTONE);
  dashboard("worldlogic_thermal",50,"worldlogic.stats.temperature",Items.BLAZE_POWDER);
  NativePeerViews.register();
 }
 private static void dashboard(String mod,int order,String title,Item item){
  var id=ResourceLocation.fromNamespaceAndPath(mod,"dashboard");var label=Component.translatable(title);
  WorldLogicTabs.registerDashboardTab(id,order,label,()->new ItemStack(item),parent->new DashboardScreen(parent,id,label));
 }
 private static void syncedDashboards(){
  var state=dev.minemods.worldlogic.DashboardSync.client();if(state==null)return;
  for(var s:state.dashboards()){
   var title=Component.translatable(s.title());
   WorldLogicTabs.registerDashboardTab(s.id(),s.order(),title,()->new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(s.icon())),parent->new DashboardScreen(parent,s.id(),title));
  }
 }
 private static ResourceLocation current(Screen screen){
  if(screen instanceof DashboardScreen d)return d.dashboardId();
  String namespace=NativePeerViews.namespace(screen);
  if(namespace!=null)return WorldLogicTabs.primaryView(namespace).map(WorldLogicTabs.Tab::id).orElse(ResourceLocation.fromNamespaceAndPath(namespace,"dashboard"));
  if(screen instanceof InventoryScreen)return INVENTORY;
  if(screen instanceof GuideScreen||screen instanceof GuideIndexScreen||screen instanceof GuideRecipesScreen)return GUIDE;
  if(screen instanceof AnalyticsScreen)return ANALYTICS;
  return OWNED.get(screen);
 }
 private static void open(Screen from,WorldLogicTabs.Tab tab){
  var mc=Minecraft.getInstance();if(mc.player==null||tab.id().equals(current(from)))return;
  if(from instanceof InventoryScreen inv&&!inv.getMenu().getCarried().isEmpty())return;
  Screen parent=from instanceof InventoryScreen&&!NativePeerViews.anatomy(from)?from:PARENTS.get(from);
  if(parent==null)parent=new InventoryScreen(mc.player);
  if(tab.id().equals(INVENTORY)){mc.setScreen(parent);return;}
  if(tab.id().equals(GUIDE)){GuideOpener.open(parent);return;}
  Screen next=tab.factory().apply(parent);
  if(next==null||next==from||next==parent)return;
  OWNED.put(next,tab.id());PARENTS.put(next,parent);mc.setScreen(next);
 }
 private static final class IconButton extends Button {
  final Supplier<ItemStack> icon;final Component caption;final ResourceLocation tabId;boolean selected;
  IconButton(ResourceLocation tabId,Component caption,Supplier<ItemStack> icon,boolean selected,OnPress press){
   super(0,0,TabLayout.SIZE,TabLayout.SIZE,Component.empty(),press,DEFAULT_NARRATION);
   this.tabId=tabId;this.icon=icon;this.caption=caption;this.selected=selected;setTooltip(Tooltip.create(caption));
  }
  @Override protected void renderWidget(GuiGraphics g,int mx,int my,float delta){
   super.renderWidget(g,mx,my,delta);
   if(selected)g.renderOutline(getX()+1,getY()+1,TabLayout.SIZE-2,TabLayout.SIZE-2,0xffeeeeee);
   if(tabId!=null&&RailIcons.draw(g,tabId,getX()+4,getY()+4))return;
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
  syncedDashboards();
  var ref=weak(screen);var rail=new Rail();
  for(var tab:WorldLogicTabs.available()){
   var b=new IconButton(tab.id(),tab.title(),tab.icon(),tab.id().equals(current(screen)),clicked->{Screen s=ref.get();if(s!=null)open(s,tab);});
   b.active=!b.selected;rail.tabs.add(b);add.accept(b);
  }
  rail.prev=new IconButton(null,Component.literal("^"),null,false,clicked->{rail.page--;Screen s=ref.get();if(s!=null)layout(s,rail);});
  rail.prev.setTooltip(Tooltip.create(Component.translatable("worldlogic.tabs.previous")));add.accept(rail.prev);
  rail.next=new IconButton(null,Component.literal("v"),null,false,clicked->{rail.page++;Screen s=ref.get();if(s!=null)layout(s,rail);});
  rail.next.setTooltip(Tooltip.create(Component.translatable("worldlogic.tabs.next")));add.accept(rail.next);
  RAILS.put(screen,rail);layout(screen,rail);
 }
 private static void layout(Screen screen,Rail rail){
  int x=screen.width-TabLayout.GUTTER+4,y=8,available=screen.height-16;
  boolean safe=true;
  if(NativePeerViews.anatomy(screen)){
   safe=TabLayout.nativeRailFits(screen.width,screen.height)&&screen instanceof InventoryScreen inv&&inv.getMenu().getCarried().isEmpty();
  }else if("worldlogic_thermal".equals(NativePeerViews.namespace(screen))){
   // The old thermal panel uses the entire window for centering; keep its
   // original geometry and back button rather than overlaying it on small screens.
   safe=screen.width>=316;
  }else if(screen instanceof InventoryScreen inv){
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
   b.selected=Objects.equals(b.tabId,current(screen));b.active=!b.selected;
   b.visible=size>0&&slot>=0&&slot<size;
   if(b.visible){b.setX(x);b.setY(y+slot*TabLayout.STEP);}
  }
  rail.prev.visible=paged;rail.next.visible=paged;
  rail.prev.active=rail.page>0;rail.next.active=rail.page+1<TabLayout.pages(rail.tabs.size(),size);
  if(paged){rail.prev.setX(x);rail.prev.setY(y+size*TabLayout.STEP);rail.next.setX(x);rail.next.setY(y+(size+1)*TabLayout.STEP);}
 }
 /** Used by legacy entry points: retain them if the shared rail cannot fit. */
 public static boolean replacesLegacy(Screen screen,String namespace){
  if(!WorldLogicTabs.hasNativeView(namespace)||!(screen instanceof InventoryScreen inv)||NativePeerViews.anatomy(screen))return false;
  int right=inv.getGuiLeft()+inv.getXSize(),x=right+4;
  var player=Minecraft.getInstance().player;
  if(player!=null&&!player.getActiveEffects().isEmpty())x=right+(screen.width-right>=124?124:36);
  return x+TabLayout.SIZE<=screen.width&&TabLayout.pageSize(WorldLogicTabs.available().size(),TabLayout.capacity(Math.min(inv.getYSize(),screen.height-inv.getGuiTop())))>0;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void init(ScreenEvent.Init.Post e){
  Screen s=e.getScreen();
  if(replacesLegacy(s,"worldlogic_thermal")){
   // Remove only the original Temperature inventory shortcut, not vanilla or third-party buttons.
   var old=e.getListenersList().stream().filter(w->w instanceof Button btn&&btn.getMessage().equals(Component.translatable("screen.worldlogic_thermal.title"))).toList();
   old.forEach(e::removeListener);
  }
  // Built-in screens install during init too, including rebuildWidgets.
  if(!(s instanceof GuideScreen)&&!(s instanceof AnalyticsScreen)&&!(s instanceof GuideIndexScreen)&&!(s instanceof GuideRecipesScreen)&&!(s instanceof DashboardScreen)&&current(s)!=null)install(s,e::addListener);
 }
 @SubscribeEvent public static void render(ScreenEvent.Render.Pre e){
  var rail=RAILS.get(e.getScreen());if(rail!=null)layout(e.getScreen(),rail);
 }
 // Trauma deliberately omits InventoryScreen.render() when anatomy is open.
 // Draw and dispatch only our rail here, leaving every anatomy/treatment control intact.
 @SubscribeEvent public static void anatomyRender(ScreenEvent.Render.Post e){
  if(!NativePeerViews.anatomy(e.getScreen()))return;
  var rail=RAILS.get(e.getScreen());if(rail==null)return;
  var all=new ArrayList<IconButton>(rail.tabs);all.add(rail.prev);all.add(rail.next);
  for(var b:all)if(b.visible)b.render(e.getGuiGraphics(),e.getMouseX(),e.getMouseY(),e.getPartialTick());
  for(var b:all)if(b.visible&&b.isMouseOver(e.getMouseX(),e.getMouseY())){
   e.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,b.caption,e.getMouseX(),e.getMouseY());break;
  }
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void anatomyClick(ScreenEvent.MouseButtonPressed.Pre e){
  if(!NativePeerViews.anatomy(e.getScreen())||e.getButton()!=0)return;
  var rail=RAILS.get(e.getScreen());if(rail==null)return;
  var all=new ArrayList<IconButton>(rail.tabs);all.add(rail.prev);all.add(rail.next);
  for(var b:all)if(b.visible&&b.isMouseOver(e.getMouseX(),e.getMouseY())){
   if(b.active)b.mouseClicked(e.getMouseX(),e.getMouseY(),e.getButton());
   e.setCanceled(true);return;
  }
 }
}
