package peridot.tree;
import peridot.Log;
import peridot.script.RModule;
import java.util.ArrayList;

/**
 * Classe de nó generico com chave para ser utilizado em um Heap.
 * @author Pitagoras Alves
 * @version 1.0
 */
public class PipelineNode
{
    public enum Status{
        QUEUE,
        READY,
        RUNNING,
        DONE,
        FAILED
    }

    private ArrayList<PipelineNode> children;
    private ArrayList<PipelineNode> parents;
    private Status status;
    private String key;
    //private RModule data;
    private boolean requiresAllParents;

    public PipelineNode(String name, ArrayList<PipelineNode> children,
                ArrayList<PipelineNode> parents, boolean requiresAllParents)
    {
        this.key = name;
        //this.data = data;
        this.children = children;
        this.parents = parents;
        this.requiresAllParents = requiresAllParents;
        this.status = Status.QUEUE;
    }

    public PipelineNode(String name, boolean requiresAllParents)
    {
        this.key = name;
        this.requiresAllParents = requiresAllParents;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.status = Status.QUEUE;
    }

    public void addChildren(PipelineNode node){
        children.add(node);
    }

    public void addParent(PipelineNode node){
        parents.add(node);
    }

    public void updateStatus(){
        if(status == Status.QUEUE){
            //Log.logger.info(key + " is in queue");
            int parents_done = 0;
            int parents_failed = 0;
            //Log.logger.info("parents:");
            for(PipelineNode parent : parents){
            //for (int i = 0; i < parents.size(); i++) {
                if (parent.isDone()) {
                    parents_done += 1;
                    //Log.logger.info(parent.getKey() + " is done");
                }else if (parent.isFailed()) {
                    parents_failed += 1;
                    //Log.logger.info(parent.getKey() + " has failed");
                }
            }

            boolean all_parents_finished = this.parents.size() == parents_done + parents_failed;

            if(all_parents_finished){
                if(this.requiresAllParents) {
                    if(parents_failed > 0){
                        this.status = Status.FAILED;
                        Log.logger.info(key + " has pre-failed :(, because one of the parents has failed");
                    }else if(parents_done == this.parents.size()){
                        this.status = Status.READY;
                        Log.logger.info(key + " is ready now!");
                    }
                }else{
                    if(parents_done != 0 || this.parents.size() == 0){
                        this.status = Status.READY;
                        Log.logger.info(key + " is ready now!");
                    }else if(parents_failed == this.parents.size() && this.parents.size() != 0){
                        this.status = Status.FAILED;
                        Log.logger.info(key + " has pre-failed :(");
                    }
                }
            }
        }

        if(status == Status.DONE || status == Status.FAILED){
            for(PipelineNode child : children){
                child.updateStatus();
            }
        }
    }

    public boolean isRunning(){
        return this.status == Status.RUNNING;
    }

    public boolean isQueued(){
        return this.status == Status.QUEUE;
    }

    public boolean isReady(){
        return this.status == Status.READY;
    }

    public boolean isFailed(){
        return this.status == Status.FAILED;
    }

    public boolean isDone(){
        return this.status == Status.DONE;
    }

    public Status getStatus(){
        return this.status;
    }

    public void markAsRunning(){
        this.status = Status.RUNNING;
    }

    public void markAsDone(){
        this.updateStatus();
        this.status = Status.DONE;
    }

    public void markAsFailed(){
        this.updateStatus();
        this.status = Status.FAILED;
    }

    public String getKey(){
        return key;
    }

    /*public RModule getData(){
        return data;
    }*/

    public boolean hasParents(){
        return this.parents.size() != 0;
    }
}