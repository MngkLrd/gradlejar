"""Conservative failure triage: matched evidence, not automatic fixes."""
import re
import json
from pathlib import Path
PATTERNS=[
 ("graphics","Graphics backend or virtual display initialization",r"Couldn.t find matching GLX visual|Failed to create backend (?:OpenGL|Vulkan)|VK_KHR_surface|GLFW error"),
 ("mixin","Mixin injection/signature or target mapping",r"InvalidInjectionException|InjectionError|MixinApplyError|Critical injection failure|InvalidMixinException"),
 ("linkage","Runtime API/dependency mismatch",r"NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|ClassNotFoundException|AbstractMethodError"),
 ("java","Incorrect Java or class-file target",r"UnsupportedClassVersionError|Unsupported class file major version|requires Java"),
 ("registry","Registry/data-pack loading failure",r"Failed to load registries|Unbound values in registry|Errors in currently selected data packs|Unknown registry key"),
 ("recipe","Recipe or resource serialization",r"Parsing error loading recipe|Failed to parse recipe|Couldn't parse data file|Couldn't load tag|Unknown block tag"),
 ("side","Client-only class loaded on server",r"invalid dist DEDICATED_SERVER|Cannot load class .* environment type SERVER|Attempted to load class .* for invalid dist"),
 ("dependency","Dependency or loader resolution",r"Could not resolve|Could not find .*\.jar|Mod resolution encountered|Missing or unsupported mandatory dependencies"),
 ("compiler","Java compilation failure",r"\.java:\d+: error:|cannot find symbol|incompatible types:"),
 ("infra","Infrastructure/download or launch failure",r"Connection timed out|Read timed out|HTTP Error (?:403|404|429|5\d\d)|Task 'runServer' not found|RCON.*did not start"),
 ("scenario","Gameplay assertion failure",r"Scenario assertion failed|No scenario assertions executed|Gameplay check failed"),
]
def analyze(text):
    lines=text.splitlines()
    matches=[]
    for kind,summary,pattern in PATTERNS:
        hit=next((line.strip() for line in lines if re.search(pattern,line,re.I)),None)
        if hit: matches.append({"category":kind,"summary":summary,"evidence":hit[:700]})
    if not matches:
        hit=next((line.strip() for line in lines if re.search(r"Caused by:|Traceback|RuntimeError|ValueError|FAILURE:",line)),None)
        if hit: matches.append({"category":"unclassified","summary":"Inspect full log; no specific classifier matched","evidence":hit[:700]})
    db=Path(__file__).with_name("crash-db.json")
    if db.is_file():
        for incident in json.loads(db.read_text("utf-8"))["incidents"]:
            hit=next((line.strip() for line in lines if re.search(incident["match"],line,re.I)),None)
            if hit:
                matches.append({"category":"known-incident","incident":incident["id"],"summary":incident["symptom"],"evidence":hit[:700],"suggested_fix":incident["fix"],"verified_scope":incident["status"]})
    return matches
