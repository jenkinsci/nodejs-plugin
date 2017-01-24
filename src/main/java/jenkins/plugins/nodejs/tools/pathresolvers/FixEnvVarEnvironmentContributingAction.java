package jenkins.plugins.nodejs.tools.pathresolvers;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;

//@Issue("JENKINS-26583")
public class FixEnvVarEnvironmentContributingAction implements EnvironmentContributingAction {

    private transient NodeJSInstallation installation;

    public FixEnvVarEnvironmentContributingAction(NodeJSInstallation installation) {
        this.installation = installation;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        String home = installation.getHome();
        // check if not already added
        if (env.get("PATH") == null || !env.get("PATH").contains(home)) {
            installation.buildEnvVars(env);
        }
    }

}