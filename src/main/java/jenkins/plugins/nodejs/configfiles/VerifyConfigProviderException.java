package jenkins.plugins.nodejs.configfiles;

/**
 * Signals an error in the a user configuration file when
 * {@link NPMConfig#doVerify()} is called.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
@SuppressWarnings("serial")
public class VerifyConfigProviderException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            the failure message.
     */
    public VerifyConfigProviderException(String message) {
        super(message);
    }

}
