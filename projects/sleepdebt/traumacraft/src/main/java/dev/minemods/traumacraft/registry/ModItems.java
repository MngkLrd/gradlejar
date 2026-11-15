package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.item.MedicalPouchItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, TraumaCraft.MOD_ID);

    private static DeferredHolder<Item, Item> medical(String id, MedicalItem.Type type, int stack) {
        return ITEMS.register(id, () -> new MedicalItem(new Item.Properties().stacksTo(stack), type));
    }
    private static DeferredHolder<Item, Item> supply(String id, int stack) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(stack)));
    }
    public static final DeferredHolder<Item, Item> MEDICAL_POUCH = ITEMS.register("medical_pouch",
            () -> new MedicalPouchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> OVERSIZED_MEDKIT = medical("oversized_medkit", MedicalItem.Type.TRAUMA_KIT, 1);

    // 5 newly craftable medical components.
    public static final DeferredHolder<Item, Item> STERILE_FABRIC = supply("sterile_fabric", 64);
    public static final DeferredHolder<Item, Item> HERBAL_EXTRACT = supply("herbal_extract", 32);
    public static final DeferredHolder<Item, Item> ANTISEPTIC = supply("antiseptic", 32);
    public static final DeferredHolder<Item, Item> SYRINGE_BODY = supply("syringe_body", 64);
    public static final DeferredHolder<Item, Item> MEDICAL_POLYMER = supply("medical_polymer", 64);

    // Existing 0.2.0 items keep their registry IDs and effects.
    public static final DeferredHolder<Item, Item> ANALGESIC_TABLETS =
            medical("analgesic_tablets", MedicalItem.Type.ANALGESIC_TABLETS, 16);
    public static final DeferredHolder<Item, Item> RAG_DRESSING = medical("rag_dressing", MedicalItem.Type.RAG_DRESSING, 32);
    public static final DeferredHolder<Item, Item> BANDAGE = medical("bandage", MedicalItem.Type.BANDAGE, 16);
    public static final DeferredHolder<Item, Item> SPLINT = medical("splint", MedicalItem.Type.SPLINT, 16);
    public static final DeferredHolder<Item, Item> MEDKIT = medical("medkit", MedicalItem.Type.MEDKIT, 8);

    // 15 newly added functional healing/buff items.
    public static final DeferredHolder<Item, Item> STERILE_PATCH =
            medical("sterile_patch", MedicalItem.Type.STERILE_PATCH, 32);
    public static final DeferredHolder<Item, Item> COMPRESSION_WRAP =
            medical("compression_wrap", MedicalItem.Type.COMPRESSION_WRAP, 16);
    public static final DeferredHolder<Item, Item> HEMOSTATIC_GEL =
            medical("hemostatic_gel", MedicalItem.Type.HEMOSTATIC_GEL, 16);
    public static final DeferredHolder<Item, Item> TOURNIQUET =
            medical("tourniquet", MedicalItem.Type.TOURNIQUET, 8);
    public static final DeferredHolder<Item, Item> FRACTURE_BRACE =
            medical("fracture_brace", MedicalItem.Type.FRACTURE_BRACE, 16);
    public static final DeferredHolder<Item, Item> BONE_MENDER =
            medical("bone_mender", MedicalItem.Type.BONE_MENDER, 8);
    public static final DeferredHolder<Item, Item> COMPACT_MEDKIT =
            medical("compact_medkit", MedicalItem.Type.COMPACT_MEDKIT, 16);
    public static final DeferredHolder<Item, Item> FIELD_MEDKIT =
            medical("field_medkit", MedicalItem.Type.FIELD_MEDKIT, 8);
    public static final DeferredHolder<Item, Item> TRAUMA_KIT =
            medical("trauma_kit", MedicalItem.Type.TRAUMA_KIT, 4);
    public static final DeferredHolder<Item, Item> SURGICAL_KIT =
            medical("surgical_kit", MedicalItem.Type.SURGICAL_KIT, 4);
    public static final DeferredHolder<Item, Item> RECOVERY_SERUM =
            medical("recovery_serum", MedicalItem.Type.RECOVERY_SERUM, 8);
    public static final DeferredHolder<Item, Item> ADRENALINE_INJECTOR =
            medical("adrenaline_injector", MedicalItem.Type.ADRENALINE_INJECTOR, 8);
    public static final DeferredHolder<Item, Item> GUARDIAN_INJECTOR =
            medical("guardian_injector", MedicalItem.Type.GUARDIAN_INJECTOR, 8);
    public static final DeferredHolder<Item, Item> FOCUS_INJECTOR =
            medical("focus_injector", MedicalItem.Type.FOCUS_INJECTOR, 8);
    public static final DeferredHolder<Item, Item> ENDURANCE_INJECTOR =
            medical("endurance_injector", MedicalItem.Type.ENDURANCE_INJECTOR, 8);

    public static final List<DeferredHolder<Item, Item>> COMPONENTS = List.of(
            STERILE_FABRIC, HERBAL_EXTRACT, ANTISEPTIC, SYRINGE_BODY, MEDICAL_POLYMER);
    public static final List<DeferredHolder<Item, Item>> MEDICINE = List.of(
            ANALGESIC_TABLETS, RAG_DRESSING, BANDAGE, SPLINT, MEDKIT, STERILE_PATCH, COMPRESSION_WRAP, HEMOSTATIC_GEL,
            TOURNIQUET, FRACTURE_BRACE, BONE_MENDER, COMPACT_MEDKIT, FIELD_MEDKIT, OVERSIZED_MEDKIT, MEDICAL_POUCH,
            TRAUMA_KIT, SURGICAL_KIT, RECOVERY_SERUM, ADRENALINE_INJECTOR,
            GUARDIAN_INJECTOR, FOCUS_INJECTOR, ENDURANCE_INJECTOR);

    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
