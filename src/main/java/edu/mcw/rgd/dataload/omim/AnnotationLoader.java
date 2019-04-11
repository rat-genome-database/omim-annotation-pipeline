package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.ontologyx.TermSynonym;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since 2/20/13
 */
public class AnnotationLoader {

    private String version;
    private OmimDAO dao = new OmimDAO();
    private int createdBy;
    private int refRgdId;
    private String deleteThresholdForStaleAnnotations;
    private String dataSource;
    private Logger log;
    private CounterPool counters;


    public void run(Logger log) throws Exception {

        Date dtStart = Utils.addDaysToDate(new Date(), -1);

        this.log = log;
        counters = new CounterPool();

        log.info("   "+dao.getConnectionInfo());
        log.info(getVersion()+"\n");

        // load all active disease terms
        List<Term> terms = dao.getActiveRDOTerms();
        List<OmimAnnotRecord> records = new ArrayList<>(terms.size());

        for(Term term: terms) {

            OmimAnnotRecord rec = new OmimAnnotRecord();
            rec.term = term;

            records.add(rec);
        }

        // QC terms in parallel
        records.parallelStream().forEach( rec -> {
            try {
                findOmimIds(rec);
                createAnnotations(rec);
                qcAnnotations(rec);
                syncWithDb(rec);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });


        // at the end delete all obsolete annotations (annotations older than one day)
        int obsoleteAnnotationsDeleted = dao.deleteObsoleteAnnotations(getCreatedBy(), dtStart,
                getDeleteThresholdForStaleAnnotations(), getRefRgdId(), getDataSource());
        counters.add("ANNOTATIONS_DELETED", obsoleteAnnotationsDeleted);

        log.info(counters.dumpAlphabetically());
    }

    void findOmimIds(OmimAnnotRecord rec) throws Exception {
        for(TermSynonym syn: dao.getTermSynonyms(rec.term.getAccId()) ) {
            // skip phenotypic series: OMIM:PSxxxxx
            if( syn.getName().startsWith("OMIM:") && !syn.getName().startsWith("OMIM:PS")) {
                // skip any zeroes
                try {
                    int omimId = Integer.parseInt(syn.getName().substring(5).trim());
                    if( rec.omimIds==null ) {
                        rec.omimIds = new HashSet<>();
                    }
                    rec.omimIds.add(Integer.toString(omimId));
                } catch(NumberFormatException e) {
                    log.warn("*** WARN: INVALID OMIM ID: "+syn.getName()+" for "+rec.term.getAccId());
                }
            }
        }
    }

    void createAnnotations(OmimAnnotRecord rec) throws Exception {
        if( rec.omimIds ==null )
            return;
        rec.annots = new ArrayList<>();

        for( String omimId: rec.omimIds ) {

            String phenotype = Utils.defaultString(dao.getOmimPhenotype(omimId)).toLowerCase();
            String qualifier = null;
            if( phenotype.contains("susceptibility") ) {
                qualifier = "susceptibility";
            }

            for( XdbId xdbId: dao.getXdbIdsForOmimId(omimId) ) {

                Annotation ann = new Annotation();
                ann.setAnnotatedObjectRgdId(xdbId.getRgdId());
                ann.setAspect("D");
                ann.setCreatedBy(getCreatedBy());
                ann.setDataSrc(getDataSource());
                ann.setEvidence("IAGP");
                ann.setLastModifiedBy(getCreatedBy());
                ann.setLastModifiedDate(ann.getCreatedDate());
                ann.setTerm(rec.term.getTerm());
                ann.setTermAcc(rec.term.getAccId());
                ann.setRefRgdId(getRefRgdId());
                ann.setQualifier(qualifier);

                Gene gene = dao.getGene(xdbId.getRgdId());
                ann.setRgdObjectKey(RgdId.OBJECT_KEY_GENES);
                ann.setObjectName(gene.getName());
                ann.setObjectSymbol(gene.getSymbol());
                insertAnnotation(ann, rec.annots);

                // create orthologous annotations
                for( Gene ortholog: dao.getActiveOrthologs(xdbId.getRgdId()) ) {

                    Annotation annOrtho = (Annotation) ann.clone();
                    annOrtho.setAnnotatedObjectRgdId(ortholog.getRgdId());
                    annOrtho.setEvidence("ISO");
                    annOrtho.setWithInfo("RGD:"+xdbId.getRgdId());
                    annOrtho.setObjectName(ortholog.getName());
                    annOrtho.setObjectSymbol(ortholog.getSymbol());
                    insertAnnotation(annOrtho, rec.annots);
                }
            }
        }
    }

    void insertAnnotation( Annotation ann, List<Annotation> annots ) {

        // ensure the annotation to be inserted is not a duplicate annotation
        // (duplicate annotations will have the same unique key:
        // TERM_ACC+ANNOTATED_OBJECT_RGD_ID+REF_RGD_ID+EVIDENCE+WITH_INFO+QUALIFIER+XREF_SOURCE
        if( !isAnnotationDuplicate(ann, annots) ) {
            annots.add(ann);
        }
    }

    // unique annotations will have different unique keys:
    // TERM_ACC+ANNOTATED_OBJECT_RGD_ID+REF_RGD_ID+EVIDENCE+WITH_INFO+QUALIFIER+XREF_SOURCE
    boolean isAnnotationDuplicate( Annotation ann, List<Annotation> annots ) {

        for( Annotation ann2: annots ) {
            if( !Utils.intsAreEqual(ann.getAnnotatedObjectRgdId(), ann2.getAnnotatedObjectRgdId()) )
                continue;
            if( !Utils.intsAreEqual(ann.getRefRgdId(), ann2.getRefRgdId()) )
                continue;
            if( !Utils.stringsAreEqual(ann.getTermAcc(), ann2.getTermAcc()) )
                continue;
            if( !Utils.stringsAreEqual(ann.getEvidence(), ann2.getEvidence()) )
                continue;
            if( !Utils.stringsAreEqual(ann.getWithInfo(), ann2.getWithInfo()) )
                continue;
            if( !Utils.stringsAreEqual(ann.getQualifier(), ann2.getQualifier()) )
                continue;
            if( !Utils.stringsAreEqual(ann.getXrefSource(), ann2.getXrefSource()) )
                continue;

            // all seven conditions are satisfied -- new annotation is a duplicate
            return true;
        }

        // no annotations was found to be duplicate
        return false;
    }

    void qcAnnotations(OmimAnnotRecord rec) throws Exception {
        if( rec.annots==null )
            return;
        rec.annotsForInsert = new ArrayList<>();
        rec.annotsForUpdate = new ArrayList<>();

        for( Annotation ann: rec.annots ) {
            Annotation annotInRgd = dao.getAnnotation(ann);
            if( annotInRgd==null )
                rec.annotsForInsert.add(ann);
            else
                rec.annotsForUpdate.add(annotInRgd);
        }
    }

    void syncWithDb( OmimAnnotRecord rec ) throws Exception {

        if( rec.annotsForInsert!=null ) {
            for( Annotation ann: rec.annotsForInsert ) {
                dao.insertAnnotation(ann);
                counters.increment("ANNOTATIONS_INSERTED");
            }
        }

        if( rec.annotsForUpdate!=null ) {
            for( Annotation ann: rec.annotsForUpdate ) {
                dao.updateLastModified(ann);
                counters.increment("ANNOTATIONS_MATCHING");
            }
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setRefRgdId(int refRgdId) {
        this.refRgdId = refRgdId;
    }

    public int getRefRgdId() {
        return refRgdId;
    }

    public void setDeleteThresholdForStaleAnnotations(String deleteThresholdForStaleAnnotations) {
        this.deleteThresholdForStaleAnnotations = deleteThresholdForStaleAnnotations;
    }

    public String getDeleteThresholdForStaleAnnotations() {
        return deleteThresholdForStaleAnnotations;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getDataSource() {
        return dataSource;
    }

    class OmimAnnotRecord {

        Term term;
        Set<String> omimIds; // OMIM ids associated with the term
        List<Annotation> annots;
        List<Annotation> annotsForInsert;
        List<Annotation> annotsForUpdate;
    }
}
