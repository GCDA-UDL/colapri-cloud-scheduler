# Changelog

All notable changes to the ColaPri reproduction package. Versions follow the GitHub releases;
the Zenodo record (once created) archives each release.

## v1.0.1 — 2026-10-19

- **New baseline `CloudletSchedulerSFQ`**: start-time fair queueing (Goyal, Vin and Cheng, 1997)
  configured with ColaPri's own per-class weights (6:3:1). It is the classical weighted-fair control:
  tags advance by service demand (length / weight), the virtual clock is re-anchored to the task in
  service, and it is always on. Registered in EXP-A (`RigorousEval`) and EXP-E (`RealTraceRigorous`).
- `results/EXP-A.log` and `results/EXP-E.log` re-captured with the new baseline. Every previously
  reported row reproduces bit-identically; only the wall-clock `overhead.ms` column differs
  (ColaPri 1.8 ms on the reference host in this capture).
- `CfsProbe` added: the probe showing that the built-in `CloudletSchedulerCompletelyFair` does not
  terminate under a staggered backlog in our configuration (kept so the issue can be reproduced).
- Figures regenerated (`figures/make_figures.py`): SFQ and DFPO added to the per-class, starvation,
  Pareto, Jain and trace plots.
- Packaging: `Dockerfile`, `docker-compose.yml`, `REPRODUCE.md`, `THIRD_PARTY.md`, this changelog;
  README updated (8 schedulers, canonical repository under the GCDA-UDL organisation).
- Metadata: authors updated in `CITATION.cff` / `.zenodo.json` (Vitor Luiz da Silva, Rosa Ana Tomás,
  Joaquim Oliva, Fernando Guirado); version 1.0.1.

## v1.0.0 — 2026-07-24

- First public release: ColaPri (`CloudletSchedulerColaPri`) plus six baselines (FCFS, SJF, strict
  priority, priority+aging, equal-weight WFQ, DFPO), four task-to-VM mappers, the six experiment
  harnesses (EXP-A..F) and the captured logs behind every table of the paper.
- Bundled PlanetLab, Bitbrains and Materna trace subsets; SHA-256 checksums and provenance for all five
  datasets (`data/`); one-command reproduction script (`scripts/run_all.sh`); `CITATION.cff` and
  `.zenodo.json` deposit metadata.
