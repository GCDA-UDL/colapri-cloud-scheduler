# ColaPri — Class-Oriented Load-Adaptive Priority scheduler

A weighted-fair cloud **task scheduler** for [CloudSim Plus](https://cloudsimplus.org), with three
properties evaluated rigorously (30 seeds, 95% CI):

1. **No starvation** — bounds the waiting time of low-priority classes (**−67%** max-wait vs. strict priority).
2. **Tunable weighted differentiation** — per-class response ratio scales with the configured weight (**1.0 → 2.6**).
3. **Load-adaptive, zero-cost at light load** — below a backlog threshold it behaves as FCFS; above it, it differentiates. Scheduler overhead ≈ **1.7 ms**.

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
| `RigorousEval`     | EXP-A · 1-VM scheduling (30 seeds) | starvation / differentiation / adaptive, 6 schedulers |
| `DatacenterEval`   | EXP-B · multi-host, 16 VMs (20 seeds) | 4 mapping policies × {default, ColaPri} |
| `ScalabilityEval`  | EXP-C · 200/500/1000 cloudlets (10 seeds) | makespan, per-class response, overhead vs. scale |
| `SensitivityEval`  | EXP-D · parameter sweeps (30 seeds) | weight ratio, adaptive threshold, load, PEs |
| `RealTraceRigorous`| EXP-E · 5 real traces (20 seeds) | per-class response on PlanetLab/Alibaba/Bitbrains/Materna/Azure |

All metrics are reported as mean ± 95% CI.

### Real traces (EXP-E)

Trace data is **not bundled** (third-party datasets). Mount a directory at `/traces` with this layout
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
| `VmMappers`                       | RoundRobin / Min-Min / Max-Min / Demand-Aware task→VM mapping |

> Note: CloudSim Plus's built-in `CloudletSchedulerCompletelyFair` hangs under a sustained staggered
> backlog, so the fair reference here is the clean `CloudletSchedulerWFQ`.

## Figures

`figures/make_figures.py` regenerates the paper figures (PDF) from the measured numbers:

```bash
python figures/make_figures.py   # requires matplotlib
```

## Repository layout

```
src/main/java/colapri/   schedulers, mappers, metrics, seeded workload, trace loader, 5 experiment harnesses
figures/                 make_figures.py + generated PDFs
pom.xml                  Maven build (CloudSim Plus 8.5.7, Java 17)
```

## Citation

```bibtex
@article{colapri,
  title  = {ColaPri: a Class-Oriented Load-Adaptive Priority Scheduler with Starvation
            Guarantees for Cloud Task Scheduling, Evaluated in CloudSim Plus},
  author = {da Silva, V{\'i}tor and others},
  year   = {2026}
}
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
