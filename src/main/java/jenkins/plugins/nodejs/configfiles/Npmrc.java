package jenkins.plugins.nodejs.configfiles;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.FileWriterWithEncoding;

/**
 * Npm config file parser.
 *
 * @author Nikolas Falco
 *
 */
public class Npmrc {
    private static final String UTF_8 = "UTF-8";

    private Map<Object, String> properties = new LinkedHashMap<>();

    public static Npmrc load(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("file " + file + " does not exists or is not file");
        }

        Path path = Paths.get(file.getAbsolutePath());
        String content = new String(Files.readAllBytes(path), UTF_8);

        Npmrc config = new Npmrc();
        config.from(content);
        return config;
    }

    public void from(String content) {
        if (content == null) {
            return;
        }

        LineIterator iterator = IOUtils.lineIterator(new StringReader(content));
        while (iterator.hasNext()) {
            String line = iterator.nextLine();
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith(";")) {
                // comment
                addComment(line.substring(1));
            } else {
                int eqIndex = line.indexOf('=');
                if (eqIndex != -1) { // NOSONAR
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    set(key, value);
                } else {
                    // parse error
                    addComment(line);
                }
            }
        }
    }

    /**
     * Add a comment line at the end of the file.
     *
     * @param comment the text content without the ';' suffix
     */
    public void addComment(String comment) {
        properties.put(new Comment() {}, comment);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Entry<Object, String> entry : properties.entrySet()) {
            String line;
            if (entry.getKey() instanceof Comment) {
                line = ';' + entry.getValue();
            } else {
                line = entry.getKey() + " = " + entry.getValue();
            }
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    public void save(File file) throws IOException {
        try (Writer writer = new FileWriterWithEncoding(file, UTF_8)) {
            IOUtils.write(toString(), writer);
        }
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Get the value for the specified property key.
     *
     * @param key
     * @return the property value
     */
    public String get(String key) {
        return properties.get(key);
    }

    /**
     * Set the value for the specified property key. If key already present it
     * will be override.
     *
     * @param key property key
     * @param value property value
     * @return the old value associated to the property key, <code>null</code>
     *         otherwise
     */
    public String set(String key, String value) {
        return properties.put(key, value);
    }

    /**
     * Set the value for the specified property key. If key already present it
     * will be override.
     *
     * @param key property key
     * @param value property value
     * @return {@code false} the old value associated to the property key,
     *         {@code true} otherwise
     */
    public boolean set(String key, boolean value) {
        return Boolean.parseBoolean(properties.put(key, Boolean.toString(value)));
    }

    /**
     * Marker interface.
     * <p>
     * This class is intended to avoid collision if a special entry key was
     * choose to represent comment in the map.
     *
     * @author Nikolas Falco
     *
     */
    private interface Comment {
    }
}
