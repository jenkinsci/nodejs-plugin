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
package jenkins.plugins.nodejs.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import hudson.tools.DownloadFromUrlInstaller;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

class InstallerPathResolversTest {
    private static Collection<String> expectedURLs;

    private final boolean testDownload = false;
    private final boolean showDownloadURL = false;

    static Collection<Object[]> data() throws Exception {
        Collection<Object[]> testPossibleParams = new ArrayList<>();

        try (InputStream is = InstallerPathResolversTest.class.getResourceAsStream("expectedURLs.txt")) {
            expectedURLs = new TreeSet<>(IOUtils.readLines(is, StandardCharsets.UTF_8));
        }

        String installablesJSONStr = Resources.toString(Resources.getResource("updates/jenkins.plugins.nodejs.tools.NodeJSInstaller.json"), Charsets.UTF_8);
        JSONArray installables = JSONObject.fromObject(installablesJSONStr).getJSONArray("list");
        for (int i = 0; i < installables.size(); i++) {
            DownloadFromUrlInstaller.Installable installable = (DownloadFromUrlInstaller.Installable) installables.getJSONObject(i).toBean(DownloadFromUrlInstaller.Installable.class);

            // Not testing pre-0.8.6 version because at the moment, installer
            // structure is not handled
            if (InstallerPathResolver.Factory.isVersionBlacklisted(installable.id)) {
                continue;
            }

            for (Platform platform : Platform.values()) {
                for (CPU cpu : CPU.values()) {
                    if (cpu.name().equals("arm64") && (platform == Platform.WINDOWS || platform == Platform.SUNOS)) {
                        // arm64 are only supported on linux and OSX
                        continue;
                    }
                    if ((cpu.name().equals("armv6l") || cpu.name().equals("armv7l")) && platform != Platform.LINUX) {
                        // armXl are only supported on linux
                        continue;
                    }
                    if (platform == Platform.AIX && !cpu.name().equals("ppc64")) {
                        // AIX only supports ppc64
                        continue;
                    }
                    String testName = String.format("version=%s,cpu=%s,platform=%s", installable.id, cpu.name(), platform.name());
                    testPossibleParams.add(new Object[] { installable, platform, cpu, testName });
                }
            }
        }

        return testPossibleParams;
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("data")
    void shouldNodeJSInstallerResolvedPathExist(DownloadFromUrlInstaller.Installable installable, Platform platform, CPU cpu, String testName) throws Exception {
        InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(installable.id);
        try {
            String path = installerPathResolver.resolvePathFor(installable.id, platform, cpu);
            URL url = new URL(installable.url + path);

            if (testDownload) {
                assertDownload(url);
            } else {
                assertThat(expectedURLs).contains(url.toString());
            }

            if (showDownloadURL) {
                System.out.println(url);
            }
        } catch (IllegalArgumentException e) {
            // some combo of platform and cpu are not supported by nodejs
        }
    }

    private void assertDownload(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(2000);
            urlConnection.connect();
            int code = urlConnection.getResponseCode();
            assertTrue(code >= 200 && code < 300);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
