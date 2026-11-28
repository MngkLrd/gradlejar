# MineMods — architectural structure design and generation standard

Status: mandatory **only for new or materially changed human-built structures**. This is the canonical architectural design, generation and review guide under [AGENTS.md](../../AGENTS.md). It is an engineering and artistic standard adopted by MineMods, not an official Mojang rulebook. Its objective is generated architecture that a skilled Minecraft builder would be willing to put in a handcrafted adventure map: credible, memorable, purposeful, usable and naturally embedded in its surroundings.

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
