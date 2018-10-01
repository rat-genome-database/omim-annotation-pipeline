package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class QCProcessor extends RecordProcessor {

    protected final Logger logMultis = LogManager.getLogger("multis");

    private OmimDAO dao;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {

        OmimRecord rec = (OmimRecord) pipelineRecord;

        // if the record does not contain gene symbol nor entrez gene id, skip it
        if( rec.getGeneId()==null || rec.getGeneSymbol()==null ||
            (rec.getGeneId().equals("-") && rec.getGeneSymbol().equals("-")) ) {

            if( rec.primaryRecord!=null )
                processAlternateGeneSymbols(rec);

            if( rec.getRgdGenes().isEmpty() ) {
                rec.setFlag("NO_DATA");

                // no data for this record
                // it is possible this omim has been retired or withdrawn;
                // in that case it has to be removed from db
                List<XdbId> omimIdsInRgd = dao.getXdbIdsForOmimId(rec.getMimNumber());

                // if there are no objects with such a omim id, nothing more to do
                if( omimIdsInRgd.isEmpty() )
                    return;

                // surprise! there are existing rgd objects with such an omim id: delete them
                rec.setOmimsForDelete(omimIdsInRgd);
                rec.setFlag("OMIM_DELETED");// flag the record: there are omims to be deleted
                return;
            }
        }

        // match to rgd gene by NCBI gene id
        if( !rec.getGeneId().equals("-") ) {
            rec.getRgdGenes().addAll(dao.getGenesByNcbiGeneID(rec.getGeneId()));
        }

        // match to rgd gene by gene symbol
        if( !rec.getGeneSymbol().equals("-") ) {
            rec.getRgdGenes().addAll(dao.getGenesBySymbol(rec.getGeneSymbol()));
        }

        qcRgdGenes(rec);
    }

    void processAlternateGeneSymbols(OmimRecord rec) throws Exception {

        for( String symbol: rec.geneSymbols ) {
            rec.getRgdGenes().addAll(dao.getGenesBySymbol(symbol));
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
        } else {
            rec.setFlag("GENE_MATCH");
        }

        // get omim ids for matching gene
        for( Gene gene: rec.getRgdGenes() ) {

            // create a new omim for insertion
            XdbId xdbId = new XdbId();
            xdbId.setAccId(rec.getMimNumber());
            xdbId.setLinkText(rec.getMimNumber());
            xdbId.setRgdId(gene.getRgdId());
            xdbId.setSrcPipeline("OMIM");
            xdbId.setXdbKey(XdbId.XDB_KEY_OMIM);
            xdbId.setModificationDate(new Date());

            List<XdbId> omimIdsInRgd = dao.getOmimIdsForGene(gene.getRgdId());

            // if the incoming omim id is not in RGD yet, it has to be added
            if( omimIdsInRgd.contains(xdbId) ) {
                rec.setFlag("OMIM_MATCHING");
                rec.getOmimsForUpdate().add(xdbId.getKey());
            }
            else {
                rec.getOmimsForInsert().add(xdbId);
                rec.setFlag("OMIM_INSERTED");
            }
        }
    }

    public OmimDAO getDao() {
        return dao;
    }

    public void setDao(OmimDAO dao) {
        this.dao = dao;
    }
}
