package jenkins.plugins.nodejs.tools;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

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
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;

@RunWith(Parameterized.class)
public class NodeJSInstallerProxyTest {

    @Parameters(name = "proxy url = {0}")
    public static String[][] data() throws MalformedURLException {
        return new String[][] { { "http://proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" },
            { "http://user:password@proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" }
        };
    }

    @Rule
    public JenkinsRule r = new JenkinsRule();
    private String host;
    private int port;
    private String username;
    private String password;
    private String expectedURL;
    private TaskListener log;
    private String noProxy;

    public NodeJSInstallerProxyTest(String url, String noProxy) throws Exception {
        URL proxyURL = new URL(url);

        this.log = new StreamBuildListener(System.out, Charset.defaultCharset());
        this.expectedURL = url;
        this.noProxy = noProxy;
        this.host = proxyURL.getHost();
        this.port = proxyURL.getPort();
        if (proxyURL.getUserInfo() != null) {
            String[] userInfo = proxyURL.getUserInfo().split(":");
            this.username = userInfo[0];
            this.password = userInfo[1];
        }
    }

    @Issue("JENKINS-29266")
    @Test
    public void test_proxy_settings() throws Exception {
        ProxyConfiguration proxycfg = new ProxyConfiguration(host, port, username, password);
        proxycfg.save();

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Whitebox.invokeMethod(installer, "buildProxyEnvVars", env, log);

        assertThat(env.keySet(), hasItems("HTTP_PROXY", "HTTPS_PROXY"));
        assertThat(env.get("HTTP_PROXY"), is(expectedURL));
        assertThat(env.get("HTTPS_PROXY"), is(expectedURL));
        assertThat(env.keySet(), not(hasItem("NO_PROXY")));
    }

    @Test
    public void test_no_proxy_settings() throws Exception {
        ProxyConfiguration proxycfg = new ProxyConfiguration(host, port, username, password, noProxy);
        proxycfg.save();

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Whitebox.invokeMethod(installer, "buildProxyEnvVars", env, log);

        assertThat(env.keySet(), hasItems("HTTP_PROXY", "HTTPS_PROXY"));
        assertThat(env.get("NO_PROXY"), is("*.npm.org,registry.npm.org"));
    }

}