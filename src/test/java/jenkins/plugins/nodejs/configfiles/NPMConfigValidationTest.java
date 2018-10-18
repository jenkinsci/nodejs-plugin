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