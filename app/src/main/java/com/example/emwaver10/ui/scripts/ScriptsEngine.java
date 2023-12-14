package com.example.emwaver10.ui.scripts;

import com.example.emwaver10.jsobjects.CC1101;
import com.example.emwaver10.jsobjects.Console;
import com.example.emwaver10.jsobjects.Serial;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * This class can be used to evaluate any string expression using the open source,
 * RHINO javascript engine.
 *
 * Add this line - compile 'org.mozilla:rhino:1.7R4'
 * To your module app dependency gradle to install the jar library.
 *
 * Follow my tutorial at
 * {@link} https://github.com/brionsilva/Android-Rhino-Example
 *
 * @author  Brion Mario
 * @version 1.0
 * @since   2017-03-08
 */

public class ScriptsEngine{

    private Context rhino;
    private Scriptable scope;

    private CC1101 cc1101;

    private Serial serial;

    private Console console;

    private ScriptsViewModel scriptsViewModel;
    private static final String SCRIPT = "function evaluate(arithmetic){ return eval(arithmetic); }";

    public ScriptsEngine(CC1101 cc1101, ScriptsViewModel scriptsViewModel, Serial serial, Console console) {
        this.cc1101 = cc1101;
        this.scriptsViewModel = scriptsViewModel;
        this.serial = serial;
        this.console = console;
    }


    public void executeJavaScript(String script) {
        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            scope = rhino.initStandardObjects();
            // Make entire CC1101 class accessible from javascript
            Object wrappedCC = Context.javaToJS(cc1101, scope);
            ScriptableObject.putProperty(scope, "CC1101", wrappedCC);
            // Make entire Serial class accessible from javascript
            Object wrappedSerial = Context.javaToJS(serial, scope);
            ScriptableObject.putProperty(scope, "Serial", wrappedSerial);
            // Make entire Console class accessible from javascript
            Object wrappedConsole = Context.javaToJS(console, scope);
            ScriptableObject.putProperty(scope, "Console", wrappedConsole);
            // Execute the JavaScript script
            rhino.evaluateString(scope, script, "JavaScript", 1, null);
        } catch (RhinoException e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }

}

