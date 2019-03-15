package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class QCProcessor extends RecordProcessor {

    protected final Logger logMultis = LogManager.getLogger("multis");

    int humanMapKey = 38; // human assembly GRCh38

    private OmimDAO dao;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        OmimRecord rec = (OmimRecord) pipelineRecord;

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

    void processGeneLocus(OmimRecord rec) throws Exception {

        if( rec.chr!=null && rec.startPos>0 && rec.stopPos>rec.startPos ) {
            GeneDAO gdao = new GeneDAO();
            List<Gene> genesInLocus = gdao.getGenesByPosition(rec.chr, rec.startPos, rec.stopPos, humanMapKey);

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

        if( Utils.isStringEmpty(rec.geneSymbols) ) {
            return;
        }

        Set<Gene> genesBySymbol = new HashSet<>();

        String[] geneSymbols = rec.geneSymbols.split("[\\,\\s]+");
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
            rec.setFlag("NO_GENE_MATCH");
            return;
        }

        // if there are multiple matching genes, raise a flag
        if( rec.getRgdGenes().size()>1 ) {
            rec.setFlag("MULTIPLE_GENE_MATCH");
            getSession().incrementCounter("CONFLICT_MULTIPLE_GENE_MATCH", 1);

            String msg = "OMIM:"+rec.mimId+" matches multiple genes\n";
            for( Gene gene: rec.getRgdGenes() ) {
                msg += "RGD_ID:"+gene.getRgdId()+"|SYMBOL:"+gene.getSymbol()+"\n";
            }
            logMultis.info(msg);
            return;
        }

        // single gene match
        rec.setFlag("GENE_MATCH");

        // get omim ids for matching gene
        for( Gene gene: rec.getRgdGenes() ) {

            qcOmimId(gene.getRgdId(), rec.getMimNumber(), rec);

            for( Integer phenotypeOmimId: rec.phenotypeMimNumbers ) {
                qcOmimId(gene.getRgdId(), phenotypeOmimId.toString(), rec);
            }
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
