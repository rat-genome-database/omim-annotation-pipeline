package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Created by mtutaj on 6/2/2017.
 */
public class OmimLoader {
    private String version;
    private PreProcessor preProcessor;
    private QCProcessor qcProcessor;

    void run(Logger log) throws Exception {

        Date cutoffDate = Utils.addMinutesToDate(new Date(), -5);

        CounterPool counters = new CounterPool();

        OmimDAO dao = new OmimDAO();
        qcProcessor.init(dao, counters);
        log.info("   "+dao.getConnectionInfo());

        log.info(getVersion());

        List<OmimRecord> incomingData = preProcessor.downloadAndParseOmimData();

        incomingData.parallelStream().forEach( rec -> {

            try {
                qcProcessor.qc(rec);
                load(rec, dao, counters);
            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
        });

        // delete stale annotations
        List<XdbId> staleOmimIds = dao.getOmimIdsModifiedBefore(cutoffDate);
        dao.deleteOmims(staleOmimIds);
        counters.add("OMIM_DELETED", staleOmimIds.size());

        // delete obsolete rows from OMIM_gene2phenotype table
        int obsoleteGene2PhenotypeRows = dao.deleteObsoleteGene2PhenotypeData(cutoffDate);
        counters.add("OMIM_GENE2PHENOTYPE_DELETED", Math.abs(obsoleteGene2PhenotypeRows));

        // dump counter statistics
        log.info(counters.dumpAlphabetically());
    }

    public void load(OmimRecord rec, OmimDAO dao, CounterPool counters) throws Exception {

        counters.increment("PROCESSED");

        // if record contained no data, increment counter
        if( rec.isFlagSet("NO_DATA") ) {
            counters.increment("NO_DATA");
        }

        if( rec.isFlagSet("OMIM_INSERTED") ) {
            dao.insertOmims(rec.getOmimsForInsert());
            counters.add("OMIM_INSERTED", rec.getOmimsForInsert().size());
        }

        if( rec.isFlagSet("OMIM_MATCHING") ) {
            dao.updateOmims(rec.getOmimsForUpdate());
            counters.add("OMIM_MATCHING", rec.getOmimsForUpdate().size());
        }

        updateOmimTable(rec, dao, counters);

        // update OMIM_gene2phenotype table
        for( int omimPhenotypeNr: rec.getPhenotypeMimNumbers() ) {
            int geneMimNr = Integer.parseInt(rec.getMimNumber());
            int updated = dao.updateGene2Phenotype(geneMimNr, omimPhenotypeNr);
            if( updated!=0 ) {
                counters.increment("OMIM_GENE2PHENOTYPE_UP_TO_DATE");
            } else {
                dao.insertGene2Phenotype(geneMimNr, omimPhenotypeNr);
                counters.increment("OMIM_GENE2PHENOTYPE_INSERTED");
            }
        }
    }

    void updateOmimTable( OmimRecord rec, OmimDAO dao, CounterPool counters ) throws Exception {
        int r = dao.updateOmimTable(rec.getMimNumber(), rec.getPhenotype(), rec.getStatus(), rec.getType());
        if( r==1 ) {
            counters.increment("OMIM_ENTRIES_INSERTED");
        } else if( r==0 ){
            counters.increment("OMIM_ENTRIES_UP_TO_DATE");
        } else if( r==2 ){
            counters.increment("OMIM_ENTRIES_UPDATED");
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public PreProcessor getPreProcessor() {
        return preProcessor;
    }

    public void setPreProcessor(PreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    public QCProcessor getQcProcessor() {
        return qcProcessor;
    }

    public void setQcProcessor(QCProcessor qcProcessor) {
        this.qcProcessor = qcProcessor;
    }
}
