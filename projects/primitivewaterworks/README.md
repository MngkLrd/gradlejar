# Primitive Waterworks 0.1.0

Early-game wooden rainwater collectors, open gutters and downspouts for TerraFirmaCraft / TerraFirmaGreg Modern.
Minecraft 1.20.1 | Forge 47.3.0+ | Java 17 | Requires TFC 3.x

## Usage
1. Place a Wooden Rain Collector where the sky is open.
2. Connect a Wooden Gutter at the collector's side, then a Wooden Downspout to descend.
3. Put a TFC barrel or TFG keg adjacent to the outlet (best: immediately below a downspout).
4. Wait for natural rain. Each collector catches up to 40 mB water/s and stores 1,000 mB.
5. Transfer runs at up to 100 mB/s per collector. A conduit never carries fluid upward.
6. Right-click the rain collector with an empty hand for stored amount.

Inputs are TFC lumber ('tfc:lumber') of all wood species, not vanilla plank blocks.
Textures are existing genuine TFC oak plank textures, obeying vanilla pixel-art principles.
Different lumber species are accepted but all placed blocks use neutral oak texture in this MVP.
Transfer is via Forge IFluidHandler and respects receiver fill restrictions.
Nothing chunk-loads; every path is capped at 128 blocks.
No direct source water placement or infinite reservoir production.
TFC may restrict automatic barrel inputs depending on barrel state and side. Check an unsealed barrel, or the large TFG keg.
No in-game test against TerraFirmaGreg Modern has yet been performed.
