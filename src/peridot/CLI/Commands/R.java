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
        if(args.length >= 3){
            System.out.println(args[0] + "     " + args[1] + "    " + args[2]);
            fail("Too many arguments for 'r-peridot r'");
        }else{
            if(args.length == 1) {
                if (isHelpArg(args[0]) == false &&
                        !(args[0].equals("update") || args[0].equals("rm")
                                || args[0].equals("choose") ||args[0].equals("run"))
                        )
                {
                    fail("Expected '--help', 'update', 'run', 'rm' or 'choose': " + args[0]);
                }
            }else if(args.length == 2){
                if(!args[0].equals("add")){
                    fail("Expected a 'r-peridot add <path/to/environment>': " + args[0] + " " + args[1]);
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
            }else if(args[0].equals("rm")){
                PeridotCmd.removeInterpreter();
            }else if(args[0].equals("choose")){
                PeridotCmd.setDefaultInterpreter();
            }else if(args[0].equals("run")){
                PeridotCmd.runDefaultR();
            }
        }else if(args.length == 2){
            if(args[0].equals("add")){
                PeridotCmd.addInterpreter(args[1]);
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
                "\t\t\t\tthe modules in a R environment\n" +
                "\tr choose\tChoose default R environment\n" +
                "\tr add <path/to/environment>\n\t\t\t\tAdd a new R environment\n" +
                "\tr rm\t\tRemove a new R environment\n" +
                "\tr --help\tPrint this help message\n";
    }
}
