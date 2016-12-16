package jenkins.plugins.nodejs.tools;

import java.text.MessageFormat;
import java.util.StringTokenizer;

/**
 * Version range. A version range is an interval describing a set of
 * {@link NodeJSVersion versions}.
 *
 * <p>
 * A range has a left (lower) endpoint and a right (upper) endpoint. Each
 * endpoint can be open (excluded from the set) or closed (included in the set).
 *
 * <p>
 * {@code NodeJSVersionRange} objects are immutable.
 */
public class NodeJSVersionRange {
    /**
     * The left endpoint is open and is excluded from the range.
     * <p>
     * The value of {@code LEFT_OPEN} is {@code '('}.
     */
    public static final char LEFT_OPEN = '(';
    /**
     * The left endpoint is closed and is included in the range.
     * <p>
     * The value of {@code LEFT_CLOSED} is {@code '['}.
     */
    public static final char LEFT_CLOSED = '[';
    /**
     * The right endpoint is open and is excluded from the range.
     * <p>
     * The value of {@code RIGHT_OPEN} is {@code ')'}.
     */
    public static final char RIGHT_OPEN = ')';
    /**
     * The right endpoint is closed and is included in the range.
     * <p>
     * The value of {@code RIGHT_CLOSED} is {@code ']'}.
     */
    public static final char RIGHT_CLOSED = ']';

    private static final String MSG_INVALID_FORMAT = "invalid range \"{0}\": invalid format";

    private static final String LEFT_OPEN_DELIMITER = "(";
    private static final String LEFT_CLOSED_DELIMITER = "[";
    private static final String LEFT_DELIMITERS = LEFT_CLOSED_DELIMITER + LEFT_OPEN_DELIMITER;
    private static final String RIGHT_OPEN_DELIMITER = ")";
    private static final String RIGHT_CLOSED_DELIMITER = "]";
    private static final String RIGHT_DELIMITERS = RIGHT_OPEN_DELIMITER + RIGHT_CLOSED_DELIMITER;
    private static final String ENDPOINT_DELIMITER = ",";

    private final boolean leftClosed;
    private final NodeJSVersion left;
    private final NodeJSVersion right;
    private final boolean rightClosed;
    private final boolean empty;

    private transient String versionRangeString /* default to null */; // NOSONAR
    private transient int hash /* default to 0 */; // NOSONAR

    /**
     * Creates a version range from the specified string.
     *
     * <p>
     * Version range string grammar:
     *
     * <pre>
     * range ::= interval | atleast
     * interval ::= ( '[' | '(' ) left ',' right ( ']' | ')' )
     * left ::= version
     * right ::= version
     * atleast ::= version
     * </pre>
     *
     * @param range
     *            String representation of the version range. The versions in
     *            the range must contain no whitespace. Other whitespace in the
     *            range string is ignored.
     * @throws IllegalArgumentException
     *             If {@code range} is improperly formatted.
     */
    public NodeJSVersionRange(String range) {
        boolean closedLeft;
        boolean closedRight;
        NodeJSVersion endpointLeft;
        NodeJSVersion endpointRight;

        try {
            StringTokenizer st = new StringTokenizer(range, LEFT_DELIMITERS, true);
            String token = st.nextToken().trim(); // whitespace or left delim
            if (token.length() == 0) { // leading whitespace
                token = st.nextToken(); // left delim
            }
            closedLeft = LEFT_CLOSED_DELIMITER.equals(token);
            if (!closedLeft && !LEFT_OPEN_DELIMITER.equals(token)) {
                // first token is not a delimiter, so it must be "atleast"
                if (st.hasMoreTokens()) { // there must be no more tokens
                    throw new IllegalArgumentException(MessageFormat.format(MSG_INVALID_FORMAT, range));
                }
                leftClosed = true;
                rightClosed = false;
                left = NodeJSVersion.parseVersion(token);
                right = null;
                empty = false;
                return;
            }
            String version = st.nextToken(ENDPOINT_DELIMITER);
            endpointLeft = NodeJSVersion.parseVersion(version);
            token = st.nextToken(); // consume comma
            version = st.nextToken(RIGHT_DELIMITERS);
            token = st.nextToken(); // right delim
            closedRight = RIGHT_CLOSED_DELIMITER.equals(token);
            if (!closedRight && !RIGHT_OPEN_DELIMITER.equals(token)) {
                throw new IllegalArgumentException(MessageFormat.format(MSG_INVALID_FORMAT, range));
            }
            endpointRight = NodeJSVersion.parseVersion(version);

            if (st.hasMoreTokens()) { // any more tokens have to be whitespace
                token = st.nextToken("").trim();
                if (token.length() != 0) { // trailing whitespace
                    throw new IllegalArgumentException(MessageFormat.format(MSG_INVALID_FORMAT, range));
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid range \"" + range + "\": " + e.getMessage(), e);
        }

        leftClosed = closedLeft;
        rightClosed = closedRight;
        left = endpointLeft;
        right = endpointRight;
        empty = isEmpty0();
    }

    /**
     * Returns the left endpoint of this version range.
     *
     * @return The left endpoint.
     */
    public NodeJSVersion getLeft() {
        return left;
    }

    /**
     * Returns the right endpoint of this version range.
     *
     * @return The right endpoint. May be {@code null} which indicates the right
     *         endpoint is <i>Infinity</i>.
     */
    public NodeJSVersion getRight() {
        return right;
    }

    /**
     * Returns the type of the left endpoint of this version range.
     *
     * @return {@link #LEFT_CLOSED} if the left endpoint is closed or
     *         {@link #LEFT_OPEN} if the left endpoint is open.
     */
    public char getLeftType() {
        return leftClosed ? LEFT_CLOSED : LEFT_OPEN;
    }

    /**
     * Returns the type of the right endpoint of this version range.
     *
     * @return {@link #RIGHT_CLOSED} if the right endpoint is closed or
     *         {@link #RIGHT_OPEN} if the right endpoint is open.
     */
    public char getRightType() {
        return rightClosed ? RIGHT_CLOSED : RIGHT_OPEN;
    }

    /**
     * Returns whether this version range includes the specified version.
     *
     * @param version
     *            The version to test for inclusion in this version range.
     * @return {@code true} if the specified version is included in this version
     *         range; {@code false} otherwise.
     */
    public boolean includes(NodeJSVersion version) {
        if (empty) {
            return false;
        }
        if (left.compareTo(version) >= (leftClosed ? 1 : 0)) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return right.compareTo(version) >= (rightClosed ? 0 : 1);
    }

    /**
     * Returns whether this version range is empty. A version range is empty if
     * the set of versions defined by the interval is empty.
     *
     * @return {@code true} if this version range is empty; {@code false}
     *         otherwise.
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Internal isEmpty behavior.
     *
     * @return {@code true} if this version range is empty; {@code false}
     *         otherwise.
     */
    private boolean isEmpty0() {
        if (right == null) { // infinity
            return false;
        }
        int comparison = left.compareTo(right);
        if (comparison == 0) { // endpoints equal
            return !leftClosed || !rightClosed;
        }
        return comparison > 0; // true if left > right
    }

    /**
     * Returns the string representation of this version range.
     *
     * <p>
     * The format of the version range string will be a version string if the
     * right end point is <i>Infinity</i> ({@code null}) or an interval string.
     *
     * @return The string representation of this version range.
     */
    @Override
    public String toString() {
        if (versionRangeString != null) {
            return versionRangeString;
        }
        String leftVersion = left.toString();
        if (right == null) {
            StringBuilder result = new StringBuilder(leftVersion.length() + 1);
            result.append(left.toString0());
            return versionRangeString = result.toString();
        }
        String rightVerion = right.toString();
        StringBuilder result = new StringBuilder(leftVersion.length() + rightVerion.length() + 5);
        result.append(leftClosed ? LEFT_CLOSED : LEFT_OPEN);
        result.append(left.toString0());
        result.append(ENDPOINT_DELIMITER);
        result.append(right.toString0());
        result.append(rightClosed ? RIGHT_CLOSED : RIGHT_OPEN);
        return versionRangeString = result.toString();
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return An integer which is a hash code value for this object.
     */
    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        if (empty) {
            return hash = 31;
        }
        int h = 31 + (leftClosed ? 7 : 5);
        h = 31 * h + left.hashCode();
        if (right != null) {
            h = 31 * h + right.hashCode();
            h = 31 * h + (rightClosed ? 7 : 5);
        }
        return hash = h;
    }

    /**
     * Compares this {@code VersionRange} object to another object.
     *
     * <p>
     * A version range is considered to be <b>equal to </b> another version
     * range if both the endpoints and their types are equal or if both version
     * ranges are {@link #isEmpty() empty}.
     *
     * @param object
     *            The {@code VersionRange} object to be compared.
     * @return {@code true} if {@code object} is a {@code VersionRange} and is
     *         equal to this object; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) { // quicktest
            return true;
        }
        if (!(object instanceof NodeJSVersionRange)) {
            return false;
        }
        NodeJSVersionRange other = (NodeJSVersionRange) object;
        if (empty && other.empty) {
            return true;
        }
        if (right == null) {
            return (leftClosed == other.leftClosed) && (other.right == null) && left.equals(other.left);
        }
        return (leftClosed == other.leftClosed) && (rightClosed == other.rightClosed) && left.equals(other.left)
                && right.equals(other.right);
    }

}