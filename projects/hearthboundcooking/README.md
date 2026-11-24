# Hearthbound Cooking 0.1.0 — development alpha
Minecraft 1.20.1 Forge, Java 17, TerraFirmaCraft 3.2.x. Install on server and client.
Uses the existing TFC soup recipe, nutrition and expiry; does not add food recipes.
- Place a vessel directly above a TFC firepit or Firmalife oven bottom. No new heater.
- Add fluid with compatible container, ingredients with right click. Sneak-click to add up to capacity of one ingredient. Use three to five equally sized ingredient groups, 1–3 groups per clay pot or 1–6 per bronze pot. Each group requires 1000 mB of water. Add protein first, then grain, then fruit/vegetables. Sneak with an empty hand toggles lid. Sneak with stick removes last ingredient group.
- Open lid is faster; closed lid simmers more slowly and retains heat. State and sound, not countdown. Use native bowls to extract servings. Breaking spills finished liquid; raw ingredients drop.
- Quality in 5% steps minimizes stack fragmentation. TFC heating recipes and normal soup receive quality; grill hot holding lowers it. Firmalife oven recipe/timer hooks are optional. Vanilla furnace and smoker receive quality and can lose quality when held hot; their recipe and output stacking rules remain. Food cooking progress arrow is hidden. Salads are graded on freshness and ingredient quality.
- Freshness remains TFC-managed. Quality adjusts saturation, never nutrition categories or hunger. No fixed recipe copies.
- Alpha limitations: no GUI yet; world interaction and models only. No native staged soup slot changes, no support for arbitrary non-soup pot recipes, no global grading of cold crafting or machine output. No actual game test yet.
Build: gradle --no-daemon clean build. JAR: build/libs/hearthboundcooking-0.1.0.jar.
