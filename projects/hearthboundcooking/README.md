# Hearthbound Cooking 0.4.1 — development alpha
Minecraft 1.20.1 Forge, Java 17, TerraFirmaCraft 3.2.x. Install on server and client.
No original food recipe is replaced. Raw ingredients are never assigned a grade by collection, inventory scanning, or eating. A supported cooking result receives a quality grade from the actual process. Previously graded ingredients contribute 20% of the final score; ungraded ingredients contribute no inherited grade, not zero.
- TFC heating recipes: actual valid heat threshold, edible result, no marking merely heated same-item raw inputs.
- Native TFC pot: soup, jam and immediate food results at native completion, remaining containers excluded. Custom stockpot uses native soup output and native crafting context.
- Salads and TFC MealModifier (including supported sandwiches): grade only assembled result; ingredients remain intact. Recipe previews can calculate a result grade but must not mutate inputs.
- Firmalife oven: optional heating-recipe hook. Removed unsafe getTicksLeft override; native timer remains for compatibility.
- Vanilla furnace/smoker: grade only a successful burn result, weight accumulated output score; no temporary tag stripping or recipe matching override. Native processing and automation stay in control.
- Quality affects saturation only, not original nutrient categories, hunger, freshness or recipe ingredients. Scores quantized in steps of five; different grades may form distinct inventory stacks.
- Data packs can exclude intermediate output items via hearthboundcooking:ungraded_outputs or opt in same-ID thermal preparations via hearthboundcooking:prepared_same_item.
- New vessels still support native soup recipes only; original pot and recipes are unaffected. Arbitrary TFG industrial machine outputs and unsupported Firmalife devices require separate verified adapters; not universally covered.
Build: gradle --no-daemon clean build. JAR: build/libs/hearthboundcooking-0.4.1.jar.
Quality arithmetic test is part of Gradle check. In-game regression testing is still required: raw food, roasted food, soups with mixed grades, jam, rice, salads, sandwiches, Firmalife ovens, hoppers, full stacks, expiry, save/reload, JEI/EMI.

## 0.4.1
- Furnace comparison uses copies, ignoring only Hearthbound quality; other native NBT checks remain.
- Saturation is scaled before TFC's original cap.
- Unequal ingredient quantities and limited water: cook maximum complete matching batches, retain spare ingredients. Fresh TFC food capability determines acceptance; native recipe matching is optional. Separate ingredient slots can hold identical ingredients. Custom vessels remain soup-only, with no container-returning ingredients.
- Staggered once-per-second heating, reused isolated inventory, no elapsed cooking credit across unloads. Native pot counters saved; vessel NBT sanitized.
- GUI diagnoses missing water and spoiled ingredients; shows planned batches and servings. No timer. Open lid before serving. Creative inventory insertion consumes the moved items.
- Runtime regression tests and industrial TFG adapters remain outstanding.

- Explicit successful grill and Firmalife oven food recipes now grade same-ID preparations without grading raw warming in generic heating recipes.

## Flexible soup hotfix 0.4.1
- Any one to five occupied ingredient slots containing fresh TFC-capability foods can form soup with sufficient water and heat. No meat requirement, no specific ingredient combination, no soup-tag gate. Addon foods with TFC food data work even outside the native soup tag.
- Exact native soup recipes take priority. Otherwise use a compatible native soup template or internal unregistered fallback with native TFC cooking defaults. Always call TFC's actual SoupPotRecipe output generator for nutrients, soup type, freshness, serving count and bowl behavior. No change to native TFC pot recipes.
- Removed the misleading No soup recipe state. Native water, batch sizes, preparation quality, leftovers and spoilage checks remain. Ingredients with crafting remainders are still rejected by the custom vessel until safe remainder handling is implemented.
