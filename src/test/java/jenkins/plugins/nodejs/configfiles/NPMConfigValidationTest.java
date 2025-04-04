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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class NPMConfigValidationTest {

    @Test
    void test_new_config() {
        String id = "test_id";
        NPMConfig config = new NPMConfig(id, "", "", "", null);
        assertEquals(id, config.id);
        assertNull(config.name);
        assertNull(config.comment);
        assertNull(config.content);
        assertNotNull(config.getRegistries());
    }

    @Test
    void test_too_many_global_registries() {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", null, null);
        NPMRegistry officialRegistry = new NPMRegistry("https://registry.npmjs.org/", null, null);

        NPMConfig config = new NPMConfig("too_many_registry", null, null, null, Arrays.asList(privateRegistry, officialRegistry));

        assertThatExceptionOfType(VerifyConfigProviderException.class) //
            .isThrownBy(config::doVerify);
    }

    @Test
    void test_empty_URL() {
        NPMRegistry registry = new NPMRegistry("", null, null);

        NPMConfig config = new NPMConfig("empty_URL", null, null, null, List.of(registry));

        assertThatExceptionOfType(VerifyConfigProviderException.class) //
            .isThrownBy(config::doVerify);
    }

    @Test
    void test_no_exception_if_URL_has_variable() throws Exception {
        NPMRegistry registry = new NPMRegistry("${URL}", null, null);

        NPMConfig config = new NPMConfig("no_exception_if_URL_has_variable", null, null, null, List.of(registry));
        config.doVerify();
    }

}