package peridot.tree;
import peridot.script.RModule;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;

public class PipelineGraph{
    private HashMap<String, PipelineNode> nodes;
    private HashMap<String, RModule> modules;
    private ArrayList<PipelineNode> roots;
    private boolean finished;

    public PipelineGraph(){
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

    private void addVertex(RModule mode){
        PipelineNode node = nodes.get(mode.name);

        if(mode.requiredScripts.size() > 0){
            for(String name : mode.requiredScripts){
                PipelineNode parent = nodes.get(name);
                node.addParent(parent);
                parent.addChildren(nodes.get(mode.name));
            }
        }else{
            roots.add(node);
        }
    }

    public void addNodes(Set<RModule> mods){
        for(RModule mod : mods){
            modules.put(mod.name, mod);
        }

        for(RModule mod : mods){
            PipelineNode node = new PipelineNode(mod.name, false);
            nodes.put(mod.name, node);
        }

        for(RModule mod : mods){
            addVertex(mod);
        }
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
                return modules.get(entry.getKey());
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