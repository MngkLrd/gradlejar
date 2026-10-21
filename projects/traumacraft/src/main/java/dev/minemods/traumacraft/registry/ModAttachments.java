package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TraumaCraft.MOD_ID);
    public static final Supplier<AttachmentType<BodyState>> BODY = TYPES.register(
            "body", () -> AttachmentType.serializable(BodyState::new).build());

    private ModAttachments() {}
    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
