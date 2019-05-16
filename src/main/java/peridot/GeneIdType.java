/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.util.LinkedList;

/**
 *  Lists the types of IDs available to be used on GeneOntology
 * @author pitagoras
 */
public class GeneIdType {
    public String idName;

    /**
     * The list of default ID types.
     */
    static public LinkedList<String> defaultIDTypes = getDefaultIDTypes();

    /**
     *
     * @param idName ID type.
     */
    public GeneIdType(String idName){
        this.idName = idName;
    }

    /**
     * Starts with the default IdType, 'None'.
     */
    public GeneIdType(){
        this.idName = "None";
    }

    /**
     * Defines the default list of IDs available
     * @return List of ID types.
     */
    public static LinkedList<String> getDefaultIDTypes(){
        LinkedList<String> list = new LinkedList<>();
        String[] ids = {"None", "kegg", "SYMBOL", "ACCNUM", "ALIAS", "ENSEMBL", "ENSEMBLPROT",
                "ENSEMBLTRANS", "ENTREZID", "ENZYME", "EVIDENCE", "EVIDENCEALL",
                "GENENAME", "GO", "GOALL", "IPI", "MAP", "OMIM", "ONTOLOGY",
                "ONTOLOGYALL", "PATH", "PFAM", "PMID", "PROSITE", "REFSEQ",
                "UCSCKG", "UNIGENE", "UNIPROT"};
        for(String id : ids){
            list.add(id);
        }

        return list;
    }
    @Override
    public String toString(){
        return idName;
    }
}
