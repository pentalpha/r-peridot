/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peridot.script.internalScript;

import java.io.InputStream;

/**
 *
 * @author Pitágoras Alves
 */
public final class ScriptStreamer {
    private ScriptStreamer(){
        throw new AssertionError();
    }
    public static InputStream getScriptStream(String name){
        return ScriptStreamer.class.getResourceAsStream(name);
    }
}
