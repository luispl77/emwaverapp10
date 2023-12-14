package com.example.emwaver10.ui.scripts;

import android.content.Intent;
import android.util.Log;

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

public class ScriptsEngine {

    private Context rhino;
    private Scriptable scope;
    private static final String SCRIPT = "function evaluate(arithmetic){ return eval(arithmetic); }";


    public void executeJavaScript(String script, final android.content.Context androidContext) {
        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            scope = rhino.initStandardObjects();

            // Implement the interface
            JsInterface jsInterface = new JsInterface() {
                public void broadcastIntent(String message) {
                    Intent intent = new Intent("com.example.ACTION_USB_DATA");
                    intent.putExtra("data", message);
                    androidContext.sendBroadcast(intent);
                    Log.i("ScriptsEngine", "intent sent");
                }
            };

            // Bind the interface implementation to the JavaScript context
            Object wrappedJsInterface = Context.javaToJS(jsInterface, scope);
            ScriptableObject.putProperty(scope, "AndroidFunction", wrappedJsInterface);

            String bindFunctionScript =
                    "function print(message) { " +
                            "  AndroidFunction.broadcastIntent(message);" +
                            "}";

            rhino.evaluateString(scope, bindFunctionScript, "JavaScript", 1, null);

            // Execute the JavaScript string
            rhino.evaluateString(scope, script, "JavaScript", 1, null);
        } catch (RhinoException e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }


    public Double evaluate(String expression) {
        // Ensure the expression is not null or empty
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty.");
        }

        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1); // Disable optimization for Android compatibility
            scope = rhino.initStandardObjects();

            rhino.evaluateString(scope, SCRIPT, "JavaScript", 1, null);
            Function function = (Function) scope.get("evaluate", scope);

            Object[] params = new Object[]{expression};
            Object result = function.call(rhino, scope, scope, params);

            return (Double) Context.jsToJava(result, Double.class);

        } catch (RhinoException e) {
            e.printStackTrace();
            return null;
        } finally {
            Context.exit();
        }
    }


}

