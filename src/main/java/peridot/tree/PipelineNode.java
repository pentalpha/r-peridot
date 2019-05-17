package peridot.tree;
import peridot.script.RModule;
import java.util.ArrayList;

/**
 * Classe de nó generico com chave para ser utilizado em um Heap.
 *
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
    private RModule data;
    private boolean requiresAllParents;

    /**
     * Construtor para objetos da classe Node
     * @param key Chave do nó.
     * @param data Dados para serem armazenados.
     */
    public PipelineNode(RModule data, ArrayList<PipelineNode> children,
                ArrayList<PipelineNode> parents, boolean requiresAllParents)
    {
        this.key = data.name;
        this.data = data;
        this.children = children;
        this.parents = parents;
        this.requiresAllParents = requiresAllParents;
        this.status = Status.QUEUE;
    }

    public void updateReady(){
        if(status == Status.QUEUE){
            int parents_done = 0;
            int parents_failed = 0;
            for (int i = 0; i < parents.size(); i++) {
                if (parents.get(i).isDone()) {
                    parents_done += 1;
                }else if (parents.get(i).isFailed()) {
                    parents_failed += 1;
                }
            }

            if(this.requiresAllParents) {
                if(parents_failed > 0){
                    this.status = Status.FAILED;
                }else if(parents_done == this.parents.size()){
                    this.status = Status.READY;
                }
            }else{
                if(parents_done != 0){
                    this.status = Status.READY;
                }else if(parents_failed == this.parents.size()){
                    this.status = Status.FAILED;
                }
            }
        }

        if(status == Status.DONE || status == Status.FAILED){
            for(int i = 0; i < children.size(); i++){
                children.get(i).updateReady();
            }
        }
    }

    public boolean isRunning(){
        return this.status == Status.RUNNING;
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

    public void markAsRunning(){
        this.status = Status.RUNNING;
    }

    public void markAsDone(){
        this.status = Status.DONE;
    }

    public String getKey(){
        return key;
    }

    public RModule getData(){
        return data;
    }

    public boolean hasParents(){
        return this.parents.size() != 0;
    }
}