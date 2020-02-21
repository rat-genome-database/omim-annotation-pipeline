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
    private LoadProcessor loadProcessor;

    void run(Logger log) throws Exception {

        Date yesterday = Utils.addDaysToDate(new Date(), -1);

        CounterPool counters = new CounterPool();

        OmimDAO dao = new OmimDAO();
        qcProcessor.init(dao, counters);
        loadProcessor.init(dao, counters);
        log.info("   "+dao.getConnectionInfo());

        OmimPS omimPSMap = new OmimPS();
        preProcessor.setOmimPSMap(omimPSMap);

        log.info(getVersion());

        List<OmimRecord> incomingData = preProcessor.downloadAndParseOmimData();

        incomingData.parallelStream().forEach( rec -> {

            try {
                qcProcessor.qc(rec);
                loadProcessor.load(rec);
            } catch( Exception e ) {
                throw new RuntimeException(e);
            }
        });

        // QC OMIM PS map
        omimPSMap.qc(dao, counters);

        // delete stale annotations
        List<XdbId> staleOmimIds = dao.getOmimIdsModifiedBefore(yesterday);
        dao.deleteOmims(staleOmimIds);
        counters.add("OMIM_DELETED", staleOmimIds.size());

        // dump counter statistics
        log.info(counters.dumpAlphabetically());

        omimPSMap.dumpPSIdsNotInRgd(dao, log);
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

    public LoadProcessor getLoadProcessor() {
        return loadProcessor;
    }

    public void setLoadProcessor(LoadProcessor loadProcessor) {
        this.loadProcessor = loadProcessor;
    }
}
