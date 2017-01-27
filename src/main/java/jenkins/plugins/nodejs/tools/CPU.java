package jenkins.plugins.nodejs.tools;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import hudson.model.Computer;
import hudson.model.Node;

/**
 * CPU type.
 */
public enum CPU {
    i386, amd64;

    /**
     * Determines the CPU of the given node.
     *
     * @param node
     *            the computer node
     * @return a CPU value of the cpu of the given node
     * @throws IOException in case of IO issues with the remote Node
     * @throws InterruptedException in case the job is interrupted by user
     */
    public static CPU of(@Nonnull Node node) throws IOException, InterruptedException {
        Computer computer = node.toComputer();
        if (computer == null) {
            throw new DetectionFailedException("Node offline");
        }
        return detect(computer.getSystemProperties());
    }

    /**
     * Determines the CPU of the current JVM.
     *
     * @return the current CPU
     * @throws DetectionFailedException
     *             when the current platform node is not supported.
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