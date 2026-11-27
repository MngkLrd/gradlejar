package dev.minemods.traumacraft.qa;
import com.mojang.authlib.GameProfile;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.medical.TreatmentManager;
import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.UUID;
/** Explicit server-only regression fixture. Not a real network-player or graphical-client test. */
public final class RegressionChecks {
    private RegressionChecks(){}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    public static void register(RegisterCommandsEvent e){
        var root=Commands.literal("traumaqa").requires(s->s.hasPermission(4)&&s.getEntity()==null);
        for(String name:new String[]{"state","treatment","bridge","damage","api","procedure","persistence","targeting","identity"})
            root.then(Commands.literal(name).executes(c->run(c.getSource(),name)));
        e.getDispatcher().register(root);
    }
    private static void setFixtureMode(ServerPlayer p,GameType mode){
        p.gameMode.changeGameModeForPlayer(mode);
    }
    private static int run(CommandSourceStack source,String name){
        FakePlayer p=FakePlayerFactory.get(source.getLevel(),new GameProfile(UUID.nameUUIDFromBytes("traumacraft-qa-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TraumaQAFixture"));
        try{
            setFixtureMode(p,GameType.SURVIVAL);
            BodyState b=BodyStates.get(p); b.reset();
            switch(name){
                case "state" -> {
                    int total=0;for(BodyPart part:BodyPart.values()){
                        total+=part.maximum;
                        require(b.health(part)==part.maximum&&b.bleeding(part)==0&&!b.fractured(part)&&!b.stabilized(part),"healthy initial zone "+part);
                    }
                    require(b.concussionTicks()==0&&b.painkillerTicks()==0,"healthy timers");
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
                    require(!b.stabilized(BodyPart.RIGHT_LEG),"opposite leg contaminated");
                    b.setFractured(BodyPart.RIGHT_LEG,true);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.SPLINT,BodyPart.RIGHT_LEG)&&b.stabilized(BodyPart.RIGHT_LEG),"independent right splint");
                    b.setFractured(BodyPart.LEFT_ARM,true);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.SPLINT,BodyPart.LEFT_ARM)&&b.stabilized(BodyPart.LEFT_ARM)&&!b.stabilized(BodyPart.RIGHT_ARM),"independent left arm");
                    b.setFractured(BodyPart.RIGHT_ARM,true);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.SPLINT,BodyPart.RIGHT_ARM)&&b.stabilized(BodyPart.RIGHT_ARM),"independent right arm");
                    b.recoverBones(4799);require(b.fractured(BodyPart.LEFT_LEG),"premature bone recovery");
                    b.recoverBones(1);for(BodyPart limb:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG,BodyPart.LEFT_ARM,BodyPart.RIGHT_ARM})require(!b.fractured(limb)&&!b.stabilized(limb),"bone recovery "+limb);
                }
                case "bridge" -> {
                    b.setFractured(BodyPart.LEFT_LEG,true);b.damage(BodyPart.RIGHT_ARM,30);
                    var survival=Class.forName("dev.minemods.worldlogic.Survival");
                    var injured=survival.getDeclaredMethod("injured",net.minecraft.world.entity.player.Player.class,long.class);injured.setAccessible(true);
                    var penalty=survival.getDeclaredMethod("penalty",net.minecraft.world.entity.player.Player.class,long.class);penalty.setAccessible(true);
                    var arm=Class.forName("dev.minemods.worldlogic.Trauma").getDeclaredMethod("arm",net.minecraft.world.entity.player.Player.class);arm.setAccessible(true);
                    long now=p.level().getGameTime();
                    require((boolean)injured.invoke(null,p,now)==WorldLogicApi.ruleEnabled("injuries"),"World Logic injury predicate");
                    require((boolean)arm.invoke(null,p)==WorldLogicApi.ruleEnabled("shoulder_impact"),"World Logic arm predicate");
                    if(WorldLogicApi.ruleEnabled("injuries")) require((double)penalty.invoke(null,p,now)>=.42f,"World Logic leg penalty");
                    require(WorldLogicBridge.mergePenalty(p,.6)==.6,"penalty stacking");
                    p.getPersistentData().putLong("worldlogic_injury",now+1000);WorldLogicBridge.migrateLegacy(p);
                    require(!p.getPersistentData().contains("worldlogic_injury"),"legacy timer not removed");
                }
                case "api" -> {
                    require(WorldLogicApi.API_VERSION==1,"unsupported API contract");
                    require(WorldLogicBridge.injuryEnabled(p)==WorldLogicApi.ruleEnabled("injuries"),"injury rule source");
                    WorldLogicApi.recoverExertion(p,100);
                    var initial=WorldLogicApi.fatigue(p);
                    require(initial.enabled()==WorldLogicApi.ruleEnabled("exertion"),"fatigue rule source");
                    WorldLogicApi.addExertion(p,10);
                    var work=WorldLogicApi.fatigue(p);
                    require(Math.abs(WorldLogicBridge.fatigue(p)-work.fatiguePercent())<.0001,"bridge reads public API");
                    require(Math.abs(work.fatiguePercent()+work.remainingStaminaPercent()-100)<.0001,"stamina invariant");
                    if(initial.enabled()&&WorldLogicApi.ruleStrength("movement")>0)
                        require(work.fatiguePercent()>initial.fatiguePercent(),"exertion API increases fatigue");
                    WorldLogicApi.recoverExertion(p,100);
                    require(WorldLogicApi.fatigue(p).fatiguePercent()==0,"core recovery");
                    setFixtureMode(p,GameType.CREATIVE);
                    require(!WorldLogicApi.fatigue(p).enabled()&&WorldLogicBridge.fatigue(p)==0,"creative fatigue exclusion");
                    boolean rejected=false;
                    try{WorldLogicApi.addExertion(p,Double.NaN);}catch(IllegalArgumentException ex){rejected=true;}
                    require(rejected,"invalid exertion input");
                }
                case "procedure" -> {
                    var arm=BodyPart.LEFT_ARM;
                    var item=(MedicalItem)ModItems.BANDAGE.get();
                    ItemStack original=new ItemStack(item,3);
                    p.inventoryMenu.setCarried(original);
                    b.setBleeding(arm,2);
                    require(TreatmentManager.begin(p,item,arm,true,null),"begin cursor treatment");
                    require(!TreatmentManager.begin(p,item,arm,true,null),"duplicate active procedure");
                    p.tickCount+=39;
                    TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2&&original.getCount()==3,"premature effect or consumption");
                    p.tickCount++;
                    TreatmentManager.tick(p);
                    require(b.bleeding(arm)==0&&original.getCount()==2,"completion: effect and exactly one item");
                    TreatmentManager.tick(p);
                    require(original.getCount()==2,"repeat consumption");
                    b.setBleeding(arm,2);
                    require(TreatmentManager.begin(p,item,arm,true,null),"begin replacement control");
                    ItemStack replacement=new ItemStack(item,2);
                    p.inventoryMenu.setCarried(replacement);
                    p.tickCount+=40;
                    TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2&&replacement.getCount()==2&&original.getCount()==2,"same-item swap canceled");
                    require(TreatmentManager.begin(p,item,arm,true,null),"restart after cancellation");
                    TreatmentManager.cancel(p);
                    p.tickCount+=40;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2&&replacement.getCount()==2,"explicit cancellation");
                    require(TreatmentManager.begin(p,item,arm,true,null),"begin mode change control");
                    setFixtureMode(p,GameType.CREATIVE);
                    TreatmentManager.tick(p);
                    setFixtureMode(p,GameType.SURVIVAL);
                    p.tickCount+=40;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2&&replacement.getCount()==2,"creative cancellation");
                }
                case "targeting" -> {
                    var arm=BodyPart.RIGHT_ARM;
                    b.setBleeding(arm,2);
                    require(MedicalAction.autoTarget(b,MedicalItem.Type.FIELD_MEDKIT)==arm,"field kit targets bleeding even at full local HP");
                    require(MedicalAction.apply(p,b,MedicalItem.Type.FIELD_MEDKIT,arm)&&!b.hasBleeding(),"field kit stops bleeding");
                    b.setFractured(arm,true);b.stabilize(arm,4800);
                    require(!MedicalAction.canApply(b,MedicalItem.Type.TRAUMA_KIT,arm),"no wasted kit on stabilized full-health limb");
                    b.setBleeding(arm,1);
                    require(!MedicalAction.canApply(b,MedicalItem.Type.RECOVERY_SERUM,arm),"serum does not treat isolated bleeding");
                    b.damage(arm,9);
                    require(MedicalAction.canApply(b,MedicalItem.Type.RECOVERY_SERUM,arm),"serum works on tissue damage");
                }
                case "persistence" -> {
                    b.damage(BodyPart.HEAD,BodyPart.HEAD.maximum);
                    b.startCriticalGrace(600);
                    require(BodyStates.flush(p),"initial save");
                    b.tickCriticalGrace();
                    require(BodyStates.flush(p),"sub-second grace save");
                    BodyStates.forget(p);
                    b=BodyStates.get(p);
                    require(b.criticalGraceTicks()==599&&b.vitalDestroyed(),"grace survives reload exactly");
                    b.heal(BodyPart.HEAD,1);
                    require(b.criticalGraceTicks()==0,"healing cancels grace");
                    float before=b.health(BodyPart.RIGHT_ARM);
                    b.damage(BodyPart.RIGHT_ARM,Float.NaN);
                    b.heal(BodyPart.RIGHT_ARM,Float.POSITIVE_INFINITY);
                    require(b.health(BodyPart.RIGHT_ARM)==before,"nonfinite input rejected");
                    var malformed=b.serializeNBT(p.registryAccess());
                    malformed.getCompound("RIGHT_ARM").putFloat("hp",Float.NaN);
                    BodyState restored=new BodyState();restored.deserializeNBT(p.registryAccess(),malformed);
                    require(restored.health(BodyPart.RIGHT_ARM)==BodyPart.RIGHT_ARM.maximum,"invalid NBT safe fallback");
                }
                case "identity" -> {
                    require(net.neoforged.fml.ModList.get().isLoaded("worldlogic_trauma"),"new loader identity");
                    require(net.neoforged.fml.ModList.get().isLoaded("worldlogic"),"required core");
                    require(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.BANDAGE.get()).toString().equals("traumacraft:bandage"),"existing item registry identity");
                    require(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.SPLINT.get()).toString().equals("traumacraft:splint"),"existing splint identity");
                    require(BodyStates.KEY.equals("traumacraft_body"),"existing save key");
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
        finally{TreatmentManager.forget(p);p.inventoryMenu.setCarried(ItemStack.EMPTY);BodyStates.forget(p);p.getPersistentData().remove(BodyStates.KEY);p.getPersistentData().remove("worldlogic_injury");p.getPersistentData().remove("worldlogic_arm_until");}
    }
}
