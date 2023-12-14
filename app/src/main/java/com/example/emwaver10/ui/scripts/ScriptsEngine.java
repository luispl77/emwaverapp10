package com.example.emwaver10.ui.scripts;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

import com.example.emwaver10.CC1101;
import com.example.emwaver10.CommandSender;
import com.example.emwaver10.Constants;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
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

    private ScriptsViewModel scriptsViewModel;
    private static final String SCRIPT = "function evaluate(arithmetic){ return eval(arithmetic); }";

    public ScriptsEngine(CC1101 cc1101, ScriptsViewModel scriptsViewModel) {
        this.cc1101 = cc1101;
        this.scriptsViewModel = scriptsViewModel;
    }


    public void executeJavaScript(String script) {
        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            scope = rhino.initStandardObjects();
            // Bind the interface implementation to the JavaScript context
            Object wrappedCC = Context.javaToJS(cc1101, scope);
            ScriptableObject.putProperty(scope, "CC1101", wrappedCC);
            // Execute the JavaScript string
            rhino.evaluateString(scope, script, "JavaScript", 1, null);
        } catch (RhinoException e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }

}

