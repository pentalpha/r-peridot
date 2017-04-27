/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot;

import java.util.LinkedList;

/**
 *
 * @author pitagoras
 */
public class GeneIdType {
    public String idName;
    static public LinkedList<String> defaultIDTypes = getDefaultIDTypes();
    public GeneIdType(String idName){
        this.idName = idName;
    }
    public static LinkedList<String> getDefaultIDTypes(){
        LinkedList<String> list = new LinkedList<>();
        list.add("None");
        list.add("kegg");
        list.add("SYMBOL");
        list.add("ACCNUM");       
        list.add("ALIAS");        
        list.add("ENSEMBL");      
        list.add("ENSEMBLPROT"); 
        list.add("ENSEMBLTRANS"); 
        list.add("ENTREZID");     
        list.add("ENZYME");       
        list.add("EVIDENCE");
        list.add("EVIDENCEALL");  
        list.add("GENENAME");     
        list.add("GO");           
        list.add("GOALL");
        list.add("IPI");          
        list.add("MAP");          
        list.add("OMIM");         
        list.add("ONTOLOGY");
        list.add("ONTOLOGYALL");  
        list.add("PATH");         
        list.add("PFAM");         
        list.add("PMID");
        list.add("PROSITE");      
        list.add("REFSEQ");       
        list.add("UCSCKG");
        list.add("UNIGENE");      
        list.add("UNIPROT");
        return list;
    }
    @Override
    public String toString(){
        return idName;
    }
}
