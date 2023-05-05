package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.dao.spring.IntListQuery;
import edu.mcw.rgd.dao.spring.StringMapQuery;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.Omim;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.ontologyx.TermSynonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * @author mtutaj
 * @since Apr 29, 2011
 * all interactions with database is handled by this class
 */
public class OmimDAO {

    protected final Logger logDeleted = LogManager.getLogger("omim_deleted");
    protected final Logger logInserted = LogManager.getLogger("omim_inserted");
    protected final Logger logAnnotsDeleted = LogManager.getLogger("annots_deleted");
    protected final Logger logAnnotsInserted = LogManager.getLogger("annots_inserted");

    AnnotationDAO annotationDAO = new AnnotationDAO();
    GeneDAO geneDAO = new GeneDAO();
    edu.mcw.rgd.dao.impl.OmimDAO omimDAO = new edu.mcw.rgd.dao.impl.OmimDAO();
    OntologyXDAO ontologyXDAO = new OntologyXDAO();
    XdbIdDAO xdbIdDAO = new XdbIdDAO();

    public String getConnectionInfo() {
        return geneDAO.getConnectionInfo();
    }

    /**
     * get active human genes given NCBI gene id
     * @param geneId NCBI gene id
     * @return list of Gene objects (could be empty list)
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getGenesByNcbiGeneID(String geneId) throws Exception {

        // get genes by NCBI gene id
        List<Gene> genes = xdbIdDAO.getActiveGenesByXdbId(XdbId.XDB_KEY_NCBI_GENE, geneId);
        // filter out non-human genes
        genes.removeIf(gene -> gene.getSpeciesTypeKey() != SpeciesType.HUMAN);
        return genes;
    }

    /**
     * get all active human genes given the symbol
     * @param symbol gene symbol
     * @return List of Gene objects; could be empty
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getGenesBySymbol(String symbol) throws Exception {
        return geneDAO.getActiveGenes(SpeciesType.HUMAN, symbol);
    }

    public List<Gene> getGenesByAlias(String alias) throws Exception {
        return geneDAO.getGenesByAlias(alias, SpeciesType.HUMAN);
    }

    /**
     * Returns a Gene based on an rgd id
     * @param rgdId rgd id
     * @return Gene object for given rgd id
     * @throws Exception thrown when there is no gene with such rgd id
     */
    public Gene getGene(int rgdId) throws Exception {
        return geneDAO.getGene(rgdId);
    }

    /**
     *  get all ortholog genes (for active genes)
     *
     * @param rgdId rgd id
     * @return list of Gene objects
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<Gene> getActiveOrthologs(int rgdId) throws Exception{
        return geneDAO.getActiveOrthologs(rgdId);
    }

    /**
     * get list of omim xdbids for given gene
     * @param geneRgdId rgd id of a gene
     * @return list of XdbId objects; empty list possible
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<XdbId> getOmimIdsForGene(int geneRgdId) throws Exception {
        return xdbIdDAO.getXdbIdsByRgdId(XdbId.XDB_KEY_OMIM, geneRgdId);
    }

    /**
     * get list of omim xdbids for given omim id
     * @param omimId value of omim id (accession id)
     * @return list of XdbId objects; empty list possible
     * @throws Exception when unexpected error in spring framework occurs
     */
    public List<XdbId> getXdbIdsForOmimId(String omimId) throws Exception {

        // setup a filter
        XdbId filter = new XdbId();
        filter.setAccId(omimId);
        filter.setSrcPipeline("OMIM");
        filter.setXdbKey(XdbId.XDB_KEY_OMIM);

        // get the results
        return xdbIdDAO.getXdbIds(filter, SpeciesType.HUMAN);
    }

    public List<XdbId> getOmimIdsModifiedBefore(java.util.Date modDate) throws Exception {

        return xdbIdDAO.getXdbIdsModifiedBefore(XdbId.XDB_KEY_OMIM, "OMIM", modDate);
    }

    /**
     * insert a bunch of OMIM IDs into rgd
     * @param omims list of XdbId objects to be inserted into rgd
     * @return number of rows actually inserted
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int insertOmims(List<XdbId> omims) throws Exception {

        for( XdbId xdbId: omims ) {
            logInserted.info(xdbId.dump("|"));
        }
        return xdbIdDAO.insertXdbs(omims);
    }

    /**
     * delete a bunch of OMIM IDs from rgd
     * @param omims list of XdbId objects to be removed from rgd
     * @return number of rows actually deleted
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int deleteOmims(List<XdbId> omims) throws Exception {

        for( XdbId xdbId: omims ) {
            logDeleted.info(xdbId.dump("|"));
        }
        return xdbIdDAO.deleteXdbIds(omims);
    }

    /**
     * for a bunch of rows identified by acc_xdb_key, set MODIFICATION_DATE to SYSDATE
     * @param accXdbKeys list of ACC_XDB_KEYs
     * @return number of actually updated rows
     * @throws Exception when unexpected error in spring framework occurs
     */
    public int updateOmims(List<Integer> accXdbKeys) throws Exception {

        return xdbIdDAO.updateModificationDate(accXdbKeys);
    }

    /**
     * get all active terms in given ontology
     * @return List of Term objects
     * @throws Exception if something wrong happens in spring framework
     */
    public List<Term> getActiveRDOTerms() throws Exception {
        return ontologyXDAO.getActiveTerms("RDO");
    }

    /**
     * get list of all synonyms for given term
     * @param termAcc term accession id
     * @return list of all synonyms; could be empty list
     * @throws Exception if something wrong happens in spring framework
     */
    public List<TermSynonym> getTermSynonyms(String termAcc) throws Exception {

        return ontologyXDAO.getTermSynonyms(termAcc);
    }

    public List<Term> getRdoTermsBySynonym(String synonymName) throws Exception {
        return ontologyXDAO.getTermsBySynonym("RDO", synonymName, "exact");
    }

    /**
     * get annotation by a list of values that comprise unique key:
     * TERM_ACC+ANNOTATED_OBJECT_RGD_ID+REF_RGD_ID+EVIDENCE+WITH_INFO+QUALIFIER+XREF_SOURCE
     * @param annot Annotation object with the following fields set: TERM_ACC+ANNOTATED_OBJECT_RGD_ID+REF_RGD_ID+EVIDENCE+WITH_INFO+QUALIFIER+XREF_SOURCE
     * @return Annotation object or null if invalid key
     * @throws Exception on spring framework dao failure
     */
    public Annotation getAnnotation(Annotation annot) throws Exception {
        return annotationDAO.getAnnotation(annot);
    }

    /**
     * Insert new annotation into FULL_ANNOT table; full_annot_key will be set
     *
     * @param annot Annotation object representing column values
     * @throws Exception
     * @return value of new full annot key
     */
    public int insertAnnotation(Annotation annot) throws Exception{

        int annotKey = annotationDAO.insertAnnotation(annot);
        logAnnotsInserted.debug(annot.dump("|"));
        return annotKey;
    }

    /**
     * update last modified date for annotation given full annot key
     * @param annot Annotation object
     * @return count of rows affected
     * @throws Exception on spring framework dao failure
     */
    public int updateLastModified(Annotation annot) throws Exception{
        return annotationDAO.updateLastModified(annot.getKey());
    }

    /**
     * delete all pipeline annotations older than given date
     *
     * @return count of annotations deleted
     * @throws Exception on spring framework dao failure
     */
    public int deleteObsoleteAnnotations(int createdBy, Date dt, String staleAnnotDeleteThresholdStr, int refRgdId, String dataSource) throws Exception{

        Logger logStatus = LogManager.getLogger("annots");

        // convert delete-threshold string to number; i.e. '5%' --> '5'
        int staleAnnotDeleteThresholdPerc = Integer.parseInt(staleAnnotDeleteThresholdStr.substring(0, staleAnnotDeleteThresholdStr.length()-1));
        // compute maximum allowed number of stale annots to be deleted
        int annotCount = annotationDAO.getCountOfAnnotationsByReference(refRgdId, dataSource, "D");
        int staleAnnotDeleteLimit = (staleAnnotDeleteThresholdPerc * annotCount) / 100;

        List<Annotation> staleAnnots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(createdBy, dt, "D");

        logStatus.info("ANNOTATIONS_COUNT: "+annotCount);
        if( staleAnnots.size()> 0 ) {
            logStatus.info("   stale annotation delete limit (" + staleAnnotDeleteThresholdStr + "): " + staleAnnotDeleteLimit);
            logStatus.info("   stale annotations to be deleted: " + staleAnnots.size());
        }

        if( staleAnnots.size()> staleAnnotDeleteLimit ) {
            logStatus.warn("*** DELETE of stale annots aborted! *** "+staleAnnotDeleteThresholdStr+" delete threshold exceeded!");
            return 0;
        }

        List<Integer> staleAnnotKeys = new ArrayList<>();
        for( Annotation ann: staleAnnots ) {
            logAnnotsDeleted.debug("DELETE "+ann.dump("|"));
            staleAnnotKeys.add(ann.getKey());
        }
        return annotationDAO.deleteAnnotations(staleAnnotKeys);
    }

    public List<StringMapQuery.MapPair> getPhenotypicSeriesMappings() throws Exception {
        return omimDAO.getPhenotypicSeriesMappings();
    }

    public void insertPhenotypicSeriesMapping( String psNumber, String phenotypeMimNumber ) throws Exception {
        omimDAO.insertPhenotypicSeriesMapping(psNumber, phenotypeMimNumber);
    }

    public void deletePhenotypicSeriesMapping( String psNumber, String phenotypeMimNumber ) throws Exception {
        omimDAO.deletePhenotypicSeriesMapping(psNumber, phenotypeMimNumber);
    }

    public List<String> getPhenotypicSeriesIdsNotInRgd() throws Exception {
        return omimDAO.getPhenotypicSeriesIdsNotInRgd();
    }

    /**
     *
     * @param mimNr
     * @param phenotype
     * @param status
     * @param mimType
     * @return 0 - up-to-date, 1 - inserted, 2 - updated
     * @throws Exception
     */
    public int updateOmimTable( String mimNr, String phenotype, String status, String mimType ) throws Exception {

        Omim omimIncoming = new Omim();
        omimIncoming.setStatus(status);
        omimIncoming.setPhenotype(phenotype);
        omimIncoming.setMimNumber(mimNr);
        omimIncoming.setMimType(mimType);

        Omim omim = omimDAO.getOmimByNr(mimNr);
        if( omim==null ) {
            omimDAO.insertOmim(omimIncoming);
            return 1;
        }

        // check if anything changed in the omim entry
        if( omim.equals(omimIncoming) ) {
            return 0;
        } else {
            omimDAO.updateOmim(omimIncoming);
            return 2;
        }
    }

    public String getOmimPhenotype(String mimNumber) throws Exception {
        return  omimDAO.getOmimPhenotype(mimNumber);
    }

    public List<Integer> getOmimGenesForOmimPhenotype(int phenotypeMimNumber) throws Exception {
        String sql = "SELECT DISTINCT gene_mim_number FROM omim_gene2phenotype WHERE phenotype_mim_number=?";
        return IntListQuery.execute(omimDAO, sql, phenotypeMimNumber);
    }

    public int updateGene2Phenotype(int geneMimNumber, int phenotypeMimNumber) throws Exception {
        String sql = "UPDATE omim_gene2phenotype SET last_modified_date=SYSDATE WHERE gene_mim_number=? AND phenotype_mim_number=?";
        return omimDAO.update(sql, geneMimNumber, phenotypeMimNumber);
    }

    public int insertGene2Phenotype(int geneMimNumber, int phenotypeMimNumber) throws Exception {
        String sql = "INSERT INTO omim_gene2phenotype (gene_mim_number,phenotype_mim_number,created_date,last_modified_date) VALUES(?,?,SYSDATE,SYSDATE)";
        return omimDAO.update(sql, geneMimNumber, phenotypeMimNumber);
    }

    public int deleteObsoleteGene2PhenotypeData(Date cutoffDate) throws Exception {
        String sql = "DELETE FROM omim_gene2phenotype WHERE last_modified_date<?";
        return omimDAO.update(sql, cutoffDate);
    }
}
