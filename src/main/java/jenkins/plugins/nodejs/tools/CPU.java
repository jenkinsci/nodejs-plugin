package jenkins.plugins.nodejs.tools;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import hudson.model.Node;

/**
 * CPU type.
 */
public enum CPU {
    i386, amd64;

    /**
     * Determines the CPU of the given node.
     * @throws IOException
     * @throws InterruptedException
     * @throws DetectionFailedException
     */
    public static CPU of(Node node) throws DetectionFailedException, InterruptedException, IOException {
        return detect(node.toComputer().getSystemProperties());
    }

    /**
     * Determines the CPU of the current JVM.
     * @throws DetectionFailedException
     */
    public static CPU current() throws DetectionFailedException {
        return detect(System.getProperties());
    }

    private static CPU detect(Map<Object, Object> systemProperties) throws DetectionFailedException {
        String arch = ((String) systemProperties.get("os.arch")).toLowerCase(Locale.ENGLISH);
        if (arch.contains("amd64") || arch.contains("86_64")) {
            return amd64;
        }
        if (arch.contains("86")) {
            return i386;
        }
        throw new DetectionFailedException("Unknown CPU architecture: " + arch);
    }

}