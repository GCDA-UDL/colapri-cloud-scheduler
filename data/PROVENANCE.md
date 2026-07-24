# Trace data — provenance, licensing and verification

EXP-E consumes five trace directories, mounted at `/traces` (layout in the main README).
`checksums.sha256` in this folder lists the SHA-256 of **every input file actually used** in the
paper's experiments (1402 files). Verify with:

```bash
cd /path/to/traces && sha256sum -c /path/to/data/checksums.sha256
```

| Dataset | Directory used | Origin | License / terms | In package? |
|---|---|---|---|---|
| PlanetLab | `planetlab/20110303` | [beloglazov/planetlab-workload-traces](https://github.com/beloglazov/planetlab-workload-traces) (the trace set long redistributed with CloudSim; cite Beloglazov & Buyya 2012) | no explicit license file at origin; de-facto freely redistributed with CloudSim for over a decade | **yes** (`data/planetlab/20110303`, 4.8 MB, unmodified) |
| Alibaba | `alibaba2018/alibaba2018_fixed_first_500_100_mostDiff` | [alibaba/clusterdata](https://github.com/alibaba/clusterdata) cluster-trace-v2018 | Alibaba's own data agreement — redistribution not clearly permitted | **no** — obtain from origin; checksums here identify the exact derived files |
| Bitbrains | `bitbrains/bitbrains_rnd_2013-8_100_mostDiff_short` | Grid Workloads Archive GWA-T-12 (TU Delft) | free for research with citation | **yes** (`data/bitbrains/...`, 7 MB derived subset; cite the GWA) |
| Materna | `materna/materna_trace1_valid_0_288_100_mostDiff` | Grid Workloads Archive GWA-T-13 | free for research with citation | **yes** (`data/materna/...`, 3 MB derived subset; cite the GWA) |
| Azure | `azure/azure_azure2019_traces_100_mostDiff` | [Azure/AzurePublicDataset](https://github.com/Azure/AzurePublicDataset) (2019) | Microsoft's dataset terms — redistribution not clearly permitted | **no** — obtain from origin; checksums here |

## Derived subsets

The two non-bundled directories (Alibaba, Azure) are **deterministic derivations** of the origin datasets produced
by the companion preprocessing pipeline (VM subset selection by maximum pairwise difference,
"`mostDiff`", plus trace-window fixes). The derivation parameters are encoded in the directory
names (`fixed_first_500`, `valid_0_288`, `100_mostDiff`, `short`). The exact derived files used in
the paper are pinned by `checksums.sha256`; researchers who cannot regenerate them from origin can
request the derived subsets from the corresponding author (redistribution restrictions permitting).

## Class assignment

Priority classes are NOT part of the data: EXP-E derives them at load time from each VM's CPU
burstiness (tertiles within the seeded 80-VM subsample; see `TraceLoader.sample`), so no label
files are needed.
