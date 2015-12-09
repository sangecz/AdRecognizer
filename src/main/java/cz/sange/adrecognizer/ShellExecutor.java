package cz.sange.adrecognizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by sange on 15/11/15.
 */
public class ShellExecutor {

    public ShellExecutor() {
    }

    public String executeCommand(String [] commands) throws IOException {

        StringBuilder output = new StringBuilder();

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        // read the output from the command
        output.append("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            output.append(s).append("\n");
        }

        // read any errors from the attempted command
        output.append("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            output.append(s).append("\n");
        }

        return output.toString();

    }

}
