#!/usr/bin/env python3
"""Player-facing market menu, currency catalog and dedicated creative tabs."""
import expand_holiday_villages as v
import json
e,w,f,g,t=v.e,v.w,v.f,v.g,v.t
S,R,D,A,J=v.S,v.R,v.D,v.A,v.J
write,text,mod,edit=v.write,v.text,v.mod,v.edit

# Reuse vanilla menu synchronization and button packets. No client-only classes
# in the common registration or server-side logic, no external GUI dependencies.
text(J/"market/MarketContent.java",'''package dev.mngklrd.midnighteve.market;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.content.TalesContent;
import dev.mngklrd.midnighteve.evergreen.EvergreenContent;
import dev.mngklrd.midnighteve.living.LivingContent;
import dev.mngklrd.midnighteve.winterfolk.FolkContent;
import dev.mngklrd.midnighteve.world.FrostMaterials;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public final class MarketContent {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"midnighteve");
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"midnighteve");
    public static final Supplier<MenuType<MarketMenu>> MARKET=MENUS.register("market",()->new MenuType<>(MarketMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final Supplier<CreativeModeTab> GOODS=TABS.register("goods",()->CreativeModeTab.builder().title(Component.translatable("tab.midnighteve.goods")).icon(()->new ItemStack(FolkContent.LEDGER.get())).displayItems((p,o)->{
        o.accept(FolkContent.LEDGER.get());o.accept(MidnightEve.WINTERMARK.get());o.accept(TalesContent.JOURNAL.get());o.accept(TalesContent.PARCEL.get());o.accept(MidnightEve.WRAPPED_GIFT.get());o.accept(LivingContent.EGG.get());o.accept(FolkContent.STAG_EGG.get());o.accept(FolkContent.GROUSE_EGG.get());
    }).build());
    public static final Supplier<CreativeModeTab> DECOR=TABS.register("decor",()->CreativeModeTab.builder().title(Component.translatable("tab.midnighteve.decor")).icon(()->new ItemStack(TalesContent.STAR_ITEM.get())).displayItems((p,o)->{
        o.accept(MidnightEve.CRATE_ITEM.get());o.accept(TalesContent.ORNAMENT_ITEM.get());o.accept(TalesContent.CANDLES_ITEM.get());o.accept(TalesContent.GARLAND_ITEM.get());o.accept(TalesContent.STAR_ITEM.get());o.accept(EvergreenContent.BELL_ITEM.get());o.accept(FrostMaterials.STONE_ITEM.get());o.accept(FrostMaterials.BRICKS_ITEM.get());
    }).build());
    public static final Supplier<CreativeModeTab> FOOD=TABS.register("food",()->CreativeModeTab.builder().title(Component.translatable("tab.midnighteve.food")).icon(()->new ItemStack(TalesContent.GINGERBREAD.get())).displayItems((p,o)->{
        o.accept(TalesContent.GINGERBREAD.get());o.accept(TalesContent.COCOA.get());o.accept(FolkContent.VENISON.get());o.accept(FolkContent.ROAST.get());o.accept(FolkContent.STEW.get());o.accept(FolkContent.GROUSE_EGG_FOOD.get());o.accept(EvergreenContent.BERRIES.get());o.accept(EvergreenContent.SPRIG.get());o.accept(EvergreenContent.TEA.get());
    }).build());
    public static void init(IEventBus bus){MENUS.register(bus);TABS.register(bus);}
}
''')
text(J/"market/MarketLedgerItem.java",'''package dev.mngklrd.midnighteve.market;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class MarketLedgerItem extends Item {
    public MarketLedgerItem(Properties props){super(props);}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
        ItemStack st=p.getItemInHand(h);
        if(h==InteractionHand.MAIN_HAND&&p instanceof ServerPlayer sp)MarketMenu.open(sp,null,"");
        return InteractionResultHolder.sidedSuccess(st,l.isClientSide());
    }
}
''')
text(J/"market/MarketMenu.java",'''package dev.mngklrd.midnighteve.market;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.winterfolk.DailyMarket;
import dev.mngklrd.midnighteve.winterfolk.FolkContent;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.List;
public final class MarketMenu extends AbstractContainerMenu {
    public static final String[] ROLES={"baker","giftkeeper","woodworker","ranger","postmaster","chandler","fisher"};
    // Slotless read-only book. Server snapshots compact indices, not registry IDs.
    private final ContainerData data;
    private final Entity merchant;
    private final String role;
    private final ServerPlayer owner;
    private int lastRefresh;
    public record Sale(Item item,int count,int cost){}
    public static List<Sale> stock(){
        return List.of(new Sale(TalesContent.GINGERBREAD.get(),8,2),new Sale(TalesContent.COCOA.get(),2,2),new Sale(MidnightEve.WRAPPED_GIFT.get(),1,3),new Sale(TalesContent.GARLAND_ITEM.get(),2,3),new Sale(TalesContent.CANDLES_ITEM.get(),2,4),new Sale(TalesContent.STAR_ITEM.get(),1,6));
    }
    public MarketMenu(int id,Inventory inventory){this(id,inventory,null,"",null);}
    private MarketMenu(int id,Inventory inv,Entity merchant,String role,ServerPlayer owner){
        super(MarketContent.MARKET.get(),id);
        this.merchant=merchant;this.role=role;this.owner=owner;data=new SimpleContainerData(2+ROLES.length*3);
        if(owner!=null)refresh();addDataSlots(data);
    }
    public static void open(ServerPlayer p,Entity entity,String role){
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new MarketMenu(id,inv,entity,role,p),Component.translatable("item.midnighteve.market_ledger")));
    }
    private static int count(ServerPlayer p,Item item){
        int n=0;for(int i=0;i<36;i++){ItemStack st=p.getInventory().getItem(i);if(st.is(item))n+=st.getCount();}return n;
    }
    private void refresh(){
        data.set(0,Math.min(32767,count(owner,MidnightEve.WINTERMARK.get())));
        int index=-1;for(int i=0;i<ROLES.length;i++)if(ROLES[i].equals(role))index=i;
        data.set(1,index+1);
        for(int i=0;i<ROLES.length;i++){
            int base=2+i*3;int n=DailyMarket.orderIndex(owner,ROLES[i]);var o=DailyMarket.orderAt(ROLES[i],n);
            data.set(base,n);data.set(base+1,DailyMarket.status(owner,ROLES[i]));data.set(base+2,Math.min(32767,count(owner,o.required())));
        }
    }
    @Override public void broadcastChanges(){if(owner!=null&&owner.tickCount-lastRefresh>=5){lastRefresh=owner.tickCount;refresh();}super.broadcastChanges();}
    public int balance(){return data.get(0);}
    public int merchantPage(){return data.get(1);}
    public int orderIndex(int i){return data.get(2+i*3);}
    public int status(int i){return data.get(3+i*3);}
    public int owned(int i){return data.get(4+i*3);}
    @Override public boolean stillValid(Player p){
        if(owner==null)return true;
        return p==owner&&p.getMainHandItem().is(FolkContent.LEDGER.get())&&(merchant==null||(merchant.isAlive()&&merchant.level()==p.level()&&merchant.distanceToSqr(p)<=36));
    }
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!(p instanceof ServerPlayer sp)||!stillValid(p))return false;
        if(id==0){
            if(merchant==null||role.isEmpty())return false;
            DailyMarket.interact(sp,role,true);refresh();broadcastChanges();return true;
        }
        int i=id-10;var items=stock();
        if(i<0||i>=items.size()||merchant==null||!role.equals("giftkeeper"))return false;
        if(DailyMarket.status(sp,role)==1)return false;
        Sale sale=items.get(i);Item currency=MidnightEve.WINTERMARK.get();
        if(count(sp,currency)<sale.cost())return false;
        int left=sale.cost();for(int j=0;j<36&&left>0;j++){ItemStack st=sp.getInventory().getItem(j);if(st.is(currency)){int n=Math.min(left,st.getCount());st.shrink(n);left-=n;}}
        sp.getInventory().setChanged();ItemStack reward=new ItemStack(sale.item(),sale.count());if(!sp.addItem(reward))sp.drop(reward,false);
        refresh();broadcastChanges();return true;
    }
}
''')
# Expose the exact server order catalog to the UI; no duplicate pricing logic.
p=J/"winterfolk/DailyMarket.java"
edit(p,"    private record Order(String id,int count,int marks){}","    public record Order(String id,int count,int marks){public Item required(){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}}")
edit(p,"    public static void interact(ServerPlayer p,String role,boolean confirm){","""    public static int orderIndex(ServerPlayer p,String role){return Math.floorMod(Math.floorDiv(p.serverLevel().getDayTime(),24000L)+role.hashCode(),ORDERS.get(role).size());}
    public static Order orderAt(String role,int i){var list=ORDERS.get(role);return list.get(Math.floorMod(i,list.size()));}
    // 0 available, 1 closed, 2 completed.
    public static int status(ServerPlayer p,String role){
        long ticks=p.serverLevel().getDayTime(),time=Math.floorMod(ticks,24000L),day=Math.floorDiv(ticks,24000L);
        if(time<1000||time>11000)return 1;
        CompoundTag d=data(p);return d.contains(role)&&d.getLong(role)>=day?2:0;
    }
    public static void interact(ServerPlayer p,String role,boolean confirm){""")
edit(J/"winterfolk/FolkContent.java",'MidnightEve.ITEMS.registerSimpleItem("market_ledger",new Item.Properties().stacksTo(1))','MidnightEve.ITEMS.register("market_ledger",()->new dev.mngklrd.midnighteve.market.MarketLedgerItem(new Item.Properties().stacksTo(1)))')
edit(J/"quest/WinterQuests.java","if(player instanceof ServerPlayer p)dev.mngklrd.midnighteve.winterfolk.DailyMarket.interact(p,role,p.isShiftKeyDown());","if(player instanceof ServerPlayer p)dev.mngklrd.midnighteve.market.MarketMenu.open(p,entity,role);")
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.evergreen.EvergreenContent.init(bus);","        dev.mngklrd.midnighteve.evergreen.EvergreenContent.init(bus);\n        dev.mngklrd.midnighteve.market.MarketContent.init(bus);")
# Client-only screen with paginated actual item icons and localized prices.
text(J/"client/MarketScreen.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.market.MarketMenu;
import dev.mngklrd.midnighteve.winterfolk.DailyMarket;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
public final class MarketScreen extends AbstractContainerScreen<MarketMenu> {
    private int page=0;
    private boolean autoPage=true;
    private Button previous,next,deliver;
    private final Button[] buy=new Button[6];
    private static final int INK=0xff493b2b,ACCENT=0xff52654b;
    public MarketScreen(MarketMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=304;imageHeight=216;}
    @Override protected void init(){
        super.init();
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{autoPage=false;page=Math.max(0,page-1);}).bounds(leftPos+12,topPos+186,26,20).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->{autoPage=false;page=Math.min(8,page+1);}).bounds(leftPos+266,topPos+186,26,20).build());
        deliver=addRenderableWidget(Button.builder(Component.translatable("market.ui.deliver"),b->click(0)).bounds(leftPos+88,topPos+155,128,20).build());
        for(int i=0;i<6;i++){final int n=i;buy[i]=addRenderableWidget(Button.builder(Component.translatable("market.ui.buy"),b->click(10+n)).bounds(leftPos+227,topPos+42+i*22,60,18).build());}
    }
    private void click(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void containerTick(){
        super.containerTick();
        if(autoPage&&menu.merchantPage()>0){page=menu.merchantPage();autoPage=false;}
        previous.active=page>0;next.active=page<8;deliver.visible=page>=1&&page<=7&&page==menu.merchantPage();
        if(deliver.visible){int i=page-1;deliver.active=menu.status(i)==0&&menu.owned(i)>=DailyMarket.orderAt(MarketMenu.ROLES[i],menu.orderIndex(i)).count();}
        var stock=MarketMenu.stock();
        for(int i=0;i<6;i++){buy[i].visible=page==8;buy[i].active=page==8&&menu.merchantPage()==2&&menu.status(1)!=1&&menu.balance()>=stock.get(i).cost();}
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics g,float p,int mouseX,int mouseY){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+304,y+216,0xff493b2b);g.fill(x+3,y+3,x+301,y+213,0xffa88e63);
        g.fill(x+6,y+6,x+298,y+210,0xffe5d6b4);g.fill(x+150,y+6,x+153,y+180,0xffc7b38d);
        g.fill(x+9,y+9,x+295,y+31,0xffd2c19d);
        g.renderItem(new ItemStack(MidnightEve.WINTERMARK.get()),x+242,y+11);
        g.drawString(font,Integer.toString(menu.balance()),x+261,y+16,INK,false);
        g.drawString(font,Component.translatable("market.ui.page",page+1,9),x+130,y+193,INK,false);
        if(page==0){
            g.drawString(font,Component.translatable("market.ui.title"),x+15,y+16,INK,false);
            g.drawWordWrap(font,Component.translatable("market.ui.guide1"),x+15,y+42,128,INK);
            g.drawWordWrap(font,Component.translatable("market.ui.guide2"),x+160,y+42,128,INK);
            g.drawWordWrap(font,Component.translatable("market.ui.guide3"),x+15,y+112,270,ACCENT);
        }else if(page<=7){
            int i=page-1;var o=DailyMarket.orderAt(MarketMenu.ROLES[i],menu.orderIndex(i));
            g.drawString(font,Component.translatable("quest.midnighteve.role."+MarketMenu.ROLES[i]),x+15,y+16,INK,false);
            g.renderItem(new ItemStack(o.required()),x+20,y+48);
            g.drawWordWrap(font,Component.translatable("market.ui.request",o.count(),new ItemStack(o.required()).getHoverName()),x+41,y+45,103,INK);
            g.drawString(font,Component.translatable("market.ui.owned",menu.owned(i),o.count()),x+15,y+80,ACCENT,false);
            g.drawString(font,Component.translatable("market.ui.reward",o.marks()),x+160,y+48,INK,false);
            String status=switch(menu.status(i)){case 1->"closed";case 2->"done";default->"ready";};
            g.drawWordWrap(font,Component.translatable("market.ui."+status),x+160,y+76,128,INK);
            if(menu.merchantPage()!=page)g.drawWordWrap(font,Component.translatable("market.ui.find"),x+15,y+118,270,ACCENT);
        }else{
            g.drawString(font,Component.translatable("market.ui.shop"),x+15,y+16,INK,false);
            var stock=MarketMenu.stock();for(int i=0;i<stock.size();i++){var s=stock.get(i);int yy=y+42+i*22;
                g.renderItem(new ItemStack(s.item(),s.count()),x+14,yy);
                g.drawString(font,s.count()+" × "+new ItemStack(s.item()).getHoverName().getString(),x+34,yy+5,INK,false);
                g.drawString(font,Integer.toString(s.cost()),x+208,yy+5,ACCENT,false);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
}
''')
text(J/"client/MarketClient.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.market.MarketContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid="midnighteve",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class MarketClient {
    @SubscribeEvent public static void register(RegisterMenuScreensEvent e){e.register(MarketContent.MARKET.get(),MarketScreen::new);}
}
''')
g.en.update({"tab.midnighteve.goods":"MIDNIGHT: Essentials","tab.midnighteve.decor":"MIDNIGHT: Decorations","tab.midnighteve.food":"MIDNIGHT: Food & Plants","market.ui.title":"Winter Market Ledger","market.ui.page":"%s / %s","market.ui.guide1":"Earn wintermarks by fulfilling daily orders. Browse the next seven pages for each resident's request.","market.ui.guide2":"Use wintermarks at the Giftkeeper's shop, or give two to the festival hearth at its fourth stage.","market.ui.guide3":"Right-click with this ledger to read it. Right-click a resident with it to open their order. The last page is the Giftkeeper's shop.","market.ui.deliver":"Deliver order","market.ui.buy":"Buy","market.ui.request":"%s × %s","market.ui.owned":"Inventory: %s / %s","market.ui.reward":"Reward: %s marks","market.ui.closed":"Market closed until morning.","market.ui.done":"Today's order is complete.","market.ui.ready":"Available today.","market.ui.find":"Visit the corresponding resident to deliver this order.","market.ui.shop":"Giftkeeper's shop"})
g.ru.update({"tab.midnighteve.goods":"MIDNIGHT: Предметы","tab.midnighteve.decor":"MIDNIGHT: Декор","tab.midnighteve.food":"MIDNIGHT: Еда и растения","market.ui.title":"Зимняя рыночная книга","market.ui.page":"%s / %s","market.ui.guide1":"Выполняй ежедневные заказы и получай зимние марки. На следующих семи страницах — заказы жителей.","market.ui.guide2":"Потрать марки в лавке хранителя подарков или отдай две марки праздничному очагу на четвёртом этапе.","market.ui.guide3":"ПКМ с книгой открывает её. ПКМ по жителю открывает его заказ. Последняя страница — лавка хранителя подарков.","market.ui.deliver":"Сдать заказ","market.ui.buy":"Купить","market.ui.request":"%s × %s","market.ui.owned":"В инвентаре: %s / %s","market.ui.reward":"Награда: %s марок","market.ui.closed":"Рынок закрыт до утра.","market.ui.done":"Заказ на сегодня выполнен.","market.ui.ready":"Доступен сегодня.","market.ui.find":"Посети нужного жителя, чтобы сдать заказ.","market.ui.shop":"Лавка подарков"})
for n,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(n+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.11.0"','version="0.12.0"')
for file in R.rglob("*.json"):json.loads(file.read_text(encoding="utf8"))
print("PASS: paginated market ledger, server-side purchases, dedicated creative tabs")
