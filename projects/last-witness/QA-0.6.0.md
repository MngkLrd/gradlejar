# Last Witness 0.6.0 - barter QA
NeoForge 1.21.1 / Java 21; client + server must both have the mod.

1. Right-click farmer and check that the new trading ledger replaces MerchantScreen.
2. Put goods in three offer slots; read-only reward and catalog slots must never yield items to mouse, shift, drag or hotbar swap.
3. Ask merchant, haggle up to twice and accept. Confirm the server consumes inputs once, gives output once and decreases stock.
4. Refuse/close/logout with goods in escrow: all inputs returned, no duplication.
5. Ask to buy chosen catalog goods without sufficient payment: server refuses accept even after forged button packet.
6. Catalog navigation, two trading modes, and individual profession goods must work.
7. Verify stock depletion, next-day restock, trust persistence, multiple concurrent players, full inventory overflow.
8. Wanted players cannot trade; guards, 133 skin PNGs and witness eyes unchanged.

CI proves build and resource packaging; runtime UI testing remains mandatory.

Build verification note: restore the matching 0.6.0 build profile *after* the 0.5.0 source patch has applied.
