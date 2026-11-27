package dev.minemods.qa.mixin;

import com.google.gson.Gson;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class RegistryDumpMixin {
    @Unique private boolean minemods$dumped;

    @Unique private static List<String> minemods$ids(Registry<?> registry) {
        return registry.keySet().stream().map(Object::toString).sorted().toList();
    }

    @Inject(method="tickServer", at=@At("HEAD"))
    private void minemods$dump(BooleanSupplier haveTime, CallbackInfo ci) {
        if (minemods$dumped) return;
        MinecraftServer server = (MinecraftServer)(Object)this;
        Map<String,List<String>> values = new TreeMap<>();
        values.put("item", minemods$ids(BuiltInRegistries.ITEM));
        values.put("block", minemods$ids(BuiltInRegistries.BLOCK));
        values.put("entity", minemods$ids(BuiltInRegistries.ENTITY_TYPE));
        values.put("effect", minemods$ids(BuiltInRegistries.MOB_EFFECT));
        values.put("menu", minemods$ids(BuiltInRegistries.MENU));
        values.put("block_entity", minemods$ids(BuiltInRegistries.BLOCK_ENTITY_TYPE));
        values.put("fluid", minemods$ids(BuiltInRegistries.FLUID));
        values.put("potion", minemods$ids(BuiltInRegistries.POTION));
        values.put("sound", minemods$ids(BuiltInRegistries.SOUND_EVENT));
        values.put("particle", minemods$ids(BuiltInRegistries.PARTICLE_TYPE));
        values.put("recipe", server.getRecipeManager().getRecipeIds().map(Object::toString).sorted().toList());
        Path temporary = Path.of("minemods-live-registries.tmp");
        try {
            Files.writeString(temporary, new Gson().toJson(Map.of("schema",1,"registries",values)));
            Files.move(temporary, Path.of("minemods-live-registries.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("MineMods registry diagnostic export failed",e);
        }
        minemods$dumped = true;
    }
}
