# Third-party components and their licenses

The Apache License 2.0 in [LICENSE](LICENSE) covers the original code, scripts, configurations and
documentation written for this repository: the ColaPri scheduler, the baseline schedulers
re-implemented here, the task-to-VM mappers, metrics, seeded workload generator, trace loader and the
experiment harnesses. The simulator and the libraries below are **not redistributed** in this
repository — Maven resolves them at build time (inside the Docker image or locally), each under **its own
license**.

| Component | Role | License (verify upstream) |
|-----------|------|---------------------------|
| **CloudSim Plus 8.5.7** (`org.cloudsimplus:cloudsimplus`) | Discrete-event cloud simulation framework that ColaPri plugs into as a `CloudletScheduler` | **GPL-3.0** (as declared in its Maven POM) |
| Apache Commons Math 3 / Lang 3 / Collections 4 | transitive dependencies of CloudSim Plus | Apache-2.0 |
| Gson | transitive dependency of CloudSim Plus | Apache-2.0 |
| Logback (classic / core) and SLF4J | logging (silenced in the harnesses) | EPL-1.0 or LGPL-2.1 (Logback); MIT (SLF4J) |
| JetBrains annotations | transitive dependency of CloudSim Plus | Apache-2.0 |
| Maven 3.9 and Eclipse Temurin JDK 21 (`maven:3.9-eclipse-temurin-21` image) | build and run toolchain | Apache-2.0; GPLv2 with Classpath Exception |
| Python 3, matplotlib, numpy (`figures/make_figures.py`) | figure generation only | PSF; matplotlib (BSD-style); BSD-3-Clause |

Notes:

- **CloudSim Plus is GPL-3.0.** This repository distributes only our own source code (Apache-2.0),
  the captured experiment logs and documentation; it ships neither CloudSim Plus binaries nor a
  pre-built image. If you redistribute a *built* artifact that bundles CloudSim Plus (a jar or a Docker
  image), the GPL-3.0 terms apply to that combined work.
- The exact versions and licenses of transitive dependencies are those resolved by Maven from
  `pom.xml` at build time (`mvn dependency:list`).
- Workload traces are **data, not software**, and keep their own terms — see
  [data/PROVENANCE.md](data/PROVENANCE.md): the PlanetLab, Bitbrains and Materna subsets are bundled;
  Alibaba and Azure must be obtained from their origins.
