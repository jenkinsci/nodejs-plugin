package jenkins.plugins.nodejs.configfiles;

/**
 * Signals that an error occurs processing the NPM user configuration file.
 *
 * @author Nikolas Falco
 *
 */
@SuppressWarnings("serial")
public class NpmConfigException extends RuntimeException {

    public NpmConfigException(String message) {
        super(message);
    }

}