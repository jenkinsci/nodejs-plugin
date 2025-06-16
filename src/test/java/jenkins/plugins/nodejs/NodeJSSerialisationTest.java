/*
 * The MIT License
 *
 * Copyright (c) 2019, Nikolas Falco
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
package jenkins.plugins.nodejs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.FreeStyleProject;

import jenkins.plugins.nodejs.cache.DefaultCacheLocationLocator;
import jenkins.plugins.nodejs.cache.PerJobCacheLocationLocator;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
class NodeJSSerialisationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    /**
     * Verify that the serialisation is backward compatible.
     */
    @LocalData
    @Test
    @Issue("JENKINS-57844")
    void test_serialisation_is_compatible_with_version_1_2_x_interpreter() {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSCommandInterpreter step = prj.getBuildersList().get(NodeJSCommandInterpreter.class);
        assertThat(step.getCacheLocationStrategy()).isInstanceOf(DefaultCacheLocationLocator.class);
    }

    /**
     * Verify reloading jenkins job configuration use the saved cache strategy instead reset to default.
     */
    @LocalData
    @Test
    @Issue("JENKINS-58029")
    void test_reloading_job_configuration_contains_saved_cache_strategy_interpreter() {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSCommandInterpreter step = prj.getBuildersList().get(NodeJSCommandInterpreter.class);
        assertThat(step.getCacheLocationStrategy()).isInstanceOf(PerJobCacheLocationLocator.class);
    }

    /**
     * Verify that the serialisation is backward compatible.
     */
    @LocalData
    @Test
    @Issue("JENKINS-57844")
    void test_serialisation_is_compatible_with_version_1_2_x_buildWrapper() {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSBuildWrapper step = prj.getBuildWrappersList().get(NodeJSBuildWrapper.class);
        assertThat(step.getCacheLocationStrategy()).isInstanceOf(DefaultCacheLocationLocator.class);
    }

    /**
     * Verify reloading jenkins job configuration use the saved cache strategy instead reset to default.
     */
    @LocalData
    @Test
    @Issue("JENKINS-58029")
    void test_reloading_job_configuration_contains_saved_cache_strategy_buildWrapper() {
        FreeStyleProject prj = j.jenkins.getAllItems(hudson.model.FreeStyleProject.class) //
                .stream() //
                .filter(p -> "test".equals(p.getName())) //
                .findFirst().get();

        NodeJSBuildWrapper step = prj.getBuildWrappersList().get(NodeJSBuildWrapper.class);
        assertThat(step.getCacheLocationStrategy()).isInstanceOf(PerJobCacheLocationLocator.class);
    }

}