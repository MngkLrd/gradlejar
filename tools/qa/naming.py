"""Public naming policy and optional byte-identical delivery copy; no release approval."""
import argparse
import hashlib
import re
import shutil
from pathlib import Path

TOKEN = re.compile(r"[a-z][a-z0-9]*")
PART = r"(?:0|[1-9][0-9]*)"
PRODUCT_VERSION = re.compile(rf"{PART}\.{PART}\.{PART}")
MC_VERSION = re.compile(rf"{PART}\.{PART}(?:\.{PART})?")
LOADERS = {"forge", "neoforge", "fabric"}

def public_filename(*, version, minecraft, loader, mod=None, series=None, addon=None):
    if mod is not None:
        if series is not None or addon is not None:
            raise ValueError("Choose standalone --mod OR --series and --addon")
        tokens = [mod]
    else:
        if series is None or addon is None:
            raise ValueError("Series requires both --series and --addon (core uses addon=core)")
        tokens = [series, addon]
    if any(not isinstance(s,str) or not TOKEN.fullmatch(s) for s in tokens):
        raise ValueError("Public name components must be lowercase ASCII letters/digits")
    if not isinstance(version,str) or not PRODUCT_VERSION.fullmatch(version):
        raise ValueError("Product version must be exactly three numeric components (MAJOR.MINOR.PATCH)")
    if not isinstance(minecraft,str) or not MC_VERSION.fullmatch(minecraft):
        raise ValueError("Use exact numeric Minecraft release version")
    if loader not in LOADERS:
        raise ValueError("Invalid loader")
    return "_".join([*tokens,version,minecraft,loader])+".jar"

def sha256(path):
    with Path(path).open("rb") as f:
        return hashlib.file_digest(f,"sha256").hexdigest()

def delivery_copy(source, out, expected_sha, **identity):
    name=public_filename(**identity)
    source=Path(source)
    if not re.fullmatch(r"[a-f0-9]{64}",expected_sha):
        raise ValueError("Expected actual lowercase SHA-256 from verified QA evidence")
    if source.suffix.lower()!=".jar" or not source.is_file():
        raise ValueError("Expected existing JAR file")
    if sha256(source)!=expected_sha:
        raise ValueError("Source JAR SHA-256 mismatch")
    out=Path(out)
    out.mkdir(parents=True,exist_ok=True)
    dest=out/name
    if dest.exists():
        if dest.is_symlink() or not dest.is_file() or sha256(dest)!=expected_sha:
            raise ValueError("Conflicting existing delivery file; refusing overwrite")
        return dest
    created=False
    try:
        with dest.open("xb") as target:
            created=True
            with source.open("rb") as original:
                shutil.copyfileobj(original,target)
        if sha256(dest)!=expected_sha:
            raise ValueError("Delivery copy SHA-256 mismatch")
    except BaseException:
        if created:
            dest.unlink(missing_ok=True)
        raise
    return dest

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mod")
    parser.add_argument("--series")
    parser.add_argument("--addon")
    parser.add_argument("--version",required=True)
    parser.add_argument("--minecraft",required=True)
    parser.add_argument("--loader",required=True,choices=sorted(LOADERS))
    parser.add_argument("--source",type=Path)
    parser.add_argument("--sha256")
    parser.add_argument("--out",type=Path)
    args=parser.parse_args(argv)
    identity={k:getattr(args,k) for k in ("mod","series","addon","version","minecraft","loader")}
    try:
        if any(x is not None for x in (args.source,args.sha256,args.out)):
            if any(x is None for x in (args.source,args.sha256,args.out)):
                raise ValueError("Copy mode requires --source, --sha256 and --out")
            print(delivery_copy(args.source,args.out,args.sha256,**identity))
        else:
            print(public_filename(**identity))
    except (ValueError,OSError) as exc:
        parser.error(str(exc))
    return 0
if __name__=="__main__":
    raise SystemExit(main())
