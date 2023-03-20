package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class QCProcessor {

    protected final Logger logMultis = LogManager.getLogger("multis");
    protected final Logger logInactive = LogManager.getLogger("omim_inactive");

    final int humanMapKey = 38; // human assembly GRCh38

    private OmimDAO dao;
    private CounterPool counters;

    public void init(OmimDAO dao, CounterPool counters) {
        this.dao = dao;
        this.counters = counters;
    }

    public void qc(OmimRecord rec) throws Exception {

        // process only OMIM records of type gene: skip phenotypes nad predominantly phenotypes
        if( !rec.getType().equals("gene") ) {
            counters.increment("MATCH_NO_GENE");
            return;
        }

        qcStatus(rec);

        // first match to rgd gene by NCBI gene id
        if( !Utils.isStringEmpty(rec.getGeneId()) ) {
            rec.getRgdGenes().addAll(dao.getGenesByNcbiGeneID(rec.getGeneId()));
        }

        // if there is a one matching gene by NCBI gene id, there is no need for secondary matching
        if( rec.getRgdGenes().size()!=1 ) {
            // second match by gene symbol
            if (rec.getRgdGenes().isEmpty() && !Utils.isStringEmpty(rec.getGeneSymbol())) {
                rec.getRgdGenes().addAll(dao.getGenesBySymbol(rec.getGeneSymbol()));
            }

            // validate gene locus and gene symbol list if available
            processGeneLocus(rec);
            processAlternateGeneSymbols(rec);
        }

        qcRgdGenes(rec);
    }

    void qcStatus(OmimRecord rec) throws Exception {

        counters.increment("OMIM_STATUS_"+rec.getStatus().toUpperCase());

        if( !rec.getStatus().equals("live") ) {
            // OMIM id is inactive -- see if there are any OMIM ids in RGD
            List<Term> terms = dao.getRdoTermsBySynonym(rec.getMimNumber());
            if( !terms.isEmpty() ) {
                counters.increment("INACTIVE_OMIM_IDS_IN_RGD");
                for( Term term: terms ) {
                    logInactive.info(rec.getMimNumber()+" in term "+term.getAccId()+" ["+term.getTerm()+"]");
                }
            }
        }
    }

    void processGeneLocus(OmimRecord rec) throws Exception {

        if( rec.getChr()!=null && rec.getStartPos()>0 && rec.getStopPos()>rec.getStartPos() ) {
            GeneDAO gdao = new GeneDAO();
            List<Gene> genesInLocus = gdao.getGenesByPosition(rec.getChr(), rec.getStartPos(), rec.getStopPos(), humanMapKey);

            // if there no RGD genes, use the ones from gene position
            if( rec.getRgdGenes().isEmpty() ) {
                rec.getRgdGenes().addAll(genesInLocus);
            } else {
                // there were some genes already -- intersect them with genes-by-position
                rec.getRgdGenes().retainAll(genesInLocus);
            }
        }
    }

    void processAlternateGeneSymbols(OmimRecord rec) throws Exception {

        if( Utils.isStringEmpty(rec.getGeneSymbols()) ) {
            return;
        }

        Set<Gene> genesBySymbol = new HashSet<>();

        String[] geneSymbols = rec.getGeneSymbols().split("[\\,\\s]+");
        for( String geneSymbol: geneSymbols ) {
            List<Gene> genes = getDao().getGenesBySymbol(geneSymbol);
            if( genes.isEmpty() ) {
                genes = getDao().getGenesByAlias(geneSymbol);
            }
            genesBySymbol.addAll(genes);
        }

        if( rec.getRgdGenes().isEmpty() ) {
            rec.getRgdGenes().addAll(genesBySymbol);
        } else {
            // intersect genes-by-symbol with the existing list of genes
            rec.getRgdGenes().retainAll(genesBySymbol);
        }
    }

    void qcRgdGenes(OmimRecord rec) throws Exception {

        // if there are no matching genes, raise a flag
        if( rec.getRgdGenes().isEmpty() ) {
            rec.setFlag("MATCH_NO_GENE");
            counters.increment("MATCH_NO_GENE");
            return;
        }

        // if there are multiple matching genes, raise a flag
        if( rec.getRgdGenes().size()>1 ) {
            rec.setFlag("MATCH_MULTIPLE_GENES");
            counters.increment("MATCH_MULTIPLE_GENES");

            String msg = "OMIM:"+rec.getMimId()+" matches multiple genes\n";
            for( Gene gene: rec.getRgdGenes() ) {
                msg += "RGD_ID:"+gene.getRgdId()+"|SYMBOL:"+gene.getSymbol()+"\n";
            }
            logMultis.info(msg);
            return;
        }

        // single gene match
        rec.setFlag("MATCH_SINGLE_GENE");
        counters.increment("MATCH_SINGLE_GENE");

        // get omim ids for matching gene
        for( Gene gene: rec.getRgdGenes() ) {

            qcOmimId(gene.getRgdId(), rec.getMimNumber(), rec);

            /*
            for( Integer phenotypeOmimId: rec.getPhenotypeMimNumbers() ) {
                qcOmimId(gene.getRgdId(), phenotypeOmimId.toString(), rec);
            }
            */
        }
    }

    void qcOmimId(int rgdId, String omimId, OmimRecord rec) throws Exception {
        // create a new omim for insertion
        XdbId xdbId = new XdbId();
        xdbId.setAccId(omimId);
        xdbId.setRgdId(rgdId);
        xdbId.setSrcPipeline("OMIM");
        xdbId.setXdbKey(XdbId.XDB_KEY_OMIM);
        xdbId.setModificationDate(new Date());

        List<XdbId> omimIdsInRgd = dao.getOmimIdsForGene(rgdId);

        // if the incoming omim id is not in RGD yet, it has to be added
        int matchIndex = omimIdsInRgd.indexOf(xdbId);
        if( matchIndex>=0 ) {
            rec.setFlag("OMIM_MATCHING");
            XdbId id = omimIdsInRgd.get(matchIndex);
            rec.getOmimsForUpdate().add(id.getKey());
        }
        else {
            rec.getOmimsForInsert().add(xdbId);
            rec.setFlag("OMIM_INSERTED");
        }
    }

    public OmimDAO getDao() {
        return dao;
    }

    public void setDao(OmimDAO dao) {
        this.dao = dao;
    }
}
