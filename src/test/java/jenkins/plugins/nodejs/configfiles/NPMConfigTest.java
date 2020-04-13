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

import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.Assertions;
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
        assertNotNull("NPMConfig descriptor not registered", descriptor);
        Assertions.assertThat(descriptor).isInstanceOf(NPMConfigProvider.class).describedAs("Unexpected descriptor class");

        NPMConfigProvider provider = (NPMConfigProvider) descriptor;
        Config config = provider.newConfig("testId");
        Assertions.assertThat(config).isInstanceOf(NPMConfig.class).describedAs("Unexpected config class");
        Assertions.assertThat(config.content).isNotBlank().describedAs("Expected the default template, instead got empty");
    }

}