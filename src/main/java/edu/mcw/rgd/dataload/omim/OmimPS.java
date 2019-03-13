package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.dao.spring.StringMapQuery;
import edu.mcw.rgd.pipelines.PipelineSession;

import java.util.*;

/**
 * @author mtutaj
 * @since 3/13/2019
 * Handles OMIM_PHENOTYPIC_SERIES table
 */
public class OmimPS {

    // internally we store PS mappings as map:
    // 'PSid|MIMid' -> 'key'

    private Set<String> incoming = new HashSet<>();

    // return true if the mapping is new
    public boolean addMapping( String psId, int phenotypeMimNumber ) {
        String mapping = psId+"|"+phenotypeMimNumber;
        return incoming.add(mapping);
    }

    public void qc( OmimDAO dao, PipelineSession session ) throws Exception {

        Set<String> inRgdMappings = new HashSet<>();
        List<StringMapQuery.MapPair> inRgdMappingsList = dao.getPhenotypicSeriesMappings();
        for( StringMapQuery.MapPair pair: inRgdMappingsList ) {
            inRgdMappings.add(pair.keyValue+"|"+pair.stringValue);
        }

        // determine to-be-added mappings
        Set<String> toBeAddedMappings = new HashSet<>(incoming);
        toBeAddedMappings.removeAll(inRgdMappings);
        session.incrementCounter("PHENOTYPIC_SERIES_ENTRIES_INSERTED", toBeAddedMappings.size());
        for( String mapping: toBeAddedMappings ) {
            String[] words = mapping.split("[\\|]");
            dao.insertPhenotypicSeriesMapping(words[0], words[1]);
        }

        // determine up-to-date mappings
        session.incrementCounter("PHENOTYPIC_SERIES_ENTRIES_UP-TO-DATE", incoming.size() - toBeAddedMappings.size());

        // determine to-be-deleted mappings
        Set<String> toBeDeletedMappings = new HashSet<>(inRgdMappings);
        toBeDeletedMappings.removeAll(incoming);
        session.incrementCounter("PHENOTYPIC_SERIES_ENTRIES_DELETED", toBeDeletedMappings.size());
        for( String mapping: toBeDeletedMappings ) {
            String[] words = mapping.split("[\\|]");
            dao.deletePhenotypicSeriesMapping(words[0], words[1]);
        }
    }
}
