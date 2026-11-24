# Screenshot Manager Studio 0.4.0
Minecraft 1.21.1 / NeoForge / Java 21. Client only. No additional mods required.
O = editor, P = library, K = copy, F8 = capture and copy.
F9 = photo mode; [ and ] change FOV; F10 = clean capture. FOV restored on leaving photo mode. 
Studio: Tools / Layers / Color tabs. Scroll tool lists. Select a layer then drag in the canvas to move it. Change style in Tools or Color and use Apply style in Layers. Hide/reorder/delete edits, undo/redo. Crop/rotate/flip are locked geometry stages. Compare shows before/after; Ctrl+Shift+S exports the comparison. Originals are preserved.
Library: type an album name, click To album on a tile, click All albums to cycle filters. Star to favorite. Empty album input + Unfile removes association. Preferences persisted under config/screenshotmanager.properties.
Build: gradle --no-daemon clean build (Gradle 9.2.1). Core smoke test checks document operations and persistent settings.
In-game and shader QA required.
