# Zenodo deposit — checklist (estado 2026-07-24)

## ✅ Ya en el paquete
- [x] 17 fuentes Java (ColaPri + 6 baselines incl. **DFPO nuevo** + 4 mappers + 6 harnesses)
- [x] `results/EXP-{A..F}.log` — logs capturados detrás de CADA tabla del paper (con CI95)
- [x] `data/planetlab/20110303` empaquetado (4.8 MB, redistribuible)
- [x] `data/checksums.sha256` — SHA-256 de los 1402 ficheros de entrada exactos (5 datasets)
- [x] `data/PROVENANCE.md` — origen, licencia y derivación de cada traza
- [x] `scripts/run_all.sh` — reproducción completa con un comando
- [x] `CITATION.cff` + `.zenodo.json` (autores+ORCIDs, afiliación nueva, MICIU PID2023-146193OB-I00)
- [x] README: fila DFPO, manifest seeds→tabla, hardware de referencia, sección results/
- [x] LICENSE (Apache-2.0) + NOTICE

## ✅ Decidido y ejecutado (2026-07-24)
- [x] Título del paper y del paquete → "Starvation-Free, Class-Oriented Load-Adaptive Priority Scheduler"
- [x] Bitbrains + Materna (GWA) **empaquetadas** en `data/` (libre para investigación con cita);
      Alibaba/Azure fuera (solo checksums)
- [x] Commit + push + tag `v1.0.0` + GitHub Release

## ⬜ Te queda a ti
1. **Activar la integración GitHub–Zenodo** en zenodo.org (necesita tu login) y hacer el depósito
   desde la release v1.0.0 — o subir el tarball a mano. Copiar el DOI resultante a `CITATION.cff`,
   README y al paper (Data availability).
2. Al aceptarse el paper: poner el DOI de la revista en `.zenodo.json` (`related_identifiers`),
   `CITATION.cff` (`preferred-citation`) y el bibtex del README.
3. Opcional: pin de imagen Docker por digest en `run_all.sh`
   (`maven:3.9-eclipse-temurin-21@sha256:...`).

## Nota
`RESULTADOS_faseA.md` (fuera del repo) queda como cuaderno histórico; la fuente canónica de números
es `results/*.log` (regenerados 2026-07-24, medias idénticas a las originales por determinismo de seeds,
overhead re-medido en el host de referencia).
