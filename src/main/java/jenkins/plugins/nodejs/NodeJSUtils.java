package jenkins.plugins.nodejs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstallation.DescriptorImpl;

/*package*/final class NodeJSUtils {

    private NodeJSUtils() {
        // default constructor
    }

    /**
     * Gets the NodeJS to invoke, or null to invoke the default one.
     *
     * @param name
     *            the name of NodeJS installation
     * @return a NodeJS installation for the given name if exists, {@code null}
     *         otherwise.
     */
    @Nullable
    public static NodeJSInstallation getNodeJS(@Nullable String name) {
        if (name != null) {
            for (NodeJSInstallation installation : getInstallations()) {
                if (name.equals(installation.getName()))
                    return installation;
            }
        }
        return null;
    }

    /**
     * Get all NodeJS installation defined in Jenkins.
     *
     * @return an array of NodeJS tool installation
     */
    @Nonnull
    public static NodeJSInstallation[] getInstallations() {
        DescriptorImpl descriptor = Jenkins.getActiveInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class);
        if (descriptor == null) {
            throw new IllegalStateException("Impossible retrieve NodeJSInstallation descriptor");
        }
        return descriptor.getInstallations();
    }

}