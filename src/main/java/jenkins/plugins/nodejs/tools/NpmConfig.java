package jenkins.plugins.nodejs.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

/**
 * Npm config file parser.
 * 
 * @author Nikolas Falco
 *
 */
public class NpmConfig {

    private static final String UTF_8 = "UTF-8";

    private File file;
    private Map<Object, String> properties;

    public static NpmConfig load(File file) throws IOException {
        NpmConfig npmConfig = new NpmConfig(file);
        npmConfig.load();
        return npmConfig;
    }

    public NpmConfig(File file) {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("file " + file + " does not exists");
        }

        this.file = file;
        this.properties = new LinkedHashMap<Object, String>();
    }

    public void load() throws IOException {
        properties.clear();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            for (String line : IOUtils.readLines(fis, UTF_8)) {
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
                        properties.put(key, value);
                    } else {
                        // parse error
                        addComment(line);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(fis);
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

    public void save() throws IOException {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(file);
            for (Entry<Object, String> entry : properties.entrySet()) {
                String line;
                if (entry.getKey() instanceof Comment) {
                    line = ';' + entry.getValue();
                } else {
                    line = entry.getKey() + " = " + entry.getValue();
                }
                IOUtils.writeLines(Arrays.asList(line), null, writer, UTF_8);
            }
        } finally {
            IOUtils.closeQuietly(writer);
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
