package peridot.CLI.Commands;

import peridot.CLI.Command;
import peridot.Operations;
import peridot.script.RModule;

/**
 * Command to import R-Peridot modules
 * Created by pentalpha on 4/28/17.
 */
public class IN extends Command{
    public IN(String[] args){
        super(args);
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "in";
        this.detail = "R-Peridot's Command: in\n" +
                "Options:\n" +
                "\tin [file-path]\tImport a module from *." + RModule.binExtension + " file, into R-Peridot\n" +
                "\tin -h\t\t\tDisplays this help message\n" +
                "\tin --help\t\tDisplays this help message\n";
    }

    public void evaluateArgs() throws Command.CmdParseException {
        if(args.length != 1){
            fail("Invalid, 'r-peridot in [args]' takes one argument.");
        }
    }

    public void run(){
        if(isHelpArg(args[0])){
            this.printDetails();
        }else{
            Operations.importModule(args[0]);
        }
    }
}
