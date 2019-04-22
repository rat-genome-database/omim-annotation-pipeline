# omim-annotation-pipeline
Loads OMIM ids and then creates OMIM disease annotations.

OMIM LOADER

 - OMIM_PS (OMIM Phenotypies Series) entries are parsed and updated in table OMIM_PHENOTYPIC_SERIES

 - inactive OMIM ids that are still in RGD, are reported

OMIM ANNOTATIONS

 - 'susceptibility' qualifier is set for all annotations associated with OMIM phenotypes that have word 'susceptibility'
   in the phenotype name

