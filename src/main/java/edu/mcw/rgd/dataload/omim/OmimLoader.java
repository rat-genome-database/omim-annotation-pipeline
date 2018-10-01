package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.pipelines.PipelineManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by mtutaj on 6/2/2017.
 */
public class OmimLoader {
    private String version;
    private PreProcessor preProcessor;
    private QCProcessor qcProcessor;
    private LoadProcessor loadProcessor;

    void run(Logger log, int qcThreadCount) throws Exception {

        log.info(getVersion());
        log.info(preProcessor.getVersion());

        OmimDAO dao = new OmimDAO();
        qcProcessor.setDao(dao);
        loadProcessor.setDao(dao);

        PipelineManager manager = new PipelineManager();
        manager.addPipelineWorkgroup(preProcessor, "PP", 1, 1000);
        manager.addPipelineWorkgroup(qcProcessor, "QC", qcThreadCount, 1000);
        manager.addPipelineWorkgroup(loadProcessor, "DL", 1, 1000);
        manager.run();

        // dump counter statistics
        manager.dumpCounters(log);

        log.info("--SUCCESS--");
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
