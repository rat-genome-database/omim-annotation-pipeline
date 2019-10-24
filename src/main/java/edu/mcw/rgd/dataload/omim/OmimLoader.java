package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineManager;
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

    void run(Logger log, int qcThreadCount) throws Exception {

        Date yesterday = Utils.addDaysToDate(new Date(), -1);

        preProcessor.init();

        OmimDAO dao = new OmimDAO();
        qcProcessor.setDao(dao);
        loadProcessor.setDao(dao);
        log.info("   "+dao.getConnectionInfo());

        OmimPS omimPSMap = new OmimPS();
        preProcessor.setOmimPSMap(omimPSMap);

        log.info(getVersion());

        final int recPoolSize = 15000;
        PipelineManager manager = new PipelineManager();
        manager.addPipelineWorkgroup(preProcessor, "PP", 1, recPoolSize);
        manager.addPipelineWorkgroup(qcProcessor, "QC", qcThreadCount, recPoolSize);
        manager.addPipelineWorkgroup(loadProcessor, "DL", 1, recPoolSize);
        manager.run();

        // QC OMIM PS map
        omimPSMap.qc(dao, manager.getSession());

        // delete stale annotations
        List<XdbId> staleOmimIds = dao.getOmimIdsModifiedBefore(yesterday);
        dao.deleteOmims(staleOmimIds);
        manager.getSession().incrementCounter("OMIM_DELETED", staleOmimIds.size());

        // dump counter statistics
        manager.dumpCounters(log);

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
