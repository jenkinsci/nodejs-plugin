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

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class NodeJSInstallerProxyTest {

    private static JenkinsRule r;

    private String host;
    private int port;
    private String username;
    private String password;
    private String expectedURL;
    private TaskListener log;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        r = rule;
    }

    static String[][] data() {
        return new String[][] {
            { "http://proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" },
            { "http://user:password@proxy.example.org:8080", "*.npm.org\n\nregistry.npm.org" }
        };
    }

    private void init(String url) throws Exception {
        URL proxyURL = new URL(url);

        this.log = new StreamBuildListener(System.out, Charset.defaultCharset());
        this.expectedURL = url;
        this.host = proxyURL.getHost();
        this.port = proxyURL.getPort();
        if (proxyURL.getUserInfo() != null) {
            String[] userInfo = proxyURL.getUserInfo().split(":");
            this.username = userInfo[0];
            this.password = userInfo[1];
        }
    }

    @Issue("JENKINS-29266")
    @ParameterizedTest(name = "proxy url = {0}")
    @MethodSource("data")
    void test_proxy_settings(String url, String noProxy) throws Exception {
        init(url);

        r.getInstance().proxy = new ProxyConfiguration(host, port, username, password);

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Method method = installer.getClass().getDeclaredMethod("buildProxyEnvVars", EnvVars.class, TaskListener.class);
        method.setAccessible(true);
        method.invoke(installer, env, log);

        assertThat(env.keySet()).contains("HTTP_PROXY", "HTTPS_PROXY");
        assertThat(env.get("HTTP_PROXY")).isEqualTo(expectedURL);
        assertThat(env.get("HTTPS_PROXY")).isEqualTo(expectedURL);
        assertThat(env.keySet()).doesNotContain("NO_PROXY");
    }

    @ParameterizedTest(name = "proxy url = {0}")
    @MethodSource("data")
    void test_no_proxy_settings(String url, String noProxy) throws Exception {
        init(url);

        r.getInstance().proxy = new ProxyConfiguration(host, port, username, password, noProxy);

        NodeJSInstaller installer = new NodeJSInstaller("test-id", "grunt", NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS);
        EnvVars env = new EnvVars();
        Method method = installer.getClass().getDeclaredMethod("buildProxyEnvVars", EnvVars.class, TaskListener.class);
        method.setAccessible(true);
        method.invoke(installer, env, log);

        assertThat(env.keySet()).contains("HTTP_PROXY", "HTTPS_PROXY");
        assertThat(env.get("NO_PROXY")).isEqualTo("*.npm.org,registry.npm.org");
    }

}