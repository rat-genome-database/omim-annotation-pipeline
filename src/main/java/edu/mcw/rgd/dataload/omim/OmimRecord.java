package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mtutaj
 * Date: Apr 29, 2011
 * custom omim data, both read from incoming data and from rgd database
 */
public class OmimRecord {

    // incoming data from mim2gene file
    private String mimId;
    private String type;
    private String phenotype;
    private String geneId; // NCBI gene id
    private String geneSymbol;
    private String ensemblGeneId;

    // incoming data from OMIM API query
    private String status; // 'live', etc
    private String preferredTitle;
    private String geneSymbols;
    private String chr;
    private int startPos;
    private int stopPos;
    private Set<Integer> phenotypeMimNumbers = new HashSet<>();

    // matching genes in rgd
    private Set<Gene> rgdGenes = new HashSet<>();

    // to be inserted omim ids
    List<XdbId> omimsForInsert = new ArrayList<XdbId>();

    // to be updated omim ids
    List<Integer> omimsForUpdate = new ArrayList<Integer>();

    private Set<String> flags = new HashSet<>();


    public String getMimNumber() {
        return mimId;
    }

    public void setMimNumber(String mimNumber) {
        this.mimId = mimNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(String phenotype) {
        this.phenotype = phenotype;
    }

    public String getGeneId() {
        return geneId;
    }

    public void setGeneId(String geneId) {
        this.geneId = geneId;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public String getEnsemblGeneId() {
        return ensemblGeneId;
    }

    public void setEnsemblGeneId(String ensemblGeneId) {
        this.ensemblGeneId = ensemblGeneId;
    }

    public Set<Gene> getRgdGenes() {
        return rgdGenes;
    }

    public List<XdbId> getOmimsForInsert() {
        return omimsForInsert;
    }

    public void setOmimsForInsert(List<XdbId> omimsForInsert) {
        this.omimsForInsert = omimsForInsert;
    }

    public List<Integer> getOmimsForUpdate() {
        return omimsForUpdate;
    }

    public void setOmimsForUpdate(List<Integer> omimsForUpdate) {
        this.omimsForUpdate = omimsForUpdate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFlagSet(String flag) {
        return flags.contains(flag);
    }

    public void setFlag(String flag) {
        flags.add(flag);
    }

    public String getMimId() {
        return mimId;
    }

    public void setMimId(String mimId) {
        this.mimId = mimId;
    }

    public String getPreferredTitle() {
        return preferredTitle;
    }

    public void setPreferredTitle(String preferredTitle) {
        this.preferredTitle = preferredTitle;
    }

    public String getGeneSymbols() {
        return geneSymbols;
    }

    public void setGeneSymbols(String geneSymbols) {
        this.geneSymbols = geneSymbols;
    }

    public String getChr() {
        return chr;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public int getStartPos() {
        return startPos;
    }

    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }

    public int getStopPos() {
        return stopPos;
    }

    public void setStopPos(int stopPos) {
        this.stopPos = stopPos;
    }

    public Set<Integer> getPhenotypeMimNumbers() {
        return phenotypeMimNumbers;
    }

    public void setPhenotypeMimNumbers(Set<Integer> phenotypeMimNumbers) {
        this.phenotypeMimNumbers = phenotypeMimNumbers;
    }

    public void setRgdGenes(Set<Gene> rgdGenes) {
        this.rgdGenes = rgdGenes;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
    }
}
