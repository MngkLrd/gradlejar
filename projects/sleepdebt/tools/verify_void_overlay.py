#!/usr/bin/env python3
from pathlib import Path
root=Path("src/main/java/dev/minemods/sleepdebt/client")
visual=(root/"SleeplessVoidOverlay.java").read_text()
boot=(root/"ClientBootstrap.java").read_text()
hud=(root/"SleepHud.java").read_text()
for needle in ["RenderGuiEvent.Pre","getEntitiesOfClass(SleeplessEntity.class",
  "g.fill(0,0,w,h","g.flush();","GLFW.GLFW_KEY_F8","preFrames","postFrames"]:
    assert needle in visual, "Missing direct render step: "+needle
assert "NeoForge.EVENT_BUS.addListener(SleeplessVoidOverlay::renderPre)" in boot
assert "NeoForge.EVENT_BUS.addListener(SleeplessVoidOverlay::renderPost)" in boot
assert "SleeplessVoidShader::register" not in boot
assert "SleeplessVoidShader.render" not in hud
print("Alpha 17 direct darkness render wiring PASS")
