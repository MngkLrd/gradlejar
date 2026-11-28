import json
import tempfile
import unittest
from pathlib import Path
from tools.qa.ecosystem import collect, catalogue, affected, validate

T = {"minecraft":"1.21.1", "loader":"neoforge"}
def mod(name, provides=(), consumes=(), ecosystem="test", target=None, parent=None):
    m = dict(schema=1, ecosystem=ecosystem, module=name, target=target or T, provides=list(provides), consumes=list(consumes), tests=[])
    if parent: m["parent_module"]=parent
    return m
def provide(id="test:wood", revision=2, domain="wood"):
    return dict(id=id,version=1,revision=revision,domains=[domain],entrypoints=["example.Wood"],documentation="projects/core/API.md")
def consume(id="test:wood", rev=1, required=False, version=1):
    return dict(id=id,version=version,min_revision=rev,required=required,status="implemented",fallback="No integration")

class EcosystemTests(unittest.TestCase):
    def test_scope_and_graph(self):
        data={"projects/core":mod("core",[provide()]),"projects/chop":mod("chop",consumes=[consume()],parent="core"),
              "projects/leaf":mod("leaf",[provide("test:leaf",domain="leaf")],[consume()]),
              "projects/saw":mod("saw",consumes=[consume("test:leaf")]),
              "projects/port":mod("chop",consumes=[consume()],target={"minecraft":"26.3","loader":"neoforge"}),
              "projects/other":mod("core",[provide("other:wood")],ecosystem="other")}
        r=catalogue(data)
        self.assertEqual([x["status"] for x in r["edges"]].count("compatible"),3)
        self.assertEqual(affected(data,["projects/core/src/A.java"]),["projects/chop","projects/core","projects/leaf","projects/saw"])
        self.assertEqual(affected(data,["projects/standalone/src/A.java"],initial=["projects/standalone"]),["projects/standalone"])
        new={p:m for p,m in data.items() if p!="projects/core"}
        self.assertEqual(affected(new,["projects/core/integration.json"],data),["projects/chop","projects/leaf","projects/saw"])
    def test_conflicts_and_versions(self):
        p={"a":mod("core",[provide()]),"b":mod("other",[provide()]),"c":mod("chop",consumes=[consume(rev=3,required=True)])}
        r=catalogue(p)
        self.assertEqual({d["kind"] for d in r["diagnostics"] if d["severity"]=="error"},{"duplicate_capability","duplicate_domain","incompatible"})
        p={"a":mod("a",[provide()], [consume("test:b",required=True)]),"b":mod("b",[provide("test:b",domain="b")],[consume(required=True)])}
        self.assertIn("required_cycle",{d["kind"] for d in catalogue(p)["diagnostics"]})
    def test_validation_and_standalone(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            for name in ("core","standalone"):
                d=root/"projects"/name;d.mkdir(parents=True)
                (d/"build-profile.json").write_text(json.dumps(T))
            (root/"projects/core/integration.json").write_text(json.dumps(mod("core",[provide()])))
            self.assertEqual(set(collect(root)),{"projects/core"})
            bad=mod("core",[provide("other:wood")])
            with self.assertRaises(ValueError): validate("projects/core",bad,T)
            with self.assertRaises(ValueError): validate("projects/core",mod("core"),{"minecraft":"26.3","loader":"neoforge"})
    def test_optional_missing_is_not_hard_error(self):
        r=catalogue({"a":mod("a",consumes=[consume()])})
        self.assertEqual(r["diagnostics"][0]["severity"],"warning")
        self.assertEqual(r["edges"][0]["status"],"missing")
