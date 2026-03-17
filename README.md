# omim-annotation-pipeline

Loads OMIM (Online Mendelian Inheritance in Man) data into RGD and creates disease annotations linking RDO terms to genes.

The pipeline has three independently runnable components:
- **OMIM Loader** (default) — loads MIM IDs and gene-phenotype mappings
- **OMIM Annotator** (`-annotations`) — creates RDO disease annotations
- **Phenotypic Series Loader** (`-phenotypic_series`) — maintains PS-to-phenotype mappings

## OMIM Loader

Downloads four data files from OMIM (mim2gene, mimTitles, genemap2, morbidmap) and loads them into RGD.

1. **Parse and merge** — Each file contributes different fields to `OmimRecord` objects: mim2gene provides MIM-to-gene links, mimTitles provides status (live/moved/removed), genemap2 provides genomic coordinates, and morbidmap provides phenotype text and phenotype-to-gene mappings.

2. **Gene matching** — For each record, the QC processor matches to RGD genes using a hierarchy: NCBI Gene ID (primary), gene symbol, genomic locus intersection, then alternate gene symbols/aliases. Only single-match records are accepted.

3. **Database sync** — Inserts or updates MIM IDs in the XDBID table (linked to human genes), updates the OMIM table with phenotype/status/type metadata, and maintains the OMIM_GENE2PHENOTYPE junction table. Stale records not seen in the current run are deleted.

4. **Reporting** — Inactive MIM IDs that still have associations in RGD are logged for review.

## OMIM Annotator

Creates disease annotations for RDO (Rare Disease Ontology) terms based on OMIM phenotype data.

1. **Find MIM IDs** — For each active RDO term, searches its synonyms for MIM IDs (entries starting with "MIM:"). Phenotypic series IDs (MIM:PSxxxxx) are skipped.

2. **Map phenotypes to genes** — Converts phenotype MIM IDs to gene MIM IDs via the OMIM_GENE2PHENOTYPE table, then looks up corresponding RGD genes.

3. **Create annotations** — Builds primary annotations with evidence code IAGP for direct gene matches, and ISO annotations for active orthologs across species. If the phenotype text contains "susceptibility", the qualifier is set accordingly.

4. **Database sync** — Inserts new annotations, updates timestamps on existing matches, and deletes stale annotations older than 1 hour (with a 5% safety threshold to prevent mass deletion).

## Phenotypic Series Loader

Downloads the phenotypicSeries.txt file from OMIM and maintains the OMIM_PHENOTYPIC_SERIES table, which maps phenotypic series (PS) IDs to individual phenotype MIM IDs. Aborts if the incoming file is empty as a safety measure.
