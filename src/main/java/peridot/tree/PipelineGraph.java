package peridot.tree;
import peridot.script.RModule;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Collection;

public class PipelineGraph{
    private HashMap<String, PipelineNode> nodes;
    private HashMap<String, RModule> modules;
    private HashMap<String, Process> processes;
    //public HashMap<String, Boolean> abort_flag;
    private ArrayList<PipelineNode> roots;
    private int n_nodes;
    private boolean finished;

    public PipelineGraph(){
        this.nodes = new HashMap<>();
        this.modules = new HashMap<>();
        this.processes = new HashMap<>();
        //this.abort_flag = new HashMap<>();
        this.roots = new ArrayList<>();
        this.finished = false;
        n_nodes = 0;
    }

    public void addNode(PipelineNode node){
        if(node.hasParents()){
            roots.add(node);
        }
        nodes.put(node.getKey(), node);
        //abort_flag.put(node.getKey(), Boolean(false));
        n_nodes += 1;
    }

    private void addVertex(RModule mode){
        PipelineNode node = nodes.get(mode.name);

        if(mode.requiredScripts.size() > 0){
            for(String name : mode.requiredScripts){
                PipelineNode parent = nodes.get(name);
                if(parent != null){
                    node.addParent(parent);
                    parent.addChildren(nodes.get(mode.name));
                }
            }
        }else{
            roots.add(node);
        }
    }

    public void addNodes(Collection<RModule> mods){
        for(RModule mod : mods){
            modules.put(mod.name, mod);
            n_nodes += 1;
            //abort_flag.put(node.getKey(), Boolean(false));
        }

        for(RModule mod : mods){
            PipelineNode node = new PipelineNode(mod.name, mod.needsAllDependencies);
            nodes.put(mod.name, node);
        }

        for(RModule mod : mods){
            addVertex(mod);
        }

        updateReady();
    }


    private void updateReady(){
        for(PipelineNode node : roots){
            node.updateStatus();
        }
    }

    public synchronized RModule getNext(){
        while(true){
            for(Map.Entry<String, PipelineNode> entry : nodes.entrySet()){
                if(entry.getValue().isReady()){
                    entry.getValue().markAsRunning();
                    return modules.get(entry.getKey());
                }
            }
            return null;
        }
    }

    public synchronized boolean isFinished(){
        int modules_used = 0;
        for(Map.Entry<String, PipelineNode> entry : nodes.entrySet()){
            if(entry.getValue().isFailed() || entry.getValue().isDone() || entry.getValue().isRunning()){
                modules_used += 1;
            }
        }
        
        if(modules_used == nodes.values().size()){
            this.finished = true;
        }
        return this.finished;
    }

    public synchronized void markAsDone(String name){
        nodes.get(name).markAsDone();
        updateReady();
    }

    public synchronized void markAsFailed(String name){
        nodes.get(name).markAsFailed();
        updateReady();
    }

    public Process getProcess(String name){
        return processes.get(name);
    }

    public synchronized void set_process(String name, Process process){
        processes.put(name, process);
    }

    public synchronized void abort(String name){
        Process p = processes.get(name);
        if(p != null){
            p.destroyForcibly();
        }else if (nodes.get(name).isReady() || nodes.get(name).isQueued()){
            markAsFailed(name);
        }
    }

    public synchronized void abortAll(){
        for(String name : nodes.keySet()){
            abort(name);
        }
    }

    public synchronized int number_of_nodes_at_status(PipelineNode.Status status){
        int n = 0;
        for(PipelineNode node : nodes.values()){
            if (node.getStatus() == status){
                n += 1;
            }
        }
        return n;
    }

    public synchronized int number_of_finished_nodes(){
        int n = 0;
        for(PipelineNode node : nodes.values()){
            if (node.getStatus() == PipelineNode.Status.FAILED
            || node.getStatus() == PipelineNode.Status.DONE){
                n += 1;
            }
        }
        return n;
    }

    public int number_of_nodes(){
        return n_nodes;
    }

    public Collection<PipelineNode> getNodes(){
        return nodes.values();
    }
}