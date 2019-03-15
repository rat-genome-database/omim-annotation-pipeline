package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.impl.AnnotationDAO;
import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.XdbIdDAO;
import edu.mcw.rgd.dao.spring.StringMapQuery;
import edu.mcw.rgd.datamodel.Gene;
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
    protected final Logger logAnnots = LogManager.getLogger("annots");

    GeneDAO geneDAO = new GeneDAO();
    XdbIdDAO xdbIdDAO = new XdbIdDAO();
    OntologyXDAO ontologyXDAO = new OntologyXDAO();
    AnnotationDAO annotationDAO = new AnnotationDAO();

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
        ListIterator<Gene> it = genes.listIterator();
        while( it.hasNext() ) {
            if( it.next().getSpeciesTypeKey()!=SpeciesType.HUMAN )
                it.remove();
        }
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
        //return xdbIdDAO.deleteXdbIds(omims);
        return 0;
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
        logAnnots.info("INSERT "+annot.dump("|"));
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

        Logger logStatus = LogManager.getLogger("status_annot");

        // convert delete-threshold string to number; i.e. '5%' --> '5'
        int staleAnnotDeleteThresholdPerc = Integer.parseInt(staleAnnotDeleteThresholdStr.substring(0, staleAnnotDeleteThresholdStr.length()-1));
        // compute maximum allowed number of stale annots to be deleted
        int annotCount = annotationDAO.getCountOfAnnotationsByReference(refRgdId, dataSource, "D");
        int staleAnnotDeleteLimit = (staleAnnotDeleteThresholdPerc * annotCount) / 100;

        List<Annotation> staleAnnots = annotationDAO.getAnnotationsModifiedBeforeTimestamp(createdBy, dt, "D");

        logStatus.info("ANNOTATIONS_COUNT: "+annotCount);
        logStatus.info("   stale annotation delete limit ("+staleAnnotDeleteThresholdStr+"): "+staleAnnotDeleteLimit);
        logStatus.info("   stale annotations to be deleted: "+staleAnnots.size());

        if( staleAnnots.size()> staleAnnotDeleteLimit ) {
            logStatus.warn("*** DELETE of stale annots aborted! *** "+staleAnnotDeleteThresholdStr+" delete threshold exceeded!");
            return 0;
        }

        List<Integer> staleAnnotKeys = new ArrayList<>();
        for( Annotation ann: staleAnnots ) {
            logAnnots.info("DELETE "+ann.dump("|"));
            staleAnnotKeys.add(ann.getKey());
        }
        return annotationDAO.deleteAnnotations(staleAnnotKeys);
    }

    public List<StringMapQuery.MapPair> getPhenotypicSeriesMappings() throws Exception {
        String sql = "SELECT phenotypic_series_number,phenotype_mim_number FROM omim_phenotypic_series";
        return StringMapQuery.execute(geneDAO, sql);
    }

    public void insertPhenotypicSeriesMapping( String psNumber, String phenotypeMimNumber ) throws Exception {
        String sql = "INSERT INTO omim_phenotypic_series (phenotypic_series_number, phenotype_mim_number) VALUES(?,?)";
        geneDAO.update(sql, psNumber, phenotypeMimNumber);
    }

    public void deletePhenotypicSeriesMapping( String psNumber, String phenotypeMimNumber ) throws Exception {
        String sql = "DELETE FROM omim_phenotypic_series WHERE phenotypic_series_number=? AND phenotype_mim_number=?";
        geneDAO.update(sql, psNumber, phenotypeMimNumber);
    }
}
