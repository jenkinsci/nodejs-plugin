package jenkins.plugins.nodejs.tools;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.powermock.reflect.Whitebox;

import hudson.EnvVars;
import hudson.ProxyConfiguration;

@Issue("JENKINS-29266")
@RunWith(Parameterized.class)
public class NodeJSInstallerProxyTest {

    @Parameters(name = "proxy url = {0}")
    public static String[] data() throws MalformedURLException {
        return new String[] { "http://proxy.example.org:8080", "http://user:password@proxy.example.org:8080" };
    }

    @Rule
    public JenkinsRule r = new JenkinsRule();
    private String host;
    private int port;
    private String username;
    private String password;
    private String expectedURL;

    public NodeJSInstallerProxyTest(String url) throws Exception {
        URL proxyURL = new URL(url);

        this.expectedURL = url;
        this.host = proxyURL.getHost();
        this.port = proxyURL.getPort();
        if (proxyURL.getUserInfo() != null) {
            String[] userInfo = proxyURL.getUserInfo().split(":");
            this.username = userInfo[0];
            this.password = userInfo[1];
        }
    }

    @Test
    public void test_proxy_settings() throws Exception {
        ProxyConfiguration proxycfg = new ProxyConfiguration(host, port, username, password);
        proxycfg.save();

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Whitebox.invokeMethod(installer, "buildProxyEnvVars", env);

        assertThat(env.keySet(), CoreMatchers.hasItems("HTTP_PROXY", "HTTPS_PROXY"));
        assertThat(env.get("HTTP_PROXY"), CoreMatchers.is(expectedURL));
        assertThat(env.get("HTTPS_PROXY"), CoreMatchers.is(expectedURL));
    }

}