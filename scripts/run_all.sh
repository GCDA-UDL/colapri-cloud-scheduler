#!/usr/bin/env bash
# Reproduce every experiment in the ColaPri paper (EXP-A..F), writing one log per experiment
# to results/. Simulated metrics are seed-deterministic: the tables reproduce bit-identically;
# only the wall-clock overhead column varies with the host.
#
# Requirements: Docker. Traces (EXP-E only) mounted at /traces — see README + data/PROVENANCE.md.
# Usage:  ./scripts/run_all.sh [/absolute/path/to/traces]
#
# Approx. runtime: a few minutes total on a modern desktop (i7-14700KF reference host).
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
TRACES="${1:-$REPO/data}"   # default: the bundled subsets (PlanetLab, Bitbrains, Materna)
IMG=maven:3.9-eclipse-temurin-21   # pin by digest for strict reproduction if desired

VOLS=(-v "$REPO:/workspace" -v colapri_m2:/root/.m2)
[ -n "$TRACES" ] && VOLS+=(-v "$TRACES:/traces")

docker run --rm "${VOLS[@]}" -w /workspace "$IMG" bash -lc '
  set -e; mvn -q compile; mkdir -p results
  run(){ echo "=== $2 ($1) start $(date -u +%T) ==="; \
         mvn -q exec:java -Dexec.mainClass=colapri.$1 -Dexec.cleanupDaemonThreads=false \
           > results/$2.log 2>&1 && echo "=== $2 OK ==="; }
  run RigorousEval     EXP-A
  run DatacenterEval   EXP-B
  run ScalabilityEval  EXP-C
  run SensitivityEval  EXP-D
  run ActivationEval   EXP-F
  if [ -d /traces ]; then run RealTraceRigorous EXP-E; else echo "EXP-E skipped (no /traces mount)"; fi
  echo ALL_DONE'
