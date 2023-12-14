package com.example.emwaver10.ui.scripts;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

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

public class ScriptsEngine {

    private Context rhino;
    private Scriptable scope;
    private static final String SCRIPT = "function evaluate(arithmetic){ return eval(arithmetic); }";


    public void executeJavaScript(String script, final android.content.Context androidContext, ScriptsViewModel scriptsViewModel) {
        try {
            rhino = Context.enter();
            rhino.setOptimizationLevel(-1);
            scope = rhino.initStandardObjects();

            // Implement the interface
            JsInterface jsInterface = new JsInterface() {
                @Override
                public byte[] sendCommandAndGetResponse(byte[] command, int expectedResponseSize, int busyDelay, long timeoutMillis) {
                    // Send the command
                    sendCommand(command, 0);

                    long startTime = System.currentTimeMillis(); // Start time for timeout

                    // Wait for the response with timeout
                    while (scriptsViewModel.getResponseQueueSize() < expectedResponseSize) {
                        if (System.currentTimeMillis() - startTime > timeoutMillis) {
                            broadcastTerminalString("Timeout occured");
                            return null; // Timeout occurred
                        }
                        try {
                            Thread.sleep(busyDelay); // Wait for it to arrive
                            //todo: try using wait/notify mechanism to really avoid busy waiting
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore the interrupted status
                            return null; // Return or handle the interruption as appropriate
                        }
                    }
                    // Retrieve the response
                    return scriptsViewModel.getAndClearResponse(expectedResponseSize);
                }

                public void broadcastTerminalString(String message) {
                    Intent intent = new Intent(Constants.ACTION_USB_DATA_RECEIVED);
                    intent.putExtra("data", message);
                    androidContext.sendBroadcast(intent);
                }
                @Override
                public void sendCommandString(String userInput, int delayMillis) {
                    Intent intent = new Intent(Constants.ACTION_SEND_DATA_TO_SERVICE);
                    intent.putExtra("userInput", userInput);
                    androidContext.sendBroadcast(intent);
                    try {
                        Thread.sleep(delayMillis); // Wait for it to arrive
                        //todo: try using wait/notify mechanism to really avoid busy waiting
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore the interrupted status
                    }
                }
                @Override
                public void sendCommand(byte[] bytes, int delayMillis) {
                    Intent intent = new Intent(Constants.ACTION_SEND_DATA_BYTES_TO_SERVICE);
                    intent.putExtra("bytes", bytes);
                    androidContext.sendBroadcast(intent);
                }
            };

            // Bind the interface implementation to the JavaScript context
            Object wrappedJsInterface = Context.javaToJS(jsInterface, scope);
            ScriptableObject.putProperty(scope, "AndroidFunction", wrappedJsInterface);

            String bindFunctionScript =
                    "function print(message) { " +
                            "  AndroidFunction.broadcastTerminalString(message);" +
                            "}" +
                    "function sendCommandString(message, delayMillis) { " +
                            "  AndroidFunction.sendCommandString(message, delayMillis);" +
                            "}" +
                    "function sendCommand(message) { " +
                            "  AndroidFunction.sendCommand(message);" +
                            "}" +
                    "function sendCommandAndGetResponse(command, expectedResponseSize, busyDelay, timeoutMillis) { " +
                            "  return AndroidFunction.sendCommandAndGetResponse(command, expectedResponseSize, busyDelay, timeoutMillis);" +
                            "}" +
                    "function sendInit() {\n" +
                            "    var command = [0x74, 0x78, 0x69, 0x6e, 0x69, 0x74]; // 't', 'x', 'i', 'n', 'i', 't' in hex\n" +
                            "    var responseString = \"Transmit init done\\n\";\n" +
                            "    var length = responseString.length;\n" +
                            "    var response = sendCommandAndGetResponse(command, length, 1, 1000);\n" +
                            "    if (response != null) {" +
                            "        var responseStr = '';" +
                            "        for (var i = 0; i < response.length; i++) {" +
                            "            responseStr += String.fromCharCode(response[i] & 0xFF);" +
                            "        }" +
                            "        print('Command Response: ' + responseStr);" +
                            "    }" +
                            "    else" +
                            "   print('response null')" +
                            "}\n";

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

