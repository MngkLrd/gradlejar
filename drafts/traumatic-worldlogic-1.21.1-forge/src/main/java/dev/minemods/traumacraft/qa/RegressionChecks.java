package dev.minemods.traumacraft.qa;
import com.mojang.authlib.GameProfile;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.worldlogic.Rules;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.RegisterCommandsEvent;
import java.util.UUID;
/** Explicit server-only regression fixture. Not a real network-player or graphical-client test. */
public final class RegressionChecks {
    private RegressionChecks(){}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    public static void register(RegisterCommandsEvent e){
        var root=Commands.literal("traumaqa").requires(s->s.hasPermission(4)&&s.getEntity()==null);
        for(String name:new String[]{"state","treatment","bridge","damage"})
            root.then(Commands.literal(name).executes(c->run(c.getSource(),name)));
        e.getDispatcher().register(root);
    }
    private static void setFixtureMode(ServerPlayer p,GameType mode){
        CompoundTag tag=new CompoundTag();tag.putInt("playerGameType",mode.getId());p.loadGameTypes(tag);
    }
    private static int run(CommandSourceStack source,String name){
        ServerPlayer p=new ServerPlayer(source.getServer(),source.getLevel(),new GameProfile(UUID.nameUUIDFromBytes("traumacraft-qa-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TraumaQAFixture"),ClientInformation.createDefault());
        try{
            setFixtureMode(p,GameType.SURVIVAL);
            BodyState b=BodyStates.get(p); b.reset();
            switch(name){
                case "state" -> {
                    int total=0;for(BodyPart part:BodyPart.values())total+=part.maximum;
                    require(total==440&&BodyPart.values().length==7,"wrong body inventory");
                    b.damage(BodyPart.LEFT_ARM,23);b.setBleeding(BodyPart.LEFT_ARM,2);
                    require(b.health(BodyPart.LEFT_ARM)==37&&b.health(BodyPart.RIGHT_ARM)==60,"localized damage");
                    b.setFractured(BodyPart.RIGHT_LEG,true);b.stabilize(BodyPart.RIGHT_LEG,4800);
                    BodyState restored=new BodyState();restored.deserializeNBT(p.registryAccess(),b.serializeNBT(p.registryAccess()));
                    require(restored.health(BodyPart.LEFT_ARM)==37&&restored.bleeding(BodyPart.LEFT_ARM)==2&&restored.stabilized(BodyPart.RIGHT_LEG),"NBT roundtrip");
                }
                case "treatment" -> {
                    require(!MedicalAction.apply(p,b,MedicalItem.Type.BANDAGE,BodyPart.HEAD),"healthy target accepted");
                    b.setBleeding(BodyPart.LEFT_ARM,2);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.BANDAGE,BodyPart.LEFT_ARM)&&!b.hasBleeding(),"bandage");
                    b.setFractured(BodyPart.LEFT_LEG,true);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.SPLINT,BodyPart.LEFT_LEG)&&b.fractured(BodyPart.LEFT_LEG)&&b.stabilized(BodyPart.LEFT_LEG),"splint must stabilize");
                    require(!MedicalAction.apply(p,b,MedicalItem.Type.SPLINT,BodyPart.LEFT_LEG),"double splint");
                    b.recoverBones(4799);require(b.fractured(BodyPart.LEFT_LEG),"premature bone recovery");
                    b.recoverBones(1);require(!b.fractured(BodyPart.LEFT_LEG),"bone recovery");
                }
                case "bridge" -> {
                    b.setFractured(BodyPart.LEFT_LEG,true);b.damage(BodyPart.RIGHT_ARM,30);
                    var survival=Class.forName("dev.minemods.worldlogic.Survival");
                    var injured=survival.getDeclaredMethod("injured",net.minecraft.world.entity.player.Player.class,long.class);injured.setAccessible(true);
                    var penalty=survival.getDeclaredMethod("penalty",net.minecraft.world.entity.player.Player.class,long.class);penalty.setAccessible(true);
                    var arm=Class.forName("dev.minemods.worldlogic.Trauma").getDeclaredMethod("arm",net.minecraft.world.entity.player.Player.class);arm.setAccessible(true);
                    long now=p.level().getGameTime();
                    require((boolean)injured.invoke(null,p,now)==Rules.on("injuries"),"World Logic injury predicate");
                    require((boolean)arm.invoke(null,p)==Rules.on("shoulder_impact"),"World Logic arm predicate");
                    if(Rules.on("injuries")) require((double)penalty.invoke(null,p,now)>=.42f,"World Logic leg penalty");
                    require(WorldLogicBridge.mergePenalty(p,.6)==.6,"penalty stacking");
                    p.getPersistentData().putLong("worldlogic_injury",now+1000);WorldLogicBridge.migrateLegacy(p);
                    require(!p.getPersistentData().contains("worldlogic_injury"),"legacy timer not removed");
                }
                case "damage" -> {
                    TraumaEvents.applyDamage(p,b,p.damageSources().fall(),5);
                    require(b.health(BodyPart.LEFT_LEG)==45&&b.health(BodyPart.RIGHT_LEG)==45&&b.fractured(BodyPart.LEFT_LEG),"single fall damage");
                    require(b.health(BodyPart.HEAD)==35&&!b.hasBleeding(),"fall negative control");
                    setFixtureMode(p,GameType.CREATIVE);
                    require(p.isCreative(),"fixture creative mode not applied");
                    TraumaEvents.applyDamage(p,b,p.damageSources().fall(),5);
                    require(b.health(BodyPart.LEFT_LEG)==45,"creative damage");
                }
            }
            source.sendSuccess(()->Component.literal("TRAUMAQA "+name+" PASS"),false);return 1;
        }catch(Exception ex){source.sendFailure(Component.literal("TRAUMAQA "+name+" FAIL: "+ex));return 0;}
        finally{BodyStates.forget(p);p.getPersistentData().remove(BodyStates.KEY);p.getPersistentData().remove("worldlogic_injury");p.getPersistentData().remove("worldlogic_arm_until");}
    }
}
