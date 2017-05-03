package peridot;

import peridot.CLI.PeridotCmd;
import peridot.CLI.UserInterface;

public class Main {
    public static void main(String[] args) {
        if(args.length == 0){
            UserInterface.printNoCommand();
        }else{
            boolean success = PeridotCmd.loadAll();
            if(!success) {
                return;
            }
            new peridot.CLI.UserInterface(args);

        }
    }
}
