package dev.bearback;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;
import java.util.UUID;

/** Fabric persistence on the bear entity, never on a global shared inventory. */
public final class BearData {
    public static final AttachmentType<CompoundTag> STATE=AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC).initializer(CompoundTag::new)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath("bearback","state"));
    private BearData(){}
    public static CompoundTag get(PolarBear bear){return ((AttachmentTarget)bear).getAttachedOrCreate(STATE);}
    public static void changed(PolarBear bear){((AttachmentTarget)bear).setAttached(STATE,get(bear).copy());}
    public static boolean saddled(PolarBear bear){return get(bear).getBoolean("BearbackSaddled");}
    public static boolean bags(PolarBear bear){return get(bear).getBoolean("BearbackBags");}
    public static void saddle(PolarBear bear,boolean flag){get(bear).putBoolean("BearbackSaddled",flag);changed(bear);}
    public static void bags(PolarBear bear,boolean flag){get(bear).putBoolean("BearbackBags",flag);changed(bear);}
    public static UUID owner(PolarBear bear){return get(bear).hasUUID("BearbackOwner")?get(bear).getUUID("BearbackOwner"):null;}
    public static void owner(PolarBear bear,UUID id){get(bear).putUUID("BearbackOwner",id);changed(bear);}
}
