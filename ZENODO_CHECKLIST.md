# Zenodo deposit — checklist (estado 2026-10-21)

Repo canónico: https://github.com/GCDA-UDL/colapri-cloud-scheduler (mirror personal:
`VitorDaSilvaUdL/colapri-cloud-scheduler`, mismo historial). Depósito **vía integración GitHub–Zenodo**
(decisión del 2026-10-21: no se usa la API con token; el registro lo crea Zenodo a partir de una release).

> La guía de autores de SIMPAT (*Research data*, **Option C**) exige depositar los datos/código en un
> repositorio y **citarlos y enlazarlos en el artículo**. El depósito debe estar hecho **antes de enviar**
> el manuscrito — el paper ya cita `colapri_zenodo` en *Data availability* y *Reproducibility*; solo
> falta el DOI.

## ✅ Ya en el paquete (main, metadatos 1.1.1)
- [x] Código: ColaPri (con resincronización) + 8 baselines (FCFS, SJF, strict, aging, EqualShare,
      PS-timeshared, DFPO, SFQ ponderado) + 4 mappers + 6 harnesses (EXP-A..F) + `CfsProbe`, `CapacityProbe`,
      `VmPatched`
- [x] `results/EXP-{A..F}.log` — logs del harness corregido (v1.1.0) detrás de CADA tabla del paper;
      `scripts/compare_logs.py` para compararlos ignorando `overhead.ms`
- [x] `data/`: PlanetLab + Bitbrains + Materna empaquetados; `checksums.sha256` (1252 empaquetados + 200 no
      redistribuibles); `PROVENANCE.md`
- [x] `Dockerfile` + `docker-compose.yml` + `scripts/run_all.sh` (monta `data/` por defecto) + `REPRODUCE.md` +
      `THIRD_PARTY.md` + `CHANGELOG.md`
- [x] `CITATION.cff` + `.zenodo.json` (version **1.1.1**): autores **da Silva · Tomás · Oliva · Guirado** con
      ORCIDs y afiliación **SEMBAC, Universitat de Lleida**; licencia Apache-2.0; MICIU
      AEI/10.13039/501100011033, PID2023-146193OB-I00 en la descripción y en `notes` (no se usa el campo
      `grants`: si Zenodo no encuentra la ayuda, la integración falla el depósito)
- [x] Release `v1.1.0` publicada **antes** de activar la integración → Zenodo NO la archiva. La que se
      deposita es la siguiente (`v1.1.1`, solo metadatos: afiliación SEMBAC).

## ⬜ Pasos (en este orden)
1. **Activar la integración**: zenodo.org → *Account* → *GitHub* (https://zenodo.org/account/settings/github/)
   → *Sync now* → encender el interruptor de `GCDA-UDL/colapri-cloud-scheduler`. Hace falta que el usuario de
   Zenodo tenga vinculada la cuenta GitHub con acceso de admin a la organización `GCDA-UDL` (si el repo no
   aparece, autorizar la app de Zenodo para la organización en GitHub → *Settings* → *Applications*).
2. **Publicar la release** `v1.1.1` en GitHub (después de activar; Zenodo solo archiva releases posteriores):
   ```bash
   cd paper5_colapri/colapri-cloud-scheduler
   awk '/^## v1.1.1/{f=1} /^## v1.1.0/{f=0} f' CHANGELOG.md > /tmp/relnotes.md
   gh release create v1.1.1 --repo GCDA-UDL/colapri-cloud-scheduler \
     --title "v1.1.1 — SEMBAC affiliation metadata (Zenodo deposit)" --notes-file /tmp/relnotes.md
   ```
   Zenodo recibe el webhook, lee `.zenodo.json` y crea el registro con **concept DOI** (el que se cita;
   nunca el DOI de versión). Comprobar en zenodo.org → *GitHub* que la release aparece con el badge.
3. Copiar el concept DOI a: `paper/refs.bib` (`colapri_zenodo`: añadir `doi={10.5281/zenodo.XXXXXXX}`,
   version 1.1.1, quitar la nota "pending"), `CITATION.cff` (`doi:`), README (badge en la cabecera + sección
   *Citation*). `.zenodo.json` no cambia. Commit "Add Zenodo DOI" en main (no requiere nueva release).
4. En Editorial Manager: enlazar el DOI en el *Data statement* (Option C).
5. Al aceptarse el paper: DOI de la revista en `.zenodo.json` (`related_identifiers`, `isSupplementTo`),
   `CITATION.cff` (`preferred-citation`) y el bibtex del README; nueva release para que Zenodo lo recoja.
6. Opcional: pin de la imagen Docker por digest en `Dockerfile` y `scripts/run_all.sh`.

## Nota
Fuente canónica de números = `results/*.log` (harness corregido, 2026-10-20). Los logs anteriores a v1.1.0
quedaron invalidados por el defecto C-01 (ver `CHANGELOG.md`).
