# omim-annotation-pipeline
Load MIM ids and then create MIM disease annotations.

OMIM LOADER

 - MIM_PS (MIM Phenotypic Series) entries are parsed and updated in table OMIM_PHENOTYPIC_SERIES

 - inactive MIM ids that are still in RGD, are reported

OMIM ANNOTATIONS

 - 'susceptibility' qualifier is set for all annotations associated with MIM phenotypes that have word 'susceptibility'
   in the phenotype name

