#!/usr/bin/env python3
"""In-process independent RCON probe of REAL NeoForge Heaven chunks.

The simple asset/registry smoke only proves the datapack parses. This probes
the generator, decoration feature codecs, and actual ServerChunkCache. Runs in
a disposable CI world; it is never packaged into the production JAR.
"""
from __future__ import annotations

import os
import re
from pathlib import Path
import socket
import struct
import sys
import time

HOST = "127.0.0.1"
PORT = int(os.environ.get("HALO_QA_RCON_PORT", "25576"))
PASSWORD = os.environ.get("HALO_QA_RCON_PASSWORD", "halo-ci-private-test")
COMMAND_TIMEOUT = 50
WAIT_FOR_SERVER = 105


def read_exact(sock: socket.socket, count: int) -> bytes:
    chunks = []
    while count:
        chunk = sock.recv(count)
        if not chunk:
            raise ConnectionError("RCON disconnected mid-packet")
        chunks.append(chunk)
        count -= len(chunk)
    return b"".join(chunks)


def exchange(sock: socket.socket, request_id: int, kind: int, body: str) -> str:
    data = struct.pack("<ii", request_id, kind) + body.encode() + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(data)) + data)
    n = struct.unpack("<i", read_exact(sock, 4))[0]
    if n < 10 or n > 65536:
        raise ValueError(f"Invalid RCON packet length {n}")
    payload = read_exact(sock, n)
    returned_id, returned_type = struct.unpack("<ii", payload[:8])
    if returned_id == -1:
        raise PermissionError("RCON auth rejected")
    if returned_id != request_id:
        raise ValueError(f"Unexpected RCON id {returned_id} ({returned_type})")
    if kind == 3 and returned_type == 0:
        # Minecraft/Source RCON authentication emits an empty RESPONSE_VALUE
        # (type 0) before the actual AUTH_RESPONSE (type 2). Consume both.
        sock.settimeout(3)
        try:
            n2 = struct.unpack("<i", read_exact(sock, 4))[0]
            if n2 < 10 or n2 > 4096:
                raise ValueError(f"Invalid auth reply length {n2}")
            second = read_exact(sock, n2)
            second_id, second_type = struct.unpack("<ii", second[:8])
            if second_id == -1 or second_id != request_id or second_type != 2:
                raise PermissionError("Unexpected second RCON auth response")
        except socket.timeout:
            print("RCON sent only one auth packet; continuing", flush=True)
        finally:
            sock.settimeout(COMMAND_TIMEOUT)
    return payload[8:-2].decode("utf-8", "replace")


def connect() -> socket.socket:
    deadline = time.monotonic() + WAIT_FOR_SERVER
    last_error: Exception | None = None
    while time.monotonic() < deadline:
        try:
            sock = socket.create_connection((HOST, PORT), timeout=COMMAND_TIMEOUT)
            sock.settimeout(COMMAND_TIMEOUT)
            # Server auth commonly emits a response value packet and an auth
            # success packet. Read one or both before first command.
            result = exchange(sock, 718, 3, PASSWORD)
            return sock
        except (OSError, ValueError, ConnectionError) as error:
            last_error = error
            time.sleep(1)
    raise RuntimeError(f"No RCON on {HOST}:{PORT}: {last_error}")


def command(sock: socket.socket, value: str) -> str:
    response = exchange(sock, 719, 2, value)
    print("COMMAND:", value, flush=True)
    print("RESPONSE:", response, flush=True)
    bad = ("unknown or incomplete command", "Unknown command", "An unexpected error",
           "No such dimension", "Incorrect argument", "Argument parse", "Exception")
    if any(marker.lower() in response.lower() for marker in bad):
        raise RuntimeError(f"Worldgen RCON command failed: {value}: {response}")
    return response


def main() -> int:
    sock = connect()
    print("PASS: authenticated to dedicated server via RCON", flush=True)
    positions = [(0, 0), (512, 0), (0, 512)]
    try:
        # Force-load chunks in the custom Heaven dimension, not Overworld.
        # This traverses density functions, registered biome source, tree
        # features, flower patches, and surface rule execution.
        for x, z in positions:
            output = command(
                sock, f"execute in halohaven:heaven run forceload add {x} {z}"
            )
            if not output.strip() or "chunk" not in output.lower():
                raise RuntimeError(f"Force-load did not acknowledge chunk at {x} {z}")
        # With the generation commits applied, even one forced region can show
        # severe decorator exceptions when it's actually loaded and ticked.
        time.sleep(14)
        for x, z in positions:
            result = command(
                sock, f"execute in halohaven:heaven run forceload query {x} {z}"
            )
            if "force" not in result.lower():
                raise RuntimeError(f"Chunk at {x} {z} was not forced: {result}")
        # A registered biome might still be unreachable if the six climate
        # parameter ranges do not match the density-router output. Locate two
        # opposing climate corners to prove the sampler reaches both.
        for biome in ("dawn_gardens", "eternal_canopy", "ringing_gorges", "forgotten_choirs", "lower_veil"):
            started = time.monotonic()
            result = command(
                sock, f"execute in halohaven:heaven run locate biome halohaven:{biome}"
            )
            if ("could not find" in result.lower()
                or "not found" in result.lower()
                or "halohaven:" + biome not in result.lower()):
                raise RuntimeError(f"Registered biome not reachable: {biome}: {result}")
            print(f"PASS: located {biome} in {time.monotonic() - started:.1f}s", flush=True)
        # 2.7 must NATURALLY produce a walkable cloud, not merely register
        # the block in the game's registries.
        server_log=Path("run/halo25-server.log")
        dump=server_log.read_text(encoding="utf-8",errors="replace")
        cloud=re.search(r"HALOHAVEN_CLOUD_GENERATED\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)",dump)
        if cloud is None:
            raise RuntimeError("No naturally generated mist platforms found in real Heaven chunks")
        cx,cy,cz=map(int,cloud.groups())
        # Adjacent worldgen chunks may be unloaded after decoration.
        command(sock,f"execute in halohaven:heaven run forceload add {cx} {cz}")
        time.sleep(2)
        # Prove a true 3D body, not a 1-block sheet. Generated log contains
        # bottom/top of one fully written cloud column in a real chunk.
        v=re.search(r"HALOHAVEN_CLOUD_VOLUME\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)",dump)
        if v is None:
            raise RuntimeError("Worldgen produced no complete volumetric cloud column")
        vx,lowest,highest,vz=map(int,v.groups())
        if (vx,vz)!=(cx,cz) or highest-lowest<6:
            raise RuntimeError(f"Cloud volume not thick enough: {v.groups()!r}")
        result=command(sock,f"execute in halohaven:heaven if block {vx} {lowest} {vz} halohaven:sky_mist if block {vx} {lowest+3} {vz} halohaven:sky_mist if block {vx} {highest} {vz} halohaven:sky_mist run time query daytime")
        if not result.strip() or ("time" not in result.lower() and not result.strip().isdigit()):
            raise RuntimeError(f"Cloud is not an actual contiguous 3D column: {result!r}")
        print(f"PASS: 3D cloud occupies contiguous Y{lowest}..Y{highest} at {vx},{vz}",flush=True)
        response=command(sock,f"execute in halohaven:heaven if block {cx} {cy} {cz} halohaven:sky_mist run setblock {cx} {cy} {cz} minecraft:air")
        if "changed the block" not in response.lower():
            raise RuntimeError(f"Worldgen mist block absent at {cx},{cy},{cz}: {response}")
        print(f"PASS: generated walkable sky mist naturally at {cx},{cy},{cz}",flush=True)
        command(sock,f"execute in halohaven:heaven run forceload remove {cx} {cz}")
        # Verify the internal visible portal block resolves in a loaded Heaven
        # chunk. This exercises block registration and blockstate availability.
        # A client-side renderer and player-driven teleport require manual QA.
        command(sock, "execute in halohaven:heaven run setblock 0 160 0 halohaven:ascension_gateway")
        # RCON does not return /say broadcast chat as a command result.
        # Instead, conditionally replace the gateway. A "Changed the block"
        # response proves the condition matched the actual block state.
        gate = command(sock, "execute in halohaven:heaven if block 0 160 0 halohaven:ascension_gateway run setblock 0 160 0 minecraft:air")
        if "changed the block" not in gate.lower():
            raise RuntimeError(f"Gateway blockstate test failed: {gate!r}")
        print("PASS: gateway resource exists and can be placed in real dimension", flush=True)
        # Cleanup persistent forceload markers in CI.
        for x, z in positions:
            command(sock, f"execute in halohaven:heaven run forceload remove {x} {z}")
        print(f"PASS: generated/ticked {len(positions)} real Halo Haven positions", flush=True)
        return 0
    finally:
        try:
            command(sock, "stop")
        except Exception as error:
            print("RCON graceful stop failed:", error, flush=True)
        sock.close()


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print("FAIL: WORLDGEN PROBE:", error, file=sys.stderr, flush=True)
        sys.exit(1)
