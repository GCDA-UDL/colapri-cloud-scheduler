# Changelog

All notable changes to the ColaPri reproduction package. Versions follow the GitHub releases;
the Zenodo record (once created) archives each release.

## v1.1.1 — 2026-10-21

Metadata-only release, created so that the GitHub–Zenodo integration (enabled after v1.1.0) archives
the package. No code, log or figure changes relative to v1.1.0.

- Author affiliation updated to **SEMBAC, Universitat de Lleida** in `CITATION.cff`, `.zenodo.json` and
  the README (the group joined SEMBAC in October 2026).
- `ZENODO_CHECKLIST.md` rewritten for the GitHub-integration flow.

## v1.1.0 — 2026-10-20

**Corrected harness: every number in `results/` and `figures/` changed.** An independent audit of
v1.0.1 found two defects that invalidated the earlier captures; both are fixed here and the full
experimental matrix was re-run. The scheduler's algorithm also gained counter resynchronisation.

- **Fixed (harness, critical):** every cloudlet was created with `UtilizationModelFull` for RAM and
  bandwidth as well as CPU. CloudSim Plus then throttled co-scheduled cloudlets to a single processing
  element (the 2-PE VMs ran as 1-PE VMs), and in EXP-B one cloudlet per VM never finished (304/320).
  `Workload.cloudlet(len)` now sets RAM/BW demand to 1% of the VM (CPU stays `Full`); `CapacityProbe`
  reproduces the old behaviour. All simulated response/waiting/makespan values are roughly halved
  relative to v1.0.1 and the qualitative ranking of the baselines changed (see the paper).
- **Fixed (simulator workaround):** `VmPatched` extends `VmSimple` so that `updateProcessing` never
  returns 0 while cloudlets remain; with plain `VmSimple`, CloudSim Plus 8.5.7 could end a simulation
  with cloudlets still queued (EXP-D θ∈[58,62] seed 15: 26/80 finished) or stall it (EXP-F seed 22).
  `Metrics.checked(finished, submitted)` now asserts completeness in every harness and prints `[LOST]`
  otherwise; the committed logs contain no such line.
- **Algorithm:** `CloudletSchedulerColaPri` resynchronises a class's counter to the frontier (largest
  tag already dispatched) when the class returns from idleness, as SFQ start tags do; `setResync(false)`
  restores the old rule for the new idle-return ablation in EXP-A. The bounded-waiting proposition is
  restated in terms of the class's own backlog at tagging time.
- **Baselines:** `CloudletSchedulerWFQ` is now reported as "EqualShare" (equal share per class, which is
  what it computes); CloudSim Plus's `CloudletSchedulerTimeShared` ("PS-timeshared", per-task processor
  sharing) added to EXP-A and EXP-E; the starvation scenario adds ColaPri 20:1, ColaPri 60:1 and SFQ
  60:1 rows (weight sweep along the FCFS–strict frontier).
- **Statistics:** confidence intervals use the Student *t* quantile for the seed count (was 1.96).
- **EXP-F protocol:** the fixed-θ grid (now {0,5,10,20,40,60,80,160,∞=FCFS}) and the ramp placement of
  the analytic and fuzzy policies are selected on the training seeds (0–9) and reported on the test
  seeds (10–29); an FCFS row (θ=∞) is added. The fuzzy controller is described as zero-order Sugeno.
- **Figures:** regenerated from the new logs; `fig_jain.pdf` and `fig_traces_speedup.pdf` removed (the
  Jain index is no longer reported in the paper, although the harness still prints it);
  `figures/make_figures.py` now writes next to itself when run from `figures/`.
- **Packaging:** `scripts/run_all.sh` mounts the bundled `data/` by default; `scripts/compare_logs.py`
  compares two logs ignoring the host-dependent overhead column; `data/checksums.sha256` regenerated to
  cover all 1252 bundled files (50 PlanetLab files with spaces in their names were missing).
- Metadata: version 1.1.0 in `CITATION.cff` / `.zenodo.json`; README numbers updated.

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
