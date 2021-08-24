/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs.configfiles;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

/**
 * Npm config file parser.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class Npmrc {
    private static final String UTF_8 = "UTF-8";

    private Map<Object, String> properties = new LinkedHashMap<>();

    /**
     * Parse the given file and store internally all user settings and
     * comments.
     *
     * @param file a valid npmrc user config file content.
     * @return the instance of parsed user config.
     * @throws IOException in case of I/O failure during file read
     */
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

    /**
     * Parse the given content and store internally all user settings and
     * comments.
     *
     * @param content a valid npmrc user config content.
     */
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
     * @param comment the text content without the ';' prefix
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

    /**
     * Write the content of user config to a file.
     *
     * @param file the destination file
     * @throws IOException in case of I/O write error
     */
    public void save(File file) throws IOException {
        FileUtils.writeStringToFile(file, toString(), UTF_8);
    }

    /**
     * Returns {@literal true} if this map contains a user config for the
     * specified key.
     *
     * @param key user setting whose presence in this config
     * @return {@literal true} if this config already contains the specified key
     */
    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    /**
     * Get the value for the specified property key.
     *
     * @param key user config entry key
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
     * @return the old value associated to the setting key, {@literal null}
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
     * @return {@literal false} the old value associated to the property key,
     *         {@literal true} otherwise
     */
    public boolean set(String key, boolean value) {
        return Boolean.parseBoolean(properties.put(key, Boolean.toString(value)));
    }

    /**
     * Get the value for the specified property key as a boolean.
     *
     * @param key user config entry key
     * @return a boolean represented by the property value or {@literal null} if
     *         the key doesn't exist or the value associated is empty.
     */
    public Boolean getAsBoolean(String key) {
        Boolean result = null;
        if (contains(key)) {
            result = Boolean.valueOf(properties.get(key));
        }
        return result;
    }

    /**
     * Get the value for the specified property key as a number.
     *
     * @param key user config entry key
     * @return an integer represented by the property value or {@literal null}
     *         if the key doesn't exist or the value associated is empty.
     */
    public Integer getAsNumber(String key) {
        Integer result = null;
        if (contains(key)) {
            result = Integer.valueOf(properties.get(key));
        }
        return result;
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