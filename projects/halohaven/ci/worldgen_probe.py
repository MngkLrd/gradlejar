#!/usr/bin/env python3
"""In-process independent RCON probe of REAL NeoForge Heaven chunks.

The simple asset/registry smoke only proves the datapack parses. This probes
the generator, decoration feature codecs, and actual ServerChunkCache. Runs in
a disposable CI world; it is never packaged into the production JAR.
"""
from __future__ import annotations

import os
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
        for biome in ("chalk_terraces", "lower_veil"):
            started = time.monotonic()
            result = command(
                sock, f"execute in halohaven:heaven run locate biome halohaven:{biome}"
            )
            if ("could not find" in result.lower()
                or "not found" in result.lower()
                or "halohaven:" + biome not in result.lower()):
                raise RuntimeError(f"Registered biome not reachable: {biome}: {result}")
            print(f"PASS: located {biome} in {time.monotonic() - started:.1f}s", flush=True)
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
