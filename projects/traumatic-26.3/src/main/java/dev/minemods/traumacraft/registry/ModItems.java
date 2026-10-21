package dev.minemods.traumacraft.registry;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TraumaCraft.MOD_ID);
    private static DeferredItem<Item> medical(String id,MedicalItem.Type kind,int size){
        return ITEMS.registerItem(id,props->new MedicalItem(props.stacksTo(size),kind));
    }
    private static DeferredItem<Item> component(String id,int size){
        return ITEMS.registerItem(id,props->new Item(props.stacksTo(size)));
    }
    public static final DeferredItem<Item> STERILE_FABRIC=component("sterile_fabric",64);
    public static final DeferredItem<Item> HERBAL_EXTRACT=component("herbal_extract",32);
    public static final DeferredItem<Item> ANTISEPTIC=component("antiseptic",32);
    public static final DeferredItem<Item> SYRINGE_BODY=component("syringe_body",64);
    public static final DeferredItem<Item> MEDICAL_POLYMER=component("medical_polymer",64);
    public static final DeferredItem<Item> BANDAGE=medical("bandage",MedicalItem.Type.BANDAGE,16);
    public static final DeferredItem<Item> SPLINT=medical("splint",MedicalItem.Type.SPLINT,16);
    public static final DeferredItem<Item> MEDKIT=medical("medkit",MedicalItem.Type.MEDKIT,8);
    public static final DeferredItem<Item> STERILE_PATCH=medical("sterile_patch",MedicalItem.Type.STERILE_PATCH,32);
    public static final DeferredItem<Item> COMPRESSION_WRAP=medical("compression_wrap",MedicalItem.Type.COMPRESSION_WRAP,16);
    public static final DeferredItem<Item> HEMOSTATIC_GEL=medical("hemostatic_gel",MedicalItem.Type.HEMOSTATIC_GEL,16);
    public static final DeferredItem<Item> TOURNIQUET=medical("tourniquet",MedicalItem.Type.TOURNIQUET,8);
    public static final DeferredItem<Item> FRACTURE_BRACE=medical("fracture_brace",MedicalItem.Type.FRACTURE_BRACE,16);
    public static final DeferredItem<Item> BONE_MENDER=medical("bone_mender",MedicalItem.Type.BONE_MENDER,8);
    public static final DeferredItem<Item> COMPACT_MEDKIT=medical("compact_medkit",MedicalItem.Type.COMPACT_MEDKIT,16);
    public static final DeferredItem<Item> FIELD_MEDKIT=medical("field_medkit",MedicalItem.Type.FIELD_MEDKIT,8);
    public static final DeferredItem<Item> TRAUMA_KIT=medical("trauma_kit",MedicalItem.Type.TRAUMA_KIT,4);
    public static final DeferredItem<Item> SURGICAL_KIT=medical("surgical_kit",MedicalItem.Type.SURGICAL_KIT,4);
    public static final DeferredItem<Item> RECOVERY_SERUM=medical("recovery_serum",MedicalItem.Type.RECOVERY_SERUM,8);
    public static final DeferredItem<Item> ADRENALINE_INJECTOR=medical("adrenaline_injector",MedicalItem.Type.ADRENALINE_INJECTOR,8);
    public static final DeferredItem<Item> GUARDIAN_INJECTOR=medical("guardian_injector",MedicalItem.Type.GUARDIAN_INJECTOR,8);
    public static final DeferredItem<Item> FOCUS_INJECTOR=medical("focus_injector",MedicalItem.Type.FOCUS_INJECTOR,8);
    public static final DeferredItem<Item> ENDURANCE_INJECTOR=medical("endurance_injector",MedicalItem.Type.ENDURANCE_INJECTOR,8);
    public static final List<DeferredItem<Item>> COMPONENTS=List.of(STERILE_FABRIC,HERBAL_EXTRACT,ANTISEPTIC,SYRINGE_BODY,MEDICAL_POLYMER);
    public static final List<DeferredItem<Item>> MEDICINE=List.of(BANDAGE,SPLINT,MEDKIT,STERILE_PATCH,COMPRESSION_WRAP,HEMOSTATIC_GEL,TOURNIQUET,FRACTURE_BRACE,
      BONE_MENDER,COMPACT_MEDKIT,FIELD_MEDKIT,TRAUMA_KIT,SURGICAL_KIT,RECOVERY_SERUM,ADRENALINE_INJECTOR,GUARDIAN_INJECTOR,FOCUS_INJECTOR,ENDURANCE_INJECTOR);
    private ModItems(){}
}
