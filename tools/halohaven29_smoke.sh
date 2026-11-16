#!/usr/bin/env bash
set -euo pipefail
cd projects/halohaven
mkdir -p run/mods
cp ../halohaven-alpha21/build/libs/halohavenalpha2-*.jar run/mods/
printf 'eula=true\n' > run/eula.txt
cat > run/server.properties <<'EOF'
online-mode=false
enable-rcon=true
rcon.port=25576
rcon.password=island-ci
server-port=25565
level-seed=halo-qa-2.8-islands
max-tick-time=120000
view-distance=6
simulation-distance=4
EOF
set +e
timeout --signal=SIGINT --kill-after=12 385s gradle --no-daemon runServer > run/halo25-server.log 2>&1 &
serverpid=$!
HALO_QA_RCON_PASSWORD=island-ci python3 ci/worldgen_probe.py > run/halo25-probe.log 2>&1
probe_status=$?
if [[ "$probe_status" -ne 0 ]]; then kill -INT "$serverpid" 2>/dev/null || :; fi
wait "$serverpid"
set -e
tail -n 90 run/halo25-server.log
cat run/halo25-probe.log
if grep -Eiq 'Errors in currently selected data packs|Failed to load registries|Exception in server tick loop|A critical error occurred|Failure during server start|Failed to parse' run/halo25-server.log; then
 echo 'FAIL: Runtime registry errors'
 exit 1
fi
# Cloud fields must never write outside the chunk being decorated.
if grep -q 'Detected setBlock in a far chunk' run/halo25-server.log; then
  echo 'FAIL: decoration wrote outside the permitted worldgen chunk'
  exit 1
fi
test "$probe_status" -eq 0
grep -q 'PASS: generated/ticked 3 real Halo Haven positions' run/halo25-probe.log
grep -q 'PASS: 3D cloud occupies contiguous' run/halo25-probe.log
for biome in dawn_gardens eternal_canopy ringing_gorges forgotten_choirs lower_veil; do
 grep -q "PASS: located $biome" run/halo25-probe.log
done
echo 'PASS 2.9: all six biomes located, three sky chunks generated and no runtime registry errors'
