package peridot;

import java.util.LinkedList;
import java.util.List;

public class Organism {
    public String speciesName;

    /**
     * The list of default ID types.
     */
    static public List<String> defaultDBs = getAvailableOrganisms();

    /**
     *
     * @param speciesName   The species of the Gene Ontology.
     */
    public Organism(String speciesName){
        this.speciesName = speciesName;
    }

    /**
     * Defines the default list of IDs available
     * @return List of ID types.
     */
    public static List<String> getAvailableOrganisms(){
        List<String> dbs = new LinkedList<>();
        dbs.add("Human");
        dbs.add("Mouse");
        dbs.add("Fly");
        return dbs;
    }

    @Override
    public String toString(){
        return speciesName;
    }
}