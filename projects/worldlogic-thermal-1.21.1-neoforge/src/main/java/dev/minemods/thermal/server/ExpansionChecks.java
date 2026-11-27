package dev.minemods.thermal.server;
import dev.minemods.thermal.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;
final class ExpansionChecks {
    private static void require(boolean ok,String label){if(!ok){org.slf4j.LoggerFactory.getLogger("WorldLogic Thermal QA").error("Expansion QA: {}",label);throw new IllegalStateException("Expansion QA: "+label);}}
    static void portable(ServerLevel l,ServerPlayer p){
        long now=ThermalPackItem.clock(l);
        var items=List.of(ThermalContent.HOT_STONE.get(),ThermalContent.LEATHER_POUCH.get(),ThermalContent.CHARCOAL_WARMER.get(),ThermalContent.HAND_FAN.get(),ThermalContent.WET_CLOTH.get(),ThermalContent.INSULATED_FLASK.get());
        for(var item:items){
            var s=new ItemStack(item);
            require(item.remaining(s,now)==0&&!item.isBarVisible(s),"unconditioned "+item.kind());
            int mode=item.kind().hot?1:-1;
            require(item.charge(s,mode,now)&&item.getBarWidth(s)==13,"full charge "+item.kind());
            require(!item.charge(s,mode,now),"no redundant recharge");
            long half=now+item.kind().seconds*10L,end=now+item.kind().seconds*20L;
            item.refresh(s,half);
            require(Math.abs(item.displayCharge(s)-.5)<.002&&item.getBarWidth(s)>=6&&item.getBarWidth(s)<=7,"half bar "+item.kind());
            require(item.remaining(s.copy(),half)>.49&&item.remaining(s.copy(),half)<.51,"copied stack retains expiry");
            item.refresh(s,end);
            require(item.remaining(s,end)==0&&item.getBarWidth(s)==0&&s.getCount()==1,"empty charge does not destroy "+item.kind());
            require(item.charge(s,mode,end),"reusable "+item.kind());
            if(item.kind().hot&&item.kind().cold)require(item.charge(s,-mode,end)&&item.mode(s)==-mode,"switch hot and cold");
            if(!item.kind().hot||!item.kind().cold)require(!item.charge(s,-mode,end),"reject wrong mode");
        }
        require(ThermalPackItem.barColor(1,1)!=ThermalPackItem.barColor(1,0)&&ThermalPackItem.barColor(-1,1)!=ThermalPackItem.barColor(-1,0)&&ThermalPackItem.barColor(1,.5)!=ThermalPackItem.barColor(-1,.5),"distinct gradients");
        require(!ThermalPackItem.heatSource(Blocks.FURNACE.defaultBlockState())&&ThermalPackItem.heatSource(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true)),"lit furnace only");
        require(ThermalPackItem.heatSource(Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT,true))&&ThermalPackItem.coldSource(Blocks.ICE.defaultBlockState()),"campfire and ice");
        var old=new ArrayList<ItemStack>();for(int i=0;i<36;i++){old.add(p.getInventory().getItem(i).copy());p.getInventory().setItem(i,ItemStack.EMPTY);}
        try{
            var stone=ThermalContent.HOT_STONE.get();var stack=new ItemStack(stone);stone.charge(stack,1,now);
            p.getInventory().setItem(9,stack);
            require(ThermalPackItem.hotbar(p,30).heat()==0,"backpack gives no warmth");
            p.getInventory().setItem(9,ItemStack.EMPTY);p.getInventory().setItem(8,stack);
            require(ThermalPackItem.hotbar(p,30).heat()>0,"last hotbar slot warms");
            p.getInventory().setItem(0,stack.copy());require(ThermalPackItem.hotbar(p,30).heat()==stone.kind().power,"duplicates do not stack");
            var pack=ThermalPackItem.hotbar(p,30);
            require(ThermalPackItem.comfort(pack,28).heat()==0&&ThermalPackItem.comfort(pack,0).heat()>0,"no overheating from portable");
            require(ThermalPackItem.comfort(pack,15,16,37).heat()==1&&ThermalPackItem.comfort(pack,0,16,38).heat()==0,"activity/insulation heat cap and body overheat guard");
            var pouch=ThermalContent.LEATHER_POUCH.get();var cold=new ItemStack(pouch);pouch.charge(cold,-1,now);p.getInventory().setItem(1,cold);
            require(ThermalPackItem.comfort(ThermalPackItem.hotbar(p,30),20).cool()==0&&ThermalPackItem.comfort(ThermalPackItem.hotbar(p,30),40).cool()>0,"cool only in heat");
        }finally{for(int i=0;i<36;i++)p.getInventory().setItem(i,old.get(i));}
    }
    static void clothes(ServerLevel l,ServerPlayer p){
        var items=List.of(ThermalContent.FUR_HAT.get(),ThermalContent.FUR_COAT.get(),ThermalContent.FUR_BOOTS.get(),ThermalContent.WOOL_UNDERWEAR.get(),ThermalContent.WAXED_CLOAK.get(),ThermalContent.FACE_MASK.get(),ThermalContent.LINEN_CLOTHES.get(),ThermalContent.DESERT_WRAP.get(),ThermalContent.STRAW_CAPE.get());
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.HEAD,EquipmentSlot.CHEST};
        for(int i=0;i<items.size();i++){
            var item=items.get(i);var slot=slots[i];var old=p.getItemBySlot(slot).copy();
            try{
                p.setItemSlot(slot,ItemStack.EMPTY);var base=EnvironmentSampler.sample(p,0).orElseThrow().exposure();
                p.setItemSlot(slot,new ItemStack(item));var sample=EnvironmentSampler.sample(p,0).orElseThrow();var equipped=sample.exposure();
                require(equipped.insulation()>=base.insulation()+item.profile().insulation()-.001,"insulation "+item);
                require(equipped.ventilation()>=base.ventilation()+item.profile().ventilation()-.001,"ventilation "+item);
                require(equipped.wind()<=base.wind()+.00001,"wind protection "+item);
                require(item.profile().rainShield()>=0&&item.profile().sunShield()>=0,"bounded protection");
            }finally{p.setItemSlot(slot,old);}
        }
    }
    static void devices(ServerLevel l,ServerPlayer p){
        BlockPos at=new BlockPos(10,100,8);
        var old=l.getBlockState(at);var above=l.getBlockState(at.above());var side=l.getBlockState(at.east());
        var list=List.of(ThermalContent.OIL_LAMP.get(),ThermalContent.BRAZIER.get(),ThermalContent.STONE_STOVE.get(),ThermalContent.FIREPLACE.get(),ThermalContent.EVAP_WALL.get(),ThermalContent.ICE_BOX.get(),ThermalContent.WIND_TOWER.get());
        try{
            for(var device:list){
                l.setBlockAndUpdate(at,device.defaultBlockState());var empty=l.getBlockState(at);
                require(device.heat(empty)==0,"empty device no heat "+device.kind());
                require(!device.accepts(new ItemStack(Items.DIRT)),"invalid fuel");
                if(device.kind()==ClimateDeviceBlock.Kind.WIND_TOWER){
                    require(!device.activate(l,at,empty)&&device.cooling(empty,40,40,1,true,1,true)>0&&device.cooling(empty,40,40,1,true,1,false)==0&&device.cooling(empty,40,40,1,true,0,true)==0,"wind tower needs wind and open sky");
                    continue;
                }
                require(device.activate(l,at,empty)&&!device.activate(l,at,l.getBlockState(at)),"single activation "+device.kind());
                var active=l.getBlockState(at);
                switch(device.kind()){
                    case LAMP,BRAZIER,STOVE,FIREPLACE -> require(device.heat(active)>0,"heat output "+device.kind());
                    case EVAP_WALL -> require(device.cooling(active,40,20,1,true,1,true)>device.cooling(active,40,90,1,true,1,true),"humidity-sensitive cooling");
                    case ICE_BOX -> require(device.cooling(active,40,20,1,true,1,true)>0&&device.cooling(active,40,20,1,true,1,true)==device.cooling(active,40,90,1,true,1,true),"ice independent of humidity");
                    default -> {}
                }
                require(device.cooling(active,0,20,1,true,1,true)==0,"no room cooling in cold");
            }
            var stove=ThermalContent.STONE_STOVE.get();l.setBlockAndUpdate(at,stove.defaultBlockState().setValue(ClimateDeviceBlock.PHASE,2));
            require(stove.heat(l.getBlockState(at))>0&&stove.heat(l.getBlockState(at))<stove.heat(stove.defaultBlockState().setValue(ClimateDeviceBlock.PHASE,1)),"retained heat is lower than active");
            l.setBlockAndUpdate(at.above(),ThermalContent.CHIMNEY.get().defaultBlockState());
            double vented=VentBlock.adjustment(l,at);require(vented>=1,"chimney nonnegative");
            l.setBlockAndUpdate(at.east(),ThermalContent.DAMPER.get().defaultBlockState().setValue(VentBlock.OPEN,false));
            require(VentBlock.adjustment(l,at)<vented,"closed damper reduces output");
            l.setBlockAndUpdate(at.above(),ThermalContent.CANOPY.get().defaultBlockState());
            require(l.getBlockState(at.above()).is(ThermalContent.CANOPY.get()),"canopy placed");
        }finally{l.setBlockAndUpdate(at,old);l.setBlockAndUpdate(at.above(),above);l.setBlockAndUpdate(at.east(),side);}
    }
    private static int count(ServerPlayer p,Item item){int count=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))count+=p.getInventory().getItem(i).getCount();return count;}
    static void textile(ServerLevel l,ServerPlayer p){
        var old=new ArrayList<ItemStack>();for(int i=0;i<36;i++){old.add(p.getInventory().getItem(i).copy());p.getInventory().setItem(i,ItemStack.EMPTY);}
        var oldOff=p.getOffhandItem().copy();var at=new BlockPos(10,100,8);var before=l.getBlockState(at);
        try{
            var spindle=ThermalContent.SPINDLE.get();var tool=new ItemStack(spindle);
            p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ThermalContent.FIBER.get(),2));
            require(spindle.operate(p,tool)&&p.getOffhandItem().isEmpty()&&count(p,ThermalContent.THREAD.get())==4&&tool.getDamageValue()==1,"spindle consumes fiber and wears");
            require(!spindle.operate(p,tool),"empty offhand rejected");
            var comb=ThermalContent.WOOL_COMB.get();tool=new ItemStack(comb);p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.WHITE_WOOL));
            require(comb.operate(p,tool)&&count(p,ThermalContent.COMBED_WOOL.get())==2,"comb wool");
            var needle=ThermalContent.BONE_NEEDLE.get();tool=new ItemStack(needle);p.setItemSlot(EquipmentSlot.MAINHAND,tool);var garment=new ItemStack(ThermalContent.FUR_COAT.get());garment.setDamageValue(20);p.setItemSlot(EquipmentSlot.OFFHAND,garment);
            require(needle.operate(p,tool)&&garment.getDamageValue()<20&&count(p,ThermalContent.THREAD.get())==3,"needle repairs with thread");
            for(var station:List.of(ThermalContent.LOOM.get(),ThermalContent.RACK.get(),ThermalContent.HIDE_FRAME.get(),ThermalContent.FELT_BOARD.get())){
                l.setBlockAndUpdate(at,station.defaultBlockState());var input=station.input();
                if(input.getItem() instanceof ThermalPackItem pack)pack.charge(input,-1,ThermalPackItem.clock(l));
                require(!station.begin(l,at,l.getBlockState(at),new ItemStack(Items.DIRT),false),"invalid textile input");
                require(station.begin(l,at,l.getBlockState(at),input,false)&&input.isEmpty(),"consumed textile batch");
                require(station.collect(l,at,l.getBlockState(at)).isEmpty(),"cannot collect before ready");
                l.setBlockAndUpdate(at,l.getBlockState(at).setValue(TextileStationBlock.PHASE,2));
                ItemStack output=station.collect(l,at,l.getBlockState(at));
                require(output.is(station.output().getItem())&&output.getCount()==station.output().getCount(),"station output");
                require(station.collect(l,at,l.getBlockState(at)).isEmpty(),"no duplicate output");
            }
        }finally{l.setBlockAndUpdate(at,before);for(int i=0;i<36;i++)p.getInventory().setItem(i,old.get(i));p.setItemSlot(EquipmentSlot.OFFHAND,oldOff);}
    }
}
