package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
        for( String arg: args ) {
            if( arg.contains("-annotations") ) {
                runAnnotationPipeline = true;
            }
        }


        Logger logStatus = LogManager.getLogger(runAnnotationPipeline ? "status_annot" : "status");
        logStatus.info(manager.getVersion());
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logStatus.info("   started at "+sdt.format(new Date(time0)));

        try {
            if (runAnnotationPipeline) {
                AnnotationLoader loader = (AnnotationLoader) (bf.getBean("annotationLoader"));
                loader.run(logStatus);
            } else {
                OmimLoader loader = (OmimLoader) (bf.getBean("omimLoader"));
                loader.run(logStatus);
            }
        } catch(Exception e) {
            // print stack trace to error stream and std out
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(bs));
            String err = bs.toString();
            logStatus.error(err);
            System.err.println(err);

            logStatus.info("=== ERROR === elapsed "+Utils.formatElapsedTime(time0, System.currentTimeMillis())+"\n");

            System.exit(-5);
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
