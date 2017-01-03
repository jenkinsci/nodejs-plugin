package jenkins.plugins.nodejs;

import hudson.Plugin;
import hudson.model.Items;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

/**
 * @author fcamblor
 * @author Nikolas Falco
 * @deprecated Do not use this anymore. This class will be removed, actually is
 *             kept to migrate persistence.
 */
@Deprecated
public class NodeJSPlugin extends Plugin {

    private NodeJSInstallation[] installations;

    @Override
    public void start() throws Exception {
        super.start();
        Items.XSTREAM2.addCompatibilityAlias("jenkins.plugins.nodejs.tools.NpmPackagesBuildWrapper", NodeJSBuildWrapper.class);
        Items.XSTREAM2.addCompatibilityAlias("jenkins.plugins.nodejs.NodeJsCommandInterpreter", NodeJSCommandInterpreter.class);
        try {
            load();
        } catch (IOException e) { // NOSONAR
            // ignore read XStream errors
        }
    }

    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        // If installations have been read in nodejs.xml, let's convert them to
        // the default persistence
        if (installations != null) {
            setInstallations(installations);
            getConfigXml().delete();
            installations = null;
        }
    }

    /**
     * Get all available NodeJS defined installation.
     * 
     * @return an array of defined {@link NodeJSInstallation}
     * @deprecated Use {@link NodeJSUtils#getInstallations()} instead of this.
     */
    @Deprecated
    @Nonnull
    public NodeJSInstallation[] getInstallations() {
        return NodeJSUtils.getInstallations();
    }

    @Nullable
    public NodeJSInstallation findInstallationByName(@Nullable String name) {
        return NodeJSUtils.getNodeJS(name);
    }

    /**
     * Set the NodeJS installation.
     * 
     * @param installations an array of {@link NodeJSInstallation}
     * @deprecated You should not set manually system NodeJS installation, in
     *             case use the standard
     *             {@link Jenkins#getDescriptorByType(Class)
     *             #setInstallations(NodeJSInstallation[])}
     */
    @Deprecated
    public void setInstallations(@Nonnull NodeJSInstallation[] installations) {
        DescriptorImpl descriptor = Jenkins.getInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class); // NOSONAR
        if (descriptor != null) {
            descriptor.setInstallations(installations);
        }
    }

}