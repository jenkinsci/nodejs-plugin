package jenkins.plugins.nodejs.configfiles;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NPMConfigValidationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    @Test
    public void test_empty_URL() throws Exception {
        NPMRegistry registry = new NPMRegistry("", null, null);

        thrown.expect(VerifyConfigProviderException.class);

        NPMConfig config = new NPMConfig("empty_URL", null, null, null, "myprovider", Arrays.asList(registry));
        config.doVerify();
    }

    @Test
    public void test_no_exception_if_URL_has_variable() throws Exception {
        NPMRegistry registry = new NPMRegistry("${URL}", null, null);

        NPMConfig config = new NPMConfig("no_exception_if_URL_has_variable", null, null, null, "myprovider", Arrays.asList(registry));
        config.doVerify();
    }

}