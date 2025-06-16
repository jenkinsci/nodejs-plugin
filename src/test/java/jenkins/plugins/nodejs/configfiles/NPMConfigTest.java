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

import org.jenkinsci.lib.configprovider.model.Config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Descriptor;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NPMConfigTest {

    @Test
    void test_load_template(JenkinsRule j) {
        Descriptor<?> descriptor = j.jenkins.getDescriptor(NPMConfig.class);
        assertThat(descriptor)
                .as("NPMConfig descriptor not registered").isNotNull()
                .as("Unexpected descriptor class").isInstanceOf(NPMConfigProvider.class);

        NPMConfigProvider provider = (NPMConfigProvider) descriptor;
        Config config = provider.newConfig("testId");
        assertThat(config).as("Unexpected config class").isInstanceOf(NPMConfig.class);
        assertThat(config.content).as("Expected the default template, instead got empty").isNotBlank();
    }

}