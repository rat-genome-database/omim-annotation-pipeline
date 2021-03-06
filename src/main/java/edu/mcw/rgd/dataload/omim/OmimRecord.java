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
    String mimId;
    String type;
    String phenotype;
    String geneId;
    String geneSymbol;

    // incoming data from OMIM API query
    String status; // 'live', etc
    String preferredTitle;
    String geneSymbols;
    String chr;
    int startPos;
    int stopPos;
    Set<Integer> phenotypeMimNumbers = new HashSet<>();

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
}
