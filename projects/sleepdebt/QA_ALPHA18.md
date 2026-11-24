# Alpha 18 — Survival realism QA

Minecraft 1.21.1 / NeoForge / Java 21. Back up an old world first.

- Set /sleepdebt debug set 1000 0 0: attack speed alone falls, caffeine masks it.
- Set /sleepdebt debug set 0 1000 0: walking, mining and damage fall. Coffee does not restore the body.
- Set /sleepdebt debug set 0 0 1000: exertion consumes more food; sleep quality and rest recovery fall.
- At critical sleep debt, occasional 2-second lapse slows movement. No hallucinations or artificial darkness.
- Use the coffee maker with water bucket + grounds + coal. Try every one of the 11 base/ingredient combinations; check output and container remainders. All recipes must be shown by JEI/EMI.
- Compare normal, extended, concentrated energy drinks. Test tolerance and the crash when masking expires.
- Mods > Sleep Debt > Config: HUD, penalty strengths, sleep and phantom toggles. Check permissions on a server.
- Sleep with 1 of 2 players at playersSleepingPercentage=50; compare both players' recovery. Test Comforts hammocks/daytime sleep and sleeping bags. Test Soaring Phantoms with phantom integration both on and off. These third-party tests are not certified by CI.
- Update Alpha17 world; old Sleepless registry ID is inert and old mob discards itself. No egg in creative catalog; old egg ID remains inert.
- Review all 47 16x16 maps at native size and 3x3 tiling, block models in game, HUD, journal and coffee slots. No artificial dark shader or horror audio in JAR.
