package dev.minemods.traumacraft.registry;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TraumaCraft.MOD_ID);
    private static RegistryObject<Item> medicine(String name, MedicalItem.Type type, int stack) {
        return ITEMS.register(name, () -> new MedicalItem(new Item.Properties().stacksTo(stack), type));
    }
    private static RegistryObject<Item> component(String name, int stack) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(stack)));
    }
    public static final RegistryObject<Item> STERILE_FABRIC = component("sterile_fabric",64);
    public static final RegistryObject<Item> HERBAL_EXTRACT = component("herbal_extract",32);
    public static final RegistryObject<Item> ANTISEPTIC = component("antiseptic",32);
    public static final RegistryObject<Item> SYRINGE_BODY = component("syringe_body",64);
    public static final RegistryObject<Item> MEDICAL_POLYMER = component("medical_polymer",64);
    public static final RegistryObject<Item> BANDAGE=medicine("bandage",MedicalItem.Type.BANDAGE,16);
    public static final RegistryObject<Item> SPLINT=medicine("splint",MedicalItem.Type.SPLINT,16);
    public static final RegistryObject<Item> MEDKIT=medicine("medkit",MedicalItem.Type.MEDKIT,8);
    public static final RegistryObject<Item> STERILE_PATCH=medicine("sterile_patch",MedicalItem.Type.STERILE_PATCH,32);
    public static final RegistryObject<Item> COMPRESSION_WRAP=medicine("compression_wrap",MedicalItem.Type.COMPRESSION_WRAP,16);
    public static final RegistryObject<Item> HEMOSTATIC_GEL=medicine("hemostatic_gel",MedicalItem.Type.HEMOSTATIC_GEL,16);
    public static final RegistryObject<Item> TOURNIQUET=medicine("tourniquet",MedicalItem.Type.TOURNIQUET,8);
    public static final RegistryObject<Item> FRACTURE_BRACE=medicine("fracture_brace",MedicalItem.Type.FRACTURE_BRACE,16);
    public static final RegistryObject<Item> BONE_MENDER=medicine("bone_mender",MedicalItem.Type.BONE_MENDER,8);
    public static final RegistryObject<Item> COMPACT_MEDKIT=medicine("compact_medkit",MedicalItem.Type.COMPACT_MEDKIT,16);
    public static final RegistryObject<Item> FIELD_MEDKIT=medicine("field_medkit",MedicalItem.Type.FIELD_MEDKIT,8);
    public static final RegistryObject<Item> TRAUMA_KIT=medicine("trauma_kit",MedicalItem.Type.TRAUMA_KIT,4);
    public static final RegistryObject<Item> SURGICAL_KIT=medicine("surgical_kit",MedicalItem.Type.SURGICAL_KIT,4);
    public static final RegistryObject<Item> RECOVERY_SERUM=medicine("recovery_serum",MedicalItem.Type.RECOVERY_SERUM,8);
    public static final RegistryObject<Item> ADRENALINE_INJECTOR=medicine("adrenaline_injector",MedicalItem.Type.ADRENALINE_INJECTOR,8);
    public static final RegistryObject<Item> GUARDIAN_INJECTOR=medicine("guardian_injector",MedicalItem.Type.GUARDIAN_INJECTOR,8);
    public static final RegistryObject<Item> FOCUS_INJECTOR=medicine("focus_injector",MedicalItem.Type.FOCUS_INJECTOR,8);
    public static final RegistryObject<Item> ENDURANCE_INJECTOR=medicine("endurance_injector",MedicalItem.Type.ENDURANCE_INJECTOR,8);
    public static final List<RegistryObject<Item>> COMPONENTS=List.of(STERILE_FABRIC,HERBAL_EXTRACT,ANTISEPTIC,SYRINGE_BODY,MEDICAL_POLYMER);
    public static final List<RegistryObject<Item>> MEDICINE=List.of(BANDAGE,SPLINT,MEDKIT,STERILE_PATCH,COMPRESSION_WRAP,HEMOSTATIC_GEL,TOURNIQUET,
        FRACTURE_BRACE,BONE_MENDER,COMPACT_MEDKIT,FIELD_MEDKIT,TRAUMA_KIT,SURGICAL_KIT,RECOVERY_SERUM,
        ADRENALINE_INJECTOR,GUARDIAN_INJECTOR,FOCUS_INJECTOR,ENDURANCE_INJECTOR);
    private ModItems(){}
}
