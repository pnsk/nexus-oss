package org.sonatype.nexus.repository.content;

/**
 * Chocolate wimple blaster.
 *
 * @since 3.0
 */
public class InvalidContentException
    extends RuntimeException
{
    public InvalidContentException(final String message) {
        super(message);
    }

    public InvalidContentException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidContentException(final Throwable cause) {
        super(cause);
    }
}
