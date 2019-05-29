package peridot;

import java.util.Collection;
import java.util.LinkedList;

import peridot.script.AnalysisModule;
import peridot.script.RModule;

public class ConsensusThreshold {

    private static int numberOfPackages = 5;
    
    public static void updateNumberOfPackages(Collection<RModule> modules){
        int nPackages = 0;
        for(RModule module : modules){
            if(module instanceof AnalysisModule){
                nPackages += 1;
            }
        }
        ConsensusThreshold.setNumberOfPackages(nPackages);
    }

    public static void setNumberOfPackages(int n){
        ConsensusThreshold.numberOfPackages = n;
    }

    public String threshold;

    public ConsensusThreshold(){
        this.threshold = "ALL";
    }

    public ConsensusThreshold(int n){
        this.threshold = Integer.toString(n);
    }

    public ConsensusThreshold(String n) throws NumberFormatException{
        if(n.equals("ALL")){
            this.threshold = n;
        }else{
            int x = Integer.parseInt(n);
            this.threshold = Integer.toString(x);
        }
    }

    public static LinkedList<String> getDefaultValues(){
        LinkedList<String> list = new LinkedList<>();
        String[] ids = {"2", "3", "4", "ALL"};
        for(String id : ids){
            list.add(id);
        }

        return list;
    }

    @Override
    public String toString(){
        if(threshold.equals("ALL")){
            return Integer.toString(numberOfPackages);
        }else{
            return threshold;
        }
    }
}