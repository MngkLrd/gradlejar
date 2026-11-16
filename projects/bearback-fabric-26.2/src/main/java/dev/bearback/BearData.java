package dev.bearback;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.PolarBear;
import java.util.UUID;

/** Fabric persistence on the bear entity, never on a global shared inventory. */
public final class BearData {
    public static final AttachmentType<CompoundTag> STATE=AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath("bearback","state"),
            builder->builder.persistent(CompoundTag.CODEC).initializer(CompoundTag::new));
    private BearData(){}
    public static CompoundTag get(PolarBear bear){return ((AttachmentTarget)bear).getAttachedOrCreate(STATE);}
    public static void changed(PolarBear bear){((AttachmentTarget)bear).setAttached(STATE,get(bear).copy());}
    public static boolean saddled(PolarBear bear){return get(bear).getBooleanOr("BearbackSaddled",false);}
    public static boolean bags(PolarBear bear){return get(bear).getBooleanOr("BearbackBags",false);}
    public static void saddle(PolarBear bear,boolean flag){get(bear).putBoolean("BearbackSaddled",flag);changed(bear);}
    public static void bags(PolarBear bear,boolean flag){get(bear).putBoolean("BearbackBags",flag);changed(bear);}
    public static UUID owner(PolarBear bear){try{return UUID.fromString(get(bear).getStringOr("BearbackOwner",""));}
        catch(IllegalArgumentException ex){return null;}}
    public static void owner(PolarBear bear,UUID id){get(bear).putString("BearbackOwner",id.toString());changed(bear);}
}
