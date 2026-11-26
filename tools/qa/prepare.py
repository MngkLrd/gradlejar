"""Emit trusted QA settings after validation of a project profile."""
import sys
from .common import profile, qa_config, compatibility

p=profile(sys.argv[1])
q=qa_config(p)
print("mode="+q["mode"])
print("runtime_java="+str(compatibility(p)[0]["java"] if q["mode"]!="legacy" else p["java"]))
