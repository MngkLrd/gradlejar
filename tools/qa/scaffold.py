"""Generate independent draft Gradle ports from the pinned compatibility registry."""
import argparse
import json
import re
from pathlib import Path
from .common import REGISTRY, require, write_json, target

MOD_ID=re.compile(r"[a-z][a-z0-9_]{1,63}")
VER=re.compile(r"[0-9]+(?:\.[0-9]+){1,3}(?:-[a-zA-Z0-9.-]+)?")
ORDER={"forge":0,"neoforge":1,"fabric":2}

def matrix():
    return [(mc,l) for mc,m in REGISTRY["minecraft"].items() for l in m["loaders"]]

def select(targets):
    available={l+"-mc"+mc:(mc,l) for mc,l in matrix()}
    if targets==["all"]:return [(mc,l) for mc,l in available.values() if REGISTRY["minecraft"][mc].get("standard",True)]
    if targets==["extended"]:return list(available.values())
    require(targets and len(targets)==len(set(targets)) and set(targets)<=set(available),"Invalid or duplicate target")
    return [available[t] for t in targets]

def tomlstr(s):
    return json.dumps(s,ensure_ascii=False)

def write(root,path,content):
    f=root/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(content,encoding="utf-8")

def generate(spec,out):
    slug=spec["id"];version=spec["version"];name=spec["name"];description=spec["description"]
    require(isinstance(slug,str) and MOD_ID.fullmatch(slug),"Invalid mod ID")
    require(isinstance(version,str) and VER.fullmatch(version),"Invalid version")
    require(all(isinstance(s,str) and s.strip() and len(s)<=500 and not any(ord(c)<32 for c in s) for s in (name,description)),"Invalid display name/description")
    mode=spec.get("mode","server");require(mode in ("server","client"),"Invalid side")
    targets=select(spec.get("targets",["all"]))
    out=Path(out).resolve()
    require(not out.exists(),"Output already exists; no overwrite")
    require("projects" not in out.parts,"Generate into a draft directory outside projects; enroll manually after implementation")
    out.mkdir(parents=True)
    cohort=[l+"-mc"+mc for mc,l in targets]
    result=[]
    for mc,l in targets:
        m=REGISTRY["minecraft"][mc];cfg=m["loaders"][l]
        java=m["java"];build_java=cfg.get("build_java",java);lv=cfg["version"]
        modern=mc.startswith("26.");legacy=mc=="1.20.1" and l!="fabric"
        root=out/f"{slug}-{l}-{mc}";root.mkdir()
        archive=f"{slug}-{l}-mc{mc}";pkg="dev.minemods.mod_"+slug
        if l=="fabric":
            plugin=f"id '{'net.fabricmc.fabric-loom' if modern else 'fabric-loom'}' version '{cfg['loom']}'"
            deps=f"minecraft 'com.mojang:minecraft:{mc}'\n"+(f"implementation 'net.fabricmc:fabric-loader:{lv}'" if modern else f"mappings loom.officialMojangMappings()\nmodImplementation 'net.fabricmc:fabric-loader:{lv}'")
            loader_code="dependencies {\n"+deps+"\n}"
            maven="https://maven.fabricmc.net/"
            impl="ClientModInitializer" if mode=="client" else "ModInitializer"
            method="onInitializeClient" if mode=="client" else "onInitialize"
            src=f"package {pkg};\nimport net.fabricmc.api.{impl};\npublic final class EntryPoint implements {impl} {{\n    @Override public void {method}() {{ /* implement actual mechanics here */ }}\n}}\n"
            write_json(root/"src/main/resources/fabric.mod.json",{"schemaVersion":1,"id":slug,"version":version,"name":name,"description":description,"environment":"client" if mode=="client" else "*","entrypoints":{"client" if mode=="client" else "main":[pkg+".EntryPoint"]},"depends":{"fabricloader":">="+lv,"minecraft":mc,"java":">="+str(java)}})
        else:
            plugin="id 'java-library'; id 'net.neoforged.moddev.legacyforge' version '2.0.147'" if legacy else "id 'java-library'; id 'net.neoforged.moddev' version '2.0.148'"
            loader_code=(f"legacyForge {{ enable {{ {'forgeVersion' if l=='forge' else 'neoForgeVersion'}='{mc}-{lv}'; disableRecompilation=true }}; runs {{ client {{ client() }}; server {{ server() }} }}; mods {{ {slug} {{ sourceSet(sourceSets.main) }} }} }}" if legacy else f"neoForge {{ version='{lv}'; runs {{ client {{ client() }}; server {{ server() }} }}; mods {{ {slug} {{ sourceSet(sourceSets.main) }} }} }}")
            maven="https://maven.neoforged.net/releases"
            annotation="net.minecraftforge.fml.common.Mod" if legacy else "net.neoforged.fml.common.Mod"
            src=f"package {pkg};\nimport {annotation};\n@Mod(\"{slug}\")\npublic final class EntryPoint {{ public EntryPoint() {{ /* implement actual mechanics here */ }} }}\n"
            dep="forge" if legacy else "neoforge";typ="mandatory=true" if legacy else 'type="required"'
            meta=f'modLoader="javafml"\nloaderVersion="{ "[47,)" if legacy else "[1,)" }"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="{slug}"\nversion="{version}"\ndisplayName={tomlstr(name)}\ndescription={tomlstr(description)}\n'
            for d,r in [(dep,"["+lv+",)"),("minecraft","["+mc+"]")]:
                meta+=f'[[dependencies.{slug}]]\nmodId="{d}"\n{typ}\nversionRange="{r}"\nordering="NONE"\nside="BOTH"\n'
            write(root,"src/main/resources/META-INF/"+("mods.toml" if legacy else "neoforge.mods.toml"),meta)
        gradle=f"plugins {{ {plugin} }}\ngroup='{pkg}'\nversion='{version}'\nbase.archivesName='{archive}'\njava.toolchain.languageVersion=JavaLanguageVersion.of({build_java})\n{loader_code}\ntasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release={java} }}\n"
        write(root,"build.gradle",gradle)
        write(root,"settings.gradle",f"pluginManagement {{ repositories {{ maven {{ url='{maven}' }}; gradlePluginPortal(); mavenCentral() }} }}\nrootProject.name='{archive}'\n")
        write(root,"gradle.properties","org.gradle.jvmargs=-Xmx2G\n")
        write(root,"src/main/java/"+pkg.replace(".","/")+"/EntryPoint.java",src)
        pack={"description":name}
        if modern:pack.update(min_format=min(m["resource"],m["data"]),max_format=max(m["resource"],m["data"]))
        else:pack["pack_format"]=m["resource"][0]
        write_json(root/"src/main/resources/pack.mcmeta",{"pack":pack})
        # Draft deliberately lacks a usable QA scenario and must not be released.
        p={"slug":slug,"version":version,"minecraft":mc,"loader":l,"java":str(build_java),"gradle":cfg["gradle"],"jar_glob":archive+"-"+version+".jar","qa":{"mode":mode,"loader_version":lv,"release_targets":cohort},"draft":True}
        if mode=="server":p["qa"]["scenario"]=f"tools/qa/scenarios/{slug}.json"
        write_json(root/"build-profile.json",p)
        write(root,"README.md",f"# {name} — DRAFT\nMinecraft {mc}, {l}, Java {java}, Gradle {cfg['gradle']}.\nRun gradle clean build. This empty entrypoint is a scaffold, NOT the implemented mod. Implement features, add real resources and regression assertions; remove draft only after review. For Forge client-only targets, configure actual side-specific initialization before enrollment.\n")
        result.append({"target":l+"-mc"+mc,"directory":root.name,"profile":p})
    write_json(out/"scaffold-manifest.json",{"schema":1,"id":slug,"version":version,"status":"draft-not-release","ports":result})
    # Empty plan is intentional. No dummy success command or fake gameplay assertion.
    write_json(out/"scenario-draft.json",{"schema":1,"name":name+" — implement gameplay assertions","steps":[]})
    return result

if __name__=="__main__":
    ap=argparse.ArgumentParser();ap.add_argument("spec");ap.add_argument("--out",required=True);a=ap.parse_args()
    rows=generate(json.loads(Path(a.spec).read_text("utf-8")),a.out)
    print("Generated",len(rows),"DRAFT ports; no gameplay implementation or release approval")
