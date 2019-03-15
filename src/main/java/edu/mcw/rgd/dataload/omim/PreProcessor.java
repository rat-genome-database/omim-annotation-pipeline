package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class PreProcessor extends RecordPreprocessor {

    private String mim2geneFile;
    private String omimApiUrl;
    private OmimPS omimPSMap;
    private int omimApiDownloadSleepTimeInMS;
    private int jsonFileCacheLifeInDays;

    @Override
    public void process() throws Exception {

        // download the file to a local folder
        String fileName = downloadFile();

        // this is a text file, tab separated
        processMim2Gene(fileName);
    }

    List<String> loadMim2Gene(String fileName) throws Exception {

        // this is a text file, tab separated
        BufferedReader reader = Utils.openReader(fileName);
        List<String> lines = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            // skip comment lines
            if (line.startsWith("#"))
                continue;
            lines.add(line);
        }
        reader.close();

        Collections.shuffle(lines);

        return lines;
    }

    void processMim2Gene(String fileName) throws Exception {

        // this is a text file, tab separated
        List<String> lines = loadMim2Gene(fileName);
        for( String line: lines ) {

            // split line into words
            String[] words = line.split("[\t]", -1);

            // the full data is for gene records
            OmimRecord rec = new OmimRecord();
            rec.setMimNumber(words[0]);
            rec.setType(words[1]);

            if( words.length>=4 ) {
                rec.setGeneId(words[2]);
                rec.setGeneSymbol(words[3]);
                rec.setRecNo(Integer.parseInt(rec.getMimNumber()));
            }

            getGeneInfoFromOmimApi(rec);

            getSession().putRecordToFirstQueue(rec);
        }
    }

    void getGeneInfoFromOmimApi(OmimRecord rec) throws Exception {

        JSONObject jsonTree = getJsonContent(rec.getMimNumber());

        JSONObject root = (JSONObject) jsonTree.get("omim");
        JSONArray records = (JSONArray) root.get("entryList");
        if( records.size()>1 ) {
            throw new Exception("Unexpected: too many records");
        }
        for (Object o : records) {
            JSONObject entry = (JSONObject) ((JSONObject) o).get("entry");

            JSONObject geneMap = (JSONObject) entry.get("geneMap");
            if( geneMap==null ) {
                continue;
            }

            String chr = (String) geneMap.get("chromosomeSymbol");
            Long locStart = (Long) geneMap.get("chromosomeLocationStart");
            Long locEnd = (Long) geneMap.get("chromosomeLocationEnd");
            String geneSymbols = (String) geneMap.get("geneSymbols");

            rec.chr = chr;
            rec.geneSymbols = geneSymbols;
            // start and stop position in json data is 0-based; in RGD, we have 1-based coordinates
            rec.startPos = locStart==null ? 0 : 1+locStart.intValue();
            rec.stopPos = locEnd==null ? 0 : 1+locEnd.intValue();

            JSONArray phenotypeMapList = (JSONArray) geneMap.get("phenotypeMapList");
            if( phenotypeMapList==null ) {
                continue;
            }
            for( Object p: phenotypeMapList ) {
                JSONObject phenotypeMap = (JSONObject) ((JSONObject) p).get("phenotypeMap");
                String psNumber = (String) phenotypeMap.get("phenotypicSeriesNumber");
                Long phenotypeMimNumber = (Long) phenotypeMap.get("phenotypeMimNumber");
                if( phenotypeMimNumber!=null ) {
                    rec.phenotypeMimNumbers.add(phenotypeMimNumber.intValue());
                }
                if( psNumber!=null && phenotypeMimNumber!=null ) {
                    getOmimPSMap().addMapping(psNumber, phenotypeMimNumber.intValue());
                }
            }
        }
    }

    JSONObject getJsonContent(String mimNumber) throws Exception {

        String jsonFileName = "data/json/" + mimNumber + ".json";
        File f = new File(jsonFileName);
        if (f.exists()) {
            // file on disk cannot be older than specified number of days
            long fileLastModifiedTime = f.lastModified();

            // file cutoff date (30 days before the current time)
            long cutoffDate = System.currentTimeMillis() - 30*24*60*60*1000l;

            if( fileLastModifiedTime<cutoffDate ) {
                f.delete();
            }
        }

        if (!f.exists()) {
            FileDownloader fd = new FileDownloader();
            fd.setExternalFile(getOmimApiUrl() + mimNumber);
            fd.setLocalFile(jsonFileName);
            fd.download();

            Thread.sleep(getOmimApiDownloadSleepTimeInMS());
        }

        String jsonContent = Utils.readFileAsString(jsonFileName);

        JSONParser parser = new JSONParser();
        JSONObject jsonTree = null;
        try {
            jsonTree = (JSONObject) parser.parse(jsonContent);
        } catch( Exception e ) {
            System.out.println("WARN: problem parsing file for OMIM:"+mimNumber);

            // delete the file and retry
            if( f.exists() ) {
                f.delete();
                System.out.println("File "+jsonFileName+" deleted. Retrying...");
                return getJsonContent(mimNumber);
            }
        }

        return jsonTree;
    }

    /**
     * download mim2gene.txt file, save it to a local directory;
     * then download morbidmap file
     * @return the name of the local copy of the file
     */
    private String downloadFile() throws Exception {

        FileDownloader downloader = new FileDownloader();
        downloader.setUseCompression(true);

        downloader.setExternalFile(getMim2geneFile());
        downloader.setLocalFile("data/mim2gene.txt.gz");
        return downloader.downloadNew();
    }

    public String getMim2geneFile() {
        return mim2geneFile;
    }

    public void setMim2geneFile(String mim2geneFile) {
        this.mim2geneFile = mim2geneFile;
    }

    public void setOmimApiUrl(String omimApiUrl) {
        this.omimApiUrl = omimApiUrl;
    }

    public String getOmimApiUrl() {
        return omimApiUrl;
    }

    public OmimPS getOmimPSMap() {
        return omimPSMap;
    }

    public void setOmimPSMap(OmimPS omimPSMap) {
        this.omimPSMap = omimPSMap;
    }

    public void setOmimApiDownloadSleepTimeInMS(int omimApiDownloadSleepTimeInMS) {
        this.omimApiDownloadSleepTimeInMS = omimApiDownloadSleepTimeInMS;
    }

    public int getOmimApiDownloadSleepTimeInMS() {
        return omimApiDownloadSleepTimeInMS;
    }

    public void setJsonFileCacheLifeInDays(int jsonFileCacheLifeInDays) {
        this.jsonFileCacheLifeInDays = jsonFileCacheLifeInDays;
    }

    public int getJsonFileCacheLifeInDays() {
        return jsonFileCacheLifeInDays;
    }
}
