package jenkins.plugins.nodejs.configfiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.jenkinsci.lib.configprovider.model.Config;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Descriptor;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;

public class NPMConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_load_template() {
        Descriptor<?> descriptor = j.jenkins.getDescriptor(NPMConfig.class);
        assertNotNull("NPMConfi descriptor not registered", descriptor);
        assertThat("Unexpected descriptor class", descriptor, instanceOf(NPMConfigProvider.class));

        NPMConfigProvider provider = (NPMConfigProvider) descriptor;
        Config config = provider.newConfig("testId");
        assertThat("Unexpected config class", config, instanceOf(NPMConfig.class));
        assertThat("Expected the default template, instead got empty", config.content, allOf(notNullValue(), is(not(""))));
    }

}