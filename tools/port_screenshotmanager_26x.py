"""Restore the actual Screenshot Manager v0.2.0 source and adapt client API for 26.x."""
from pathlib import Path
import json
import shutil
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "projects/screenshotmanager"
DESTINATIONS = {"projects/screenshotmanager262": ("26.2", "26.2.0.93", 88),
                "projects/screenshotmanager263": ("26.3", "26.3.0.95", 97)}

def replace_required(source, old, new):
    if old not in source:
        raise RuntimeError("Expected source code not found: " + old[:100])
    return source.replace(old, new)

def main():
    dest_key = sys.argv[1] if len(sys.argv) > 1 else ""
    if dest_key not in DESTINATIONS:
        raise ValueError("Not a 26.x Screenshot Manager destination: " + dest_key)
    mc, loader, resource_format = DESTINATIONS[dest_key]
    dest = ROOT / dest_key
    if not (SRC / "src/main/java").is_dir():
        raise RuntimeError("Missing restored original Screenshot Manager sources")
    shutil.copytree(SRC / "src", dest / "src", dirs_exist_ok=True)
    client_dir = dest / "src/main/java/com/kirill/screenshotmanager/client"
    f = client_dir / "ScreenshotManagerClient.java"
    s = f.read_text(encoding="utf8")
    s = replace_required(s, "import org.lwjgl.glfw.GLFW;", "import net.minecraft.resources.Identifier;")
    s = replace_required(s, '''        openLatestEditorKey = new KeyMapping(
                "key.screenshotmanager.open_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "key.categories.screenshotmanager"
        );''', '''        KeyMapping.Category category = new KeyMapping.Category(
                Identifier.fromNamespaceAndPath("screenshotmanager", "main"));
        event.registerCategory(category);
        openLatestEditorKey = new KeyMapping(
                "key.screenshotmanager.open_editor",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_O,
                category
        );''')
    # Minecraft 26.2 still uses KEYSYM; 26.3 renamed the device to KEYBOARD.
    if mc == "26.2":
        s = s.replace("InputConstants.Type.KEYBOARD", "InputConstants.Type.KEYSYM")
    s = s.replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
    s = s.replace("minecraft.screen == null", "minecraft.gui.screen() == null")
    s = s.replace("minecraft.player.displayClientMessage(Component.literal(message), false)",
                  "minecraft.player.sendSystemMessage(Component.literal(message))")
    f.write_text(s,encoding="utf8")

    f = client_dir / "ImageTexture.java"
    s = f.read_text(encoding="utf8")
    s = replace_required(s,"net.minecraft.resources.ResourceLocation","net.minecraft.resources.Identifier")
    s = s.replace("ResourceLocation", "Identifier")
    s = replace_required(s, "new DynamicTexture(nativeImage)", 'new DynamicTexture(() -> "Screenshot Manager", nativeImage)')
    s = replace_required(s, 'Identifier location = Minecraft.getInstance().getTextureManager().register(name, texture);',
                         'Identifier location = Identifier.fromNamespaceAndPath("screenshotmanager", name.substring(name.lastIndexOf("/") + 1));\n        Minecraft.getInstance().getTextureManager().register(location, texture);')
    f.write_text(s,encoding="utf8")

    for filename in ("ScreenshotPreviewScreen.java", "ScreenshotEditorScreen.java"):
        f = client_dir / filename
        s = f.read_text(encoding="utf8")
        s = replace_required(s, "import net.minecraft.client.gui.GuiGraphics;",
                             "import net.minecraft.client.gui.GuiGraphicsExtractor;\nimport net.minecraft.client.input.MouseButtonEvent;")
        s = s.replace("GuiGraphics", "GuiGraphicsExtractor").replace("GuiGraphicsExtractorExtractor", "GuiGraphicsExtractor")
        s = replace_required(s, "void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)",
                             "void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)")
        s = s.replace("super.render(graphics, mouseX, mouseY, partialTick);",
                      "super.extractRenderState(graphics, mouseX, mouseY, partialTick);")
        s = s.replace("graphics.drawString(", "graphics.text(").replace("graphics.drawCenteredString(", "graphics.centeredText(")
        for loc, w, h in (("preview", "previewW", "previewH"), ("image", "imageW", "imageH")):
            s=s.replace(f'graphics.blit(texture.location(), {loc}X, {loc}Y, {w}, {h}, 0.0F, 0.0F, texture.width(), texture.height(), texture.width(), texture.height());',
                        f'graphics.blit(texture.location(), {loc}X, {loc}Y, {loc}X + {w}, {loc}Y + {h}, 0.0F, 1.0F, 0.0F, 1.0F);')
        s=s.replace("public boolean mouseClicked(double mouseX, double mouseY, int button) {",
                    "public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();")
        s=s.replace("return super.mouseClicked(mouseX, mouseY, button);","return super.mouseClicked(event, doubleClick);")
        s=s.replace("public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {",
                    "public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();")
        s=s.replace("return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);",
                    "return super.mouseDragged(event, dragX, dragY);")
        s=s.replace("public boolean mouseReleased(double mouseX, double mouseY, int button) {",
                    "public boolean mouseReleased(MouseButtonEvent event) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();")
        s=s.replace("return super.mouseReleased(mouseX, mouseY, button);", "return super.mouseReleased(event);")
        s = s.replace("Minecraft.getInstance().setScreen(", "Minecraft.getInstance().gui.setScreen(")
        if filename == "ScreenshotEditorScreen.java":
            s = s.replace("import net.minecraft.Util;", "import java.awt.Desktop;")
            s = s.replace("Util.getPlatform().openFile(folder.toFile());", '''try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            }
        } catch (IOException | UnsupportedOperationException exception) {
            status = "Could not open screenshot folder: " + exception.getMessage();
            return;
        }''')
        f.write_text(s,encoding="utf8")

    lang = dest / "src/main/resources/assets/screenshotmanager/lang/en_us.json"
    lang.write_text(lang.read_text(encoding="utf8").replace(
        "key.categories.screenshotmanager", "key.category.screenshotmanager.main"),encoding="utf8")
    pack = dest / "src/main/resources/pack.mcmeta"
    min_format = [97,1] if mc == "26.3" else 88
    pack.write_text(json.dumps({"pack":{"description":"Screenshot Manager resources",
                                       "min_format":min_format,"max_format":resource_format}})+"\n",encoding="utf8")
    print("Ported original Screenshot Manager 0.2.0 to",mc,"with NeoForge",loader)

if __name__ == "__main__":
    main()
