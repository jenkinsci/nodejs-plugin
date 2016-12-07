package jenkins.plugins.nodejs.tools;

import java.io.IOException;

/**
 * Indicates the failure to detect the OS or CPU.
 */
@SuppressWarnings("serial")
public final class DetectionFailedException extends IOException {
    DetectionFailedException(String message) {
        super(message);
    }

    public DetectionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}