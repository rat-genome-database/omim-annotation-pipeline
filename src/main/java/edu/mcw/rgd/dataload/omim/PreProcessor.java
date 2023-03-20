package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedReader;
import java.util.*;

/**
 * @author mtutaj
 * @since Apr 28, 2011
 */
public class PreProcessor {

    private String mim2geneFile;
    private String mimTitlesFile;
    private String genemap2File;
    private String morbidmapFile;
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
        processGeneMap2(genemap2FileName, recordMap);
        processMorbidMap(morbidmapFileName, recordMap);

        return records;
    }

    List<String> loadFile(String fileName) throws Exception {

        // this is a text file, tab separated
        BufferedReader reader = Utils.openReader(fileName);
        List<String> lines = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            // skip comment lines
            if( !line.startsWith("#") ) {
                lines.add(line);
            }
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
            String[] words = line.split("[\\t]", -1);

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
            String[] words = line.split("[\\t]", -1);

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

                // example: "DISCS LARGE, DROSOPHILA, HOMOLOG OF, 3; DLG3"
                //  we want only first part, up to first "; "
                int semiPos = preferredTitle.indexOf("; ");
                if( semiPos>0 ) {
                    preferredTitle = preferredTitle.substring(0, semiPos);
                }
                rec.setPreferredTitle(preferredTitle);

                if( rec.getType().contains("phenotype") ) {
                    rec.setPhenotype(rec.getPreferredTitle());
                }
            }
        }
    }

    void processGeneMap2(String fileName, Map<String, OmimRecord> recordMap) throws Exception {

        // this is a text file, tab separated
        List<String> lines = loadFile(fileName);
        for( String line: lines ) {

            // split line into words
            String[] words = line.split("[\\t]", -1);

            String chr = words[0]; // 'chr1'
            String geneSymbols = words[6]; // f.e. RBMY1A1, RBM1, YRRM1, RBM2
            String omimId = words[5]; // MIM Number for Gene/Locus (OMIM)

            int startPos = 0;
            int stopPos = 0;
            if( !Utils.isStringEmpty(words[1]) ) {
                startPos = Integer.parseInt(words[1]);
            }
            if( !Utils.isStringEmpty(words[2]) ) {
                stopPos = Integer.parseInt(words[2]);
            }


            OmimRecord rec = recordMap.get(omimId);
            rec.setChr(chr.substring(3)); // remove prefix 'chr'
            rec.setStartPos(startPos);
            rec.setStopPos(stopPos);
            rec.setGeneSymbols(geneSymbols);
        }
    }

    void processMorbidMap(String fileName, Map<String, OmimRecord> recordMap) throws Exception {

        // this is a text file, tab separated
        List<String> lines = loadFile(fileName);
        for( String line: lines ) {

            // split line into words
            String[] words = line.split("[\\t]", -1);

            String phenotypeStr = words[0]; //
            String omimId = words[2]; // MIM Number for Gene/Locus (OMIM)
            String phenotype = null;

            // parse out phenotype mim id
            String phenotypeMimId = null;
            // example: '46XX sex reversal 1, 400045 (4)'
            //
            // strip out phenotype mapping key
            int lastParenthesisPos = phenotypeStr.lastIndexOf(" (");
            if( lastParenthesisPos <=0 ) {
                throw new Exception("was expecting phenotype mapping key! "+phenotypeStr);
            }
            String phenotype2 = phenotypeStr.substring(0, lastParenthesisPos);
            // if available, phenotype MIM id will be in format ', xxxxxx', f.e. ', 617321'
            if( phenotype2.length()> 8 ) {
                String tmp = phenotype2.substring(phenotype2.length() - 8);
                boolean isValidMimId = tmp.matches(", \\d{6}");
                if( isValidMimId ) {
                    phenotypeMimId = tmp.substring(2, 2+6);
                    phenotype = phenotype2.substring(0, phenotype2.length()-8);
                } else {
                    phenotype = phenotype2;
                }
            } else {
                phenotype = phenotype2;
            }

            // if phenotypeMimId is not give, set it to gene mim id
            if( phenotypeMimId==null ) {
                phenotypeMimId = omimId;
            }


            OmimRecord rec = recordMap.get(omimId);
            rec.setPhenotype(phenotype);
            rec.getPhenotypeMimNumbers().add( Integer.parseInt(phenotypeMimId) );

            rec = recordMap.get(phenotypeMimId);
            rec.setPhenotype(phenotype);
        }
    }

/*
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
*/
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

    public void setApiKeyFile(String apiKeyFile) {
        this.apiKeyFile = apiKeyFile;
    }

    public String getApiKeyFile() {
        return apiKeyFile;
    }
}
