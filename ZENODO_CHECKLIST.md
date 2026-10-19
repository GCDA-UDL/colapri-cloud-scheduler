# Zenodo deposit — checklist (estado 2026-10-19)

Repo canónico: https://github.com/GCDA-UDL/colapri-cloud-scheduler (mirror personal:
`VitorDaSilvaUdL/colapri-cloud-scheduler`, mismo historial).

> La guía de autores de SIMPAT (*Research data*, **Option C**) exige depositar los datos/código en un
> repositorio y **citarlos y enlazarlos en el artículo** (o justificar por qué no). El depósito en Zenodo
> debe estar hecho, por tanto, **antes de enviar** el manuscrito — el paper ya cita `colapri_zenodo` en
> *Data availability* y *Reproducibility*; solo falta el DOI.

## ✅ Ya en el paquete (v1.0.1)
- [x] Código: ColaPri + 7 baselines (FCFS, SJF, strict, aging, WFQ-fair, DFPO, **SFQ ponderado**) + 4 mappers
      + 6 harnesses (EXP-A..F) + `CfsProbe`
- [x] `results/EXP-{A..F}.log` — logs capturados detrás de CADA tabla del paper (CI95); A y E recapturados
      en v1.0.1 (filas de v1.0.0 reproducidas bit a bit; solo cambia `overhead.ms`)
- [x] `data/`: PlanetLab + Bitbrains + Materna empaquetados; `checksums.sha256` (1402 ficheros de los 5
      datasets); `PROVENANCE.md` (Alibaba/Azure no redistribuibles → obtener en origen)
- [x] `Dockerfile` + `docker-compose.yml` + `scripts/run_all.sh` + `REPRODUCE.md` + `THIRD_PARTY.md` + `CHANGELOG.md`
- [x] `CITATION.cff` (version 1.0.1) + `.zenodo.json`: autores **da Silva · Tomás · Oliva · Guirado** con ORCIDs y
      afiliación; MICIU AEI/10.13039/501100011033, PID2023-146193OB-I00
- [x] LICENSE (Apache-2.0) + NOTICE

## ⬜ Cuando el paquete esté maduro (y antes del envío a SIMPAT)
1. **Activar la integración GitHub–Zenodo** para el repo de la organización: zenodo.org → *GitHub* →
   encender `GCDA-UDL/colapri-cloud-scheduler` (hace falta ser admin del repo/organización en GitHub y
   el login de Zenodo que vaya a ser dueño del registro).
2. Publicar una **nueva release** en GitHub *después* de activar la integración (Zenodo solo archiva las
   releases posteriores a la activación): p. ej. `v1.1.0` con las notas de `CHANGELOG.md`. Zenodo crea el
   registro y el **concept DOI** (el que se cita; nunca el DOI de versión).
3. Copiar el concept DOI a: `paper/refs.bib` (`colapri_zenodo`, añadir `doi={10.5281/zenodo.XXXXXXX}` y
   quitar la nota "pending"), `CITATION.cff` (`doi:`), README (badge comentado en la cabecera + sección
   *Citation*), `.zenodo.json` no hace falta (lo rellena Zenodo).
4. En Editorial Manager: enlazar el repositorio/DOI en el *Data statement* (Option C).
5. Al aceptarse el paper: DOI de la revista en `.zenodo.json` (`related_identifiers`, `isSupplementTo`),
   `CITATION.cff` (`preferred-citation`) y el bibtex del README.
6. Opcional: pin de la imagen Docker por digest en `Dockerfile` y `scripts/run_all.sh`
   (`maven:3.9-eclipse-temurin-21@sha256:...`).

## Nota
Fuente canónica de números = `results/*.log`. `RESULTADOS_faseA.md` (fuera del repo) es solo cuaderno
histórico.
