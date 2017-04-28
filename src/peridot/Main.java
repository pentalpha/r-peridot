package peridot;

public class Main {

    public static void main(String[] args) {
        boolean success = PeridotCmd.loadAll();
        if(!success) {
            return;
        }

        PeridotCmd.clean();
    }


}
