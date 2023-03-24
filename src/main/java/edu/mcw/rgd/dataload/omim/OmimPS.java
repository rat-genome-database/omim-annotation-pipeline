package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.spring.StringMapQuery;
import edu.mcw.rgd.process.CounterPool;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.util.*;

/**
 * @author mtutaj
 * @since 3/13/2019
 * Handles OMIM_PHENOTYPIC_SERIES table
 */
public class OmimPS {

    private String psFile;

    protected final Logger log = LogManager.getLogger("omim_ps");

    // internally we store PS mappings as map:
    // 'PSid|MIMid' -> 'key'
    private Set<String> incoming = new HashSet<>();

    public void loadAll(String apiKey) throws Exception {

        FileDownloader fd = new FileDownloader();
        fd.setExternalFile(getPsFile().replace("{{APIKEY}}", apiKey));
        fd.setLocalFile("data/phenotypicSeries.tsv");
        fd.setUseCompression(true);
        fd.setPrependDateStamp(true);
        String fileName = fd.downloadNew();
        BufferedReader in = Utils.openReader(fileName);

        String line;
        while( (line=in.readLine())!=null ) {
            // sample line to parse and load
            // # Phenotypic Series Number TAB MIM Number TAB Phenotype
            // PS100070 TAB 609782 TAB Aortic aneurysm, familial abdominal 2
            String[] words = line.split("[\\t]");
            if( words.length!=3 ) {
                continue;
            }
            String psId = words[0];
            String omimId = words[1];
            if( psId.matches("PS\\d{6}") || omimId.matches("\\d{6}")) {
                addMapping(psId, "OMIM:"+omimId);
            }
        }
        in.close();

        CounterPool counters = new CounterPool();
        counters.add("PHENOTYPIC_SERIES_ENTRIES_INCOMING", incoming.size());

        OmimDAO dao = new OmimDAO();
        qc(dao, counters);
        log.info(counters.dumpAlphabetically());

        dumpPSIdsNotInRgd(dao, log);
    }

    // return nr of new mappings added
    public int addMapping( String psIds, String phenotypeMimNumber ) {
        int addedMappings = 0;

        // split PS is into multiple types
        for( String psId: psIds.split("[\\,]") ) {
            String mapping = "OMIM:"+psId + "|" + phenotypeMimNumber;
            if( incoming.add(mapping) ) {
                addedMappings++;
            }
        }

        return addedMappings;
    }

    public void qc( OmimDAO dao, CounterPool counters ) throws Exception {

        Set<String> inRgdMappings = new HashSet<>();
        List<StringMapQuery.MapPair> inRgdMappingsList = dao.getPhenotypicSeriesMappings();
        for( StringMapQuery.MapPair pair: inRgdMappingsList ) {
            inRgdMappings.add(pair.keyValue+"|"+pair.stringValue);
        }

        // determine to-be-added mappings
        Set<String> toBeAddedMappings = new HashSet<>(incoming);
        toBeAddedMappings.removeAll(inRgdMappings);
        counters.add("PHENOTYPIC_SERIES_ENTRIES_INSERTED", toBeAddedMappings.size());
        for( String mapping: toBeAddedMappings ) {
            String[] words = mapping.split("[\\|]");
            log.info("INSERTED "+words[0]+" "+words[1]);
            dao.insertPhenotypicSeriesMapping(words[0], words[1]);
        }

        // determine up-to-date mappings
        counters.add("PHENOTYPIC_SERIES_ENTRIES_UP-TO-DATE", incoming.size() - toBeAddedMappings.size());

        // determine to-be-deleted mappings
        Set<String> toBeDeletedMappings = new HashSet<>(inRgdMappings);
        toBeDeletedMappings.removeAll(incoming);
        counters.add("PHENOTYPIC_SERIES_ENTRIES_DELETED", toBeDeletedMappings.size());
        for( String mapping: toBeDeletedMappings ) {
            String[] words = mapping.split("[\\|]");
            log.info("DELETED "+words[0]+" "+words[1]);
            dao.deletePhenotypicSeriesMapping(words[0], words[1]);
        }
    }

    public void dumpPSIdsNotInRgd(OmimDAO dao, Logger log) throws Exception {

        List<String> psIdsNotInRgd = dao.getPhenotypicSeriesIdsNotInRgd();
        if( !psIdsNotInRgd.isEmpty() ) {
            log.info("===");
            log.info("OMIM PS ids not in RGD (yet): "+psIdsNotInRgd.size());
            for( String psIdNotInRgd: psIdsNotInRgd ) {
                log.info("   "+psIdNotInRgd);
            }
        }
    }

    public void setPsFile(String psFile) {
        this.psFile = psFile;
    }

    public String getPsFile() {
        return psFile;
    }
}
