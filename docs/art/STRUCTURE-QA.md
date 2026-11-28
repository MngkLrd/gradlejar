# MineMods — architectural structure design and generation standard

Status: mandatory **only for new or materially changed human-built structures**. This is the canonical architectural design, generation and review guide under [AGENTS.md](../../AGENTS.md). It is an engineering and artistic standard adopted by MineMods, not an official Mojang rulebook. Its objective is generated architecture that a skilled Minecraft builder would be willing to put in a handcrafted adventure map: credible, memorable, purposeful, usable and naturally embedded in its surroundings.

## Quick navigation for architectural tasks

Read the common procedure in sections 0–20, then select only applicable advanced reference chapters. Ordinary building updates may go directly to the affected architectural detail and QA checks; do not expand scope to unrelated families.

| Task | Advanced chapter |
| --- | --- |
| Historical, regional or fantasy style | 21 — architectural grammars |
| Curves, towers, domes, diagonal or multi-level geometry | 22 — complex geometry |
| Bridges, waterworks, roads, factories, infrastructure | 23 — engineering and industrial structures |
| Castles, walls, monumental complexes | 24 — fortifications and monumental architecture |
| Villages, cities, routes and plot layout | 25 — settlements and spatial economy |
| Ruins, archaeology, damage and burial | 26 — architectural history and aging |
| Concrete block-level construction and furnishing | 27 — Minecraft block construction library |
| From micro buildings to huge complexes | 28 — scaling and generation budgets |
| Underwater, cliff, fantasy and unusual structures; defect diagnosis | 29 — special cases and risk-directed review |
| Research limitations and application | 30 — advanced references |

## 0. Scope, activation and cost control

**Read and apply this document only when the task creates or materially changes a building, settlement, inhabited or abandoned man-made construction, or a built part of an underground or underwater complex.** Examples: houses, barns, workshops, mills, farm buildings, towers, forts, castles, temples, docks, bridges, roads belonging to settlements, mines with human-built shafts/supports, tunnels, stations, dungeons, ruins, camps, bunkers and their interiors. A change to an existing structure generator, template, architectural palette, room furnishing, layout, placement/foundation, piece connector, degradation or architectural processor is in scope if it affects the built result. A biome or terrain change that demonstrably breaks a supported building generator warrants a targeted compatibility regression, not a review of every building in the repository.

Excluded by default: natural trees (including custom trunks, branches, leaves, fruit), plants, vegetation features, natural caves, boulders, reefs, mountains, rivers, ores, geology, terrain, ordinary block/item art, entities, UI, and unchanged vanilla structures. Technical registration as a Structure, StructureTemplate, Feature or jigsaw piece does not determine the artistic scope. A custom natural tree saved as NBT is still a tree; a house generated in Java as a Feature is still architecture. A treehouse is in scope for the built house and its supports, not for unrelated tree species. A natural cave is excluded; a mine inside it is in scope for its engineered passages. Unmodified vanilla buildings used as references are not new review targets.

No global structure scan, mandatory client boot for every mod, audit of unrelated vanilla generation, all-loader matrix, automatic migration of published mods, or additional CI job for every project. Reuse existing product QA. Run the architectural checks on the active target and affected templates/palettes/assemblies only. Ordinary texture creation still uses [common Visual QA](VISUAL-QA.md) and its relevant art branch. Creating a building from already existing approved blocks does not require redrawing or reviewing the PNG of each block. This document does not expand the Art Review Lock to unrelated projects or certify architecture from a green texture report. A scope-specific build/structural failure must be fixed; a missing graphical backend must be explicitly reported.

## 1. Quality target and design hierarchy

A building is not a box with a decorated front. It is the combination of five coordinated layers: site, massing, structural logic, material language and human activity. Think in that order. A building with a good silhouette, correct proportions and believable rooms remains attractive without shaders. A bad silhouette cannot be fixed by hundreds of trapdoors, random slabs or glow blocks.

Work at three visual scales:
- Far: identifiable silhouette, roof hierarchy, relation to landscape and neighboring buildings.
- Middle: depth, repeated architectural bays, openings, foundations, materials, roof junctions and entrances.
- Near: block-state details, furniture, joints, supports, wear, functional objects, lighting and believable clutter.

Use a strong primary shape, a subordinate volume and a few small accents. Symmetry is useful for formal buildings; asymmetry is useful for ordinary dwellings and additions. Neither is automatically better. Repeat a coherent visual grammar, then introduce variation where it tells a story. A medieval warehouse, Nordic longhouse, desert courtyard home and modern concrete workshop should not all be generated as the same building with different colors. Flat roofs and plain walls can be intentional (desert architecture, industrial buildings, modernism, defenses). Their appropriateness must be evident from function and style.

Beauty is not a global metric for block count, random texture variance, decoration density or percentage of stairs. A humble shed may be simple. An inhabited castle must not be a giant empty shell. The target is deliberate craft, not maximum complexity.

## 2. Research before building — role, region, era, story

For every new architectural family, form a short design brief before writing a generator. Record it in a compact project-local note or existing design/QA manifest, not an extra bureaucratic document when the same fields already exist.

| Decision | Required answer |
| --- | --- |
| Function | What was this structure built to do? Who uses or used it? |
| Context | Region, terrain, climate, water access, era/technology, culture and nearby resources |
| Size | Occupants, activity and inventory determine the rooms and capacity |
| Life state | Inhabited, maintained, poor, wealthy, working, abandoned, damaged, reclaimed or intentionally fantastic |
| Shape | Main silhouette, smaller attached forms, roof type, floor heights, orientation |
| Materials | Foundation, structural frame, wall infill, roof, finish, accent, aged variants |
| Interior | Named rooms/zones, access graph, furnishings, lighting, functional or narrative props |
| Placement | Suitability, slope, foundation limits, collision policy, allowed rotations, rarity and variants |
| Evidence | Comparable real building and/or Minecraft reference, actual asset IDs, required QA IDs |

Choose one or more visually comparable references from real architecture and at least one Minecraft building reference when practical. Study elevation, side and rear, interior or plan, roof and material close-up. Prefer multiple angles to a single thumbnail. Use image references to understand massing and construction, not to trace someone else's protected building or import unknown assets. Look at structures with similar role, scale, climate and technology, not merely a generic medieval house. For WorldLogic, use the actual regional/climatic information and established materials of the ecosystem when designing architecture.

Example reference queries: "traditional Finnish log farmhouse floor plan", "Baltic timber barn roof framing", "medieval blacksmith workshop interior", "historic mine adit timber support", "desert courtyard house section", "Minecraft medieval farmhouse full interior", "Minecraft stone bridge arch abutment". Record author/title/link if reference is used. Do not claim a reference was examined if only a title or thumbnail was accessible.

## 3. Scale, proportions, floor plan and massing

Start with usable interior space, not external dimensions alone. Minecraft has coarse one-block units; a standard door is two blocks tall and standing entities require appropriate clearance. Use the actual target collision shapes. A full-size Minecraft block is an approximate design unit, not a reason to simulate precise structural engineering. Suggested design starting points below are not universal mandates.

| Element | Useful starting point | Reason |
| --- | --- | --- |
| Normal inhabited room | 3–4 clear blocks high; larger public rooms 5+ if justified | Space for furniture, beams and player |
| Small house | about 7x9 to 11x15 exterior, adjusted for walls | Recognizable home rather than a cube |
| Corridor | 2 clear blocks where possible, 1 for intentional tight service access | Movement and readability |
| Stair flight | 1–2 blocks wide, two-block player clearance throughout, landings if useful | Safe continuous traversal |
| Normal exterior opening | 1x2 door, 1x1 or 1x2 window, larger paired openings when scale warrants | Human reference scale |
| Larger room or hall | Multiple bays, exposed beams/columns or coherent spanning elements | Prevents anonymous giant volume |
| Roof overhang | Often 0.5–1 block; greater in appropriate styles | Shadow, shelter and termination |

Use odd/even dimensions intentionally. Center a single entrance or roof ridge with an odd-width composition where useful; paired entries or twin bays may benefit from even widths. Do not force every building onto the same rectangle. Try rectangles with unequal sides, L and T plans, offset wings, courtyards, connected volumes, recessed entrances, porch, lean-to, tower or exterior stair if the function justifies them. One well-formed secondary volume is better than ten arbitrary bumps. Keep service extensions lower or narrower than the principal volume unless the design demands a tower or hall.

Plan before detailing. Draw a top-down block grid per floor, marking walls, usable space, doors, stairs, windows, props, columns and entrances. Mark the front, terrain contact and roof ridge. Align vertical supports, floor beams and partitions across floors where possible. Leave room for ceilings, upper-floor joists and wall thickness. A small building can have one wall block thickness; massive masonry may require two or more or a visually credible buttress. Thick masonry needs deep reveals, not windows floating on the facade.

Create a rough massing prototype in real blocks first. Inspect front/rear/sides, at eye level and high angle. Do not produce dozens of variants before the representative building is reviewed. Preserve deliberate simplicity where appropriate.

Example: 9x13 timber dwelling, one main rectangular wing, optional 3x4 side porch (not counted in the main footprint), wall height 4, gable roof, sleeping loft if clearance permits. Conceptual ground-floor plan (each cell one block; exact furniture positions must be reviewed in the actual game):

    #########
    #w.....w#
    #.B...S.#
    #.......#
    #...T...#
    #...T...#
    w.......w
    #..##D###
    #.C.....#
    #.F....R#
    #......R#
    #w.....w#
    ####D####

    # wall, w window, D door, B bed zone, S storage,
    T table zone, C cabinet, F hearth, R stair zone, . walkable area.

The plan is an illustration of zoning, not an executable structure or finished floor plan. Check actual bed length, stair run, door access, chimney clearances and furniture collision. If the stair cannot fit, revise the plan or remove the loft. Don't insert an impossible stair simply because the outline looks good.

## 4. Foundation, structural logic and visual support

Every permanent architectural mass needs a readable load path into something stable, even if Minecraft does not simulate gravity for that block. Foundation / pier / abutment -> post / bearing wall / buttress -> beam / floor -> roof. This is visual plausibility, not a guarantee of real engineering safety. Floating architecture is allowed only as an explicit fantasy premise with intentional support/magic language; accidental levitation is a defect.

- Ground-floor walls and load-bearing posts meet foundations. A one-block visible stone or brick plinth can separate wood from moist soil; use actual buried footings and stepped bases where appropriate.
- A raised floor requires piers, continuous walls or a plausible suspended frame. A balcony needs cantilever beams, brackets, corbels, columns or visible ties. A dock needs piles anchored below water or explicit floating pontoons. A bridge needs abutments/piles and a deck connection.
- Upper-floor projection should read as a supported jetty or cantilever. Align floor beam ends with lower supports. Use upside-down stairs, walls or brackets to communicate load transfer; do not pretend a decorative tiny fence is enough to support a giant tower.
- A chimney starts from the hearth/furnace or a clear flue route and exits the actual roof. Cap it, keep roof intersection clean and avoid putting it halfway up an unsupported side wall.
- Timber framing: vertical corner posts, intermediate posts forming meaningful bays, horizontal plates and floor beams, diagonal braces or stair brackets where appropriate. Log axis must match the direction of the member. Infill sits inside the frame. Do not draw decorative logs with random axes.
- Masonry: stone piers and corners anchor openings, arches meet abutments, lintels cap openings, setbacks and buttresses match wall height. Massive stone above large open gaps needs a visible arch or lintel.
- Roofs need plausible ridge, rafters/eaves, columns or bearing walls. Large industrial halls can use repeated trusses instead of a wall grid.
- Furniture, paintings, lanterns, chains, signposts, shelving, rails, doors and ladders must be attached or physically supported as appropriate. Do not hang a chain from empty air or place a candle on the wrong side of a slab. Check support face and actual block-state survival.
- No universal "every block must have a block below" check: arches, overhangs, window trim, legitimate spans, hanging lanterns and floating pontoons may be valid. A checker should understand support roots and attachments and flag suspicious components for inspection. It cannot certify architectural strength.

## 5. Material language, regional palettes and block compatibility

Design the palette as architectural materials, not a list of unrelated attractive textures. Assign roles before selecting blocks.

| Role | What it communicates | Examples from vanilla (verify target registry) |
| --- | --- | --- |
| Foundation | Ground contact, mass, durability | stone bricks, cobblestone, local stone |
| Primary wall | Main material and age | planks, sandstone, brick, terracotta, masonry |
| Frame | Structure, rhythm, separation | stripped logs, logs, darker masonry |
| Roof | Weather protection and silhouette | stairs/slabs of a suitable wood, stone, tile-like family, copper |
| Trim | Reveals, cornices, sills, junctions | matching slabs/stairs, walls, beams |
| Accent | Identity, signage, restrained color | shutters, banners, painted doors, copper, foliage planters |
| Wear | History of local exposure and repair | cracked/mossy variants, damp base, patched infill |

A useful first pass uses one primary wall family, one framing/foundation family, one roof family, and a small set of accents. On a given elevation, a dominant/subordinate/accent balance around 60/30/10 is only a design heuristic, never a hard test or a requirement to add a third color. Small buildings may use 3–6 main families; palaces can support more organized sub-palettes. Do not count air or furniture in a wall palette. Review in ordinary lighting with default resource packs and the exact installed mod textures. Colors that match numerically can still clash due to grain, pattern scale or reflectivity.

Material relationships:
- Similar hue/value and texture scale make transitions look coherent. Use contrast deliberately: dark beams against pale infill, light trim against darker mass. Keep adjacent outlines visible at distance.
- Wood grain and cut ends have direction. Use vertical posts and horizontal beams with correct axis; planks as infill/floors, not bark on every surface.
- Pair full blocks with their actual stair/slab/wall variants when possible. A roof of one material with random unrelated stairs interrupts material continuity.
- Masonry can use a base of one species plus close cut/rough/cracked variants. Do not randomly blend every grey block, ore or polished industrial block into weathered stone.
- Smooth plaster may remain largely uniform, with cracks/repairs in localized patches. Brick needs consistent bonds and recognizable courses, not colorful pixel noise.
- Establish intentional edge transitions: plinth to timber, corner piers, sill/lintel, wall to roof, main to lean-to. Use a trim strip rather than a harsh checkerboard.
- Patina/wear is spatial: damp base, exposed corner, drain line, soot at hearth, shaded moss, weather-facing roof, repair patch, trafficked threshold. Prefer connected clusters or believable courses with a stable primary material. Do not independently randomize each block at high contrast. Gradients are tools, not compulsory fades; a sharp masonry-to-timber boundary can be exactly right.
- Never texture a silhouette problem away. Fix the large shape before adding more materials.

WorldLogic/new mod blocks: explicitly inspect the actual registered construction blocks, models and available shape variants of the active ecosystem. Prefer believable local species and geology (e.g. region's rock for plinth, regional timber for framing, clay/stone roof as appropriate). A new construction material should appear where it makes sense, not merely be mentioned in a loot chest or splattered across every wall. Do not require every modded block in every building. Do not invent registry IDs or assume "oak-like" means that all states and shapes exist.

Resolve materials by semantic roles with exact IDs or reviewed tag-based candidates; use a coherent family for full block/slab/stair/wall/door/fence where available. Resolve the entire palette for a piece or building once, not every cell independently. Use a defined fallback family with compatible states and behavior. If a necessary family cannot be supplied, choose a reviewed alternate template/palette or reject placement. A missing stair must not silently become air or a full cube that closes a staircase. Optional dependencies must not produce missing blocks or invalid palette references when absent. Check the actual texture of custom blocks adjacent to intended neighbors. Consume WorldLogic Core and relevant addon public APIs/data rather than duplicate climate, geography, geology or material physics. Do not generate imaginary timber species or rock types from names.

## 6. Regional architectural families — design patterns, not stereotypes

These are illustrative Minecraft translations of construction responses, not a claim that all people in a climate build alike. Validate against the setting, era, references and available local materials.

| Context | Massing / envelope | Details and functional cues |
| --- | --- | --- |
| Boreal / snowy / cold | Compact volume, robust timber/stone, often pitched roof, sheltered entry | hearth and flue, fuel storage, smaller practical openings, beams, drying/storage space |
| Wet temperate / maritime | Rain-shedding roof, eaves, durable plinth, ventilated sections | gutters implied by trim where appropriate, protected threshold, moss in shaded wet spots |
| Dry hot / arid | Thick earthen or stone mass, courtyard/shade, flat or shallow roof where precipitation allows | deep reveals, roof parapet, shaded arcade, storage jars, access to water |
| Tropical / seasonally wet | Ventilation, shaded openings, raised floor if flood risk, wide overhangs | light screens, connected porches, stable piers, drying platforms |
| Alpine / hillside | Narrow terraces, stepped foundations, site-aware orientation | retaining walls, uphill entry or split level, downhill piers, drainage |
| Coastal / river | Placement above relevant flood conditions, protected access to water | boathouse, mooring posts, tied dock, cargo storage, net-drying, pilings |
| Industrial / mining | Functional bays, reinforced spans, service access | loading doors, beams/trusses, rail connection, tools, stockpiles, maintenance spaces |
| Monumental / civic | Hierarchy, deliberate axes or courtyards, recognizable public entrance | readable scale, repeated bays, access to levels, ornament concentrated at focal points |

Climate influences building choices but should not erase cultural or economic variety. Avoid using dark spruce in a treeless arid location unless trade/history explains it. Snow shedding, waterlogging, drainage, fire hazards and sunlight are gameplay/environment considerations when the owning ecosystem exposes them. Do not fake real physics: if the actual mod does not simulate a roof protecting against weather, document that rather than claiming it does.

## 7. Facades: depth, rhythm and detail vocabulary

Compose the wall as foundation, middle and termination. Review all four elevations, not just the front. Each face may have different openings/decoration because of function, view, privacy or exposure. Rear/service walls may be quieter but must still be finished. Use large forms first (projections and recesses), then medium ones (bays/openings), then small detail.

A typical small building can benefit from a recessed infill panel between posts, sills and reveals, a shallow projecting plinth, a shadow line at eaves, and one entrance hierarchy. Scale matters: a one-block projection on a 5-block shed may be huge; on a 30-block facade it may be subtle. Repetition creates rhythm; break it at a meaningful entrance or corner. Do not attach identical trapdoors, fences and stairs to every empty coordinate.

| Technique | Implementation vocabulary | Use / avoid |
| --- | --- | --- |
| Plinth | full foundation block + slab/stair lip | grounded edge; don't leave it floating on downhill side |
| Frame bay | logs/posts, horizontal beams, recessed wall | use coherent intervals and aligned axes |
| Recessed window | opening behind outer frame, pane inside, slab sill | real glass opening; avoid blind fake windows on occupied rooms unless intentional |
| Lintel | horizontal log, solid stone, decorative stair underside | ends meet posts or masonry |
| Arch | full blocks, stairs (often upside-down), slabs, corner stairs | symmetric curve to spring line; no unsupported disconnected top |
| Corbel/bracket | upside-down stair, wall, timber support | under ledge/jetty/eave, not arbitrary tooth pattern |
| Cornice | stair/slab layers, trim bands | finish top edge; avoid excessive repeated stripes |
| Shutter | trapdoor with correct state/hinge and orientation | opens/reads like a shutter; no collision blocking passage |
| Balcony | supported deck, fence/wall rail, access door | proper floor and railing; not a shelf glued to wall |
| Entrance | recess, frame, step, canopy, sign or lantern | readable approach and usable door |
| Buttress | tapering steps/slabs, masonry pier | logical at high wall or vault, anchored to ground |
| Bay window | projection with side returns and roof | usable interior, supported underside |
| Roof verge | stair/slab rake trim and return | end closes cleanly; do not leave random holes |
| Fence / railing | connected fence/wall and gate | preserve access; correct connectivity at corners |

Use stairs and slabs as geometry, not as confetti. Vertical stair face can imply bracket depth; inverted stairs can provide underside profiles; inner/outer corners round an arch or eave; slabs make a thin sill, landing or cornice; walls/fences can slim a support or screen; trapdoors can be shutters, shelf fascia or grates; signs and banners can identify a room or workshop. Verify actual orientation/half/shape and neighboring collision. A visual trick must not create inaccessible rooms, unpredictable redstone updates, accidental water leakage or invisible support.

## 8. Roof design and difficult junctions

Design the roof while planning massing. It is usually the strongest long-distance identifier of a building. Select one main roof, supporting roof forms and the intended ridge direction. Match material, span, local precipitation and building use. Do not leave a flat top unless the design calls for a real flat roof with a believable edge, drainage/access or parapet.

| Roof family | Block treatment | Appropriate uses |
| --- | --- | --- |
| Gable | stair rows rising toward ridge, blocks/slabs closing top and gable | dwellings, barns, workshops |
| Hip | inward-sloping sides with coherent inner/outer corner states | compact buildings, exposed locations |
| Shed / lean-to | single slope of slabs/stairs, high-wall flashing implied by trim | porch, stable, service wing |
| Steep gable | taller repeating rise using full blocks and stairs | snow-oriented or historic timber forms where justified |
| Shallow | paired slab steps, low parapet or eaves | service roofs, drier regions, industrial architecture |
| Cross gable | joined primary/secondary ridges and valleys | wings and more complex plans |
| Mansard | steep lower and shallow upper courses, actual floor space | urban or larger inhabited structures |
| Tower roof | controlled taper, tiers or pyramid, capped peak | tower and lookout |
| Flat | slab/full surface, edge/parapet, roof access/drain cue where relevant | courtyard, modern, industrial and deliberate climate responses |

A 1:1 rise/run is naturally expressed with stair courses; a 1:2 rise/run can use half-block steps; 2:1 may use additional full-block rise per course. Verify real geometry by placing a strip and viewing a side profile. For odd widths plan a ridge cap; for even widths plan a balanced join. Do not accidentally mirror stairs so they face away from the ridge. Exact state orientation must be tested in the active loader/game.

For a small gable, build both eaves, rise symmetrically inward, close the ridge, fill end wall and add verge/eave trim. Use overhangs intentionally, often half to one block, and cover underside where appropriate. The roof must cover the actual enclosed rooms; gable infill, dormers, skylights, chimney penetration and intersections need clean transitions. Cross-gables require a real valley and correct intersection; do not overlap two full roof templates so inaccessible voids remain. If the roof volume is substantial, give it a plausible attic or deliberately open truss area. A dormer must connect to an actual interior or be a documented decorative exception. Test roof coverage/weather behavior only to the extent supported by the game and actual mods; do not call a collision test a waterproofing simulation.

## 9. Interiors are mandatory for accessible architectural space

The interior is designed at the same time as the shell. Every intended accessible floor/room has an identifiable role, entry, walking routes, furniture/activity areas and appropriate lighting. This applies to every distinct layout and procedural furnishing variant. Not every square needs an object; empty circulation, prayer halls, loading bays, animal pens and deliberate storage capacity are legitimate. An enormous unoccupied room with a single chest is not a believable furnished home.

Use the room program to design coherent small groups instead of random prop scattering. Example: bed + light + storage near it; hearth + preparation surface + fuel nearby; workbench + tools + materials + output storage; dining table + seating with a walking route. Objects at appropriate human height, not clutter hanging at head level. Keep space to approach functional faces, open doors/chests where required, use workstations, sit (if supported), access ladders, enter beds and reach upper floors. Never block a doorway with a decoration, or place a chest under a solid block if that prevents its intended use. A prop can be decorative, but don't label it as interactive if it isn't.

| Space | Minimum credible program, adapted to role |
| --- | --- |
| Dwelling | sleep, storage, food preparation or shared hearth, light, entry/circulation; table/seat if space/wealth fits |
| Inn / tavern | customer entrance, common room, serving area, storage, sleeping areas if lodging, stairs and service route |
| Forge / smithy | hearth/furnace, anvil/work zone, fuel/ore/metal, quenching cue, ventilation/flue, clear access |
| Workshop | task-specific bench, tools, raw input, finished goods, task light, usable workspace |
| Barn / stable | wide door, pens or open storage, feed/hay, service aisle, roof structure, appropriate flooring |
| Farm outbuilding | storage/work surfaces, path to fields, water or tool area as relevant |
| Warehouse | load entrance, clear lanes, stacked storage groups, handling area; upper access if second floor |
| Tower / fort | reachable lookout, stairs/ladder, watch/guard area, supplies, defense access |
| Temple / civic hall | ceremony/meeting focal point, circulation, side/service rooms as appropriate |
| Mine | adit/shaft, support bays, work/access route, drainage/ventilation cues as needed, tool/ore staging |
| Dock / boathouse | approach, mooring/working edge, vessel or cargo space, gear store |
| Ruin | evidence of former room uses, broken furniture/roof, salvage and believable remaining access |

Furniture vocabulary in vanilla scale: stairs or slabs as benches (do not claim a sitting mechanic unless implemented), fence post + pressure plate/trapdoor as small table where safe, upside-down stairs/counter slab for worktop, barrels/chests/bookshelves as storage, item frames/signs for identification, cauldron or approved vessel for wash/quench, beds, plants, carpets and banners for domestic cues, lanterns/candles/torches for light. Use custom furniture, shelves, barrels, lamps or tools when the modpack provides them and state/interaction compatibility is verified. Place objects in coherent relationship to the room, not just to maximize diversity. Check collision shape and behavior, especially pressure plates, campfires, water, gravity blocks, doors, trapdoors, container lids and hanging entities. Do not turn decorative blocks into hazards unless intended.

Detail floors and ceilings: coherent planks, stone/brick courses, localized wear, hearth-safe material, threshold, rug or mat only where useful; visible beam patterns, light mounting, ceiling changes where justified. Align stair opening and landing vertically; no beam intersects the path. Separate public/private/service zones when scale supports it. Every window visible outside has a credible interior relationship; no window through a cupboard, blocked wall or arbitrary floor slab except intentional bricked-up/false windows. Avoid eye-height beams across walking routes, unreachable attics, impossible furniture and duplicated placeholder rooms.

Lighting: design readable light sources and their supports. Daylight enters through actual openings; night lighting highlights circulation and use zones. Verify relevant target spawn/light rules rather than rely on outdated light-level assumptions. A functioning inhabited building should not unintentionally spawn hostile mobs; abandoned hostile structures may intentionally be dark. Do not flood an entire room with invisible light blocks solely to hide weak lighting design. Hidden lighting can be justified where appropriate to style and actual supported features. Inspect default lighting and night; shaders are optional polish, not a substitute.

## 10. Building types, contextual detailing and environmental storytelling

Function drives silhouette and content:
- Farmhouse: domestic wing, sheltered entry, fenced yard/field access, tool/fuel/produce storage. Keep agricultural/work and living spaces believable.
- Mill: machinery location explains layout; a watermill relates to real water level and wheel/sluice, a windmill needs sails and clearance if represented. A decorative mill must not be described as a functioning machine.
- Smithy: visible flue, roof/fire-safe zone, open service frontage or ventilation, anvil and fuel stock. Avoid fire-sensitive props next to flame when the active mechanics would destroy them.
- Castle: silhouette of halls, defensive walls, gatehouse and towers, access to battlements, inhabited and service spaces, storage, water and circulation. Scale defensive walls separately from residential rooms; avoid a giant hollow cube decorated with battlements.
- Village: buildings of different size/wealth/role sharing related local materials; routes join actual doors, wells, working areas and public spaces. Use focal public building and smaller subordinate forms; orient entrances toward relevant paths, not all toward one compass direction. Avoid perfectly uniform grids unless planned settlement is intentional.
- Bridge: select banks, believable abutment and deck level, supports/piers if span needs them, railings if design warrants, connected approaches on both ends. No deck that stops above the ground.
- Dock: real shoreline and water depth; piles/supports, land access, working edge, cargo/boat clearance; do not fill navigable water with decorative debris.
- Mine: see dedicated underground rules below.
- Ruin: use damage logic tied to original architectural form. Preserve enough of former outline and room program to read its history. Local roof collapse can imply debris below and moisture entry; broken wall needs a coherent edge, remaining columns support surviving spans. A reclaimed structure can have local vegetation. Do not simply replace independent random blocks with air or moss.
- Wealth, age, occupancy and repairs are independent variables; a well-maintained small house and a derelict mansion are both plausible. Use repaired patches, secondhand furnishing, tools at workstations, storage in service zones, signs and wear at the entrance. Leave quiet surfaces and negative space. Do not require lore text for the building's purpose to be readable.

Exterior finish: path and doorstep, small fence/gate/retaining wall, yard, drainage, woodpile, bench, sign, planter or garden as role warrants. Place them in useful zones; don't copy the same garden outside every building. Landscaping is in scope only as it relates to the building interface and does not trigger a review of unrelated vegetation systems. Road connections must meet actual terrain and door levels. Nearby plants should not obscure all entrances or appear in occupied interiors without an intentional ruin/greenhouse narrative.

## 11. Underground construction: mines, shafts and built tunnels

Natural caves and ore generation are not in scope. A mine is a human-built system inserted into geology, not a random network of empty cuboid rooms. Choose ore/work purpose, approximate era and operational status. An adit enters a slope or suitable rock face; a vertical shaft has a credible surface entrance/shafthead and a continuous travel mechanism; galleries branch from a reachable main route. Main travel corridors and working faces are distinguishable. Account for ore-bearing regions via the existing geology provider where applicable, without forcing a mine through every ore vein.

- Plan the graph: entrance -> main passage/shaft -> junctions -> working chambers, ore staging/storage and any escape/service route appropriate to scale. Terminate unused connectors in believable walls, collapsed ends or intentional faces. No accidental isolated rooms or doors into solid rock.
- A main gallery often starts around 2–3 clear blocks wide and 2–3 clear high depending on rail/support style; tight branches can be narrower. Actual player collision, rails and custom entities determine dimensions. Maintain headroom over stairs and rails.
- Repeating support bays: timber posts meet floor or sill and beam meets roof/side rock. Use cross-bracing/arches where justified. In a strong rock section supports may be sparse; loose sections may be more reinforced. Structural variation follows terrain/material, not arbitrary decoration noise.
- Rail routes need continuous valid supporting blocks, connected slopes/corners, clearance and meaningful termini. Inspect real rail state after placement and neighbor updates. A decorative broken abandoned rail can be intentional.
- Light is mounted on supports/walls, not floating. Work areas have tools, ore containers, rubble, supports and staging where believable; don't scatter valuable loot independently in every tunnel.
- Drainage and ventilation are contextual cues: channels, sumps, pumps (if implemented), shafts or air passages. Do not claim real simulated ventilation without integration. Preserve water/cave hazards unless explicitly drained/sealed by designed engineering. Test for unintended inundation, lava exposure, rubble in the only required access and unsupported gravel/sand.
- Distinguish entrance ruins, collapsed branches and active pathways. A deliberately inaccessible branch is marked in the design; the intended player path still works. Place hazards and rewards according to gameplay progression.
- For underground placement, sample rock/cavity/liquid along the actual volume and footprint. Reject or intentionally integrate large caves rather than leave floating tunnel sections. Respect vertical limits and aquifers. No deep forced chunk scanning or global ore search.

## 12. Site selection, orientation and terrain blending

A handcrafted building placed poorly becomes a bad generated building. Choose site before placing architecture. A heightmap sample at one corner/center is insufficient. Determine footprint (including porch, eaves, path and foundation/support area), sample relevant ground and fluids, assess slope, local relief, biome/climate, water/flood risk, existing structures, cave voids and a safe approach. A site may be rejected rather than "fixed" by flattening a huge area.

Separate placement policy by type: flat/low-slope house, hillside terraced house, shoreline dock, over-water stilt structure, cliffside adit, underground mine. Define per-family allowed slope/cut/fill/support-depth limits and bounded attempt count, not one universal flatness number. Example pilot defaults for a small ordinary house: sample corners, perimeter and bearing supports (full footprint if cheap); a 0–2 block change can often be absorbed with steps and foundation, a bigger change should use a designed hillside variant or rejection. This is a starting heuristic, not a requirement for every world. A tall basement/pier is not automatically wrong if deliberately designed.

Suggested placement algorithm (pseudocode, not a specific loader API):
1. Derive deterministic candidate RNG from world seed, structure ID/salt and candidate coordinates. Select viable regional family and orientation(s).
2. Determine full planned bounding boxes, site samples, terrain type and approach. Check world border, dimension height, fluids, collision with known placements, protected/nonreplaceable blocks as applicable.
3. Score candidates by role suitability; use hard rejection for impossible supports, unwanted deep water, unsafe access, forbidden overlap, excessive cut/fill or required height out of range. Keep all sampling and attempts bounded.
4. Calculate finished floor level and individual footing endpoints using suitable ground, not leaves or temporary surface objects. Plan foundations only where structural contact is needed. Avoid filling a cave or water body as an unintended side effect. For a pier, verify the intentional underwater contact or flotation design.
5. Calculate local earthwork/steps/retaining and access, then verify the combined plan again. Write only within the authorized generated footprint and bounded blend area. If unsuitable, try another bounded candidate or skip.
6. Place with the target world's sanctioned generation mechanism, deterministic piece transforms and clipping. Add structural components, interior and support-sensitive blocks in safe order or use correct processors; preserve intentional air.
7. Verify final supports, ingress, intact connections, block states, fluids and neighboring terrain. Keep generation independent of player observation, load order and client-side rendering. Do not rerun and duplicate loot/decor on chunk load.

Terrain blend:
- Follow nearby topography: stepped foundations on small slopes, split-level floors on larger designed terraces, bounded retaining walls with plausible returns, stairs/paths that meet the terrain. Preserve hillside shape instead of creating a giant rectangular stone slab.
- Cut and fill locally, feather transitions with existing regional surface materials. Avoid a square hole, sheer artificial plateau, dirt cube, unsupported platform, unnatural vertical cliff or a giant mound hiding the lower floor.
- Distinguish floor foundation contact from clearance beneath intended raised buildings; no air gap under an ordinary ground-floor plinth. Check support around corners and load-bearing posts, not just central columns.
- Inspect terrain before and after placement, including the opposite side and lower elevation. Door thresholds must meet walkable exterior; path should not end in a two-block wall or drop. Provide steps or reject.
- Keep existing caves, water bodies, adjacent structures and natural terrain outside the authorized blend envelope unchanged. Respect custom world presets. No regeneration of existing player builds; ordinary generation belongs to new chunks. Intentional retro-generation is a separately authorized feature with a safe migration strategy.
- WorldLogic must use authoritative geography, hydrology, geology, climate and applicable material APIs. If missing, ask provider through project process and avoid inventing duplicate world classifiers. Do not synchronously force chunk loads in worldgen to ask APIs that require a live fully loaded level; use a worldgen-safe capability or defer/skip with a reported limitation.

## 13. Implementation architecture and reusable templates

Prefer **hand-designed, reviewed architectural templates plus controlled procedural assembly and material/furnishing variants** over free-form block-by-block random geometry for normal buildings. Fully procedural architecture is valid only when the actual generated results meet the same quality. Use data-driven templates if adequate; custom Java when site adaptation, topology, integration or specialized placement actually needs it. Do not introduce a new structure framework per addon or a mandatory external building mod at runtime.

Java Edition implementation options:
- Author in an isolated creative world using the active version's real blocks; use Structure Blocks or suitable editor/exporter. Save native structure NBT and review actual captured bounds, block states, block entities, intentional air and palette. Do not include an accidental layer of terrain outside the plan. Large designs may need multiple connected pieces; verify the target version's actual exporter/placement limits.
- For jigsaw generation, declare structure/structure set/template pools/processors and biome applicability in the version-appropriate data layout. For 1.21.1 Java, templates are normally under data/<namespace>/structure/ and worldgen registries under data/<namespace>/worldgen/structure/, structure_set/, template_pool/ and processor_list/. Earlier versions may use plural template paths. Verify schema and mappings in the real target sources before implementation; do not copy later-version JSON wholesale.
- Jigsaw is useful for modular settlements, mines and large compositions. Define connector roles (entry, road, corridor, upper-floor stair, wing, dead end, bridge bank), allowed orientations, height offsets and closure/fallback pieces. Match compatible openings and floor levels; reserve actual transformed volume including eaves/supports; prevent collisions or unintended interpenetration. Cap piece count, depth, distance and retries. Weights must not substitute for structural constraints.
- Distinguish rigid architectural rooms from terrain-following exterior routes; vertical terrain matching across individual columns can distort rigid buildings. Terrain adaptation settings are not automatic foundation engineering. Explicitly test the resulting site blend and supports. Processors may alter appearance or remove blocks, but must preserve load-bearing and path-critical blocks unless a deliberate ruin template permits damage. Keep variation masks/regions and state-aware substitutions.
- If using custom Java generation, separate SiteEvaluator, PaletteResolver, LayoutPlanner, Piece/TemplatePlacement and Furnisher (names illustrative, not compulsory APIs). Build an immutable or bounded local plan before committing to placement; use the correct target loader/game APIs and worldgen region. Do not call client code or unrestricted level/chunk queries from generation.
- Use deterministic named random streams or stable derived seeds for independent decisions: family/layout, orientation, materials, furnishing, wear. Stable generation must not depend on global shared random, thread execution order or player timing. Keep actual IDs, salts and versioned save behavior stable on updates.
- Keep authored/editable source plans or NBT templates under the owning project with meaningful names. The final JAR must contain actual packaged assets, not only generated documentation or a screenshot. Use project-local generation data; avoid huge hardcoded lists in one Java class.
- Include clear registry IDs, config/datapack overrides for spacing/rarity and suitable allowed biomes/regions, safe defaults, bounded complexity and a discoverable debug method. Respect normal configured rarity; use a debug fixture to inspect a rare structure without changing shipping rates. Do not ship permanent test commands, forced placement or altered world spawn without a reason.

Suggested project-local data vocabulary (illustrative, not a newly implemented schema): family ID, variant/template ID, function, layout rooms, palette family/roles, foundation contacts, exterior access points, walkable route endpoints, supports/attachments, intended hazards, intentional inaccessible areas, permitted rotations, climate/site constraints, bounds, connector types, loot contexts, debug fixture IDs and QA assertion IDs. Reuse existing profiles/scenarios rather than create a redundant system.

## 14. Correct block states, composition and post-placement behavior

A block inventory or an attractive voxel sketch is not a valid Minecraft building. Review the real BlockState and collision shapes under rotation/mirroring. Particularly inspect:
- Stairs: facing, half, shape (straight/inner/outer), waterlogged where applicable. Slabs: bottom/top/double and waterlogged. A stair corner inferred from neighbor states may change on update; inspect after updates and after reload.
- Logs/pillars: axis; doors: lower/upper, hinge, facing, open/powered states; beds: head/foot, facing, both halves; tall/two-part blocks: complete pairs and correct support.
- Trapdoors, fence gates, fences, walls, glass panes, chains, lanterns, buttons, ladders, signs, banners, rails: orientation/attachment, actual connections, floor/ceiling/wall mount and waterlogging. Rotating a position without rotating its state is a defect.
- Gravity blocks, foliage decay where relevant, fragile blocks, water/lava, fire, redstone, ticking blocks: ensure intended behavior after placement and bounded updates. No accidental cascade, flood, self-destruction, burning furniture, disconnected rail or exploding machinery. Custom material physics, fire, weather or fluid behavior may be stricter than vanilla.
- Block entities: correct registry type, valid state/NBT, sanitized and intended contents. Chest pairs/lids and access; no repeated unique loot because a chunk is revisited. Use context-appropriate loot tables with progression, rarity and balance; do not ship author-world inventories, private text, player UUIDs, command blocks, arbitrary commands or unapproved spawners/entities from imports.
- Distinguish air (intentional clearing) from template omission/structure void (preserve terrain where intended). Never paste a bounding box of air over unrelated land or other structures. In a room, unwanted terrain intrusions are defects; in a ruin or underground design, deliberate rock/rubble must match the plan.
- Recompute/verify connector states and support-sensitive neighbor effects using sanctioned mechanisms. Never indiscriminately trigger full updates across a huge worldgen volume. Ensure no residual jigsaw/structure/debug/marker blocks or unexplained barriers in the player-facing world.
- Test actual placed states for each allowed rotation and mirror of asymmetric content, or disable unsupported transforms explicitly. For multipart generation, test chunk-boundary clipping and save/reload.
- Validate all referenced namespace IDs, template/pool/processor references, resource layout, NBT data versions and optional dependencies against the active target. A code compilation result is not proof of correct data-driven worldgen.

## 15. Variation without loss of artistic direction

Generate variation at the level a builder would choose: whole building shape, regional family, room function, window rhythm, optional porch/wing, roof style, coherent material family, maintained/abandoned state, furniture arrangement. Preserve relationships. Do not independently choose a random block for every position.

- Define primary templates, compatible modules and controlled variant slots. Example: three dwellings with different footprints/rooflines, two porch types, region-appropriate timber/stone families, alternative furniture sets that fit actual rooms. The number is illustrative; author fewer good structures before expanding.
- Keep each family recognizable by consistent proportions, frame rhythm, materials and roof details. Change masses and plans between families, not only recolors. Use same-family details with matching grammar.
- Select decor by room type and available placement sockets/clearances: floor-standing, wall-mounted, ceiling-hung, shelf-top, outdoor. Verify support, facing, size, neighboring cells and access. Reserve doors, stairs, walkways and functional faces before placing clutter. Use deterministic bounded placement; skip optional decor when it doesn't fit.
- Wear/destruction is conditioned by age, exposure and structure role. Keep essential intact supports/routes unless the template explicitly models collapse. A collapsed area may be inaccessible by design; provide an alternative path if the gameplay requires it. Use debris below collapsed roof, patches at repairs and coherent remaining spans.
- A settlement should have variety at site scale: public anchor, clustered houses, work spaces, open yards, paths, deliberate orientation and distribution of building roles. Avoid spawning a full village's worth of identical huts every few chunks. Configure rarity with actual sampling; never make buildings common merely to simplify QA.
- Use a deterministic seed matrix covering every distinct template/layout and material-role family, plus representative combination extremes. Where independent slots are proven constrained, avoid expensive Cartesian explosion. Verify each distinct allowed transform, conditional geometry branch and critical connector type in the cheap fixture layer. A representative runtime test does not mean every random combination was rendered.
- When a newly generated design is poor, improve the template/layout grammar or constraints instead of increasing randomness. Do not produce mass variants before approving a good representative.

## 16. Review workflow: architecture before production

Follow this sequence for a new family, or only affected steps for a narrow update:
1. Research and brief: role, site, references, scale, region, materials, room program, generation constraints. Confirm relevant active target and registered block families.
2. Blockout: floor plan, massing and structure; choose roof, entrances, sections and supports. Inspect silhouette, elevations and accessible interior volumes.
3. Palette mock-up: actually place a small wall/corner/roof junction using the intended target blocks, including new mod blocks. Check default textures together at near and middle range. Review colors/material states before batch generation.
4. Author representative complete design: all sides, roof closure, foundation, interior, path, attachments, block states. Make a real template or implementation; not an AI-generated concept image as a deliverable.
5. Review in actual client where supported: walk from approach through every intended room/level, inspect full exterior and roof, night/day, supports and interior. A slice/cutaway or schematic viewer helps find hidden defects; it does not replace rendered game evidence. If actual graphical testing is unavailable, inspect the best real block-state/template geometry and report graphical review NOT_RUN.
6. Fix concrete defects before proliferating variants. Evaluate generated siblings on real target palettes. Keep only the useful variation.
7. Wire existing machine-readable QA and validate final JAR on real installed loader, natural generation and persistence as relevant. Report results against actual artifact. Don't pretend /place alone proves rarity/site selection or worldgen.
8. Preserve real evidence in existing CI/project QA, not a duplicate long report. Review notes may be compact (template ID/hash, view/seed/coords, finding, fix and disposition).

For a newly designed building, a representative screenshot set or rendered review should show at least approach/three-quarter, rear/other sides, roof from above, interior on each accessible level, and the foundation on problematic terrain. Large complexes add connectors and cutaways. Use fewer views for a targeted small edit when they still cover it. Inspect ordinary game lighting/default textures first; shaders may hide holes or exaggerate attractiveness. Evidence must be tied to the actual generated template/build, not an unrelated concept rendering or handbuilt stand-in. Do not make the user operate the bot, write a test assignment or handcheck every port.

## 17. Executable QA and structural acceptance

Use [QA assignment](../qa/QA-ASSIGNMENT-TEMPLATE.md), [QA guide](../qa/QA-V2.md) and current Build Hub. New or modified architectural mechanics get stable executable test IDs in the owning scenario/manifest. Existing server/RCON, GameTest, production loader and player tools are preferred as supported. Do not claim a generic automated building-aesthetic evaluator or collision solver exists merely because this document describes what to test. No global pipeline expansion is required. Add a scoped checker in the owning project if current tools cannot express a critical deterministic condition; if a backend is missing, report precise NOT_RUN, not PASS.

### Minimum applicable test layers

| Layer | Evidence and example checks |
| --- | --- |
| Template/data validation | All referenced IDs/states resolve, expected bounds, intact multipart blocks, no marker leaks, valid NBT/loot/pools, actual packaged resources |
| Deterministic plan/fixture | Every distinct layout and geometry branch, permitted rotations, coherent palette fallback, support contacts, connectivity/clearance, no forbidden collisions, bounded attempts |
| Production placement | Real loader with exact JAR; force/debug placement for inspection of each relevant family, variant and connector where supported; verify real states and updates |
| Natural generation | Fresh worlds on representative seeds and intended regions/sites, valid placement and rejection, terrain blend, actual registration/rarity evidence, no floating builds, chunk seam and save/reload |
| Gameplay | Reachable entries and intended rooms/levels, working doors/containers/workstations/rails when claimed, intended lighting/hazards, no duplicate contents |
| Client appearance | Actual client screenshots/walkthrough tied to seed/coords/template/artifact when supported; visible craft, material coherence, all sides/levels, no voids/holes |
| Performance/compatibility | Bounded candidate search/piece count/footprint, no synchronous forced chunk cascades, stable worldgen on relevant presets and dependency combinations |

For a rare structure, use deterministic fixture placement for detailed checks and a bounded natural-generation sample for placement and spacing. Natural generation tests should include at least representative suitable and unsuitable sites, separate terrain cases appropriate to family (flat, slope, water/coast, cave if applicable), chunk-edge/rotation and a reload where relevant. Check a second seed or different candidate ordering when procedural placement changes; use additional cases only for meaningful risks. Sample across enough candidates to evaluate the configured rarity and report the actual sample size; a small sample does not establish exact frequency. Do not increase normal spawn rate just to make a test green. For a village/mine, verify connector outcomes including terminal/collision-rejected cases. Debug placement is not evidence that natural placement works. An ordinary unmodified vanilla building is not a regression target.

### Candidate deterministic checks (implement only applicable assertions)

| Check ID pattern | Positive and negative evidence |
| --- | --- |
| structure.<family>.template | Valid template loads and all expected blocks/state references resolve; missing required ID fails |
| structure.<family>.site | Suitable site accepts and reaches terrain; unsuitable site rejects within attempt budget |
| structure.<family>.foundation | Intended supports meet valid ground; engineered span exception documented; ordinary building with air gap rejected |
| structure.<family>.access | Connected outside-to-door-to-room/level routes with actual collision clearance; blocked door/landing detected |
| structure.<family>.roof | Required enclosed rooms have intended roof coverage; known open courtyard/ruin excluded explicitly |
| structure.<family>.state | Correct transformed stairs/doors/logs/containers after neighbor updates and reload |
| structure.<family>.palette | Valid material families and no absent optional-mod references; fallback preserves geometry |
| structure.<family>.connections | Compatible jigsaw endpoints and walkable transitions; overlap/unclosed required end detected |
| structure.<family>.placement | No unintended changes outside allowed footprint; no protected overlap or invalid fluids |
| structure.<family>.lifecycle | Same seed/layout stable on repeated fresh generation; reload does not duplicate blocks/loot or change states |
| structure.<family>.interactions | Claimed container, door, path, rail, workstation or entity use actually works |
| structure.<family>.budget | Bounded samples/retries/pieces; real timing/observations where available, no invented universal threshold |

A support checker is not a naive flood-fill of all blocks: define meaningful ground contacts and attachments, detect wholly detached structural components, allow arches, lintels, cantilevers, connected ceilings and intended hanging items. Distinguish decorative adjacency from collision/support faces. A walkability checker must account for body/head clearance, stair/slab elevations, doors and supported transitions; a 2D flood-fill ignoring collision shapes is only advisory. Check support-sensitive blocks after updates, not just raw NBT. Roof coverage assertions must exclude deliberate skylights, courtyards and ruins where appropriate. Quantitative indicators (wall depth, block palette distribution, empty room ratio, footing length) are useful diagnostic signals, not proof of beauty. Do not invent minimum decor percentages or change a good design to satisfy metrics.

### Hard structural blockers

Unexpected floating foundations/parts, unreachable mandatory interior/level, impassable staircase/door, incorrectly transformed multipart blocks, unsupported decor that falls/breaks, invalid registry/state/template references, unintended fluid/fire/gravity destruction, terrain intrusions blocking intended rooms, unclosed required connectors, destructive overlap, unsafe unbounded chunk generation, broken persistence/duplication, missing required QA. The fix is to correct the design/generator, not remove the assertion.

### Artistic rejection signals

These are review findings, not blind numeric gates: generic unarticulated box unrelated to style, disproportionate roof, facade-only decoration, empty accessible living spaces, inconsistent or noisy palette, fake inaccessible windows/doors, repeated purposeless details, visible floating/bracketless masses, unexplained abrupt foundation, huge artificial platform, excessive identical clones, random decay, incoherent narrative props. A deliberate stylistic exception must be justified by the actual design; "procedural generation" is not a justification. Fix the root issue (layout/massing/palette/support/placement) before ornament.

### Compact final evidence

For affected architectural families provide a compact existing QA summary with active target, template/variant scope, exact commit and final JAR, applicable assertion PASS/FAIL/NOT_RUN, natural-world seed/coordinate cases and actual visual evidence availability. Include first unresolved defect, unsupported backend, material fallback or major design exception. Group truly equivalent assets only when all affected variants are checked. Do not claim complete architectural quality from CI that only compiles. Do not claim actual graphical review from NBT render/preview or RCON. A documentation-only change requires documentation/link tests, not generation of a sample world.

## 18. Asset acquisition, reference libraries and import workflow

Primary workflow is original building design with deliberate references. If original output remains below builder quality, consider a high-quality reference schematic, licensed template or commissioned builder rather than repeatedly accepting weak boxes. Do not copy entire buildings from images or another mod, nor import and recolor an unlicensed structure. Study principles independently where reuse rights are absent. See [MOD-ADAPTATION](../development/MOD-ADAPTATION.md) if adapting another project.

Useful discovery sources (these are catalogs or tools, not blanket asset licenses):
- [Planet Minecraft — example build schematic collection](https://www.planetminecraft.com/collection/324978/build-schematics/): varied map projects, houses and full-interior examples; inspect individual creator page, credits and license.
- [Abfielder](https://www.abfielder.com/): searchable schematics, paid and free listings. Prefer actual downloadable structure with interiors, material list, multiple views and correct version. An example [EcoSMP listing](https://www.abfielder.com/Products/ProductDetails.php?id=11744) states BY-NC-SA; that is a reference example, not authorization to redistribute in MineMods.
- [Minecraft Schematics](https://www.minecraft-schematics.com/): browse buildings, settlements, bridges and other downloadable creations; verify original author and actual permission.
- [BuildSchematics](https://buildschematics.com/about): inspect compatible dimensions, palette and viewer where available. A browser viewer is useful for design evaluation, not license clearance or live gameplay QA.
- [MarkCraft terms](https://markcraft.org/schematics/terms/): example of a creator-specific license with restrictions; read exact terms and get permission for intended mod distribution if not expressly allowed. A download marked free is not necessarily freely redistributable.
- [Grian — roof Build School](https://www.youtube.com/watch?v=u9mJcQ79AQ8) and [Interior Build School](https://www.youtube.com/watch?v=2cKFre8I7Z8): practical reference tutorials; use techniques, not unlicensed extracted assets. These video listings were identified during research; do not claim a frame-by-frame review of videos without actually watching them.
- [HueBlocks](https://github.com/1280px/hueblocks) and [MinecraftMaps palette tool](https://www.minecraftmaps.com/tools/block-palette-generator): color/gradient planning; inspect actual target textures, shape family and material roles afterward. Do not use a generated gradient blindly.
- [WorldEdit clipboard documentation](https://worldedit.enginehub.org/en/latest/usage/clipboard/): editor workflow and Sponge schematic format. Use a trusted version-compatible editor, not a random executable.

Search by architectural role + style + "full interior" / "schematic" / "world download" / "floor plan" / "creative commons" as appropriate. Before acquisition check footprint, actual floor plans and interior, all elevations, technical version, required mods, file format, author, source link and rights. Paid purchase or public download alone does not grant right to redistribute. Obtain express rights for inclusion in source repository and distributed mod JAR, adaptation, and intended commercial/noncommercial use as applicable. Prefer compatible permissive licenses or clear written permission. Review [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/) and [licensee considerations](https://creativecommons.org/share-your-work/licensing-considerations/version4/) for attribution, modifications and license conditions; NC, ND and SA restrictions must be evaluated for the actual distribution. Unknown permission means reference-only; do not import. Preserve creator attribution, title, source/license URL, version, changes and relevant notices. Do not imply endorsement. If permission is incompatible or unclear, choose another asset or create an independent design.

Import process:
1. Obtain original licensed archive from a trusted source; record exact origin, author, permission and checksum. Review archive content; do not execute included scripts or tools. Keep source records and any required notices.
2. Identify file format: modern WorldEdit .schem, Litematica .litematic, legacy .schematic, or Java native structure .nbt. They are not interchangeable. Use version-compatible converters/editor and inspect the resulting block-state and entity data.
3. Open in a disposable isolated test world at the active Minecraft/loader, not a player's world. Inspect actual dimensions, palette, interior, roof, exterior and supports; remove hidden invalid/unsafe payloads (commands, spawners, entities, personal NBT and inventories unless specifically intended/reviewed).
4. Adapt to MineMods coherently: actual local materials/shape families, real room function, loot balance, foundations, climate/site placement, connector interfaces and access. Do not blindly replace material IDs; map block-state properties correctly and verify geometry.
5. Export native target templates or an appropriate project-local representation. Split large structures where necessary with coherent connectors and clipping; validate rotation, mirroring, data version, preserved air semantics, support and access. A schematic file sitting in resources is not an implemented worldgen structure.
6. Apply the same structural, artistic, natural generation and compatibility checks as original architecture. Preserve provenance in project notices and final distribution as required. Do not ship assets with unverified rights, hidden missing dependencies or a worse adapted result than the original.

External assets are not mandatory, and no catalog is approved in bulk. Commissioning a builder with an explicit redistributable mod-asset agreement is an option if the artistic bar cannot be reached with original design.

## 19. Source basis and technical reading

Research reviewed 2026-11-28. The design rules, examples, acceptance criteria, numerical starting points and QA strategy above are MineMods' own applied synthesis, not copied tutorials or guaranteed official engine specifications. Read the relevant source sections when implementing an unfamiliar architectural pattern; do not reread every external link on unrelated tasks. Sources are not instructions to import assets.

- [Minecraft.net — How to do MEGA builds](https://www.minecraft.net/fr-fr/article/how-do-mega-builds): first-party published builder case study featuring Grian and PearlescentMoon, useful for massing and detail hierarchy. Borrow design workflow, not giant scale for ordinary buildings.
- [Minecraft Wiki — Adding beauty to constructions](https://minecraft.wiki/w/Tutorial:Adding_beauty_to_constructions): community guidance on architectural depth, materials, interiors and exterior treatment.
- [Minecraft Wiki — Roof construction guidelines](https://minecraft.wiki/w/Tutorial:Roof_construction_guidelines): roof geometry and discrete slope/junction references.
- [Minecraft building guides index](https://minecraft.pl/en/guides/building-guides) and [Minecraft builder's guide: roofs, walls and arches](https://blockaria.com/en/tutorials/build-roofs-walls-arches/): supplementary practical design vocabulary; verify actual block availability and don't treat 26.x counts as 1.21.1.
- [Historic England — vernacular architecture and prevailing climate](https://historicengland.org.uk/education/training-skills/training/webinars/recordings/webinar-on-the-physical-elements-of-vernacular-architecture-and-their-relationship-to-the-prevailing-climate/): why region, climate and material availability belong in the brief, not merely the color palette.
- [Minecraft Wiki — custom structures](https://minecraft.wiki/w/Tutorial:Custom_structures), [jigsaw structure](https://minecraft.wiki/w/Jigsaw_structure), [structure definition](https://minecraft.wiki/w/Structure_definition), [structure file](https://minecraft.wiki/w/Structure_file), [Structure Block](https://minecraft.wiki/w/Structure_Block): community technical reference; version-check each field against actual runtime sources.
- [NeoForge 1.21.1 registries](https://docs.neoforged.net/docs/1.21.1/concepts/registries/) and [resources](https://docs.neoforged.net/docs/1.21.1/resources/): target-specific data registry/resource fundamentals. Actual code/mappings are authority for exact Java methods.
- [WorldEdit clipboard](https://worldedit.enginehub.org/en/latest/usage/clipboard/) and creator/license resources in section 18: format/import and rights boundaries.
- [Common Visual QA](VISUAL-QA.md) and [block art](BLOCK-ART.md): apply when creating actual new art; their pixel-level requirements are separate from architectural composition.

## 20. Final rule

Build like a map builder, place like a world-generation engineer, test like a player. A generated house is done only when the actual structure, not the idea or its front screenshot, is coherent inside and out, attached to its site, technically sound and adequately verified within the declared scope. Choose fewer excellent designs over many superficial ones. This document is a conditional development contract and reference, not an executable detector of architecture, a universal new release gate, or proof of a graphical review.

---

# Advanced architecture handbook — selective reference modules

Sections 21–30 deepen the universal procedure above. Use the relevant chapters for the assigned architectural family; a small hut does not require the city, dome or hydraulic chapters. These are original Minecraft design translations of the listed architectural references, not construction specifications for real buildings, a guarantee of structural engineering, or a claim that the example templates have been tested in-game. The same artistic and gameplay acceptance applies to a building regardless of which generation backend creates it.

## 21. Historical and cultural architectural grammars

### 21.1 How to apply a style without creating a caricature

A style is a coherent set of relationships: plan, section, scale, construction method, roof, wall openings, material joints, ornament hierarchy, site and activity. Pick a specific locality, approximate period and actual building role before choosing a style. Climate, era, cultural tradition, access to materials, construction technology and social status are separate inputs. A small medieval farm building and a cathedral built nearby do not share the same facade vocabulary. Avoid treating continents, modern national borders or religions as homogeneous palettes.

Study two or three references of the same typology, ideally an exterior and section/floor plan. Abstract the three strongest characteristics at far range, three at player range and three at close range. Decide which need literal representation and which can be simplified to Minecraft's block resolution. At small scale prioritize massing and structural grammar over symbols. At large scale carry the grammar through window rhythm, bays, interior and service spaces. Add decorative motifs only after the building reads correctly without them. Never attach recognizable sacred motifs to an unrelated design simply to signal an ethnicity. If a fantasy culture combines traditions, document the coherent construction story and intentional departures.

Example style record: `family=temperate_masonry_roman­esque_inspired; period=medieval; role=small_monastic_hall; plan=single_aisle_with_apse; primary=local_cut_stone; secondary=rough_stone; openings=round_headed_deep_reveals; roof=timber_gable; ornament=portal_and_cornice; prohibited=glassy_curved_roof,random_spires`. This is a local design description, not a required new schema. A grammar is tested by selecting a coherent variant, not by forbidding legitimate historical evolution.

### 21.2 Reference matrix — form, construction and Minecraft implementation

| Family / appropriate reference | Plan and massing grammar | Block translation and detail priorities | Reject |
| --- | --- | --- | --- |
| Ancient Egyptian monumental masonry | Axis, pylons, courtyard, repeated column bays, solid enclosure | Local warm sandstone-like families; battered walls with deliberate setbacks; large plain surfaces, patterned bands and column capitals | Every small dwelling becomes a pyramid; striped walls without role |
| Classical Greek and Roman civic | Raised base, ordered columns, entablature, pediment; Roman public halls may employ arches and vaults | Stone/quartz-like family as appropriate; column base, shaft, capital, level entablature; repeated arcade with proper spring supports | Random columns not aligned with roof, unsupported pediment, all-white default for every historical material |
| Romanesque | Heavy masonry, round-headed portals, compact towers, barrel/groin vaults where appropriate | Thick piers, deep reveals, stone courses; modest upper openings, stepped round arch | Gothic lancets and delicate glass everywhere; giant thin unsupported walls |
| Gothic | Vertical bays, pointed openings, articulated buttresses and ribbed ceiling where scale/typology warrants | Slender paired piers, tall lancets with controlled tracery, stepped ribs and clustered roof mass; actual interior height | Covering a cube with spires; flying supports into empty space; windows crossing floors |
| Byzantine / domed basilica | Central or longitudinal space, semi-domes or central dome, supporting transition from square bays | Massive pier groups; layered brick/stone, geometric surface decoration; stepped pendentive/squinch transition, readable interior volume | Dome balanced on decorative fences or solid-filled prayer hall |
| Renaissance | Measured bay rhythm, balanced axes, rusticated base, articulated orders and cornices | Consistent floor bands, restrained window surrounds, pediments, orderly facade; courtyard plan for suitable palaces | Uniform windows without interior alignment; mixing unrelated medieval trim |
| Baroque / Rococo-inspired | Hierarchy, central accent, curves or broken planes, designed sequence of rooms and approach | Strong center, paired wings, selectively enriched portals and cornices; ornament scaled to architecture | Random gilding on every block; no primary focus; oversized decoration on ordinary service sheds |
| Mediterranean domestic, locality specified | Courtyard or street-facing plaster/stone mass, shutters, tiled or climate-appropriate roof | Warm muted masonry/plaster, deep openings, shaded terrace, restrained roof color, stairs meeting slopes | Generic orange roof on every building regardless of region and period |
| Central European half-timbered | Post-and-beam bays, masonry plinth, infill, sometimes projecting upper floor | Vertical logs, horizontal floor plates, diagonal braces approximated only where readable; recessed plaster/brick infill, roof | Random log stripes and diagonal noise; timber running into window openings |
| Northern European rural timber | Compact log or framed volume, practical entrance and pitched roofs, often service annex | Horizontal log axes for log walls, visible corner joints, stone footing, subdued timber roof and functional yard | Treating all Nordic construction as exaggerated fantasy Viking longhouses |
| Scandinavian stave architecture | Upright structural posts, stepped roof hierarchy and perimeter bays in appropriate monumental cases | Structural staves, dark timber, steep layered roofs; use real base support and internal nave when applicable | Stacking floating roofs without structural connection |
| Karelian / northern Eastern European timber | Horizontal timber courses and corner joints, practical winter spaces; elaborate towered forms only for suitable monuments | Log wall axes, projected eaves, nested roof volumes, restrained carved trim; octagonal drums for large towers | Onion domes on every cottage; transplanting monumental church details onto a barn |
| Eastern European brick / vernacular masonry | Local typology: farmhouse, market townhouse, brick church or civic building | Choose actual local rock/brick and roof family; distinguish simple rural construction from urban ornament; articulated base/window heads | One generic "Slavic" palette for unrelated places and periods |
| Iranian earthen / desert courtyard | Solid envelope, internal shaded open space, deep reveals; local water/ventilation cues where supported | Terracotta/mud/adobe-like blocks, calm broad walls, parapets, stepped arch or shade screen, roof access | Imported steep spruce roofs everywhere; wind tower or water channel with no site/function |
| Nasrid / Andalusian palace-inspired | Court-centered sequence, arcades, narrow-to-open spatial hierarchy, water/garden relationship | Fine repeated slender bays, warm plaster/stone and selective patterned tiles, proportioned pools | Treating all Islamic architecture as identical domes; excessive tiny ornament at unreadable scale |
| Ottoman monumental | Central dome and subordinate volumes, surrounding courtyard/service buildings as appropriate | Layered drum, supporting arches and piers, thin towers only at suitable scale, courtyard axis | Solid dome with no interior; random detached towers |
| Japanese minka / machiya | Timber frame and coherent bays, carefully shaped roof, low-scale domestic spaces; urban forms may be narrow and deep | Wood posts, restrained light infill, screens represented with compatible block shapes, deep eaves, hearth/entry/floor transition | Red pagoda details on every home; huge decorative roof hiding a box |
| Chinese courtyard dwelling / hall | Court-and-wing sequence, coherent bay count, roof hierarchy and entrance progression | Primary beam/posts, modest layered brackets at scale, disciplined roof curvature, roof tiles represented by related blocks | Every roof has exaggerated upturned tips; missing circulation between wings |
| Fujian tulou-inspired communal architecture | Enclosed large ring or other coherent plan around a real courtyard, repeated residential bays | Earthen outer wall family, inner galleries and access stairs, repeatable room modules | One hollow ring with no entrances, internal floors or rooms |
| Khmer temple complex | Layered terraces, gates, galleries, tower hierarchy and spatial axis tied to site | Warm masonry, stepped profiles, repeated galleries; focal tower with articulated base and roof/tier silhouette | Single generic pyramid covered in random moss; no real routes through complex |
| Andean terrace / masonry | Site-led stepped platforms, retaining stone and routes adapted to relief | Coherent local stone, purposeful irregular course treatment, stairs and drainage cues, avoid artificial flat pad | Building on massive unsupported cliffs; identical stepped triangles for every function |
| Great Zimbabwe-inspired drystone | Curved monumental walling or enclosures, meaningful passages and relationship to hill terrain | Local stone family, broad continuous walls, selected geometric bands, resolved gateways | Copying European turrets or fantasy battlements onto unrelated stone traditions |
| Industrial brick / iron | Production bay and machinery layout, trusses, repeated large windows, loading routes and chimney where justified | Brick, dark steel/iron-like elements if available, glass, structural columns, service stairs, distinct plant zones | Random gears, smokestacks and copper without process or access |
| Modernist / Bauhaus-inspired | Function-led volumes, horizontal/vertical relationships, deliberate flat surfaces and openings | Concrete/plaster/glass palette, clean transitions, structural grid and thin controlled slabs | Automatically classifying flat-roof modern buildings as bad; empty glass cube with no actual program |
| Art Deco / early high-rise | Tiered massing, vertical piers, repeated windows, restrained geometric entrance motifs | Stepped setbacks, coordinated light/dark stone or brick bands, readable base and crown | Patterning all faces equally, fake windows not connected to floor geometry |
| Brutalist / infrastructural concrete | Legible solid mass, recesses, service circulation and deliberate material expression | Muted concrete-like variants, deep shadow reveals, robust stair cores and repeated forms | Random cobblestone noise on smooth concrete; no entrances or habitable volumes |
| Fantasy / supernatural / futuristic | Deliberate rules of the setting, construction logic adapted to non-real technologies | Stable shape grammar, visual support or explicit intentional levitation, meaningful material/energy nodes | Using fantasy as excuse for missing floors, inaccessible interiors or bad collision |

Historical examples are not recipes to reproduce exact monuments. For an unfamiliar culture, extend the matrix with primary references rather than reuse a generic European house. Sources: [Met Gothic art](https://www.metmuseum.org/es/essays/gothic-art), [Met Renaissance architecture](https://www.metmuseum.org/essays/architecture-in-renaissance-italy), [Historic England traditional farmstead character](https://historicengland.org.uk/images-books/publications/national-farmsteads-character-statement/national-farmsteads-character-statement/), [Urnes](https://whc.unesco.org/en/list/58), [Kizhi](https://whc.unesco.org/en/list/544/), [Yazd](https://whc.unesco.org/en/list/1544/), [Alhambra](https://whc.unesco.org/en/list/314/), [Selimiye](https://whc.unesco.org/en/list/1366), [Japanese architectural museum](https://www.japan.travel/en/japans-local-treasures/fukushima-city-minkaen-open-air-architectural-museum-2023/), [Fujian Tulou](https://whc.unesco.org/en/list/1113), [Angkor](https://whc.unesco.org/en/list/668), [Andean road system](https://whc.unesco.org/en/list/1459/), [Great Zimbabwe](https://whc.unesco.org/en/list/364), [Bauhaus](https://whc.unesco.org/en/list/729). Site listings and essays are starting references; they are not complete measured plans or permission to redistribute imagery/templates.

### 21.3 Architecture family variant test

For each new historical family, compare two actual variants at equivalent scale: silhouette alone, materials without furniture, section/interior and completed normal-lit view. A recolor of the same generic roof and plan is not a new architectural family. A small vernacular building may use simplified elements from the matrix; do not force every historic characteristic into a 5-block-wide footprint. If region/material substitution changes roof, window or support grammar, review the new result rather than treating it as a color-only variant. Keep cultures and periods data-driven; do not encode all possible architectural styles as a rigid Java enum.

## 22. Complex geometry — discretization, structural transitions and curved forms

### 22.1 Geometry rules shared by all shapes

Use integer local coordinates, an explicit origin and fixed orientation. World positions of blocks are integer indices; if calculating continuous geometry, define cell centers consistently (e.g. index+0.5). For symmetric odd-width footprints use center of a cell; for even width use boundary between cells. Do not mix conventions in the same piece. Generate the positive half of symmetric geometry, mirror by the same convention, and verify center joins. Avoid independently rounding opposite quadrants. Keep outer geometry, interior cavity and structural thickness distinct.

For circular and elliptical footprints, construct a filled outer mask using normalized squared distance to the chosen center, then subtract an inner mask with smaller radii for the cavity. A ring must have continuous usable block faces where closure or collision matters; diagonally touching cells alone do not form a watertight or walkable wall. For tiny radii an octagonal plan often reads better than a mathematically exact circle. Use horizontal rings at each height for tapered roofs and domes; review consecutive rings, interior headroom, supporting drum, and cap closure. Smooth the silhouettes by controlling changes in integer run lengths; don't use arbitrary random offsets to disguise voxel stair steps.

Use a geometry tool such as [WorldEdit shape generation](https://worldedit.enginehub.org/en/latest/usage/generation/) as authoring assistance. A generated sphere is a starting volume, not architecture. Prefer an explicit voxel mask or reviewed authored piece for difficult curves. Never assume a native structure rotation supports 45 degrees: standard block-grid transforms are cardinal rotations/mirrors; diagonal buildings need purpose-authored geometry or an explicitly tested rasterization process with regenerated states.

### 22.2 Exact plan examples — patterns, not ready-made NBT

Local plan symbols: # solid wall/structure, . open interior, D intended opening, blank outside. All patterns are original illustrative drafts; thickness, stairs, collisions and material properties still need active-target verification.

A compact 9x9 octagonal tower outline (north at top, entrance south):

    ..#####..
    .#.....#.
    #.......#
    #.......#
    #.......#
    #.......#
    #.......#
    .#.....#.
    ..##D##..

Here the outer dots in corner positions are outside; enclosed dots are interior. The diagonal corner transitions need extra wall cells or suitable actual corner geometry where four-neighbor continuity, fluid sealing or access requires it. Use a full-height continuous bearing wall and inset upper tiers rather than shrinking each floor to zero usable space. Place stair core or wall-adjacent stair before windows.

A robust discrete circular ring algorithm for odd diameter 11:

    center = (5,5)
    outer = all cells with (x-5)^2+(z-5)^2 <= 25
    inner = all cells with (x-5)^2+(z-5)^2 <= 9
    wall = outer - inner

This produces a relatively thick ring, not an optimal room plan. Adjust radii and prove cavity size; inspect thickness, face continuity and openings. Doorway carving is a separate operation with a lintel or arch. Hollowing must not delete structural piers. For a thinner decorative ring, use a contour mask with repaired diagonal corner transitions. Preserve the same center on every level.

Diagonal wall from (0,0) to (6,3): use a consistent 2:1 rasterized step rhythm, for example segments of two cells in X then one in Z with overlapping corner cell/face connectivity. A 45-degree diagonal requires both axes and deliberately resolved corners. Extrude the footprint vertically; thickness and interior offsets must be planned before wall opening placement. Windows should be placed on meaningful straight segments or specialized corner bays; do not rotate a one-block door 45 degrees into non-existent state. For diagonal roads use connected walkable width, not a line of isolated diagonally touching slabs. Test actual player traversal and corner collision.

### 22.3 Arches, vaults, domes and roof transitions

| Form | Construction method | Essential checks |
| --- | --- | --- |
| Lintel | Span opening with structural material seated onto jambs; trim below with slabs/inverted stairs as needed | Actual clear opening and supports on both ends |
| Round arch | Establish spring line, draw symmetric stepped semicircle above it, thicken arch ring and resolve at piers | Continuous readable ring, closure at crown, usable opening |
| Segmental arch | Broad low rise across opening with deeper abutments; use stepped slabs/stairs at small scale | Not a flat unsupported row masquerading as arch |
| Pointed arch | Two symmetric curves/stepped slopes converge at higher center | Point reads as single crown; no unclosed top or random tracery |
| Corbel arch | Successive inward-projected courses until cap, distinct from true curved arch | Each course visually overlaps support below |
| Barrel vault | Extrude repeated arch cross section along nave/corridor | Continuous thickness and wall supports, no protruding beams into path |
| Groin vault | Intersect two perpendicular barrel-vault cavities inside a common supported bay | Four corner piers, no holes in roof at intersections |
| Rib vault | Deliberate ribs along diagonal and transverse lines, webbing between them | Rib paths meet supports and crown; interior geometry readable |
| Dome | Concentric narrowing rings on a supporting drum; use hemisphere/ellipsoid mask or authored profile | Closed crown, interior cavity, transition from square to round, stable supports |
| Onion / bulbous dome | Authored profile swells above drum then narrows toward cap; visible continuous curves | Not a plain pyramid or oversized sphere; proportions suit actual style |
| Conical/spire roof | Narrowing profile with consistent slope, tiered height rhythm, capped peak | Roof remains attached, no open seams, base matches tower footprint |
| Compound roof | Explicit intersection masks for ridges/valleys, gable closures and dormers | No duplicate material layers blocking rooms, no holes at joins |

Example Minecraft arch drafts, frontal 7-column cross-sections, top row first. # = structural masonry, o = opening; all cells are part of the planned bay, not complete building. In a usable room add wall thickness, real jambs and depth. Stairs/slabs may soften the step corners, never reduce required collision clearance.

    Round-headed approximation   Pointed-head approximation
    #######                     #######
    ##ooo##                     ###o###
    #ooooo#                     ##ooo##
    #ooooo#                     #ooooo#
    #ooooo#                     #ooooo#
    #ooooo#                     #ooooo#

These are small illustrative cutouts, not true engineering arch profiles. Increase width/height and use a contrasting continuous ring when the arch itself must be visible. Check stair facing and top/bottom orientation with a single target-version mock-up. A full-block lintel is often better than a malformed three-block pseudo-arch.

Dome workflow: reserve a 13x13 footprint, choose a circular/octagonal drum with adequate thickness, establish a section drawing and build rings using a common center. Example design radii by local height (illustrative, not exact hemisphere): 6, 6, 5, 5, 4, 3, 2, 1, then cap. Outer radius minus an intended thickness defines cavity; inspect the actual voxel result for gaps between layers. Transition square room to circular support with corner arches or bracketed/squinch-like masses, not a detached ring on four isolated posts. For a large interior choose an explicit ceiling/rib structure and daylight openings. Do not add a dome to a building whose plan or role cannot support it.

### 22.4 Multi-level circulation and complex stairs

Straight stair: reserve run, start and landing; test two-block standing clearance and actual stair shape throughout. L-shaped: reserve turn landing, wall/rail and clearance around corner. U-shaped: two runs with common landing, don't place low ceiling above bottom run. Switchback routes on cliffs use bounded terraces and retaining edges. Spiral/helix: reserve a central column or open core, an outer walkable ring, landings aligned with floors, and enough rise per revolution to prevent head collision. For a small tower use a safe ladder or straight switchback instead of an impossible miniature spiral. On a generated spiral, assert real reachable floor landings, not simply presence of stairs. Avoid one-block drops as the only route back unless intentional gameplay. Custom movement/size/trauma systems may require different accessible routes; use actual capabilities rather than assume vanilla conditions.

### 22.5 Geometry verification

Inspect sections along both axes, roof from above and the underside. Check all intended closed surfaces with collision/face semantics, support contacts, diagonal corner continuity, route clearance, transform symmetry, sealed versus intentionally open cavities, and chunk-edge assembly. Test even and odd dimensions if both supported, min/max radius, shortest stair run, alternate rotation, differing connector height and a rejected fit. Do not silently clamp a complex footprint into a smaller unsuitable box or use half-block geometry where an actual needed shape is unavailable.

## 23. Engineering and industrial structures — function before decoration

An engineered structure is defined by a process, connection or service, not by its roof. Begin with a graph of what moves through it: people, water, cargo, ore, power, animals or vehicles. A structure can be purely decorative if that is the declared feature, but its visible routes still need plausible starts/ends. Real operation is claimed only when the actual game/mod API and test support it. Never introduce real physics in an architecture addon merely to imitate a reference; use authoritative domain providers in an ecosystem.

### 23.1 Infrastructure design matrix

| Type | Site / section / composition | Content and endpoints | Required specific QA |
| --- | --- | --- | --- |
| Stone arch bridge | Stable banks, graded approach, deck datum, repeated piers/arches with coherent spans | Abutments, parapet, drainage cue, continuous deck | Both ends reachable, clear passage beneath as designed, no unintended water dam |
| Timber trestle | Deep or uneven valley, bent frames linked by deck | Visible bracing, anchored legs, deck rails where appropriate | No leg over void unless intentionally spanned; seams and footing |
| Suspension-inspired bridge | Anchor masses at banks, towers, cable silhouette and hanging deck | Continuous deck, anchor/cable connections; decorative cables if no physics | No detached cables/anchors, accessible deck; do not claim real tension simulation |
| Viaduct | Route elevation across multiple valleys, repeating bays adapted at ends | Pier alignment, carriageway/rail, service approach | Preserve clearance and underlying channels; bounded length and pier depths |
| Aqueduct | Defined source and destination, planned channel grade across terrain | Water trough, supports/arches, channel transitions, optional settling basin | Flow only if implemented and observed; no unwanted flooding, destination meaningful |
| Canal | Continuous navigable or service-water route at deliberate levels | Banks/retaining, crossing bridges, towpath and terminal | Continuous route, water containment, avoid accidental river obstruction |
| Lock | Two water levels and chamber between gates | Gate pairs, service platform, approach; stateful transfer only if implemented | Correct level transitions and no fluid leak/duplication; boat path if claimed |
| Dam / weir | Valley cross-section, explicit upstream/downstream context | Abutment contact, crest, spillway/outlet cue, maintenance route | No surprise mass flooding or chunk cascade; decorative vs real flow separate |
| Water tower / cistern | Storage above served area or protected underground volume | Tank, access, inlet/outlet and supports, safe service path | Stable attachment, actual containment; pressure behavior only via supported API |
| Well / qanat-inspired waterwork | Water table/geology or historical context, shaft/covered route | Rim, cover or lifting cue, safe access, collection area | No unsupported claim of renewable water, no unplanned lethal drop |
| Watermill | Existing suitable watercourse and wheel clearance, floor height tied to channel | Inlet/millrace, wheel/axle, work floor, storage, outlet | Water preserved, wheel rotation/production only when implemented |
| Windmill | Exposed suitable site, tower/body support, reserved sail envelope | Hub, sail/shaft cue, mill room, access and output storage | Sail clearance and support, no imaginary moving collision |
| Mine headframe / hoist | Shaft aligned to actual ore-work levels and surface logistics | Supported frame, winding/rope cue, reachable shaft access and loading | Continuous shaft travel, no entrance into void, no duplicate ore |
| Smeltery / kiln / foundry | Production floor, source storage, hot zone and exhaust | Furnace/kiln, charge/input, fuel, slag/output, safe circulation | Actual heat/fire/processing only as supported; no unintended burning |
| Factory / workshop | Material path from loading to processing to finished stock | Repeating work bays, machinery footprints, power/service route, human access | Machine clearance, actual IO if implemented, no sealed production area |
| Railway station | Rail alignment before building, platforms at appropriate height | Approach, station hall, platform, siding/yard where relevant | Rails connect and update correctly; player/vehicle clearances |
| Lighthouse | Prominent navigational site with real sight toward water | Stable tower, stair or ladder, lantern room, service quarters if role warrants | Reachable light level and actual light/emission if claimed |
| Shipyard / dry dock | Shoreline geometry, slip or basin, access to water | Work sheds, crane cue, storage, continuous loading paths | Fit vessel only if required; no disconnected slip or accidental flooding |
| Harbor / breakwater | Coast shape, prevailing exposure context and access | Piers, protected water area, quays, stores, land connection | Navigation corridors preserved; bounds and water contacts |
| Road / pass / retaining route | Origin/destination, terrain grade, controlled crossing | Steps/ramps, walls, drainage, rest/waypoint if appropriate | Continuous route and no impossible climbs; no huge terraforming footprint |
| Underground city / service complex | Stable excavated volumes, vertical circulation, surface connections | Habitation, water/service rooms, work zones and ventilation cues | All required rooms reachable; preserve natural voids outside designated envelope |

Reference cases: [Pont du Gard](https://whc.unesco.org/en/list/344) for multi-level water crossing; [Kinderdijk](https://whc.unesco.org/en/list/818/) for water management as a network of connected assets; [Ironbridge Gorge](https://whc.unesco.org/en/list/371/) and [Røros](https://whc.unesco.org/en/list/55/) for industrial context; [Historic England industrial buildings](https://historicengland.org.uk/images-books/publications/dlsg-industrial/) for typologies. Adapt visual and spatial logic; do not infer exact engineering calculations from heritage descriptions.

### 23.2 Linear structure generation

Plan both endpoints before committing any permanent blocks. Sample route as a bounded graph of positions, not an unlimited search for the opposite bank. Candidate edge cost may combine grade, local earthwork, length, fluid crossing, existing protected volume and biome constraints. Hard constraints reject outside world height, untraversable geometry and unacceptable footprint. Record maximum search length, sampling density and retry count in the family design; don't run global pathfinding in a chunk callback. For long routes, use a predetermined bounded segment graph or authored regional sites; if no legal connection exists, skip or intentionally terminate at an authored endpoint, not a cliff.

Separate rigid spans from adaptable approaches. Example bridge: select stable west/east landings; fit deck datum and approach stairs; choose supported span family; plan each support against actual ground/water depth; reserve rails, eaves and clearances; validate crossing and channel; only then emit pieces. A repeated pier does not have to occur at an arbitrary fixed interval if one lands in a deep void; choose an alternate span family or reject. Verify each segment seam, approach, support state and path after clipping/reload.

### 23.3 Water, fluids and machinery

A visual channel is not a hydraulic model. Do not create a vanilla water source inside an incompatible custom-water system simply to make a filled trough. Resolve the real fluid provider, valid waterlogging behavior, water levels, allowed containment, aquifer/cave interactions and block updates. For WorldLogic, hydrology and physical materials remain authoritative in Core; water organisms/other systems remain owned by their providers. If a real flow/sluice/pump interface is missing, record the missing contract; decorative architecture may remain clearly decorative. Do not introduce a new water engine or force arbitrary chunks to load. A static channel intended to carry water must still have coherent source, direction/grade, joins and outlet. A true operating network requires actual positive and negative gameplay assertions for transfer, shutoff, persistence and nonduplication.

Machinery decoration: a wheel should connect to an axle, an axle to a real work bay; belts, gears and shafts should have coherent directions and not intersect a wall or human route. Reserve animated envelope and operating clearance; inspect collision and state transitions where real mechanics exist. Avoid adding industrial props solely to increase detail count. Distinguish human access, material flow, maintenance and hazardous zones. For a historical workshop, show the input, work and output in the room arrangement rather than insert modern machine objects without reason.

## 24. Castles, fortifications and monumental complexes

This chapter concerns game architecture, historical visual typology and accessible exploration, not real-world military engineering or tactical optimization. Select the historical/fantasy context and the role of the complex: residence, watchtower, fortified town, monastery, citadel, ruin or ceremonial site. Most structures have service needs as well as a strong silhouette. A castle is a collection of buildings and spaces with different functions; not a single rectangular arena with towers pasted to corners.

### 24.1 Typology and form library

| Type | Spatial organization | Readable architecture and furnishing |
| --- | --- | --- |
| Hillfort / earthwork enclosure | Topography-led enclosure and entrance, internal open space | Earthen bank/stone facing, palisade where appropriate, footpath and storage huts |
| Motte-and-bailey inspired | Raised tower/keep site and adjacent lower enclosure | Deliberate mound blended into relief, stair/approach, working yard and residence |
| Stone keep / donjon | Dominant central tower, real levels and vertical circulation | Thick base, differentiated hall and storage/rooms, roof or parapet, coherent openings |
| Curtain-wall castle | Courtyard, perimeter and attached halls/towers | Wall walk if intended, corner/interval towers integrated with real wall junctions, gatehouse, service zones |
| Gatehouse / barbican-inspired | Distinct threshold and passage to enclosure | Clear entry sequence, supported upper mass, doors/gates that fit the actual opening |
| Bastioned / star-shaped historic fort | Geometric polygon or angled outer silhouette, coherent interior yard | Earthen/masonry ramparts, angled projections, period-appropriate materials, service buildings |
| Fortified town | Urban fabric with perimeter and multiple relevant entrances | Wall follows land/streets, gates connect actual roads, inhabited districts and civic anchor |
| Cliff / island castle | Natural site determines footprint, separate levels and approach | Grounded terraces, foundations on actual bearing surfaces, reachable bridge/steps |
| Monastery / palace complex | Sequence of courts, halls, service and quiet zones | Primary ceremonial mass, secondary cloister/service volumes, meaningful room program |
| Ruined stronghold | Surviving wall segments, former internal routes and collapsed areas | Broken but legible outline, surviving supports, evidence of earlier rooms and use |

For historical appearance study [NPS Castillo de San Marcos](https://www.nps.gov/casa/learn/historyculture/construction.htm) and [NPS cultural landscape overview](https://www.nps.gov/articles/000/castillo-de-san-marcos-and-fort-matanzas-national-monuments-550167-550153.htm); use morphology and materials only. Do not turn the architectural brief into real tactical placement guidance. Fantasy walls may exaggerate height and scale when the visual rules are coherent.

### 24.2 Exterior and interior program

First design the ground graph (approach -> threshold -> yard -> main hall and service spaces). Then the vertical graph (ground -> tower stairs -> upper rooms/wall walk where intended). Choose primary silhouette (keep, tower cluster or hall), secondary roofs, perimeter and landscape. Walls need convincing thickness/height and coherent caps; battlements or parapets follow an intentional rhythm. Towers must join walls at compatible elevations; do not generate open gaps where the pieces meet. Give towers actual floor structure, stairs/ladders, windows and rooms or a documented inaccessible solid mass. Large halls need a program: gathering, dining, court or ceremony; kitchens, storage, workshops, stables, guard or living rooms as appropriate. Do not put luxury bedrooms in every tower. An occupied complex needs light and routes to ordinary activities; an unoccupied ruin may be partially inaccessible.

Moat, drawbridge, gate and barriers are optional architectural/gameplay features, not universal requirements. A moat must not drain or flood unrelated natural water. An animated gate/bridge needs actual shape, movement, collision and persistence tests; a static gate must look and behave consistently. Review all approaches, enclosed spaces, roof/terraces and visible wall walks. A monumental open courtyard is a functional void, not missing interior.

### 24.3 Large structure modules

Reserve distinct categories: exterior shell/wall; corner/tower junction; gate/entrance; circulation core; hall; service wing; courtyard; terrain adapter; terminal/cap. Each has explicit connection height, opening dimensions, transformed clearance, shared wall ownership and internal program. Choose a real assembly graph first, then module variants. Do not blindly chain towers until the maximum depth. Keep the primary focal mass recognizable and reserve secondary modules lower or quieter. Verify each intended door/stair route and whole enclosure geometry including adjacent chunks. For large complexes use a cutaway and a map of rooms, not only an aerial screenshot.

## 25. Urbanism, settlements and spatial economy

A settlement is a network of meaningful places, not a shuffled pool of houses. Plan why it exists: agriculture, fishing, market crossroads, river crossing, mining, administration, religious center, industrial service, transport or refuge. Population is a design input for capacity and building mix, not necessarily a simulated mechanic. Avoid assuming that a town requires dynamic NPC jobs or economic simulation; if a provider offers them, integrate rather than duplicate. An inhabited street should visibly explain movement and work even if no NPC is present.

### 25.1 Settlement patterns and hierarchy

| Pattern | Spatial grammar | Good use and pitfalls |
| --- | --- | --- |
| Isolated farmstead | Main house, work yard, barn/storage, field access | Buildings clustered by shared activity; not a random lone hut plus empty fences |
| Hamlet | Small group of homes/work structures around route, water or common | Few distinct footprints and shared local palette; avoid miniature city grid |
| Linear roadside village | Main route with actual door/frontage links, yards behind | Respect terrain and curves; avoid entrances facing impassable slope |
| Green/courtyard village | Common open center and connected surrounding buildings | Keep central space useful, not a meaningless square of blocks |
| Crossroads market town | Route junction, market/civic node, side streets and work areas | Distinguish public and residential frontages, connect goods access |
| River or port town | Waterfront work strip, streets rising toward inland areas | Maintain dock/shore connectivity and access to actual water; don't fill all waterfront |
| Hill / terraced settlement | Level bands, stairs/paths, stepped plots and retaining | Respect grade, provide connected entrances, no isolated level platform |
| Walled town | Street network and building plots within coherent boundary | Gates match roads, public spaces and service access; no disconnected district |
| Planned grid | Deliberate regular road hierarchy with blocks and varied parcels | Can be historically appropriate; vary building role/scale, don't make all parcels clones |
| Mining/industrial town | Work site, logistics route, housing and services | Production graph and terrain dictate building arrangement, not arbitrary decor |
| Underground settlement | Nodes of excavated space, main and secondary circulation, shafts | Meaningful entry/service/living zones, safe continuity, local geology and fluids |

Use a hierarchy of trunk route, local street, lane and door approach. A narrow lane may be intentionally tight; a market street needs room for foot traffic and stall/activity zones. Suggested in-game starting widths: 1–2 blocks for a private path, 2–3 for local lanes, 4–6 for public streets when scale supports it. These are design heuristics, not universal real-world dimensions or required widths. Preserve accessibility and vehicle/mod collision requirements. Make streets and plots fit each other; a rotated home must have its entrance aligned to a real street or connected private path. Treat a plaza as an intentional void with boundaries, access, focal feature and adjacent public uses. Make open space look planned or historically evolved, not leftover random gaps.

### 25.2 Plot grammar and staged growth

Design plot shapes from road frontages, site constraints and planned function. Reserve building envelope, eaves, yard, access, shared boundary and service space. A townhouse can share party walls if the assembly defines who owns each wall; a rural dwelling should usually have separate grounds. Do not intersect neighboring roofs or create unusable 1-block alley voids by default. Mix widths, heights, orientations and roles by coherent family. Repeat local materials across multiple properties, with variation in age, wealth, renovation and role. A city skyline needs anchors and quiet background, not every house taller and more ornate than its neighbor.

Design history in phases: initial nucleus (water/road/work), ordinary growth along accessible streets, denser infill, newer outskirts/expansion, repairs or abandonment. This may be a deterministic static design, not actual in-world temporal simulation. Old roads can curve around existing plots; later additions can have a different material family within a coherent palette. Preserve story and intentional access. Do not simulate global settlement growth on each chunk tick unless explicitly authorized.

### 25.3 Settlement generation recipe

1. Select the settlement type, terrain/context, intended approximate scale and activity mix. Bound the total area and resource budget before planning.
2. Establish entrance/external network nodes, public anchor and main route candidates. Resolve terrain grades and water crossings. Reject disconnected or impossible layouts.
3. Build a deterministic graph of routes, open spaces and potential parcels. Allocate a small set of role quotas or constraints based on actual function, not arbitrary identical-house probability. Keep role relationships: storage by work/dock, residential paths to public node, fields accessible from farm.
4. Reserve transformed building envelopes, foundation/contact sites and entrances. Fit reviewed family variants; generate terrain adaptation bounded to each plot and shared route; avoid overlapping cut/fill plans.
5. Connect secondary lanes, yards, bridges, steps and entrances; validate the route graph including actual height transitions. Add coherent landscaping only around the built interface.
6. Generate each piece with stable ownership and seeds; crossing chunk boundaries must not create partial roads, duplicated entities/loot or second houses at shared endpoints. No global mutable generation state.
7. Test a representative smallest/largest supported settlement, each role family, optional plot rejection, street-to-door connectivity, seam and persistence. Audit the distribution across bounded multiple seeds rather than visually reviewing every possible city.

Illustrative 35x45 small settlement diagram (not to scale; W=water, = main road, -= lane, H=home, B=barn, S=smithy, P=public space, F=fields):

    W W W       F F F
    W dock---B--F F F
    W     H  |  H
    ========P========
          H  |  S
             ---H

In actual placement, reserve full widths, terrain level, safe crossings, separate footprints and entrances. Do not treat ASCII topology as proof that player collision works. For a port, the dock is a working node with service access; for a village not tied to water, the public anchor may be a common, well or market. [UN-Habitat public-space assessment](https://unhabitat.org/knowledge/publications/city-wide-public-space-assessment-toolkit-a-guide-to-community-led-digital-inventory-and-assessment) supplies a useful network/accessibility lens, not a mandate to impose modern urban planning on historical worlds.

## 26. Archaeology, aging, ruins and buried architecture

Design the intact predecessor before generating a ruin. Without an original plan, damage becomes random holes. Define cause, history, material durability, climate/exposure and present state. A single ruin may combine construction phases, repair, abandonment, local collapse, burial, reuse and vegetation. Never use a simple uniform independent per-block deletion probability for inhabited architecture. Structural survival, debris and access are coupled decisions.

### 26.1 Material and history model

Represent a building as regions/roles: foundation, bearing wall/post, nonbearing infill, floor, roof, opening, furniture, decoration, contents, terrain interface. Define a finite ordered sequence of modifications to the original plan. Example: original timber hall on masonry plinth -> service extension in later brick -> roof failure over eastern bay -> localized floor decay and rubble -> partial soil accumulation near wall -> later temporary shelter in surviving west bay. This history can generate one static result. Record intended inaccessible areas and preserve the gameplay route, not every old doorway.

| Cause / phase | Spatial pattern | Minecraft representation |
| --- | --- | --- |
| Repaired wall | Local patch crossing earlier joints, aligned with actual wall | Related but distinct material family or finish; no independent speckling |
| Roof decay | Region of roof loses closure; exposed room below ages differently | Broken rafters/eaves, collapsed debris beneath, light/water exposure |
| Timber decay | Deterioration concentrated near damp contacts/exposed ends or abandoned spans | Missing local members only if surviving structure still plausible; broken floor region below |
| Masonry collapse | Local wall segment or arch failure with recognizable surviving returns | Uneven connected broken silhouette, rubble at base, remaining supports |
| Fire aftermath (fictional static design) | Damage differs by material and exposure | Charred/damaged family where available, loss of light elements, coherent ash/debris cues; no uncontrolled ongoing fire |
| Flood/sediment | Lower areas and flow/deposition zones | Silt/earth at actual low elevations, damaged finish, submerged lower space as intended |
| Frost/weather | Local exposed edges and moisture paths | Restrained cracked material and worn trim; intact sheltered faces |
| Vegetation reclaim | Open roof, moist crevices, courtyard or ground contact | Local moss/vines and natural growth if permitted; not random plants inside sealed rooms |
| Salvage/reuse | Valuable removable fixtures missing; retained useful masonry | Empty socket, removed door/window, new patch or secondary shelter |
| Burial | Site-specific sediment slope or deliberate covered level | Upper elements partly visible, excavation exposure/approach, ground layers consistent with local geology |

These are aesthetic/environmental heuristics, not validated failure predictions. For a world with active material-physics simulation, use the actual material properties and assess whether generated remnants will immediately fail. Don't introduce uncontrolled dynamic collapse merely to produce static ruin variants.

### 26.2 Generate coherent damage masks

Assign regions before damage. Choose a bounded cluster (e.g. two adjacent roof bays) rather than individually removing random blocks. Mark which roof/floor/partition elements disappear; recompute required structural supports and intended access. Preserve surviving visible wall shape and avoid completely detached unsupported groups. For each missing overhead mass, place plausible bounded debris below if appropriate, allowing for removal/salvage history. Debris occupies actual volume, avoids blocking mandatory paths and does not randomly fill chests. Distinguish deliberate rubble on a floor from actual gravity blocks that might cascade into caves. Check resulting states after update; if the intended remnant cannot survive with existing physics, revise template or use stable material justified by design.

A ruin with 10% missing blocks can be more convincing than one with 60% random deletion. Do not set damage quotas as artistic acceptance. For progressive variants define readable stages: maintained, weathered, abandoned but intact, partially collapsed, archaeological remnant. The same base building should remain identifiable across stages, while rooms and accessible volume change coherently. A remote ruin may have one meaningful accessible chamber and no complete roof; do not enforce domestic furnishing requirements on a fully collapsed foundation remnant.

### 26.3 Buried remains and archaeological layers

Choose a former ground level and later deposit/terrain profile. Model construction cuts, foundations, floor surfaces, destruction debris and later fill as ordered regions, not globally replacing the entire chunk with layers. A foundation is below the former floor and supporting walls; occupation finds belong near relevant contexts; collapse rubble can overlie the old floor; later soil can cover earlier remains. Avoid placing all rare loot at one arbitrary vertical Y. Use meaningful find contexts (storage, activity area, collapse debris, earlier occupation) and actual loot/archaeology mechanics where available. Do not overwrite ore, caves or existing structures outside the authorized built site. Decorative burial must not spawn isolated empty rooms surrounded by unbounded air excavation; intentionally buried inaccessible remains may require discovery via an explicitly designed entrance or digging mechanic.

Reference [Historic England building recording guidance](https://historicengland.org.uk/images-books/publications/understanding-historic-buildings/) and [recording historic buildings and landscapes](https://historicengland.org.uk/advice/technical-advice/recording-heritage/recording-buildings/) for understanding phases and relationships. The design synthesis here borrows interpretive sequence, not real archaeological reporting requirements.

### 26.4 Archaeology QA

Compare intact and damaged versions in section and actual placed states. Verify deliberate missing regions, surviving foundation/structure, debris location and collision, coherent old/new material boundaries, burial envelope, intended routes, loot context, no unsupported floating groups, no update cascade, no fluid intrusion outside design and save/reload. A ruin should tell a legible story before a player reads an explanatory book. Do not require artificial repair of intentional damage to satisfy generic roof or furnishing rules.

## 27. Practical Minecraft block construction library

This is a recipe atlas of architectural details. Use semantic palette roles, not hardcoded assumptions about modded block families. Block shapes listed are vocabulary; inspect actual target-version states and support. Recipes are original starting patterns, not finished model assets, in-game screenshots or proof of working interactions. At least one real block mock-up is required for a novel connection family before replicating it. Common block art applies only when new texture/model assets are actually created.

### 27.1 Architectural elements and their assembly order

| Element | Assemble in this order | Scale / verification |
| --- | --- | --- |
| Timber corner | Stone footing -> vertical stripped/log post -> horizontal beam ending at post -> inset wall -> eave bracket if needed | Axis of each log, solid contact, all three visible faces |
| Timber frame bay | Two posts -> sill and top plate -> infill -> window opening/lintel -> local brace | Repeat compatible bay widths; no brace cutting access |
| Jetty upper story | Lower wall/posts -> projecting joist/beam -> bracket or corbel underside -> upper floor edge -> upper wall/trim | Footprint projection usually 1 block at small scale, resolve end corners |
| Masonry plinth | Footing -> robust base courses -> projecting cap/slab -> narrower wall above | Actual terrain contact at each step; no unsupported slab lip |
| Quoin corner | Continuous masonry core -> alternating coherent larger corner accents by course | Don't expose checkerboard at corner or change every cell |
| Column | Base course (full block/slab/stair ring) -> narrower shaft (wall/pillar or full block at scale) -> capital -> supported entablature | Real floor support and beam contact; at small scale simplify |
| Pilaster | Wall plane -> shallow attached vertical band -> base/capital aligned with facade | Not an isolated post blocking sidewalk |
| Window reveal | Structural opening -> inset pane/glass -> sill -> lintel -> optional shutters | Clear interior relationship and actual orientation |
| Paired window | Two equal openings -> shared pier/mullion -> continuous sill/lintel where appropriate | Preserve central support and matching floors |
| Arched portal | Jambs on base -> ring/stepped arch -> deeper recess -> door/gate inside | Usable opening, support and actual two-part state |
| Balcony | Beam/corbel attached to wall -> deck -> rail -> entrance -> optional canopy | No floating fence platform; path from room |
| Eave | Roof slope -> overhang -> stair/slab underside/trim -> end return | Correct corner orientation; no random tooth row |
| Cornice | Main wall -> narrow projecting slab -> optional deeper profile -> upper termination | Clear shadow line, consistent corners; not layers for their own sake |
| Pediment | Supported horizontal entablature -> triangular upper infill -> stepped edge/cap | Roof joins real gable; no detached triangle |
| Buttress | Grounded pier -> narrowing stepped upper courses -> cap/connection to wall | Width and height appropriate to supported mass |
| Balustrade | Continuous landing/deck -> connected walls/fences or authored rail posts -> terminal | Safe paths; rail gaps only intentional |
| Chimney | Hearth zone -> vertical route through floors -> roof penetration -> upper shaft/cap | Material/fire behavior; no obstruction of stair or room |
| Dormer | Reserve roof opening -> side returns -> actual window -> little roof intersecting main slope | Interior headroom, no leaking unclosed roof intersection |
| Parapet | Continuous supported roof edge -> wall/rail profile -> corners/access | A real roof floor behind it, not fake floating wall |
| Boarded window / ruin | Original opening -> attached uneven repair boards or masonry infill -> local wear | Mark intentional blocked opening, never silently block route |
| Retaining wall | Footing -> land-facing retaining mass -> stepped cap -> coherent end returns | Terrain on intended side, no wall suspended over air |

### 27.2 Exact coordinate mock-ups

Examples use local XYZ, y up, x across, z depth. All ranges inclusive; full blocks are role placeholders. An orientation is stated as intent because raw facing names and placement behavior must be confirmed from actual target block states. These are small mock-up instructions, not a universal executable palette or a finished house.

**T01 — 5x5 timber bay with recessed opening.** Build a vertical stone plinth at y=0, x=0..4, z=0 with support underneath. Put posts x=0,4 at y=1..4,z=0; horizontal beam x=1..3,y=4,z=0 with X axis. Infill panel x=1..3,y=1..3,z=1, leaving window x=2,y=2..3,z=1. Place pane in opening only if connected and intended; use slab sill at x=2,y=1,z=0 where it does not conflict with plinth or path. Optional lintel is beam above, not a second unrelated stripe. Add side returns in actual building. Review front and side: post face should project one block ahead of infill; no visible gap at bottom or top. Use local stone and wood roles. Don't make plinth support depend on this front-only mock-up.

**T02 — recessed masonry window, one-block clear width.** Construct a wall thickness of two cells z=0..1, x=0..4,y=0..4 with actual ground support. Omit x=2,y=2..3 across both depths. Set pane at x=2,y=2..3,z=1 (or actual suitable depth), leaving front recess. At x=2,y=1,z=0 use intended sill profile in the already occupied wall course, not a second overlapping block. Add coherent lintel across x=1..3,y=4,z=0 and matching rear support as needed. Place adjacent stairs only if they don't close desired light/opening. Confirm pane attaches and room-side opening is meaningful.

**T03 — porch beam and supported canopy.** On a 5x3 ground-contact porch x=0..4,z=0..2, reserve entrance centered at x=2 toward house rear z=2. Put ground-supported posts x=0,4 at front z=0, y=1..3 relative to porch floor y=0. Beam x=1..3,y=3,z=0; canopy spans between front beam and real house wall/support at z=2, with appropriate eaves and slope. Interior of porch must have clear height and unobstructed door. Add only side railing where wanted, never a fence across approach. Inspect connection at house, post bases and rain-shedding profile.

**T04 — continuous one-block stair run.** Floor start at y=0; reserve positions x=0..3 at z=0 for four ascending stairs whose collision surfaces rise in one-block increments; connect to upper landing at matching final surface. Select stair facing so walking +X ascends. Keep at least two blocks of standing clearance above actual walking surface along entire run and landings; upper floor opening covers stair envelope. Supports beneath if visible/appropriate. Test walking both directions, ceiling and any railing. Do not infer success from stair count.

**T05 — roof eave mock-up.** Place a supported 5-block long wall and three repeating cross-sections of selected roof slope. Test roof stairs in their intended rise direction, bottom/top state and matching trim. Add one terminal return at the end and one internal/external corner if used in the full design. View from underneath and roof top after neighbor updates. Confirm no holes at gable/edge. Use a corresponding family of blocks/slabs/stairs; if a modded material lacks the necessary shape, change the design/family rather than fill missing shapes with air.

**T06 — support pier on uneven terrain.** Choose a finished deck y and two footing coordinates with different actual ground y. Build each contact pier from suitable ground to beam underside, using coherent material and cap. Beam spans between posts; deck rests above it. For normal small ground-floor buildings a long exposed pier may be inappropriate; reject if support-depth limit exceeded rather than generate a dirt pillar. For an actual stilt/dock design, keep intentional underfloor clearance and test contact or flotation semantics.

**T07 — bookcase / storage wall.** Reserve a 3-block wide back-wall segment with open aisle in front. Use shelf/bookcase/storage blocks along base or wall with coherent heights. A slab shelf may occupy upper half if actual supported objects fit on it. Put lantern on proper support and one usable container with lid/access clearance. Do not create random chest stacks solely for loot. Test player approach, container open and all attachment states. Include custom cabinetry only if registry and collision allow.

**T08 — kitchen/workbench corner.** Reserve 3x2 service zone with accessible front aisle. Place appropriate worktop, actual furnace/hearth or decorative fire-safe object, adjacent fuel/storage and tool/food-preparation surface. Exhaust/vent cue connects above if relevant, not through an upper occupied room without planned route. Avoid using pressure plate/table decor that toggles machinery accidentally. Confirm cooking/processing only when real block mechanics are involved.

**T09 — hanging light.** Choose actual supported ceiling face at y=H, align chain (if intended) below it and lantern below chain, or mount torch/lantern directly using valid target attachment. Reserve walking headroom beneath. Validate support and state after placement and neighbor updates. If chandelier spans several chains, each visible attachment terminates in a real support. A free-floating lamp is allowed only as intentional fantasy with actual supported behavior.

**T10 — bridge span junction.** Use two authored segments with matching deck level, clear width and rail boundaries. Assign a shared seam owner so two pieces do not overlap half slabs or duplicate rail blocks. At seam, inspect centerline, edges and underside. Include actual approach blocks at both terminal modules. Test crossing across chunk boundary and reload; inspect water/terrain beneath.

### 27.3 Interior and exterior prop kits

Organize props as activity clusters, with a collision reservation mask. Suggested role kits (select only appropriate parts):

| Kit | Primary object / supporting props | Placement logic |
| --- | --- | --- |
| Sleep | bed, storage, small light | bed head against suitable wall, accessible side/foot, unobstructed interaction |
| Meal | table, seats, serving/storage surface | circulation around at least intended approach; no decorative trapdoors trapping player |
| Cooking | hearth/furnace, worktop, food/fuel storage, flue | service corner, safe heat behavior, no blocked chest or overhead collision |
| Smithing | anvil, furnace, fuel, input/output storage, quench cue | human workspace beside actual working face, no decorative item in path |
| Writing/library | desk, books, stool, light, shelving | light close to task, shelves on wall, no inaccessible room |
| Bath/wash | vessel, bench, storage/partition, drainage cue | compatible liquid and floor support; no simulated flow claim |
| Stable | pen boundary, gate, trough/feed, bedding | entity-sized clearance and safe access; don't create enclosure too small for intended mob |
| Warehouse | storage groups, pallets/stacking cue, aisle | distinct loading/service side, actual lid/opening and path |
| Dock work | cargo, rope/mooring cue, net/tool storage | leave boat/path clearance, actual pile supports |
| Mine work | rail/support, ore bins, lantern, tools | position at work bay, keep main route usable |
| Ruin | coherent remnant of former kit, debris or removed item | tie to prior room and damage region; no random fully stocked kitchen in total collapse |

Small visual tricks: inverted stairs can communicate counter undersides, slabs can form low shelves/benches, signs/trapdoors can suggest drawer fronts, walls/fences can slim a furniture leg, barrels offer real storage, chains can connect ceiling-hung elements. Do not misuse blocks in a way that causes unpredictable gameplay. Use functional custom furniture when available rather than reproduce a vanilla approximation of an actually provided object. Test the exact variant in hand/world if new models created, but don't rereview unrelated existing assets. Avoid block entity clutter solely for aesthetics and preserve real server performance.

### 27.4 Palette adjacency and joints — sample families

A palette is evaluated across real joints, not swatches alone. For each new family make a 3x3 or larger actual wall/corner, floor threshold and roof transition. Suggested combinations below are role examples, not mandatory hardcoded IDs or visual approval.

| Design | Base / field / structure | Trim / roof / accent | Transition rule |
| --- | --- | --- | --- |
| Temperate timber | local grey stone / muted pale infill / darker oak-like beam | matching wood stairs/slabs / darker coherent roof / subdued shutters | preserve stone-to-wood plinth and beam axes |
| Cold log cabin | local rough stone / horizontally aligned regional timber | related slab/stair eaves / roof suited to role / limited dark iron | stable corner joints and protected entry |
| Pale masonry | local cut light stone / related smooth wall / heavier pier | same-family stair/sill / muted roof / dark openings | contrast at openings, no random bright polished stone flecks |
| Warm earthen courtyard | durable stone base / mud/terracotta-like wall / thicker enclosure | coherent parapet, timber shade elements, restrained colored tile | limit exposed moisture-sensitive material according to actual behavior |
| Urban brick | stone base / coherent brick / pier or pilaster | stone lintel and cornice / restrained metal/roof / timber door | maintain courses, align floor bands |
| Mine / work | actual local rock / regional timber support / reinforced junction | low-key metal, rail and light / storage | contrast support from host rock and leave route |
| Industrial | stone/brick base / brick or concrete / dark structural members | matching windows, trim and roof / controlled metal | material follows load and process zones |
| Coastal | stable masonry or piles / weather-appropriate wood | protected eave/rail / restrained accent | avoid arbitrary weathering below actual waterline |
| Ruin | intact predecessor palette | locally damaged/worn corresponding materials | patch/collapse masks follow history, not uniform noise |

For actual visual review compare blocks in default game lighting by hue, lightness, saturation, pattern scale, edge detail, join continuity and architectural role. An adjacent neutral stone and saturated wood may work if separated by trim; closely matching two unrelated noisy patterns can still look poor. Favor neighboring values within a material family, stronger controlled contrast at silhouette and important openings, small vivid accents at focal points. Do not apply a global palette ratio or numerical score as a pass criterion. Use the new mod's real local building blocks where fitting; if shape variants are missing, specify design-compatible fallback and tell provider where useful.

## 28. Scaling from shelter to megastructure and bounded procedural architecture

Scaling is not multiplying every block coordinate. At larger sizes, human dimensions (doors, stairs, furniture, usable rooms) stay approximately constant while structural bay counts, hierarchy, circulation and service networks expand. A tiny shelter can be single-room with few details; a city requires intentional districts and connections. Avoid megastructure screenshots that conceal an empty building.

| Scale band (illustrative footprint, not limits) | Design unit | Content / review priorities |
| --- | --- | --- |
| Micro (3–7 blocks across) | Single functional object/shelter | Readable silhouette, contact, simple usable opening, one purposeful prop |
| Small (7–15) | One or few rooms | Plan, roof, entry, local material, real furnishing and supports |
| Medium (15–35) | Multiple rooms/floors and bays | Vertical circulation, room hierarchy, coherent facade sections, roof joins |
| Large (35–80) | Wings, courtyards, structural grid | Public/service routes, phased masses, complete interior program, modular seams |
| Very large (80+) | Multiple connected structures/complex | Site and masterplan, volume hierarchy, building-level modules, bounded generation |
| Regional network | Routes and discrete sites | Nodes/endpoints, geography, placement budgets, chunk-safe edges, no global per-chunk search |

Dimensions vary by role. A narrow bridge can be long; a 5-block tower can be tall; a buried chamber can have a small visible footprint. Select budget by actual transformed volume, affected chunks, number of pieces and runtime work, not an arbitrary max width. When a scale exceeds one template's safe format or runtime limits, split into authored modules with explicit bounds, connectors and seam ownership. No hardcoded universal chunk radius or block count. Establish real caps and measure target behavior.

### 28.1 Large design decomposition

Masterplan -> districts/major wings -> building or structural bay -> room and circulation -> detail kit. Set primary focal volumes at masterplan scale, secondary landmarks at neighborhood/wing scale, background rhythm at room/bay scale. Keep long facades coherent with repeated bays and occasional meaningful breaks, not arbitrary thousands of unique details. Plan vertical sections and roof systems before multiplying the number of rooms. Large interiors need accessible functional areas, not necessarily every maintenance cavity accessible. Mark inaccessible decorative mass explicitly; do not label it as a playable floor.

When generating an array of repeated units, vary the correct level: room kit and occupant details within same bay; one facade variant per wing; major volume variation per building. Maintain common grid, door/floor elevations and supporting material roles. For a large building, review distant mass, main approach, multiple interior paths, cross-section through roof/levels and a representative repeated bay plus all meaningful distinct variants. Grian's [MEGA builds case study](https://www.minecraft.net/fi-fi/article/how-do-mega-builds) is useful for differentiating large-scale shape from fine surface detail. A beautiful aerial silhouette does not prove interior or gameplay.

### 28.2 Deterministic grammar, solver and budgets

Use an authored grammar of compatible components. Each component declares dimensions, required anchors, occupied and reserved volume, interface points, floor levels, room/role, palette constraints, structural contact, optional slots and mandatory route endpoints. Reuse an existing mechanism rather than create a new shared framework automatically. Start with a simple template family and extend only when actual requirements justify it.

An implementation sequence:
- Build deterministic candidate graph using stable seed, structure ID, site and component identity. Avoid iteration-order dependence (sort IDs/coordinates where applicable).
- Choose mandatory modules and place fixed anchors. Solve compatible connectors with bounded alternatives; reject conflict using transformed occupied and reserved volume. A constraint failure is not license to clip through another room.
- Resolve route continuity, site/footing envelope, region palette and critical support geometry. Select compatible optional modules only after mandatory plan works.
- Resolve furnisher from room zones, reserve movement/interaction clearance and cap prop attempts. Resolve wear masks from design history if applicable.
- Emit block plan with per-piece ownership, clip writes to permitted generation area. No unbounded recursion, asynchronous nondeterministic shared state, loading distant chunks, or storing all world blocks in memory.
- Validate plan before writing, then actual post-placement behavior and natural generation. A failed placement can skip or choose bounded fallback, not leave half a building. If generation API writes piecemeal, plan full required shape first and honor its supported chunk lifecycle; do not claim transaction/rollback capability that does not exist.

Set per-family limits: candidates inspected, terrain samples, modules/pieces, maximum graph depth, bounding volume, foundation depth, earthwork area, post-update work, block entity count as relevant, and natural frequency. These are measured engineering decisions; no universal limits from this document. Test cap and failure behavior with deliberately impossible site or connector. A giant underground complex may be rarer and have larger authorized bounds than a roadside shelter; don't run giant-complex tests for a shelter.

### 28.3 Chunk seams, persistence and performance

Treat piece transforms and region boundaries as first-class. Generate crossing pieces from stable source data, not from assumptions that neighboring chunk was already decorated. Include eaves, foundations, access and intentional clearing in actual reserved volumes. Verify seam ownership for duplicated blocks, double chests, doors/beds, rails, pipes, lighting, terrain edits, loot and marker replacement. Do not globally force chunk loads to find missing pieces. Test two fresh worlds with same seed and different relevant chunk generation order where practical; compare final block states and intended entities/loot provenance (actual randomness contracts may differ). Reload and inspect no duplicate loot or entities. Negative cases: rejected overlap, incompatible site, world-border/height limit, absent optional material pack, exhausted search, oversized piece, blocked path. Capture actual runtime or sample counts where performance matters, not fabricated universal timing limits.

## 29. Additional specialized construction cases and risk-directed review

Use these as a quick coverage router for edge cases not captured by a typical house. A project may define additional families; never limit supported architecture to the table.

| Special family | Key distinction | Additional focused checks |
| --- | --- | --- |
| Underwater dome/base | Pressure/fluid context, seals, air interior, access tube | Actual fluid compatibility, intentional flooded/dry regions, sealing after updates |
| Stilt village / floating platform | Intentional elevated/float architecture, safe water access | Anchored supports or explicit flotation model, connected walkways, no accidental water obstacles |
| Cliffside monastery / rock-cut hall | Natural cavity/rock as part of built design | Carving restricted to envelope, rock contacts, usable ascent and ledges |
| Cave dwellings | Distinguish natural cave from occupied built spaces | Natural geology outside planned interface preserved; lighting and route |
| Catacombs / crypts | Burial room topology and historical program | Intended accessible route, deliberate sealed chambers, no random isolated room |
| Labyrinth / dungeon | Gameplay topology separate from visual theme | Solvable required routes, intentional locks/hazards, reset/persistence if applicable |
| Observatory | Sight/opening, elevated location, instrument zone | Roof aperture intentional, reachable platform and actual mechanics if any |
| Amphitheater / arena | Open central space, tiered seating, entrances and circulation | Actual stair/seat tier geometry, accessible floor and exits |
| Greenhouse / conservatory | Light, glazing, interior planting/work | Supported glazed spans, actual crop/temperature behavior only if implemented |
| Monument / statue / triumphal arch | Non-habitable symbolic volume; not a residential room | Pedestal/anchor, silhouette at player range, deliberately solid mass exempt from interior |
| Shipwreck / beached built hull | Hull volume, structural remains, coast/water integration | Intentional flooded sections, access and loot, no unsupported detached wreck fragments |
| Airship / magical island buildings | Explicit fictional support premise | Consistent support/magic vocabulary, actual routes and collision; unrelated terrain unchanged |
| Modern high-rise | Structural bay grid, cores, facade floor rhythm | Reachable intended floors, ceiling clearance, no fake floor windows |
| Transit tunnel / station | Linear route, clear envelope, platform or service spaces | Continuous connection, no blocked approach, fluid/rail states as applicable |
| Temporary shelter / campsite | Lightweight construction and activity, minimal rooms | Actual anchoring/ground contact, weather role as supported, no needless monumental detail |

For multi-domain projects, define which subsystem owns building generation, fluid behavior, material physics, fire, crop growth, entities and interactions. For WorldLogic, consult actual public capabilities; keep architecture data and generation in the appropriate owning module. No universal dependency or new central API is created by this handbook.

### 29.1 Review by role, not maximum coverage count

Select required assertions by family and affected risk. A monument can be solid; an inhabited hotel cannot. A ruin can have missing roof; a functional dry warehouse cannot. A floating fantasy island may have deliberately floating mass; ordinary ground house cannot. A bridge does not need beds; a town needs a meaningful road graph; an aqueduct does not need living rooms but does need coherent channel geometry. Decorative machinery must not be reported as operational. In a real feature assignment, explicitly map each claimed behavior to a check, plus intended exceptions. Never lower a test after finding a defect without correcting the design or receiving explicit scope change. Use existing machine-readable scenarios; do not require new universal manifests from unrelated mods.

### 29.2 Review rubric and concrete remediation

For a scoped architectural family evaluate: role readability, proportion/massing, style/material coherence, structural/contact logic, site blend, access and spatial program, appropriate detail, variation, actual runtime behavior. Record specific observed defects and locations rather than numerical artistic grades. Structural blockers cannot be compensated by good looks. Examples:

| Observed problem | Likely root cause | Fix at source |
| --- | --- | --- |
| Houses all look like recolored cubes | Same mass/roof/plan grammar | Author distinct reviewed plans and silhouette hierarchy before decor |
| Looks good from front, empty behind | Facade-only design | Complete sections, rear/service zones and room program |
| Stairs interrupt facade or roof | Unplanned shape states/orientation | Mock up connection, correct actual block-state transform |
| Building floats on one side | Center-only height sampling | Sample contacts, use stepped footing or reject |
| Entire landscape is a square platform | Rigid template with unlimited fill | Bound earthwork, site-aware adapter, alternate footprint |
| Random blocks look noisy | Per-cell high-contrast palette selection | Assign material roles and connected weathering regions |
| Interior full but unusable | Decor afterthought without clearance reservation | Reserve path/functional faces, place kits by room role |
| Village road stops at blank wall | Independent building orientation | Plan road/entrance graph before template fit |
| Mine supports hang in a cave | Rigid tunnel footprint ignoring cavities | Sample actual host/void; adapt, span intentionally or reject |
| Ruin is floating scattered fragments | Independent random deletion | Damage coherent regions, re-evaluate surviving supports and debris |
| Dome looks flat/has holes | Inconsistent ring centers or layer joins | Inspect section, revise profile and closure masks |
| Imported building has missing blocks | Unreviewed material substitution | Map shape/state family, inspect in disposable world |
| Screenshots look good with shaders only | Lighting or geometry hides defect | Review default resources and ordinary lighting first |
| Huge structure causes lag | Unbounded site scan, writes or updates | Add actual caps, deterministic modules, scoped measurements |

## 30. Advanced references and final application

The advanced chapters were researched on 2026-11-28 from the indexed reference descriptions and technical documentation listings cited inline. Source excerpts were used to identify design principles and relevant case studies. They do not constitute a complete survey of historical architecture; no external CAD plans, template downloads, video frames or game renders were acquired or tested in this documentation revision. Treat unmeasured proportions, coordinate mock-ups, style matrix details, generation algorithms and all QA criteria as MineMods-authored starting designs that need actual project-level verification. When a family falls outside existing examples, research its actual local typology and extend the same universal framework rather than force it into an existing house style.

For further practical design references use [Historic England agricultural building typologies](https://historicengland.org.uk/images-books/publications/dlsg-agricultural-buildings/), [Japanese architectural overview](https://www.japan.travel/en/guide/japanese-architecture/), [Met Gothic design](https://www.metmuseum.org/fr/exhibitions/gothic-by-design-the-dawn-of-architectural-draftsmanship/inside-the-galleries), and [WorldEdit generation reference](https://worldedit.enginehub.org/en/latest/usage/generation/). Review active Minecraft version APIs and actual registered blocks, not merely external reference pictures.

**Production rule:** choose the appropriate design vocabulary, build the complete actual result, verify the site and player experience, and stop once the relevant feature is well finished. This expanded handbook does not create a mandatory test for natural worldgen, a universal CI stage, a building-render backend, or a requirement to reread every specialty chapter for every structure. The best architectural system delivers believable variety at the right scale, not needless complexity.
