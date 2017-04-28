package peridot;

import peridot.Archiver.Places;
import peridot.script.RScript;
import peridot.script.Task;

import javax.swing.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) {
        boolean success = Peridot.loadAll();
        if(!success) {
            return;
        }

        Peridot.clean();
    }


}
