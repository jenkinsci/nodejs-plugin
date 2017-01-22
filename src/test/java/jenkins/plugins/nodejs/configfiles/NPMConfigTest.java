package jenkins.plugins.nodejs.configfiles;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.jenkinsci.lib.configprovider.model.Config;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Descriptor;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;

public class NPMConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    @Test
    public void test_new_config() {
        String id = "test_id";
        NPMConfig config = new NPMConfig(id, "", "", "", "myprovider", null);
        assertEquals(id, config.id);
        assertNull(config.name);
        assertNull(config.comment);
        assertNull(config.content);
        assertNotNull(config.getRegistries());
    }

    @Test
    public void test_too_many_global_registries() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", null, null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, null);

        thrown.expect(VerifyConfigProviderException.class);

        NPMConfig config = new NPMConfig("too_many_registry", null, null, null, "myprovider", Arrays.asList(privateRegistry, officalRegistry));
        config.doVerify();
    }

}