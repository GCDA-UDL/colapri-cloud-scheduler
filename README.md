# ColaPri — Class-Oriented Load-Adaptive Priority scheduler

A weighted-fair cloud **task scheduler** for [CloudSim Plus](https://cloudsimplus.org), with three
properties evaluated rigorously (30 seeds, 95% CI):

1. **No starvation** — bounds the waiting time of low-priority classes (**−67%** max-wait vs. strict priority).
2. **Tunable weighted differentiation** — per-class response ratio scales with the configured weight (**1.0 → 2.6**).
3. **Load-adaptive, zero-cost at light load** — below a backlog threshold it behaves as FCFS; above it, it differentiates. Scheduler overhead ≈ **1.8 ms**.

ColaPri is implemented as a `CloudletScheduler` using **persistent virtual-time tags** (the *k*-th arrival
of class *c* gets tag *k/wₑ*, assigned once), which realises weighted round-robin without starvation.
It is orthogonal to the task-to-VM mapping policy.

> The algorithmic core is Weighted Fair Queuing / Weighted Round-Robin (WFQ, DRR, GPS; 1989–1996). The
> contribution here is the adaptive, no-starvation, per-class formulation and a rigorous cloud evaluation.

## Requirements

- **Option A (recommended):** Docker. No local Java needed.
- **Option B:** JDK 17+ and Maven 3.9+.

CloudSim Plus 8.5.7 is pulled automatically as a Maven dependency.

## Quick start

```bash
# Option A — Docker (Maven + JDK 21 image); -v m2 caches dependencies between runs
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
| `RigorousEval`     | EXP-A · 1-VM scheduling (30 seeds) | starvation / differentiation / adaptive, 8 schedulers (incl. the weighted-SFQ control) |
| `DatacenterEval`   | EXP-B · multi-host, 16 VMs (20 seeds) | 4 mapping policies × {default, ColaPri} |
| `ScalabilityEval`  | EXP-C · 200/500/1000 cloudlets (10 seeds) | makespan, per-class response, overhead vs. scale |
| `SensitivityEval`  | EXP-D · parameter sweeps (30 seeds) | weight ratio, adaptive threshold, load, PEs |
| `RealTraceRigorous`| EXP-E · 5 real traces (20 seeds) | per-class response on PlanetLab/Alibaba/Bitbrains/Materna/Azure |
| `ActivationEval`   | EXP-F · automatic activation (10 train + 20 test seeds) | always-on / fixed / analytic / fuzzy / learned threshold |

All metrics are reported as mean ± 95% CI. **`results/EXP-{A..F}.log`** are the captured logs behind
every table in the paper. Simulated metrics (response, waiting, makespan) are seed-deterministic —
re-running reproduces them bit-identically; only the wall-clock `overhead.ms` column varies with the
host (reference host: Intel Core i7-14700KF, 32 GB RAM, Windows 11, Docker, Temurin JDK 21).
Reproduce everything with one command: `./scripts/run_all.sh [/path/to/traces]`.

### Seeds / paper-table manifest

| Experiment | Seeds | Feeds |
|---|---|---|
| EXP-A | 0–29 (30) | per-class + starvation tables/figures (incl. the weighted-SFQ control), abstract's 1.8 ms |
| EXP-B | 0–19 (20) | datacenter mapping×scheduler table |
| EXP-C | 0–9 (10) | scalability table |
| EXP-D | 0–29 (30) | weight-ratio / threshold / load / PEs sweeps |
| EXP-E | 0–19 (20) | production-trace table (priority = burstiness tertiles per subsample) |
| EXP-F | train 0–9, test 10–29 | activation-policy table + θ sweep |

### Self-tuning activation (EXP-F)

`ActivationPolicy` turns ColaPri's threshold into an activation degree `α ∈ [0,1]` (0 = FIFO, 1 = full
weighted-RR), set by one of: `alwaysOn`, `fixed(θ)`, `analytic(lo,hi)` (closed-form ramp),
`fuzzy(lo,hi)` (Mamdani), or `learned(table,width)` (offline lookup). All are O(1) and deterministic at
inference. EXP-F shows the cheap **analytic** controller matches the offline-**learned** lookup and a
hand-tuned fixed θ within CI — so **no ML is needed** to tune ColaPri.

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
| `CloudletSchedulerColaPri`        | **ColaPri** (weighted virtual-time tags, adaptive threshold, no starvation) |
| `CloudletSchedulerStrictPriority` | strict priority (anti-baseline: starves low class) |
| `CloudletSchedulerPriorityAging`  | priority + aging (ad-hoc anti-starvation) |
| `CloudletSchedulerSJF`            | shortest-job-first |
| `CloudletSchedulerWFQ`            | equal-weight weighted fair queuing (fair-share reference) |
| `CloudletSchedulerDFPO`           | DFPO (Saxena et al. 2016): three buckets, weighted-RR quanta 3:2:1 — closest weighted baseline |
| `CloudletSchedulerSFQ`            | start-time fair queueing (Goyal et al. 1997) with ColaPri's own weights 6:3:1 — classical weighted-FQ control (tags advance by length/weight, always on) |
| `VmMappers`                       | RoundRobin / Min-Min / Max-Min / Demand-Aware task→VM mapping |
| `CfsProbe`                        | probe showing that the built-in `CloudletSchedulerCompletelyFair` does not terminate under a staggered backlog (kept so the issue can be reproduced) |

> Note: CloudSim Plus's built-in `CloudletSchedulerCompletelyFair` hangs under a sustained staggered
> backlog, so the fair reference here is the clean `CloudletSchedulerWFQ`.

## Figures

`figures/make_figures.py` regenerates the paper figures (PDF) from the measured numbers:

```bash
python figures/make_figures.py   # requires matplotlib
```

## Repository layout

```
src/main/java/colapri/   schedulers (ColaPri + 7 baselines), mappers, metrics, seeded workload,
                         trace loader, 6 experiment harnesses
results/                 captured logs (EXP-A..F) behind every table in the paper
data/                    bundled PlanetLab traces + checksums.sha256 + PROVENANCE.md
scripts/run_all.sh       one-command reproduction of all experiments
figures/                 make_figures.py + generated PDFs
pom.xml                  Maven build (CloudSim Plus 8.5.7, Java 17)
CITATION.cff, .zenodo.json   citation + Zenodo deposit metadata
```

## Citation

```bibtex
@article{colapri,
  title  = {ColaPri: a Starvation-Free, Class-Oriented Load-Adaptive Priority Scheduler
            for Cloud Task Scheduling, Evaluated in CloudSim Plus},
  author = {da Silva, V{\'i}tor and others},
  year   = {2026}
}
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
