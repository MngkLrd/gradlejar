"""Convert Screenshot Manager NeoForge 0.2.0 sources to Fabric on CI."""
import shutil, json, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
PORTS={"projects/screenshotmanagerfabric1211":("projects/screenshotmanager","1.21.1"),
       "projects/screenshotmanagerfabric262":("projects/screenshotmanager262","26.2"),
       "projects/screenshotmanagerfabric263":("projects/screenshotmanager263","26.3")}

def main():
    key=sys.argv[1] if len(sys.argv)>1 else ""
    if key not in PORTS: raise ValueError("Unknown Fabric project "+key)
    source,mc=PORTS[key]
    base=ROOT/source/"src/main"
    out=ROOT/key/"src/main"
    if not (base/"java/com/kirill/screenshotmanager/client/ScreenshotManagerClient.java").is_file():
        raise RuntimeError("Original Screenshot Manager source not extracted")
    shutil.copytree(base,out,dirs_exist_ok=True)
    (out/"java/com/kirill/screenshotmanager/ScreenshotManagerMod.java").unlink()
    (out/"resources/META-INF/neoforge.mods.toml").unlink(missing_ok=True)
    (out/"java/com/kirill/screenshotmanager/ScreenshotManagerFabricClient.java").write_text(
        "package com.kirill.screenshotmanager;\n"
        "import com.kirill.screenshotmanager.client.ScreenshotManagerClient;\n"
        "import net.fabricmc.api.ClientModInitializer;\n"
        "public final class ScreenshotManagerFabricClient implements ClientModInitializer {\n"
        " @Override public void onInitializeClient() { ScreenshotManagerClient.init(); }\n"
        "}\n",encoding="utf8")
    f=out/"java/com/kirill/screenshotmanager/client/ScreenshotManagerClient.java"
    s=f.read_text(encoding="utf8")
    s="".join(x for x in s.splitlines(keepends=True) if not x.startswith("import net.neoforged."))
    s=s.replace("import net.minecraft.client.KeyMapping;",
                "import net.minecraft.client.KeyMapping;\n"
                "import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;\n"
                "import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;")
    start=s.index("    public static void init(IEventBus modBus) {")
    end=s.index("    private static void onClientTickPost(ClientTickEvent.Post event) {")
    if mc=="1.21.1":
        keybind='''    private static void registerKeyMappings() {
        openLatestEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.screenshotmanager.open_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories.screenshotmanager"
        ));
    }

'''
    else:
        typ="KEYSYM" if mc=="26.2" else "KEYBOARD"
        keybind='''    private static void registerKeyMappings() {
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("screenshotmanager", "main"));
        openLatestEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.screenshotmanager.open_editor",
                InputConstants.Type.''' + typ + ''',
                InputConstants.KEY_O,
                category
        ));
    }

'''
    s=s[:start]+'''    public static void init() {
        registerKeyMappings();
        ClientTickEvents.END_CLIENT_TICK.register(ScreenshotManagerClient::onClientTickPost);
    }

'''+keybind+s[end:]
    s=s.replace("    private static void onClientTickPost(ClientTickEvent.Post event) {\n"
                "        Minecraft minecraft = Minecraft.getInstance();",
                "    private static void onClientTickPost(Minecraft minecraft) {")
    if "ClientTickEvent.Post event" in s or "IEventBus" in s:
        raise RuntimeError("Incomplete Fabric migration")
    f.write_text(s,encoding="utf8")
    metadata={"schemaVersion":1,"id":"screenshotmanager","version":"$"+"{version}",
              "name":"Screenshot Manager","description":"Screenshot capture, editing and clipboard sharing.",
              "authors":["Kirill"],"license":"MIT","environment":"client",
              "entrypoints":{"client":["com.kirill.screenshotmanager.ScreenshotManagerFabricClient"]},
              "depends":{"fabricloader":">=0.19.5","minecraft":mc,
                         "java":">=21" if mc=="1.21.1" else ">=25","fabric-api":"*"}}
    (out/"resources/fabric.mod.json").write_text(json.dumps(metadata,indent=2)+"\n",encoding="utf8")
    print("Fabric converted:",mc)

if __name__=="__main__": main()
