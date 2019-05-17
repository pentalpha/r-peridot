package peridot.tree;
import peridot.script.RModule;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/*
* This is NOT A TREE (it can have many roots),
* but I take the creative freedom to call it as such.
* */
public class PipelineTree{
    private HashMap<String, PipelineNode> nodes;
    private ArrayList<PipelineNode> roots;
    private boolean finished;

    public PipelineTree(){
        this.nodes = new HashMap<>();
        this.roots = new ArrayList<>();
        this.finished = false;
    }

    public void addNode(PipelineNode node){
        if(node.hasParents()){
            roots.add(node);
        }
        nodes.put(node.getKey(), node);
    }

    private void updateReady(){
        for(PipelineNode node : roots){
            node.updateReady();
        }
    }

    public synchronized RModule getNext(){
        for(Map.Entry<String, PipelineNode> entry : nodes.entrySet()){
            if(entry.getValue().isReady()){
                entry.getValue().markAsRunning();
                return entry.getValue().getData();
            }
        }

        int not_finished = 0;
        for(Map.Entry<String, PipelineNode> entry : nodes.entrySet()){
            if(entry.getValue().isReady() || entry.getValue().isRunning()){
                not_finished += 1;
            }
        }
        if(not_finished > 0){
            this.finished = true;
        }

        return null;
    }

    public boolean isFinished(){
        return this.finished;
    }

    public synchronized void markAsDone(String name){
        nodes.get(name).markAsDone();
        updateReady();
    }
}