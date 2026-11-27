package dev.minemods.traumacraft.integration;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.WoundKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Compatibility for pre-0.8 Thermal, which publishes exposure but has no limb-injury sender.
 * Newer Thermal owns its own frost dose and reports through TraumaApi: never run both.
 * No independent climate simulation or vanilla damage is introduced.
 */
public final class LegacyFrostExposure {
    public static final BodyPart[] LIMBS={BodyPart.LEFT_ARM,BodyPart.RIGHT_ARM,BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG};
    private static final String KEY="traumacraft_legacy_frost";
    private static final DefaultArtifactVersion LOCAL_OWNER=new DefaultArtifactVersion("0.8.0-rc1");
    private LegacyFrostExposure(){}
    public static boolean legacyVersion(String version){
        if(version==null||!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+].*)?"))return false;
        return new DefaultArtifactVersion(version).compareTo(LOCAL_OWNER)<0;
    }
    private static boolean ownsLegacy(){
        return ModList.get().getModContainerById(ThermalBridge.MOD_ID)
            .map(c->legacyVersion(c.getModInfo().getVersion().toString())).orElse(false);
    }
    private static double bound(double x){return Double.isFinite(x)?Math.max(0,Math.min(1,x)):0;}
    public static double dose(double previous,double felt,double protection,double wet){
        double skin=felt+bound(protection)*18*(1-.75*bound(wet));
        return Math.max(0,Math.min(180,(Double.isFinite(previous)?previous:0)+(skin<-3?Math.min(3,(-3-skin)/6):-2)));
    }
    /** One second, same thresholds and tissue damage as current Thermal. */
    public static boolean advance(BodyState b,CompoundTag doses,double felt,double wet,double armProtection,double legProtection){
        if(!Double.isFinite(felt)||!Double.isFinite(wet))return false;
        boolean injured=false;
        for(BodyPart part:LIMBS){
            String key=part.name();
            double d=dose(doses.getDouble(key),felt,part.isLeg()?legProtection:armProtection,wet);
            if(d>=120){
                if(b.health(part)>0){b.wound(part,1.5f,WoundKind.FROSTBITE);injured=true;}
                d=60;
            }
            doses.putDouble(key,d);
        }
        return injured;
    }
    static double protection(ItemStack stack){
        if(stack.isEmpty())return 0;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name=id.getPath();
        if(id.getNamespace().equals(ThermalBridge.MOD_ID)){
            // Actual Thermal 0.7 ClimateGarment profiles: insulation*2.2 + windShield*.3.
            return switch(name){
                case "fur_coat" -> .996;
                case "fur_boots" -> .828;
                case "wool_underwear" -> .675;
                case "waxed_cloak" -> .471;
                case "linen_clothes" -> .066;
                case "straw_cape" -> .222;
                default -> name.contains("grass")?.55:.25;
            };
        }
        if(name.contains("grass")||name.startsWith("leather_"))return .55;
        if(name.contains("iron")||name.contains("gold")||name.contains("diamond")||name.contains("netherite"))return .12;
        return .25;
    }
    public static boolean tick(ServerPlayer p,BodyState b){
        if(!WorldLogicBridge.injuryEnabled(p)||!ownsLegacy()){clear(p);return false;}
        var root=p.getPersistentData();
        var sample=root.getCompound(ThermalBridge.MOD_ID);
        // Thermal owns and updates these legacy fields. Never infer exposure from biome or body temperature.
        if(!sample.contains("felt",Tag.TAG_ANY_NUMERIC)||!sample.contains("wet",Tag.TAG_ANY_NUMERIC)
            ||ThermalBridge.condition(p)==ThermalCondition.NONE)return false;
        double arms=protection(p.getItemBySlot(EquipmentSlot.CHEST));
        double legs=(protection(p.getItemBySlot(EquipmentSlot.LEGS))+protection(p.getItemBySlot(EquipmentSlot.FEET)))*.5;
        var doses=root.getCompound(KEY);
        boolean result=advance(b,doses,sample.getDouble("felt"),sample.getDouble("wet"),arms,legs);
        root.put(KEY,doses);
        return result;
    }
    public static void clear(Player p){p.getPersistentData().remove(KEY);}
    public static void clonePlayer(Player from,Player to,boolean death){
        clear(to);
        if(!death&&from.getPersistentData().contains(KEY,Tag.TAG_COMPOUND))
            to.getPersistentData().put(KEY,from.getPersistentData().getCompound(KEY).copy());
    }
}
