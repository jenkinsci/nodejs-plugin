package jenkins.plugins.nodejs.tools;

import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.plugins.nodejs.NodeJSPlugin;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.tasks.SimpleBuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author fcamblor
 */
public class NpmPackagesBuildWrapper extends SimpleBuildWrapper {

    private final String nodeJSInstallationName;

    @DataBoundConstructor
    public NpmPackagesBuildWrapper(String nodeJSInstallationName){
        this.nodeJSInstallationName = nodeJSInstallationName;
    }

    public String getNodeJSInstallationName() {
        return nodeJSInstallationName;
    }

    @Override
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars env) throws IOException, InterruptedException {

        final Computer computer = Computer.currentComputer();

        NodeJSInstallation nodeJSInstallation =
            NodeJSPlugin.instance().findInstallationByName(nodeJSInstallationName);

        nodeJSInstallation = nodeJSInstallation.forNode(computer.getNode(), listener);
        nodeJSInstallation = nodeJSInstallation.forEnvironment(env);
        String binFolder = nodeJSInstallation.getBinFolder();

        context.env("PATH+NODEJS", binFolder);
    }


    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        /**
         * @return available node js installations
         */
        public NodeJSInstallation[] getInstallations() {
            return NodeJSPlugin.instance().getInstallations();
        }

        public String getDisplayName() {
            return jenkins.plugins.nodejs.tools.Messages.NpmPackagesBuildWrapper_displayName();
        }
    }
}
