# Screenshot Manager Studio 0.5.0
Minecraft 1.21.1 / NeoForge / Java 21. Client-only; no extra mods.
O editor, P library, K copy, F8 capture and copy, F9 photo mode, [ ] FOV, F10 clean capture.
F6 start/stop a short clip. Up to 8 seconds, ~10 FPS and max 640 px long side. Recording opens GIF Studio automatically; set start/end on the timeline and export a looping GIF. Output frame rate (5/8/10), resolution and decimal KB ceiling are selectable. The encoder reduces resolution and then samples frames as needed; fails rather than exceeding the ceiling. Exports to screenshots/clips. No audio. Recording stops when leaving gameplay; use F6 first to retain the clip. Existing game HUD may be captured; use F1 before recording to hide it. In-game readback and shader compatibility require testing.
F7 captures five normal screenshots spaced ~350 ms apart.
Studio File tab: Save/Load project stores current editable operations in a .smproject sidecar next to its original, linked by SHA-256. It is not a standalone image; do not share sidecars containing private annotation text. Export PNG/JPEG with JPEG quality, long-side size and optional KB ceiling.
Library: right-click two to six image tiles, choose Grid/Vertical, then Collage. Saves a PNG in screenshots/edited; originals are unchanged.
Build: gradle --no-daemon clean build (Gradle 9.2.1). Mandatory coreSmoke tests cover image operations, settings, project round trip, export, collage and GIF frame sequence.
