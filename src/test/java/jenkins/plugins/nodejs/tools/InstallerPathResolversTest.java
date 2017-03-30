package jenkins.plugins.nodejs.tools;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import hudson.tools.DownloadFromUrlInstaller;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author fcamblor
 */
@RunWith(Parameterized.class)
public class InstallerPathResolversTest {
    private static Collection<String> expectedURLs;

    private DownloadFromUrlInstaller.Installable installable;
    private final Platform platform;
    private final CPU cpu;
    private final boolean testDownload = false;

    public InstallerPathResolversTest(DownloadFromUrlInstaller.Installable installable, Platform platform, CPU cpu,
            String testName) {
        this.installable = installable;
        this.platform = platform;
        this.cpu = cpu;
    }

    @Parameterized.Parameters(name = "{index}: {3}")
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> testPossibleParams = new ArrayList<Object[]>();

        try (InputStream is = InstallerPathResolversTest.class.getResourceAsStream("expectedURLs.txt")) {
            expectedURLs = new TreeSet<>(IOUtils.readLines(is));
        }

        String installablesJSONStr = Resources.toString(
                Resources.getResource("updates/jenkins.plugins.nodejs.tools.NodeJSInstaller.json"), Charsets.UTF_8);
        JSONArray installables = JSONObject.fromObject(installablesJSONStr).getJSONArray("list");
        for (int i = 0; i < installables.size(); i++) {
            DownloadFromUrlInstaller.Installable installable = (DownloadFromUrlInstaller.Installable) installables
                    .getJSONObject(i).toBean(DownloadFromUrlInstaller.Installable.class);

            // Not testing pre-0.8.6 version because at the moment, installer structure is not handled
            if (InstallerPathResolver.Factory.isVersionBlacklisted(installable.id)) {
                continue;
            }

            for (Platform platform : Platform.values()) {
                for (CPU cpu : CPU.values()) {
                    if (cpu.name().startsWith("arm") && platform != Platform.LINUX) {
                        // arm are only supported on linux
                        continue;
                    }
                    String testName = String.format("version=%s,cpu=%s,platform=%s", installable.id, cpu.name(), platform.name());
                    testPossibleParams.add(new Object[] { installable, platform, cpu, testName });
                }
            }
        }

        return testPossibleParams;
    }

    @Test
    public void shouldNodeJSInstallerResolvedPathExist() throws IOException {
        InstallerPathResolver installerPathResolver = InstallerPathResolver.Factory.findResolverFor(this.installable);
        String path;
        try {
            path = installerPathResolver.resolvePathFor(installable.id, this.platform, this.cpu);
            URL url = new URL(installable.url + path);
            if (testDownload) {
                assertDownload(url);
            } else {
                assertThat(expectedURLs, hasItem(url.toString()));
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
