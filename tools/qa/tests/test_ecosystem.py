import json
import tempfile
import unittest
from pathlib import Path
from tools.qa.ecosystem import collect, catalogue, affected, validate, preview, build_preview

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
        self.assertEqual({d["kind"] for d in r["diagnostics"] if d["severity"]=="error"},{"duplicate_capability","duplicate_domain","ambiguous"})
        self.assertIsNone(r["edges"][0]["provider"])
        self.assertEqual(affected(p,["a/api.java"]),["a","c"])
        r=catalogue({"a":mod("core",[provide()]),"c":mod("chop",consumes=[consume(rev=3,required=True)])})
        self.assertEqual(r["edges"][0]["status"],"incompatible")
        p={"a":mod("a",[provide()], [consume("test:b",required=True)]),"b":mod("b",[provide("test:b",domain="b")],[consume(required=True)])}
        self.assertIn("required_cycle",{d["kind"] for d in catalogue(p)["diagnostics"]})
    def test_validation_and_standalone(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            for name in ("core","standalone"):
                d=root/"projects"/name;d.mkdir(parents=True)
                (d/"build-profile.json").write_text(json.dumps(T))
            (root/"projects/core/API.md").write_text("Fixture API")
            (root/"projects/core/integration.json").write_text(json.dumps(mod("core",[provide()])))
            self.assertEqual(set(collect(root)),{"projects/core"})
            bad=mod("core",[provide("other:wood")])
            with self.assertRaises(ValueError): validate("projects/core",bad,T)
            with self.assertRaises(ValueError): validate("projects/core",mod("core"),{"minecraft":"26.3","loader":"neoforge"})
    def test_optional_missing_is_not_hard_error(self):
        r=catalogue({"a":mod("a",consumes=[consume()])})
        self.assertEqual(r["diagnostics"][0]["severity"],"warning")
        self.assertEqual(r["edges"][0]["status"],"missing")

    def test_required_missing_and_unimplemented_distinct(self):
        required=consume(required=True)
        r=catalogue({"a":mod("a",consumes=[required])})
        self.assertEqual(r["diagnostics"][0]["severity"],"error")
        planned=consume(required=True);planned.update(status="planned",issue="https://github.com/x/y/issues/1")
        r=catalogue({"a":mod("a",consumes=[planned]),"b":mod("b",[provide()])})
        self.assertEqual(r["edges"][0]["status"],"not_implemented")
        self.assertEqual(r["edges"][0]["resolution"],"compatible")
        self.assertEqual(r["runtime_verification"],"NOT_RUN")
        self.assertIn("contract_not_implemented",{d["kind"] for d in r["diagnostics"]})

    def test_parent_impact_cycle_and_target_isolation(self):
        d={"projects/core":mod("core"),"projects/addon":mod("addon",parent="core"),
           "projects/port":mod("addon",parent="core",target={"minecraft":"26.3","loader":"neoforge"}),
           "projects/standalone":mod("core",ecosystem="other")}
        self.assertEqual(affected(d,["projects/core/src/A.java"]),["projects/addon","projects/core"])
        self.assertEqual(affected({k:v for k,v in d.items() if k!="projects/core"},["projects/core/integration.json"],d),["projects/addon"])
        r=catalogue({"a":mod("a",parent="b"),"b":mod("b",parent="a")})
        self.assertIn("parent_cycle",{x["kind"] for x in r["diagnostics"]})

    def test_root_membership_and_missing_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);(root/"build-profile.json").write_text(json.dumps(T))
            m=mod("core",[provide()])
            (root/"integration.json").write_text(json.dumps(m))
            with self.assertRaisesRegex(ValueError,"missing capability documentation"):collect(root)
            path=root/"projects/core/API.md";path.parent.mkdir(parents=True);path.write_text("Fixture")
            m["tests"]=[dict(id="contract",capabilities=["test:wood"],ref="tests/contract.json",status="implemented")]
            (root/"integration.json").write_text(json.dumps(m))
            with self.assertRaisesRegex(ValueError,"missing implemented test"):collect(root)
            m["tests"][0]["status"]="planned";(root/"integration.json").write_text(json.dumps(m))
            self.assertEqual(set(collect(root)),{"."})
            self.assertEqual(affected(collect(root),["src/main/A.java"]),["."])
            self.assertEqual(affected(collect(root),["projects/unrelated/src/A.java"]),[])

    def test_reject_malformed_manifest_and_example(self):
        from tools.qa.ecosystem import ROOT
        example=json.loads((ROOT/"docs/development/integration-manifest.example.json").read_text())
        validate("example",example,{**T,"ecosystem_membership":{k:example[k] for k in ("ecosystem","module","role","core_module")}})
        for bad in ("./API.md","../API.md","a//API.md","/API.md","a\\API.md"):
            p=provide();p["documentation"]=bad
            with self.assertRaises(ValueError):validate("projects/core",mod("core",[p]),T)
        for bad in ([],{},None):
            c=consume();c["status"]=bad
            with self.assertRaises(ValueError):validate("projects/core",mod("core",consumes=[c]),T)
        with self.assertRaises(ValueError):validate("projects/core",mod("core",[provide()],[consume()]),T)

    def test_build_preview_exact_previous_graph_and_no_matrix_expansion(self):
        import subprocess
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            def git(*args):
                return subprocess.check_output(["git",*args],cwd=root,text=True).strip()
            git("init","-q");git("config","user.email","fixture@example.invalid");git("config","user.name","Fixture")
            for name,m in [("core",mod("core",[provide()])),("addon",mod("addon",consumes=[consume(required=True)],parent="core"))]:
                d=root/"projects"/name;d.mkdir(parents=True)
                (d/"build-profile.json").write_text(json.dumps(T))
                (d/"integration.json").write_text(json.dumps(m))
            (root/"projects/core/API.md").write_text("Fixture")
            git("add",".");git("commit","-qm","previous");base=git("rev-parse","HEAD")
            (root/"projects/core/integration.json").unlink()
            git("add","-A");git("commit","-qm","provider removed")
            report=build_preview(root,[],base)
            self.assertEqual(report["selected_build_projects"],[])
            self.assertEqual(report["additional_consumers_to_review"],["projects/addon"])
            self.assertEqual(report["removed_projects"],["projects/core"])
            self.assertEqual(report["integration_qa"],"NOT_RUN")
            self.assertTrue(report["previous_graph_available"])
            self.assertTrue(any(x["kind"]=="missing" and x["severity"]=="error" for x in report["diagnostics"]))
            self.assertIsNone(build_preview(root,["projects/standalone"]))

    def v2(self, name="core", core="core", path="projects/core", requires=False):
        cap=f"test:{name}_feature"
        p=provide(cap,domain=name)
        p.update(documentation=path+"/API.md",source_paths=[path+"/src/API.java"],
                 operations=[dict(id="read",kind="query",entrypoint="example.Wood",semantics="Return immutable state in documented units.")])
        uses=[consume("test:core_feature",required=True)] if requires else []
        m=mod(name,[p],uses,parent=core if name!=core else None)
        m.update(schema=2,role="core" if name==core else "addon",core_module=core,
                 features=[dict(id="feature",source_paths=[path+"/src/API.java"],provides=[cap],
                                consumes=[c["id"] for c in uses],integration_review="Reviewed actual peer API and ownership decisions.")],
                 tests=[dict(id="contract",capabilities=[cap]+[c["id"] for c in uses],ref=path+"/tests.json",status="implemented")])
        profile={**T,"ecosystem_membership":{k:m[k] for k in ("ecosystem","module","role","core_module")}}
        return m,profile

    def test_api_first_roles_and_contracts(self):
        core,cp=self.v2()
        addon,ap=self.v2("addon",path="projects/addon",requires=True)
        validate("projects/core",core,cp);validate("projects/addon",addon,ap)
        r=catalogue({"projects/core":core,"projects/addon":addon})
        self.assertEqual(r["diagnostics"],[])
        self.assertEqual([x["project"] for x in r["first_party_cores"]],["projects/core"])
        self.assertIn("parent_not_in_checkout",{x["kind"] for x in catalogue({"projects/addon":addon})["diagnostics"]})
        legacy=mod("core")
        self.assertIn("parent_not_promoted_to_core",{x["kind"] for x in catalogue({"projects/core":legacy,"projects/addon":addon})["diagnostics"]})
        other,op=self.v2("second",core="second",path="projects/second")
        self.assertIn("multiple_cores",{x["kind"] for x in catalogue({"projects/core":core,"projects/second":other})["diagnostics"]})
        for mutate in (lambda m:m["provides"].clear(),lambda m:m["provides"][0]["operations"].clear(),
                       lambda m:m["features"][0]["provides"].clear(),lambda m:m["tests"].clear(),
                       lambda m:m.update(role="addon"),lambda m:m.update(schema=1)):
            import copy
            changed=copy.deepcopy(core);mutate(changed)
            with self.assertRaises(ValueError):validate("projects/core",changed,cp)

    def test_scoped_api_first_gate(self):
        from tools.qa.ecosystem import check_project
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);path="projects/core";d=root/path;d.mkdir(parents=True)
            m,p=self.v2()
            (d/"build-profile.json").write_text(json.dumps(p))
            with self.assertRaisesRegex(ValueError,"missing integration.json"):check_project(root,path)
            (d/"integration.json").write_text(json.dumps(m))
            (d/"API.md").write_text("API");(d/"tests.json").write_text("{}")
            with self.assertRaisesRegex(ValueError,"missing API/feature source"):check_project(root,path)
            (d/"src").mkdir();(d/"src/API.java").write_text("fixture")
            self.assertEqual(check_project(root,path)["status"],"STATIC_EVIDENCE_VALIDATED")
            self.assertEqual(check_project(root,path)["integration_qa"],"NOT_RUN")
            other=root/"projects/thirdparty";other.mkdir()
            (other/"build-profile.json").write_text(json.dumps(T))
            self.assertEqual(check_project(root,"projects/thirdparty")["status"],"NOT_ENROLLED")
            (d/"integration.json").write_text("invalid json")
            self.assertEqual(check_project(root,"projects/thirdparty")["status"],"NOT_ENROLLED")
            with self.assertRaises(ValueError):check_project(root,"projects/..")
