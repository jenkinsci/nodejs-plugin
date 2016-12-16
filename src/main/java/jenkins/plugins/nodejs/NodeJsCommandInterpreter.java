package jenkins.plugins.nodejs;

import hudson.*;
import hudson.model.*;
import jenkins.plugins.nodejs.tools.Messages;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import hudson.tasks.*;
import hudson.util.ArgumentListBuilder;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.Map;

/**
 * This class executes a JavaScript file using node. The file should contain
 * NodeJS script specified in the job configuration.
 * @author cliffano
 */
public class NodeJsCommandInterpreter extends Builder {

    private String command;
    private String nodeJSInstallationName;

    /**
     * Constructs a {@link NodeJsCommandInterpreter} with specified command.
     * @param command
     *            the NodeJS script
     * @param nodeJSInstallationName
     *            the NodeJS label configured in Jenkins
     */
    @DataBoundConstructor
    public NodeJsCommandInterpreter(final String command, final String nodeJSInstallationName) {
        super();
        this.command = command;
        this.nodeJSInstallationName = nodeJSInstallationName;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        return perform(build,launcher,(TaskListener)listener);
    }

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener) throws InterruptedException { // NOSONAR
        FilePath ws = build.getWorkspace();
        if (ws == null) {
            Node node = build.getBuiltOn();
            if (node == null) {
                throw new NullPointerException("no such build node: " + build.getBuiltOnStr());
            }
            throw new NullPointerException("no workspace from node " + node + " which is computer " + node.toComputer() + " and has channel " + node.getChannel());
        }
        FilePath script=null;
        try {
            try {
                script = createScriptFile(ws);
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError(hudson.tasks.Messages.CommandInterpreter_UnableToProduceScript()));
                return false;
            }

            int r;
            try {
                EnvVars envVars = build.getEnvironment(listener);
                // on Windows environment variables are converted to all upper case,
                // but no such conversions are done on Unix, so to make this cross-platform,
                // convert variables to all upper cases.
                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet()) {
                    envVars.put(e.getKey(),e.getValue());
                }

                // Building arguments
                ArgumentListBuilder args = new ArgumentListBuilder();

                NodeJSInstallation selectedInstallation = NodeJSPlugin.instance().findInstallationByName(nodeJSInstallationName);
                selectedInstallation = selectedInstallation.forNode(build.getBuiltOn(), listener);
                selectedInstallation = selectedInstallation.forEnvironment(envVars);
                String exe = selectedInstallation.getExecutable(launcher);
                args.add(exe);

                args.add(script.getRemote());

                r = launcher.launch().cmds(args).envs(envVars).stdout(listener).pwd(ws).join();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace(listener.fatalError(hudson.tasks.Messages.CommandInterpreter_CommandFailed()));
                r = -1;
            }
            return r==0;
        } finally {
            try {
                if(script!=null)
                script.delete();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError(hudson.tasks.Messages.CommandInterpreter_UnableToDelete(script)) );
            } catch (Exception e) {
                e.printStackTrace( listener.fatalError(hudson.tasks.Messages.CommandInterpreter_UnableToDelete(script)) );
            }
        }
    }

    /**
     * Creates a script file in a temporary name in the specified directory.
     * 
     * @throws InterruptedException If the job
     * @throws IOException If the temporary script file could not be deleted
     */
    public FilePath createScriptFile(@Nonnull FilePath dir) throws IOException, InterruptedException { // NOSONAR
        return dir.createTextTempFile("hudson", ".js", this.command, false);
    }

    public String getCommand() {
        return command;
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    /**
     * Provides builder details for the job configuration page.
     * @author cliffano
     * @author nfalco79
     */
    @Extension
    public static final class NodeJsDescriptor extends Descriptor<Builder> {

        /**
         * Default public constructor.
         */
        public NodeJsDescriptor() {
            // public constructor called by reflection
        }

        /**
         * Customise the name of this job step.
         * 
         * @return the builder name
         */
        @Override
        public String getDisplayName() {
            return Messages.NodeJsCommandInterpreter_displayName();
        }

        /**
         * Returns all available NodeJS defined installations.
         * 
         * @return all NodeJS installations
         */
        public NodeJSInstallation[] getInstallations() {
            return NodeJSPlugin.instance().getInstallations();
        }

        /**
         * Return the help file.
         * 
         * @return the help file URL path
         */
        @Override
        public String getHelpFile() {
            return "/plugin/nodejs/help.html";
        }
    }
}
