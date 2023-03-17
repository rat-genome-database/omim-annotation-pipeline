package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class PreProcessor {

    private String mim2geneFile;
    private String mimTitlesFile;
    private String genemap2File;
    private String morbidmapFile;

    private String omimApiUrl;
    private OmimPS omimPSMap;
    private int omimApiDownloadSleepTimeInMS;
    private int jsonFileCacheLifeInDays;
    private String apiKeyFile;
    private String apiKey;

    public List<OmimRecord> downloadAndParseOmimData() throws Exception {

        apiKey = Utils.readFileAsString(getApiKeyFile()).trim();

        // download the files to a local folder
        String mim2geneFileName = downloadFile(getMim2geneFile(), "mim2gene");
        String mimTitlesFileName = downloadFile(getMimTitlesFile(), "mimTitles");
        String genemap2FileName = downloadFile(getGenemap2File(), "genemap2");
        String morbidmapFileName = downloadFile(getMorbidmapFile(), "morbidmap");

        // this is a text file, tab separated
        List<OmimRecord> records = processMim2Gene(mim2geneFileName);

        Map<String, OmimRecord> recordMap = new HashMap<>();
        for( OmimRecord rec: records ) {
            recordMap.put(rec.getMimNumber(), rec);
        }

        processMimTitles(mimTitlesFileName, recordMap);

        return records;
    }

    List<String> loadFile(String fileName) throws Exception {

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

    List<OmimRecord> processMim2Gene(String fileName) throws Exception {

        List<OmimRecord> results = new ArrayList<>();

        // this is a text file, tab separated
        List<String> lines = loadFile(fileName);
        for( String line: lines ) {

            // split line into words
            String[] words = line.split("[\t]", -1);

            // the full data is for gene records
            OmimRecord rec = new OmimRecord();
            rec.setMimNumber(words[0]);
            rec.setType(words[1]);
            rec.setGeneId(words[2]);
            rec.setGeneSymbol(words[3]);
            rec.setEnsemblGeneId(words[4]);

            results.add(rec);
        }

        return results;
    }

    void processMimTitles(String fileName, Map<String, OmimRecord> recordMap) throws Exception {

        // this is a text file, tab separated
        List<String> lines = loadFile(fileName);
        for( String line: lines ) {

            // split line into words
            String[] words = line.split("[\t]", -1);

            String prefix = words[0];
            String omimId = words[1];
            String preferredTitle = words[2];

            OmimRecord rec = recordMap.get(omimId);

            if( prefix.equals("Caret") ) {
                if( preferredTitle.startsWith("MOVED TO") ) {
                    rec.setStatus("moved");
                } else if( preferredTitle.startsWith("REMOVED FROM DATABASE") ) {
                    rec.setStatus("removed");
                } else {
                    throw new Exception("*** unexpected Caret: "+preferredTitle);
                }
            } else {
                rec.setStatus("live");
            }
        }
    }

    List<OmimRecord> processMim2Gene_(String fileName) throws Exception {

        List<OmimRecord> results = new ArrayList<>();

        // this is a text file, tab separated
        List<String> lines = loadFile(fileName);
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
            }
            if( words.length>=5 ) {
                rec.setEnsemblGeneId(words[4]);
            }

            getGeneInfoFromOmimApi(rec);

            results.add(rec);
        }

        return results;
    }

    void getGeneInfoFromOmimApi(OmimRecord rec) throws Exception {

        AtomicInteger retryCount = new AtomicInteger(0);

        JSONObject jsonTree = getJsonContent(rec.getMimNumber(), retryCount);

        JSONObject root = (JSONObject) jsonTree.get("omim");
        JSONArray records = (JSONArray) root.get("entryList");
        if( records.size()>1 ) {
            throw new Exception("Unexpected: too many records");
        }
        for (Object o : records) {
            JSONObject entry = (JSONObject) ((JSONObject) o).get("entry");

            rec.status = (String) entry.get("status");

            JSONObject titles = (JSONObject) entry.get("titles");
            if( titles!=null ) {
                String preferredTitle = (String) titles.get("preferredTitle");
                // example: "DISCS LARGE, DROSOPHILA, HOMOLOG OF, 3; DLG3"
                //  we want only first part, up to first "; "
                int semiPos = preferredTitle.indexOf("; ");
                if( semiPos>0 ) {
                    preferredTitle = preferredTitle.substring(0, semiPos);
                }
                rec.preferredTitle = preferredTitle;

                if( rec.getType().contains("phenotype") ) {
                    rec.setPhenotype(rec.preferredTitle);
                }
            }

            JSONArray phenotypeMapList = null;
            JSONObject geneMap = (JSONObject) entry.get("geneMap");
            if( geneMap!=null ) {
                String chr = (String) geneMap.get("chromosomeSymbol");
                Long locStart = (Long) geneMap.get("chromosomeLocationStart");
                Long locEnd = (Long) geneMap.get("chromosomeLocationEnd");
                String geneSymbols = (String) geneMap.get("geneSymbols");

                rec.chr = chr;
                rec.geneSymbols = geneSymbols;
                // start and stop position in json data is 0-based; in RGD, we have 1-based coordinates
                rec.startPos = locStart==null ? 0 : 1+locStart.intValue();
                rec.stopPos = locEnd==null ? 0 : 1+locEnd.intValue();

                phenotypeMapList = (JSONArray) geneMap.get("phenotypeMapList");
            }


            if( phenotypeMapList==null ) {
                phenotypeMapList = (JSONArray) entry.get("phenotypeMapList");
            }
            if( phenotypeMapList==null ) {
                continue;
            }

            for( Object p: phenotypeMapList ) {
                JSONObject phenotypeMap = (JSONObject) ((JSONObject) p).get("phenotypeMap");
                String psNumbers = (String) phenotypeMap.get("phenotypicSeriesNumber");
                Long phenotypeMimNumber = (Long) phenotypeMap.get("phenotypeMimNumber");
                if( phenotypeMimNumber!=null ) {
                    rec.phenotypeMimNumbers.add(phenotypeMimNumber.intValue());
                }
                if( psNumbers!=null && phenotypeMimNumber!=null ) {
                    getOmimPSMap().addMapping(psNumbers, "OMIM:"+phenotypeMimNumber.intValue());
                }
                if( phenotypeMapList.size()==1 ) {
                    rec.setPhenotype((String) phenotypeMap.get("phenotype"));
                }
            }
        }
    }

    JSONObject getJsonContent(String mimNumber, AtomicInteger retryCount) throws Exception {

        String jsonFileName = "data/json/" + mimNumber + ".json";
        File f = new File(jsonFileName);
        if (f.exists()) {
            // file on disk cannot be older than specified number of days
            long fileLastModifiedTime = f.lastModified();

            // file cutoff date (30 days before the current time)
            long cutoffDate = System.currentTimeMillis() - getJsonFileCacheLifeInDays()*24*60*60*1000l;

            if( fileLastModifiedTime<cutoffDate ) {
                f.delete();
            }
        }

        if (!f.exists()) {

            String omimApiUrl = getOmimApiUrl().replace("{{APIKEY}}", apiKey);

            FileDownloader fd = new FileDownloader();
            fd.setExternalFile(omimApiUrl + mimNumber);
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

            if( f.exists() ) {

                final int maxRetryCount = 5;
                int thisRetryCount = retryCount.incrementAndGet();
                if( thisRetryCount<maxRetryCount ) {

                    // delete the file and retry
                    f.delete();
                    System.out.println("File "+jsonFileName+" deleted. Retrying...");

                    return getJsonContent(mimNumber, retryCount);
                } else {
                    //exceeded maximum retry count
                    throw new RuntimeException("ERROR: problem with downloading the file "+jsonFileName+" -- reached maximum number of download retries == 5 -- aborting the pipeline");
                }
            }
        }

        return jsonTree;
    }

    /**
     * download mim2gene.txt file, save it to a local directory
     * @return the name of the local copy of the file
     */
    private String downloadFile(String url, String fileName) throws Exception {

        FileDownloader downloader = new FileDownloader();
        downloader.setUseCompression(true);
        downloader.setPrependDateStamp(true);

        String url2 = url.replace("{{APIKEY}}", this.apiKey);

        downloader.setExternalFile(url2);
        downloader.setLocalFile("data/"+fileName+".txt.gz");
        return downloader.downloadNew();
    }

    public String getMim2geneFile() {
        return mim2geneFile;
    }

    public void setMim2geneFile(String mim2geneFile) {
        this.mim2geneFile = mim2geneFile;
    }

    public String getMimTitlesFile() {
        return mimTitlesFile;
    }

    public void setMimTitlesFile(String mimTitlesFile) {
        this.mimTitlesFile = mimTitlesFile;
    }

    public String getGenemap2File() {
        return genemap2File;
    }

    public void setGenemap2File(String genemap2File) {
        this.genemap2File = genemap2File;
    }

    public String getMorbidmapFile() {
        return morbidmapFile;
    }

    public void setMorbidmapFile(String morbidmapFile) {
        this.morbidmapFile = morbidmapFile;
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

    public void setApiKeyFile(String apiKeyFile) {
        this.apiKeyFile = apiKeyFile;
    }

    public String getApiKeyFile() {
        return apiKeyFile;
    }
}
