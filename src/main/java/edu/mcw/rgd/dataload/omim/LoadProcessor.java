package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.pipelines.PipelineRecord;
import edu.mcw.rgd.pipelines.RecordProcessor;

/**
 * @author mtutaj
 * Date: Apr 28, 2011
 */
public class LoadProcessor extends RecordProcessor {

    private OmimDAO dao;

    @Override
    public void process(PipelineRecord pipelineRecord) throws Exception {
        
        OmimRecord rec = (OmimRecord) pipelineRecord;

        getSession().incrementCounter("PROCESSED", 1);

        // if record contained no data, increment counter
        if( rec.isFlagSet("NO_DATA") ) {
            getSession().incrementCounter("NO_DATA", 1);
        }

        // if record was not matched, increment counter
        if( rec.isFlagSet("NO_GENE_MATCH") ) {
            getSession().incrementCounter("NO_GENE_MATCH", 1);
        }

        if( rec.isFlagSet("OMIM_INSERTED") ) {
            dao.insertOmims(rec.getOmimsForInsert());
            getSession().incrementCounter("OMIM_INSERTED", rec.getOmimsForInsert().size());
        }

        if( rec.isFlagSet("OMIM_MATCHING") ) {
            dao.updateOmims(rec.getOmimsForUpdate());
            getSession().incrementCounter("OMIM_MATCHING", rec.getOmimsForUpdate().size());
        }
    }

    public OmimDAO getDao() {
        return dao;
    }

    public void setDao(OmimDAO dao) {
        this.dao = dao;
    }
}
