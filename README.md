# ColaPri — Class-Oriented Load-Adaptive Priority scheduler (reproduction package)

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
![Reproducible with Docker](https://img.shields.io/badge/reproducible-Docker-2496ED.svg)
<!-- After the Zenodo deposit add: [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.XXXXXXX.svg)](https://doi.org/10.5281/zenodo.XXXXXXX) -->

Reproducible code, experiment harnesses, captured results and trace provenance for the paper:

> **ColaPri: a Starvation-Free, Class-Oriented Load-Adaptive Priority Scheduler for Cloud Task
> Scheduling** (submitted to *Simulation Modelling Practice and Theory*).

**Authors:** Vitor L. da Silva, Rosana Tomàs, Joaquim Oliva, Fernando Guirado — SEMBAC, Universitat de
Lleida, Spain.
Canonical repository: [GCDA-UDL/colapri-cloud-scheduler](https://github.com/GCDA-UDL/colapri-cloud-scheduler)
(mirror: `VitorDaSilvaUdL/colapri-cloud-scheduler`). Step-by-step instructions: [REPRODUCE.md](REPRODUCE.md).

A weighted-fair cloud **task scheduler** for [CloudSim Plus](https://cloudsimplus.org), with three
properties evaluated rigorously (up to 30 seeds, 95% CI):

1. **No starvation** — bounds the waiting time of low-priority classes (**−83%** max-wait vs. strict
   priority at the default 6:1 weights, with high-class response within 3% of FCFS; the weight moves
   ColaPri along the FCFS ↔ strict-priority frontier: 20:1 → −35%, 60:1 → −9% at +7% high-class response).
2. **Tunable weighted differentiation** — per-class response ratio scales with the configured weight (**1.0 → 2.4**).
3. **Load-adaptive, zero-cost when the backlog is small** — below a backlog threshold it behaves as FCFS (reproduced to the decimal against a θ=0 control at ρ≈1.1); above it, it differentiates. Scheduler overhead ≈ **1.4 ms**.

ColaPri is implemented as a `CloudletScheduler` using **persistent virtual-time tags**: the *k*-th tagged
task of class *c* gets tag *k/w_c*, assigned once, with *k* resynchronised to the frontier (largest tag
already dispatched) when a class returns from idleness. This realises per-class virtual-clock / stride
proportional sharing without starvation, and it is orthogonal to the task-to-VM mapping policy.

> The algorithmic core is classical proportional-share scheduling (WFQ, VirtualClock, DRR, WF²Q, SFQ,
> lottery/stride; 1989–1997). The contribution here is the length-agnostic, load-adaptive, per-class
> formulation with a per-task bound, the CloudSim Plus realisation and a rigorous cloud evaluation.

## Requirements

- **Option A (recommended):** Docker. No local Java needed.
- **Option B:** JDK 17+ and Maven 3.9+.

CloudSim Plus 8.5.7 is pulled automatically as a Maven dependency.

## Quick start

```bash
# Option A — Docker Compose (builds the image once, dependencies pre-fetched)
docker compose build
docker compose run --rm sim RigorousEval          # EXP-A; any other experiment main class works too

# Option A' — stock Maven image, no build step; -v m2 caches dependencies between runs
docker run --rm -v "$PWD:/workspace" -v colapri_m2:/root/.m2 -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -q compile exec:java -Dexec.mainClass=colapri.RigorousEval

# Option B — local
mvn -q compile exec:java -Dexec.mainClass=colapri.RigorousEval
```

## Experiments

Each is a `main` class; select it with `-Dexec.mainClass=colapri.<Class>`.

| Class | Experiment | What it shows |
|-------|------------|---------------|
| `RigorousEval`     | EXP-A · 1-VM scheduling (30 seeds) | per-class / starvation (with ColaPri and SFQ weight sweeps) / adaptive / idle-return (resync ablation), 9 schedulers |
| `DatacenterEval`   | EXP-B · multi-host, 16 VMs (20 seeds) | 4 mapping policies × {default, ColaPri} |
| `ScalabilityEval`  | EXP-C · 200/500/1000 cloudlets (10 seeds) | makespan, per-class response, overhead vs. scale |
| `SensitivityEval`  | EXP-D · parameter sweeps (30 seeds) | weight ratio, adaptive threshold, load, PEs |
| `RealTraceRigorous`| EXP-E · 5 real traces (20 seeds) | per-class response on PlanetLab/Alibaba/Bitbrains/Materna/Azure |
| `ActivationEval`   | EXP-F · automatic activation (10 train + 20 test seeds; calm → storm → drain → calm) | always-on / FCFS / fixed / analytic / fuzzy / learned + fixed-weight controls 3:1, 2:1, 1:1 |

All metrics are reported as mean ± 95% CI. **`results/EXP-{A..F}.log`** are the captured logs behind
every table in the paper. Simulated metrics (response, waiting, makespan) are seed-deterministic —
re-running reproduces them bit-identically; only the wall-clock `overhead.ms` column varies with the
host (reference host: Intel Core i7-14700KF, 32 GB RAM, Windows 11, Docker, Temurin JDK 21).
The `JFIw` / `JFI1` columns (EXP-A MAIN, EXP-B, EXP-C) are Jain's fairness index over the per-class
dispatches normalised by weight (`JFIw`, the proportional-share criterion) or by 1 (`JFI1`, equal share),
counted up to the first class drain (`Metrics.jainNormalisedService`).
Reproduce everything with one command: `./scripts/run_all.sh [/path/to/traces]`.

### Seeds / paper-table manifest

| Experiment | Seeds | Feeds |
|---|---|---|
| EXP-A | 0–29 (30) | per-class, starvation (weight sweep), gate (3 load levels × θ=15/θ=0/FCFS) and idle-return tables/figures, Jain-index table, abstract's 1.4 ms; paired per-seed differences |
| EXP-B | 0–19 (20) | datacenter mapping×scheduler table |
| EXP-C | 0–9 (10) | scalability table |
| EXP-D | 0–29 (30) | weight-ratio / threshold / load / PEs sweeps |
| EXP-E | 0–19 (20) | production-trace table (priority = burstiness tertiles per subsample) |
| EXP-F | train 0–9, test 10–29 | activation-policy table + θ sweep |

### Self-tuning activation (EXP-F)

`ActivationPolicy` turns ColaPri's threshold into an activation degree `α ∈ [0,1]` (0 = FIFO, 1 = full
weighted-RR), set by one of: `alwaysOn`, `fixed(θ)`, `analytic(lo,hi)` (closed-form ramp),
`fuzzy(lo,hi)` (zero-order Sugeno), or `learned(table,width)` (offline lookup). All are O(1) and
deterministic at inference. Every hyper-parameter (fixed θ, ramp placement, learned table) is selected on
the 10 training seeds and reported on the 20 test seeds. Because α scales the effective weight
(`1 + α·(w−1)`), EXP-F also runs fixed-weight controls (always on, 3:1 / 2:1 / 1:1): they trace the
response/starvation frontier along which the backlog-driven policies move. The cheap **analytic** ramp
lands at or slightly inside that frontier, beats every on/off threshold and the offline-**learned**
lookup — so **no ML is needed** to tune ColaPri.

### Real traces (EXP-E)

PlanetLab (`data/planetlab/`), Bitbrains and Materna (`data/bitbrains/`, `data/materna/`; GWA — free
for research with citation) are bundled; see `data/PROVENANCE.md`. Alibaba and Azure are third-party
datasets **not bundled** — `data/checksums.sha256` pins the SHA-256 of
every exact input file used in the paper, and `data/PROVENANCE.md` documents origins, licenses and the
derivation of the 100-VM subsets. Mount a directory at `/traces` with this layout
(CPU-utilisation series; the loader derives a priority class from each VM's burstiness):

```
/traces/planetlab/<dir>/        one integer per line
/traces/alibaba2018/<dir>/      CSV, cpu col 2
/traces/bitbrains/<dir>/        CSV ';', col 4
/traces/materna/<dir>/          CSV ';', col 4 (decimal comma)
/traces/azure/<dir>/            CSV, avg col 3
```

Sources: [PlanetLab](https://github.com/beloglazov/planetlab-workload-traces),
[Alibaba](https://github.com/alibaba/clusterdata), Bitbrains (GWA), Materna, Azure.
Adjust the dataset paths in `RealTraceRigorous.java` to match your mount, then:

```bash
docker run --rm -v "$PWD:/workspace" -v "/path/to/traces:/traces" -v colapri_m2:/root/.m2 \
  -w /workspace maven:3.9-eclipse-temurin-21 \
  mvn -q compile exec:java -Dexec.mainClass=colapri.RealTraceRigorous
```

## Schedulers and baselines

| File | Role |
|------|------|
| `CloudletSchedulerColaPri`        | **ColaPri** (weighted virtual-time tags with frontier resynchronisation, adaptive threshold, no starvation; `setResync(false)` = ablation) |
| `CloudletSchedulerStrictPriority` | strict priority (anti-baseline: starves low class) |
| `CloudletSchedulerPriorityAging`  | priority + aging (ad-hoc anti-starvation) |
| `CloudletSchedulerSJF`            | shortest-job-first |
| `CloudletSchedulerWFQ`            | equal share per class ("EqualShare" in the logs): ColaPri's tag rule with weight 1 for every class |
| `CloudletSchedulerTimeShared` (CloudSim Plus) | per-task processor sharing ("PS-timeshared" in the logs): the simulator's built-in fair reference |
| `CloudletSchedulerDFPO`           | DFPO (Saxena et al. 2016): three buckets, weighted-RR quanta 3:2:1 — closest weighted baseline |
| `CloudletSchedulerSFQ`            | start-time fair queueing (Goyal et al. 1997) with ColaPri's own weights 6:3:1 — classical weighted-FQ control (tags advance by length/weight, always on) |
| `VmMappers`                       | RoundRobin / Min-Min / Max-Min / Demand-Aware task→VM mapping |
| `CfsProbe`                        | probe showing that the built-in `CloudletSchedulerCompletelyFair` does not terminate under a staggered backlog (kept so the issue can be reproduced) |
| `CapacityProbe`                   | probe for the v1.0.x defect: cloudlets with `UtilizationModelFull` for RAM/BW are throttled to one PE and one per VM may never finish (kept as evidence) |
| `VmPatched`                       | `VmSimple` with a two-line workaround for a CloudSim Plus 8.5.7 corner case (`updateProcessing` returning 0 ends the simulation with cloudlets queued); every harness uses it and asserts that all submitted cloudlets finish (`Metrics.checked`) |
| `Workload.cloudlet(len)`          | factory for CPU-bound cloudlets (RAM/BW demand 1% of the VM) used by every harness |

> Note: CloudSim Plus's built-in `CloudletSchedulerCompletelyFair` hangs under a sustained staggered
> backlog, so the fair references here are `CloudletSchedulerTimeShared`, `CloudletSchedulerWFQ`
> (equal share per class) and the weighted `CloudletSchedulerSFQ` control.

## Figures

`figures/make_figures.py` regenerates the paper figures (PDF) from the measured numbers:

```bash
python figures/make_figures.py   # requires matplotlib
```

## Repository layout

```
src/main/java/colapri/   schedulers (ColaPri + 8 baselines), mappers, metrics, seeded workload,
                         trace loader, 6 experiment harnesses, CfsProbe
results/                 captured logs (EXP-A..F) behind every table in the paper
data/                    bundled PlanetLab / Bitbrains / Materna subsets + checksums.sha256 + PROVENANCE.md
scripts/run_all.sh       one-command reproduction of all experiments (stock Maven image)
Dockerfile, docker-compose.yml   reproduction image (deps pre-fetched) and `docker compose run sim <Exp>`
REPRODUCE.md             step-by-step reproduction guide; THIRD_PARTY.md: third-party licenses
figures/                 make_figures.py + generated PDFs
pom.xml                  Maven build (CloudSim Plus 8.5.7, Java 17)
CITATION.cff, .zenodo.json, CHANGELOG.md   citation + Zenodo deposit metadata + release notes
```

## Citation

```bibtex
@article{colapri2026,
  title   = {ColaPri: a Starvation-Free, Class-Oriented Load-Adaptive Priority Scheduler
             for Cloud Task Scheduling},
  author  = {da Silva, Vitor Luiz and Tom{\`a}s, Rosana and Oliva, Joaquim and Guirado, Fernando},
  journal = {Simulation Modelling Practice and Theory},
  year    = {2026},
  note    = {Under review. Software: https://github.com/GCDA-UDL/colapri-cloud-scheduler (Zenodo DOI pending)}
}
```

See also `CITATION.cff` (machine-readable, picked up by GitHub and Zenodo).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
