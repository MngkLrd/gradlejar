package dev.minemods.excavation;
import java.util.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class ExcavationQA {
    private static void check(boolean test,String message){if(!test)throw new IllegalStateException(message);}
    static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("wlexcavation").requires(s->s.hasPermission(2))
          .then(Commands.literal("qa").then(Commands.argument("pos",BlockPosArgument.blockPos())
            .then(Commands.argument("case",StringArgumentType.word()).executes(c->{
                var l=c.getSource().getLevel();var p=BlockPosArgument.getLoadedBlockPos(c,"pos");String name=StringArgumentType.getString(c,"case");
                try{
                    run(l,p,name);c.getSource().sendSuccess(()->Component.literal("Excavation QA PASS "+name),false);return 1;
                }catch(RuntimeException failure){c.getSource().sendFailure(Component.literal("Excavation QA FAIL "+name+": "+failure.getMessage()));return 0;}
            })))));
    }
    private static void aim(FakePlayer fake,BlockPos p,boolean south){
        fake.setPos(p.getX()+.5,p.getY()+.5-fake.getEyeHeight(),p.getZ()+.5+(south?2.5:-2.5));
        fake.setYRot(south?180:0);fake.setYHeadRot(south?180:0);fake.setXRot(0);fake.setOnGround(true);
    }
    private static List<ItemEntity> drops(ServerLevel l,BlockPos p){return l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2));}
    private static int oreUnits(ServerLevel l,BlockPos p,Catalog.Spec spec){return drops(l,p).stream().mapToInt(e->Catalog.units(e.getItem(),spec.family())).sum();}
    private static void run(ServerLevel l,BlockPos pos,String name){
        var state=l.getBlockState(pos);var spec=Catalog.spec(state);
        check(spec!=null&&!(state.getBlock() instanceof ChipBlock),"Fixture must be a fresh catalog block");
        check(drops(l,pos).isEmpty(),"Dirty item fixture");
        FakePlayer fake=FakePlayerFactory.getMinecraft(l);aim(fake,pos,false);
        var api=dev.minemods.worldlogic.api.WorldLogicCapabilities.find(ExcavationApi.KEY,0).orElseThrow();
        ItemStack tool=name.equals("hands")?ItemStack.EMPTY:new ItemStack(spec.soil()?(name.equals("soilfast")?Items.DIAMOND_SHOVEL:Items.IRON_SHOVEL):name.equals("bad")?Items.WOODEN_PICKAXE:name.equals("iron")||name.equals("switch")?Items.IRON_PICKAXE:Items.DIAMOND_PICKAXE);
        fake.setItemSlot(EquipmentSlot.MAINHAND,tool);Excavation.qaReset(fake);
        if(name.equals("bad")){
            check(api.strike(fake,pos).status()==ExcavationApi.Status.WRONG_TOOL,"Wrong tier accepted");
            check(l.getBlockState(pos).equals(state)&&drops(l,pos).isEmpty()&&tool.getDamageValue()==0,"Wrong tool changed state/yield/wear");
            check(!fake.gameMode.destroyBlock(pos),"Vanilla bypass");
            return;
        }
        var timing=api.timing(fake,pos).orElseThrow();
        int expected=timing.hits();
        check(timing.targetTicks()<=2L*timing.vanillaTicks(),"Timing cap");
        check(timing.targetTicks()==(long)timing.vanillaTicks()*(spec.ore()?4:3)/2,"Category multiplier");
        int elapsed=0;for(int n=1;n<expected;n++)elapsed+=timing.gapAfter(n);
        check(expected==1||elapsed==timing.targetTicks(),"Exact scheduled duration");
        var expectedSoil=spec.soil()?net.minecraft.world.level.block.Block.getDrops(state,l,pos,null,fake,tool):List.<ItemStack>of();
        int total=0,key=0,work=0,used=0;
        for(int i=0;i<9;i++){
            Excavation.qaReset(fake);aim(fake,pos,name.equals("switch")&&(i&1)==1);
            if(name.equals("switch")&&i==1){tool=new ItemStack(Items.DIAMOND_PICKAXE);fake.setItemSlot(EquipmentSlot.MAINHAND,tool);}
            int beforeDamage=tool.getDamageValue();
            var result=api.strike(fake,pos);
            check(result.status()==ExcavationApi.Status.ACCEPTED||result.status()==ExcavationApi.Status.BROKEN,"Strike "+i+" status "+result.status());
            if(!tool.isEmpty())check(tool.getDamageValue()==beforeDamage+1,"Durability must cost exactly one");
            used++;total+=result.emittedOreUnits();
            if(spec.ore()){
                check(result.emittedOreUnits()>0&&oreUnits(l,pos,spec)==total,"Ore reserve/entity yield mismatch");
                check(total<=9,"Reserve exceeded");
            }else check(result.emittedOreUnits()==0,"Stone emitted ore");
            if(result.status()==ExcavationApi.Status.BROKEN){
                check(l.getBlockState(pos).isAir()&&api.inspect(l,pos).isEmpty(),"Final normal break did not remove block");
                if(spec.ore())check(total==9,"Ore total must equal nine units");
                else if(spec.soil()){
                    // Random-sensitive loot (e.g. clay) is deterministic for the selected compact soils.
                    int count=expectedSoil.stream().mapToInt(ItemStack::getCount).sum();
                    check(drops(l,pos).stream().mapToInt(e->e.getItem().getCount()).sum()==count,"Soil final native loot count");
                    for(var stack:expectedSoil)check(drops(l,pos).stream().filter(e->e.getItem().is(stack.getItem())).mapToInt(e->e.getItem().getCount()).sum()==stack.getCount(),"Soil native loot type");
                }else {
                    Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.drop()));
                    check(drops(l,pos).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum()==1,"Stone must drop one final block");
                }
                if(!name.equals("switch"))check(used==expected,"Unexpected number of strikes: "+used+" expected "+expected);
                if(name.equals("iron"))check(used==5,"Iron calibration");
                if(name.equals("diamond"))check(used==3,"Diamond calibration");
                return;
            }
            var snapshot=result.remaining().orElseThrow();
            check(snapshot.work()>work&&snapshot.depthKey()!=key,"No progressive notch");
            check(NotchGeometry.get(snapshot.depthKey()).remaining()<NotchGeometry.get(key).remaining(),"Notch has no physical volume change");
            check(l.getBlockState(pos).getBlock() instanceof ChipBlock&&l.getBlockEntity(pos) instanceof ChipProgress,"Missing persistent chip");
            if(spec.ore())check(snapshot.remainingOreUnits()==9-total,"API reserve mismatch");
            else check(drops(l,pos).isEmpty(),"Stone dropped early");
            if(i==0){
                check(api.strike(fake,pos).status()==ExcavationApi.Status.COOLDOWN,"Immediate repeated hit accepted");
                check(!fake.gameMode.destroyBlock(pos),"Damaged vanilla bypass");
            }
            key=snapshot.depthKey();work=snapshot.work();
        }
        throw new IllegalStateException("Did not finish in nine strikes");
    }
}
