package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.pipelines.RecordPreprocessor;
import edu.mcw.rgd.process.FileDownloader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: Apr 28, 2011
 * Time: 5:30:25 PM
 */
public class PreProcessor extends RecordPreprocessor {

    private String version;
    private String mim2geneFile;
    private String morbidmapFile;
    private String localMorbidMapFile;
    private String genemapFile;
    private String genemapKeyFile;
    private String omimTxtFile;

    @Override
    public void process() throws Exception {

        // download the file to a local folder
        String fileName = downloadFile();

        // for testing
        //String fileName = "data/20121024_mim2gene.txt";
        //localMorbidMapFile = "data/20121024_morbidmap";

        // map of all OMIM records keyed by omim id
        Map<String, OmimRecord> records = new HashMap<String, OmimRecord>();

        // this is a text file, tab separated
        processMim2Gene(fileName, records);

        processMorbidMap(localMorbidMapFile, records);

        for( OmimRecord rec: records.values() ) {
            getSession().putRecordToFirstQueue(rec);
        }
    }

    void processMim2Gene(String fileName, final Map<String, OmimRecord> records) throws Exception {

        // this is a text file, tab separated
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
        String line;
        while( (line=reader.readLine())!=null ) {
            // skip comment lines
            if( line.startsWith("#") )
                continue;
            // split line into words
            String[] words = line.split("[\t]", -1);
            if( words.length<4 )
                continue; // there must be at least four columns present

            // parse the data
            OmimRecord rec = new OmimRecord();
            rec.setMimNumber(words[0]);
            rec.setType(words[1]);
            rec.setGeneId(words[2]);
            rec.setGeneSymbol(words[3]);
            rec.setRecNo(Integer.parseInt(rec.getMimNumber()));

            records.put(rec.getMimNumber(), rec);
        }

        // cleanup
        reader.close();
    }

    void processMorbidMap(String fileName, final Map<String, OmimRecord> records) throws Exception {

        // this is a text file, tab separated
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
        String line;
        while( (line=reader.readLine())!=null ) {
            // skip comment lines
            if( line.startsWith("#") )
                continue;
            // split line into words
            String[] words = line.split("[|]", -1);
            if( words.length<4 )
                continue; // there must be at least four columns present

            // parse the data
            String disorder = words[0];
            String geneSymbols = words[1];
            String primaryOmimId = words[2];
            String cytoPos = words[3];

            // parse phenotype
            // f.e. Aortic aneurysm, familial thoracic 4, 132900 (3)|MYH11, AAT4, FAA4|160745|16p13.11
            // will have secondaryOmimId = '132900'
            int lastCommaPos = disorder.lastIndexOf(", ");
            if( lastCommaPos>0 ) {

                String omimId;

                // see if there are parentheses -- truncate them
                int parPos = disorder.indexOf('(', lastCommaPos);
                if( parPos>0 )
                    omimId = disorder.substring(lastCommaPos+2, parPos);
                else
                    omimId = disorder.substring(lastCommaPos+2);
                omimId = omimId.trim();

                OmimRecord rec = records.get(omimId);
                if( rec==null ) {
                    rec = new OmimRecord();
                    rec.setMimNumber(omimId);
                }
                rec.primaryRecord = records.get(primaryOmimId);
                rec.geneSymbols.addAll(Arrays.asList(geneSymbols.split(", ")));
                rec.cytoPos.add(cytoPos);

            }
        }

        // cleanup
        reader.close();
    }

    /**
     * download mim2gene.txt file, save it to a local directory;
     * then download morbidmap file
     * @return the name of the local copy of the file
     */
    private String downloadFile() throws Exception {

        FileDownloader downloader = new FileDownloader();

        downloader.setExternalFile(getMorbidmapFile());
        downloader.setLocalFile("data/morbidmap.txt.gz");
        downloader.setPrependDateStamp(true); // prefix downloaded files with the current date
        downloader.setUseCompression(true);
        localMorbidMapFile = downloader.downloadNew();

        downloader.setExternalFile(getGenemapFile());
        downloader.setLocalFile("data/genemap.txt.gz");
        downloader.downloadNew();

        downloader.setExternalFile(getGenemapKeyFile());
        downloader.setLocalFile("data/genemap2.txt.gz");
        downloader.downloadNew();

        downloader.setExternalFile(getOmimTxtFile());
        downloader.setLocalFile("data/mimTitles.txt.gz");
        downloader.downloadNew();

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

    public void setMorbidmapFile(String morbidmapFile) {
        this.morbidmapFile = morbidmapFile;
    }

    public String getMorbidmapFile() {
        return morbidmapFile;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setGenemapFile(String genemapFile) {
        this.genemapFile = genemapFile;
    }

    public String getGenemapFile() {
        return genemapFile;
    }

    public void setGenemapKeyFile(String genemapKeyFile) {
        this.genemapKeyFile = genemapKeyFile;
    }

    public String getGenemapKeyFile() {
        return genemapKeyFile;
    }

    public void setOmimTxtFile(String omimTxtFile) {
        this.omimTxtFile = omimTxtFile;
    }

    public String getOmimTxtFile() {
        return omimTxtFile;
    }
}
