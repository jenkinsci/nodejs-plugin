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
package jenkins.plugins.nodejs;


import java.io.File;
import java.io.IOException;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;

import static org.assertj.core.api.Assertions.assertThat;

abstract class VerifyEnvVariableBuilder extends Builder {
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        verify(env);
        return true;
    }

    public abstract void verify(EnvVars env);

    public static final class FileVerifier extends VerifyEnvVariableBuilder {
        @Override
        public void verify(EnvVars env) {
            String var = NodeJSConstants.NPM_USERCONFIG;
            String value = env.get(var);

            assertThat(env).as("variable " + var + " not set").containsKey(var);
            assertThat(value).as("empty value for environment variable " + var).isNotNull();
            assertThat(new File(value)).as("file of variable " + var + " does not exists or is not a file").isFile();
        }
    }

    public static final class EnvVarVerifier extends VerifyEnvVariableBuilder {

        private final String name;
        private final String value;

        public EnvVarVerifier(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public void verify(EnvVars env) {
            assertThat(env).as("variable " + name + " not set").containsEntry(name, value);
        }
    }
}