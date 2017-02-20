package jenkins.plugins.nodejs.tools;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import hudson.model.Computer;
import hudson.model.Node;

/**
 * Supported platform.
 */
public enum Platform {
    LINUX("node", "npm", "bin"), WINDOWS("node.exe", "npm.cmd", ""), OSX("node", "npm", "bin");

    /**
     * Choose the file name suitable for the downloaded Node bundle.
     */
    public final String nodeFileName;
    /**
     * Choose the file name suitable for the npm bundled with NodeJS.
     */
    public final String npmFileName;
    /**
     * Choose the folder path suitable bin folder of the bundle.
     */
    public final String binFolder;

    Platform(String nodeFileName, String npmFileName, String binFolder) {
        this.nodeFileName = nodeFileName;
        this.npmFileName = npmFileName;
        this.binFolder = binFolder;
    }

    public boolean is(String line) {
        return line.contains(name());
    }

    /**
     * Determines the platform of the given node.
     *
     * @param node
     *            the computer node
     * @return a platform value that represent the given node
     * @throws DetectionFailedException
     *             when the current platform node is not supported.
     */
    public static Platform of(Node node) throws DetectionFailedException {
        try {
            Computer computer = node.toComputer();
            if (computer == null) {
                throw new DetectionFailedException("No executor available on Node " + node.getDisplayName());
            }
            return detect(computer.getSystemProperties());
        } catch (IOException | InterruptedException e) {
            throw new DetectionFailedException("Error getting system properties on remote Node", e);
        }
    }

    public static Platform current() throws DetectionFailedException {
        return detect(System.getProperties());
    }

    private static Platform detect(Map<Object, Object> systemProperties) throws DetectionFailedException {
        String arch = ((String) systemProperties.get("os.name")).toLowerCase(Locale.ENGLISH);
        if (arch.contains("linux")) {
            return LINUX;
        }
        if (arch.contains("windows")) {
            return WINDOWS;
        }
        if (arch.contains("mac")) {
            return OSX;
        }
        throw new DetectionFailedException("Unknown OS name: " + arch);
    }

}