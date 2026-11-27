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
        for(String name:new String[]{"state","treatment","bridge","damage","api","procedure","persistence","targeting","identity","thermal","respawn","bloodloss","physiology"})
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
                    require(original.getCount()==2,"immediate one item consumption");
                    require(!TreatmentManager.begin(p,item,arm,true,null)&&original.getCount()==2,"duplicate rejected without consumption");
                    p.tickCount+=39;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2,"effect not yet applied");
                    ItemStack replacement=new ItemStack(item,2);
                    p.inventoryMenu.setCarried(replacement);
                    p.tickCount++;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==0&&replacement.getCount()==2&&original.getCount()==2,"cursor replacement does not interrupt or consume twice");
                    TreatmentManager.tick(p);
                    require(replacement.getCount()==2,"repeat consumption absent");
                    require(!TreatmentManager.begin(p,item,arm,true,null)&&replacement.getCount()==2,"invalid target no consumption");
                    b.setBleeding(arm,2);
                    require(TreatmentManager.begin(p,item,arm,true,null)&&replacement.getCount()==1,"second procedure consumes one");
                    p.inventoryMenu.setCarried(ItemStack.EMPTY);
                    p.tickCount+=40;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==0,"empty cursor cannot interrupt accepted procedure");
                    b.setBleeding(arm,2);p.inventoryMenu.setCarried(replacement);
                    require(TreatmentManager.begin(p,item,arm,true,null),"final procedure");
                    TreatmentManager.cancel(p);
                    p.tickCount+=40;TreatmentManager.tick(p);
                    require(b.bleeding(arm)==2&&replacement.isEmpty(),"explicit cancellation does not refund consumed treatment");
                }
                case "physiology" -> {
                    require(b.bloodMl()==5000&&b.pain()==0&&b.shockStage()==0,"healthy physiology");
                    require(PhysicalFunction.mining(b,BodyPart.RIGHT_ARM)==1&&PhysicalFunction.canSprint(b),"healthy capabilities");
                    b.damage(BodyPart.LEFT_ARM,60);
                    require(PhysicalFunction.arm(b,BodyPart.LEFT_ARM)==0&&PhysicalFunction.arm(b,BodyPart.RIGHT_ARM)==1,"independent arms");
                    require(!PhysicalFunction.canUse(b,BodyPart.LEFT_ARM,false)&&PhysicalFunction.canUse(b,BodyPart.RIGHT_ARM,false)&&!PhysicalFunction.canUse(b,BodyPart.RIGHT_ARM,true),"hand and two-hand controls");
                    b.setFractured(BodyPart.LEFT_LEG,true);
                    require(!PhysicalFunction.canSprint(b)&&PhysicalFunction.jump(b)<.5,"fracture limits movement");
                    float broken=b.limbFunction(BodyPart.LEFT_LEG);
                    b.stabilize(BodyPart.LEFT_LEG,4800);
                    require(b.limbFunction(BodyPart.LEFT_LEG)>broken,"splint restores partial function");
                    float pain=b.pain();b.setPainkillerTicks(100);
                    require(b.pain()<pain&&b.limbFunction(BodyPart.LEFT_ARM)==0,"pain relief not limb repair");
                    b.reset();b.setBleeding(BodyPart.RIGHT_ARM,1);b.bleedStep();require(b.bloodMl()==4982,"weak blood volume loss");
                    b.recoverBlood(100);require(b.bloodMl()==4982,"no blood recovery while bleeding");
                    b.reset();b.setBleeding(BodyPart.RIGHT_ARM,2);b.bleedStep();require(b.bloodMl()==4935,"severe blood volume loss");
                    b.stopBleeding(BodyPart.RIGHT_ARM);b.recoverBlood(6);require(b.bloodMl()==4941,"rest recovery");
                    b.loseBlood(3000);require(b.shockStage()==3&&!PhysicalFunction.canSprint(b)&&PhysicalFunction.movementPenalty(b)>=.58f,"critical shock");
                    BodyState restored=new BodyState();restored.deserializeNBT(p.registryAccess(),b.serializeNBT(p.registryAccess()));
                    require(restored.bloodMl()==b.bloodMl()&&restored.shockStage()==3,"persistent blood");
                    b.loseBlood(1000);require(b.fatalBloodLoss(),"fatal threshold");
                    b.reset();require(!b.fatalBloodLoss()&&b.bloodMl()==5000&&b.pain()==0,"new life resets physiology");
                    if(WorldLogicApi.ruleEnabled("injuries")){
                        var main=WorldLogicBridge.mainArm(p);
                        var mining=Class.forName("dev.minemods.worldlogic.Trauma").getDeclaredMethod("mining",net.minecraft.world.entity.player.Player.class);
                        mining.setAccessible(true);
                        b.damage(main,main.maximum);
                        require((double)mining.invoke(null,p)==0,"core mining replacement");
                        dev.minemods.traumacraft.event.FunctionEvents.update(p);
                        var a=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("traumacraft","arm_attack_speed");
                        require(a!=null&&a.getModifier(id)!=null&&a.getModifier(id).amount()<0,"attack speed modifier");
                        b.reset();dev.minemods.traumacraft.event.FunctionEvents.update(p);
                        require(a.getModifier(id)==null,"healthy removes modifier");
                    }
                }
                case "bloodloss" -> {
                    b.damage(BodyPart.LEFT_ARM,60);
                    require(!b.vitalDestroyed(),"black arm not lethal");
                    require(!b.healMostDamaged(10)&&b.health(BodyPart.LEFT_ARM)==0,"destroyed limb cannot regenerate passively");
                    b.bleedStep();
                    for(var part:BodyPart.values())if(part!=BodyPart.LEFT_ARM)
                        require(b.health(part)==part.maximum,"black nonbleeding arm cannot drain "+part);
                    b.setBleeding(BodyPart.LEFT_ARM,1);
                    b.bleedStep();
                    for(var part:BodyPart.values())if(part!=BodyPart.LEFT_ARM)
                        require(b.health(part)<part.maximum,"weak systemic loss "+part);
                    float weak=BodyPart.HEAD.maximum-b.health(BodyPart.HEAD);
                    b.reset();b.damage(BodyPart.LEFT_ARM,60);b.setBleeding(BodyPart.LEFT_ARM,2);
                    b.bleedStep();
                    require(BodyPart.HEAD.maximum-b.health(BodyPart.HEAD)>weak,"strong bleed faster");
                    b.stopBleeding(BodyPart.LEFT_ARM);float head=b.health(BodyPart.HEAD);b.bleedStep();
                    require(b.health(BodyPart.HEAD)==head,"bandage stops systemic loss");
                    b.reset();b.setBleeding(BodyPart.LEFT_ARM,2);b.bleedStep();
                    require(b.health(BodyPart.HEAD)==BodyPart.HEAD.maximum&&b.health(BodyPart.LEFT_ARM)<60,"local wound before destruction");
                    b.damage(BodyPart.HEAD,35);require(b.vitalDestroyed(),"black head lethal predicate");
                    b.reset();b.damage(BodyPart.CHEST,85);require(b.vitalDestroyed(),"black chest lethal predicate");
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
                case "thermal" -> {
                    var none=dev.minemods.traumacraft.integration.ThermalCondition.NONE;
                    var cold=dev.minemods.traumacraft.integration.ThermalCondition.VERY_COLD;
                    var extreme=dev.minemods.traumacraft.integration.ThermalCondition.HYPERTHERMIA;
                    var key=dev.minemods.traumacraft.integration.ThermalBridge.MOD_ID;
                    var tag=new net.minecraft.nbt.CompoundTag();
                    tag.putDouble("body",35.5);tag.putString("stage","VERY_COLD");
                    p.getPersistentData().put(key,tag.copy());
                    if(!net.neoforged.fml.ModList.get().isLoaded(key))
                        require(dev.minemods.traumacraft.integration.ThermalBridge.condition(p)==none,"absent addon ignores NBT");
                    else require(dev.minemods.traumacraft.integration.ThermalBridge.condition(p)==cold,"installed addon stage");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.read(true,true,"VERY_COLD",35.5)==cold,"thermal contract");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.read(false,true,"VERY_COLD",35.5)==none,"optional absence");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.read(true,false,"VERY_COLD",35.5)==none,"inactive player");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.read(true,true,"INVALID",37)==none,"unknown stage");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.read(true,true,"VERY_COLD",Double.NaN)==none,"invalid temperature");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.actual("HYPERTHERMIA","SUNSTROKE",40)==dev.minemods.traumacraft.integration.ThermalCondition.SUNSTROKE,"confirmed solar injury");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.actual("HYPERTHERMIA","HEATSTROKE",40)==dev.minemods.traumacraft.integration.ThermalCondition.HEATSTROKE,"confirmed heat injury");
                    require(dev.minemods.traumacraft.integration.ThermalCondition.actual("COMFORTABLE","SUNSTROKE",37)==dev.minemods.traumacraft.integration.ThermalCondition.COMFORTABLE,"stale diagnosis ignored");
                    b.damage(BodyPart.LEFT_ARM,10);b.setFractured(BodyPart.LEFT_LEG,true);b.stabilize(BodyPart.LEFT_LEG,20);
                    float hp=b.health(BodyPart.LEFT_ARM);
                    dev.minemods.traumacraft.integration.ThermalBridge.recover(b,extreme,true);
                    require(b.health(BodyPart.LEFT_ARM)==hp&&b.stabilized(BodyPart.LEFT_LEG),"extreme pauses recovery");
                    dev.minemods.traumacraft.integration.ThermalBridge.recover(b,cold,true);
                    require(b.health(BodyPart.LEFT_ARM)>hp&&b.stabilized(BodyPart.LEFT_LEG),"half-speed recovery");
                    dev.minemods.traumacraft.integration.ThermalBridge.recover(b,cold,true);
                    require(!b.fractured(BodyPart.LEFT_LEG),"two half-speed recovery steps");
                    b.damage(BodyPart.RIGHT_ARM,10);float right=b.health(BodyPart.RIGHT_ARM);
                    require(MedicalAction.apply(p,b,MedicalItem.Type.MEDKIT,BodyPart.RIGHT_ARM)&&b.health(BodyPart.RIGHT_ARM)>right,"medicine remains functional");
                    require(p.getPersistentData().getCompound(key).equals(tag),"thermal state not mutated");
                    p.getPersistentData().remove(key);
                }
                case "respawn" -> {
                    var legacy=p.getPersistentData();
                    // First join with old core flags: no fabricated anatomy.
                    BodyStates.forget(p);legacy.remove(BodyStates.KEY);
                    legacy.putLong("worldlogic_injury",Long.MAX_VALUE);
                    legacy.putLong("worldlogic_arm_until",Long.MAX_VALUE);
                    TraumaEvents.initializeLogin(p);
                    require(!BodyStates.get(p).hasInjury(),"first join must be healthy");
                    require(!legacy.contains("worldlogic_injury")&&!legacy.contains("worldlogic_arm_until"),"legacy timers cleared");
                    b=BodyStates.get(p);
                    b.damage(BodyPart.LEFT_ARM,23);b.setBleeding(BodyPart.LEFT_ARM,2);
                    b.setFractured(BodyPart.RIGHT_LEG,true);b.stabilize(BodyPart.RIGHT_LEG,4800);
                    b.addConcussion(600);b.setPainkillerTicks(100);
                    BodyStates.flush(p);BodyStates.forget(p);
                    TraumaEvents.initializeLogin(p);
                    require(BodyStates.get(p).health(BodyPart.LEFT_ARM)==37&&BodyStates.get(p).stabilized(BodyPart.RIGHT_LEG),"reconnect preserves injury");
                    // Separate entity with copied persistent data, as on cloning.
                    FakePlayer next=FakePlayerFactory.get(source.getLevel(),new GameProfile(UUID.nameUUIDFromBytes("trauma-qa-respawn-target".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"TraumaQARespawn"));
                    try{
                        setFixtureMode(next,GameType.SURVIVAL);
                        BodyStates.copy(p,next,false);
                        require(BodyStates.get(next).health(BodyPart.LEFT_ARM)==37&&BodyStates.get(next).stabilized(BodyPart.RIGHT_LEG),"nondeath clone retains state");
                        BodyStates.copy(p,next,true);
                        require(!BodyStates.get(next).hasInjury(),"death clone healthy");
                        // Simulate copied old data after clone, including cached injury.
                        next.getPersistentData().put(BodyStates.KEY,p.getPersistentData().getCompound(BodyStates.KEY).copy());
                        BodyStates.forget(next);
                        require(BodyStates.get(next).hasInjury(),"stale copied NBT fixture");
                        next.getPersistentData().putLong("worldlogic_injury",Long.MAX_VALUE);
                        next.getPersistentData().putLong("worldlogic_arm_until",Long.MAX_VALUE);
                        TraumaEvents.resetAfterDeath(next);
                        BodyStates.forget(next);
                        BodyState clean=BodyStates.get(next);
                        for(BodyPart part:BodyPart.values())
                            require(clean.health(part)==part.maximum&&clean.bleeding(part)==0&&!clean.fractured(part)&&!clean.stabilized(part),"respawn clean "+part);
                        require(clean.concussionTicks()==0&&clean.painkillerTicks()==0&&clean.criticalGraceTicks()==0,"respawn clears timers");
                        require(!next.getPersistentData().contains("worldlogic_injury")&&!next.getPersistentData().contains("worldlogic_arm_until"),"respawn clears legacy");
                    }finally{BodyStates.forget(next);next.getPersistentData().remove(BodyStates.KEY);}
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
        finally{p.getPersistentData().remove("worldlogic_thermal");TreatmentManager.forget(p);p.inventoryMenu.setCarried(ItemStack.EMPTY);BodyStates.forget(p);p.getPersistentData().remove(BodyStates.KEY);p.getPersistentData().remove("worldlogic_injury");p.getPersistentData().remove("worldlogic_arm_until");}
    }
}
