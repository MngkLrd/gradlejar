# Screenshot Manager | Minecraft 1.21.1 / NeoForge

This CI project is sourced from the uploaded Screenshot Manager v0.2.0 source ZIP.
The complete Java sources and resources are restored with:

```sh
python3 tools/unpack_screenshotmanager.py
cd projects/screenshotmanager
gradle --no-daemon clean build
```

Java 21; Gradle 9.2.1; NeoForge 21.1.262; ModDevGradle 2.0.148.
The build JAR is `projects/screenshotmanager/build/libs/screenshotmanager-0.2.0-neoforge.jar`.

This is a client-only mod; validation in a real Minecraft client is separate from Gradle compilation.
