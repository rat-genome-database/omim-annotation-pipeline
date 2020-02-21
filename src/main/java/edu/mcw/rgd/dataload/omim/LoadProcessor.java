package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.process.CounterPool;

/**
 * @author mtutaj
 * Date: Apr 28, 2011
 */
public class LoadProcessor {

    private OmimDAO dao;
    private CounterPool counters;

    public void init(OmimDAO dao, CounterPool counters) {
        this.dao = dao;
        this.counters = counters;
    }

    public void load(OmimRecord rec) throws Exception {
        
        counters.increment("PROCESSED");

        // if record contained no data, increment counter
        if( rec.isFlagSet("NO_DATA") ) {
            counters.increment("NO_DATA");
        }

        // if record was not matched, increment counter
        if( rec.isFlagSet("NO_GENE_MATCH") ) {
            counters.increment("NO_GENE_MATCH");
        }

        if( rec.isFlagSet("OMIM_INSERTED") ) {
            dao.insertOmims(rec.getOmimsForInsert());
            counters.add("OMIM_INSERTED", rec.getOmimsForInsert().size());
        }

        if( rec.isFlagSet("OMIM_MATCHING") ) {
            dao.updateOmims(rec.getOmimsForUpdate());
            counters.add("OMIM_MATCHING", rec.getOmimsForUpdate().size());
        }

        updateOmimTable(rec);
    }

    void updateOmimTable( OmimRecord rec ) throws Exception {
        getDao().updateOmimTable(rec.getMimNumber(), rec.getPhenotype(), rec.getStatus(), rec.getType());
    }

    public OmimDAO getDao() {
        return dao;
    }

    public void setDao(OmimDAO dao) {
        this.dao = dao;
    }
}
