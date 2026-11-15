# Independent Minecraft mod projects

Place each new Gradle mod in its own folder:

```text
projects/
  umbra-shift/
    build-profile.json
    settings.gradle
    build.gradle
    gradle.properties
    src/main/java/...
    src/main/resources/...
  another-mod/
    build-profile.json
    settings.gradle
    build.gradle
    src/...
```

Then run GitHub Actions **Minecraft Mod Build Hub**, and set `project_dir` to `projects/umbra-shift` or `projects/another-mod`.

Do not copy the build profile from FieldKit unchanged; pin the versions actually supported by each mod.
