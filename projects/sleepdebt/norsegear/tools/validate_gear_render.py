#!/usr/bin/env python3
"""Lightweight no-client regression guards for armor and thrown 3D orientation."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / "src/main/java/dev/norsegear"
client = (ROOT / "client/ClientRegistration.java").read_text()
armor = (ROOT / "client/model/NorseArmorModel.java").read_text()
projectiles = (ROOT / "client/renderer/ThrownWeaponRenderer.java").read_text()
spear = (ROOT / "item/SpearItem.java").read_text()
assert "copyArmorPose(original, model);" in client
assert "copyPropertiesTo(target)" in client
for part in ("nasal_guard", "brow", "helm_cap", "left_temple", "right_temple",
             "mail_front", "upper_chest_strap", "belt_buckle", "back_plate", "pauldron", "knee_plate", "boot_toe"):
    assert part in armor, f"Missing armor geometry/mask: {part}"
assert 'head = root.addOrReplaceChild("head", CubeListBuilder.create()' in armor
assert 'body = root.addOrReplaceChild("body", CubeListBuilder.create()' in armor
assert 'SlingStoneRenderer::new' in client
assert "rotationDegrees(-interpolatedYaw)" in projectiles
assert "rotationDegrees(90.0F + interpolatedPitch)" in projectiles
assert "stack.getDamageValue() >= stack.getMaxDamage() - 1" in spear
print("PASS: armor pose/masking, thrown projectile orientation and spear wear guards")
