package peridot.CLI.Commands;

import peridot.CLI.Command;
import peridot.CLI.PeridotCmd;

/**
 * Created by pentalpha on 25/01/2018.
 */
public class R extends Command {
    public R(String[] args){
        super(args);
    }

    public void evaluateArgs() throws CmdParseException{
        if(args.length > 1){
            fail("'r-peridot ls' takes only zero or one arguments.");
        }else{
            if(args.length == 1) {
                if (isHelpArg(args[0]) == false &&
                        !(args[0].equals("update") || args[0].equals("add")
                                || args[0].equals("rm") || args[0].equals("choose")
                            )
                        )
                {
                    fail("Expected '--help', 'update', 'add' or 'rm': " + args[0]);
                }
            }
        }
    }

    public void run(){
        if(args.length == 0){
            PeridotCmd.listInterpreters();
        }else if(args.length == 1){
            if(isHelpArg(args[0])){
                this.printDetails();
            }else if(args[0].equals("update")){
                PeridotCmd.updateInterpreter();
            }else if(args[0].equals("add")){
                PeridotCmd.addInterpreter();
            }else if(args[0].equals("rm")){
                PeridotCmd.removeInterpreter();
            }else if(args[0].equals("choose")){
                PeridotCmd.setDefaultInterpreter();
            }
        }
    }

    public void defineCmdNameAndDetails(){
        this.commandStr = "r";
        this.detail = "R-Peridot's Command: r\n" +
                "Options:\n" +
                "\tr\t\t\tList all of the R environments\n" +
                "\t\t\t\tcurrently available for R-Peridot\n" +
                "\tr update\tInstall packages required by\n" +
                "\t\t\t\tthe modules in one/all R environments\n" +
                "\tr choose\tChoose default R environment\n" +
                "\tr add\tAdd a new R environment\n" +
                "\tr rm\tRemove a new R environment\n" +
                "\tr --help\tPrint this help message\n";
    }
}
