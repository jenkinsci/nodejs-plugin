package hudson.plugins.nodejs;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.tasks.CommandInterpreter;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * This class executes a JavaScript file using node. The file should contain
 * NodeJS script specified in the job configuration.
 * @author cliffano
 */
public class NodeJsCommandInterpreter extends CommandInterpreter {

    /**
     * Constructs a {@link NodeJsCommandInterpreter} with specified command.
     * @param command
     *            the NodeJS script
     */
    public NodeJsCommandInterpreter(final String command) {
        super(command);
    }

    /**
     * Builds the command line.
     * @param filePath
     * @return an array containing node command and the script location
     */
    public String[] buildCommandLine(final FilePath filePath) {
        return new String[] { "node", filePath.getRemote() };
    }

    /**
     * @return the command
     */
    public String getContents() {
        return command;
    }

    /**
     * @return the file extension
     */
    public String getFileExtension() {
        return ".js";
    }

    /**
     * @return the descriptor
     */
    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final NodeJsDescriptor DESCRIPTOR = new NodeJsDescriptor();

    /**
     * Provides builder details for the job configuration page.
     * @author cliffano
     */
    public static final class NodeJsDescriptor extends Descriptor<Builder> {

        /**
         * Constructs a {@link NodeJsDescriptor}.
         */
        private NodeJsDescriptor() {
            super(NodeJsCommandInterpreter.class);
        }

        /**
         * Retrieve the NodeJS script from the job configuration page, pass it
         * to a new command interpreter.
         * @param request
         *            the Stapler request
         * @param json
         *            the JSON object
         * @return new instance of {@link NodeJsCommandInterpreter}
         */
        @Override
        public Builder newInstance(final StaplerRequest request,
                final JSONObject json) {
            return new NodeJsCommandInterpreter(json
                    .getString("nodejs_command"));
        }

        /**
         * @return the builder instruction
         */
        public String getDisplayName() {
            return "Execute NodeJS script";
        }

        /**
         * @return the help file URL path
         */
        @Override
        public String getHelpFile() {
            return "/plugin/nodejs/help.html";
        }
    }
}
