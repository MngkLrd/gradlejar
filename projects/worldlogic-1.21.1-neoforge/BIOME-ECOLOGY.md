# RC26 biome refinement and Terrarian integration

RC22 terrain density, drainage, sea level and climate fields remain unchanged; RC25 ore and cave models unchanged. This iteration partitions ten existing climate intervals without moving the geographic shape. Names are real-world habitat descriptions, not invented fantasy zones. WWF terrestrial ecoregions: https://files.worldwildlife.org/wwfcmsprod/files/Publication/file/5xdxix5fsv_The_Global_200_Priority_Ecoregions_for_Global_Conservation.pdf ; FAO soil suitability and drainage: https://www.fao.org/4/t0715e/t0715e06.htm . Biome range cutoffs, color, surface variants and habitat indices are gameplay estimates, not measured global ecological datasets. Terrarian owns all vegetation. New classes inherit provisional animal lists, no new assets or plant features. Actual biome map and client visual validation pending.

| Parent | New ID | Name | Climate split |
|---|---|---|---|
| polar_desert | polar_gravel_desert | Полярная щебнистая пустыня | humidity >= -0.82 |
| dry_tundra | lichen_tundra | Лишайниковая тундра | humidity < -0.39 |
| continental_taiga | light_conifer_taiga | Светлохвойная тайга | humidity < -0.06 |
| humid_taiga | dark_conifer_taiga | Темнохвойная тайга | humidity >= 0.3 |
| temperate_steppe | shortgrass_steppe | Низкотравная степь | humidity < -0.4 |
| prairie | tallgrass_prairie | Высокотравная прерия | humidity >= 0 |
| mixed_forest | temperate_deciduous_forest | Умеренный листопадный лес | humidity >= 0.3 |
| mediterranean_scrub | sclerophyll_woodland | Жестколистное редколесье | humidity >= -0.4 |
| hot_desert | rock_desert | Каменистая пустыня | humidity >= -0.83 |
| tropical_monsoon_forest | seasonal_tropical_forest | Сезонный тропический лес | humidity < 0.32 |

API revision 4: see API.md. Use existing worldgen generator state; pass caller-read ground and biome. All estimates are deterministic, no chunk queries in sampler.
