package edu.mcw.rgd.dataload.omim;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.pipelines.PipelineRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mtutaj
 * Date: Apr 29, 2011
 * Time: 8:45:07 AM
 * custom omim data, both read from incoming data and from rgd database
 */
public class OmimRecord extends PipelineRecord {

    // incoming data from mim2gene file
    String mimId;
    String type;
    String geneId;
    String geneSymbol;

    // incoming data from morbidmap file
    Set<String> geneSymbols = new HashSet<String>();
    Set<String> cytoPos = new HashSet<String>();
    OmimRecord primaryRecord;


    // matching genes in rgd
    private Set<Gene> rgdGenes = new HashSet<>();

    // to be inserted omim ids
    List<XdbId> omimsForInsert = new ArrayList<XdbId>();

    // to be updated omim ids
    List<Integer> omimsForUpdate = new ArrayList<Integer>();

    // to be removed omim ids
    List<XdbId> omimsForDelete = new ArrayList<XdbId>();

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

    public List<XdbId> getOmimsForDelete() {
        return omimsForDelete;
    }

    public void setOmimsForDelete(List<XdbId> omimsForDelete) {
        this.omimsForDelete = omimsForDelete;
    }
}
