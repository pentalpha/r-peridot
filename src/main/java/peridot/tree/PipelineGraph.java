package peridot.tree;
import peridot.Output;
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
    private HashMap<String, Output> outputs;
    private HashMap<String, Process> processes;
    private ArrayList<PipelineNode> roots;
    private boolean finished;

    public PipelineGraph(){
        this.nodes = new HashMap<>();
        this.modules = new HashMap<>();
        this.outputs = new HashMap<>();
        this.processes = new HashMap<>();
        this.roots = new ArrayList<>();
        this.finished = false;
    }

    public void addNode(PipelineNode node){
        if(node.hasParents()){
            roots.add(node);
        }
        nodes.put(node.getKey(), node);
        outputs.put(node.getKey(), new Output());
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

    public void addNodes(Collection<RModule> mods){
        for(RModule mod : mods){
            modules.put(mod.name, mod);
        }

        for(RModule mod : mods){
            PipelineNode node = new PipelineNode(mod.name, false);
            nodes.put(mod.name, node);
            outputs.put(mod.name, new Output());
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

            int not_finished = 0;
            for(Map.Entry<String, PipelineNode> entry : nodes.entrySet()){
                if(entry.getValue().isReady() || entry.getValue().isRunning()){
                    not_finished += 1;
                }
            }
            
            if(not_finished > 0){
                this.finished = true;
                return null;
            }else{
                //Log.logger.info(not_finished + " modules running, but none is ready. Waiting...");
                //Thread.sleep(1000);
                return null;
            }
        }
    }

    public boolean isFinished(){
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

    public Output getOutput(String name){
        return outputs.get(name);
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
}