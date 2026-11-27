package dev.minemods.worldlogicterrain.food;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.Map;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ForageChecks {
    private record Expect(int hunger,float saturation,double recovery) {}
    private static final Map<String,Expect> EXPECTED=Map.ofEntries(
        Map.entry("strawberry",new Expect(2,1.20f,2)),
        Map.entry("blueberry",new Expect(2,1.00f,2)),
        Map.entry("raspberry",new Expect(2,1.40f,2)),
        Map.entry("blackberry",new Expect(2,1.60f,0)),
        Map.entry("grape",new Expect(3,1.80f,5)),
        Map.entry("pineapple",new Expect(5,4.00f,6)),
        Map.entry("tomato",new Expect(3,2.10f,0)),
        Map.entry("cucumber",new Expect(2,0.80f,0)),
        Map.entry("radish",new Expect(2,1.80f,0)),
        Map.entry("onion",new Expect(2,1.60f,0)),
        Map.entry("cabbage",new Expect(4,4.00f,0)),
        Map.entry("bell_pepper",new Expect(3,2.70f,0))
    );
    private static final GameProfile PROFILE=new GameProfile(UUID.fromString("631a76d8-526b-47be-8e90-83cf5f0447fa"),"[TerrainQA]");
    private static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("food_check").then(Commands.argument("food",StringArgumentType.word()).executes(c->{
            String id=StringArgumentType.getString(c,"food");
            if(!EXPECTED.containsKey(id))return 0;
            try {
                check(c.getSource().getLevel(),id,EXPECTED.get(id));
                c.getSource().sendSuccess(()->Component.literal("Forage verified "+id+": loot, food, saturation, consumption, core stamina"),false);
                return 1;
            } catch (IllegalStateException ex) {c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          }))));
    }
    private static void check(ServerLevel level,String id,Expect expect) {
        var key=ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id);
        var item=BuiltInRegistries.ITEM.get(key);
        var plant=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","wild_"+id));
        require(item instanceof ForageFoodItem,"missing food "+id);
        var p=FakePlayerFactory.get(level,PROFILE);
        var pos=level.getSharedSpawnPos();
        var drops=Block.getDrops(plant.defaultBlockState(),level,pos,null,p,ItemStack.EMPTY);
        require(drops.size()==1&&drops.getFirst().is(item)&&drops.getFirst().getCount()==1,"wrong harvest "+id);
        var stack=new ItemStack(item,2);
        var food=stack.get(DataComponents.FOOD);
        require(food!=null&&food.nutrition()==expect.hunger()&&Math.abs(food.saturation()-expect.saturation())<.001&&!food.canAlwaysEat(),"food properties "+id);
        var data=p.getFoodData();
        int oldFood=data.getFoodLevel();float oldSat=data.getSaturationLevel();
        require(WorldLogicApi.fatigue(p).enabled(),"core exertion must be enabled for food QA");
        try{
            data.setFoodLevel(8);data.setSaturation(0);
            WorldLogicApi.recoverExertion(p,100);WorldLogicApi.addExertion(p,40);
            double before=WorldLogicApi.fatigue(p).fatiguePercent();
            ItemStack left=stack.finishUsingItem(level,p);
            require(left.getCount()==1,"not consumed "+id);
            require(data.getFoodLevel()==8+expect.hunger(),"hunger not restored "+id+" actual="+data.getFoodLevel());
            require(Math.abs(data.getSaturationLevel()-expect.saturation())<.01,"saturation not restored "+id);
            double after=WorldLogicApi.fatigue(p).fatiguePercent();
            require(Math.abs(after-Math.max(0,before-expect.recovery()))<.001,"wrong core recovery "+id+" before="+before+" after="+after);
        }finally{
            data.setFoodLevel(oldFood);data.setSaturation(oldSat);
            WorldLogicApi.recoverExertion(p,100);
        }
    }
}
