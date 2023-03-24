package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 * class to load omim data
 */
public class Manager {

    private String version;

    public static void main(String[] args) throws Exception {

        long time0 = System.currentTimeMillis();

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));

        Manager manager = (Manager) (bf.getBean("manager"));

        boolean runAnnotationPipeline = false;
        boolean loadPhenotypicSeries = false;

        for( String arg: args ) {
            if( arg.contains("-annotations") ) {
                runAnnotationPipeline = true;
            }
            if( arg.contains("-phenotypic_series") ) {
                loadPhenotypicSeries = true;
            }
        }


        Logger logStatus = LogManager.getLogger(runAnnotationPipeline ? "annots" : loadPhenotypicSeries ? "omim_ps" : "status");
        logStatus.info(manager.getVersion());
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logStatus.info("   started at "+sdt.format(new Date(time0)));

        try {
            if (runAnnotationPipeline) {
                AnnotationLoader loader = (AnnotationLoader) (bf.getBean("annotationLoader"));
                loader.run(logStatus);

            } else if( loadPhenotypicSeries ) {
                OmimPS omimPS = (OmimPS) (bf.getBean("omimPS"));

                PreProcessor preProcessor = (PreProcessor) (bf.getBean("preProcessor"));
                String apiKey = Utils.readFileAsString(preProcessor.getApiKeyFile()).trim();
                omimPS.loadAll(apiKey);

            } else {
                OmimLoader loader = (OmimLoader) (bf.getBean("omimLoader"));
                loader.run(logStatus);
            }
        } catch(Exception e) {
            // print stack trace to error stream and std out
            Utils.printStackTrace(e, logStatus);

            logStatus.info("=== ERROR === elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis())+"\n");

            throw new Exception(e);
        }

        logStatus.info("=== OK === elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis())+"\n");
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
