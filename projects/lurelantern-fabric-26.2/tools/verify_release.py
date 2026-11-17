from pathlib import Path
import runpy,sys
script=Path(__file__).resolve().parents[3]/'tools'/'verify_lurelantern_jar.py'
sys.argv=[str(script),sys.argv[1]]
runpy.run_path(str(script),run_name='__main__')
