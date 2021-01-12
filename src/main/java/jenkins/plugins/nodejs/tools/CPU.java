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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.output.NullOutputStream;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import hudson.util.StreamTaskListener;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.nodejs.Messages;

/**
 * CPU type.
 */
public enum CPU {
    i386, amd64, armv7l, armv6l, arm64, ppc64;

    /**
     * Determines the CPU of the given node.
     *
     * @param node
     *            the computer node
     * @return a CPU value of the architecture of the given node
     * @throws DetectionFailedException
     *             when the current CPU node is not supported.
     */
    public static CPU of(@Nonnull Node node) throws DetectionFailedException {
        try {
            Computer computer = node.toComputer();
            if (computer == null) {
                throw new DetectionFailedException(Messages.SystemTools_nodeNotAvailable(node.getDisplayName()));
            }
            return detect(computer, computer.getSystemProperties());
        } catch (IOException | InterruptedException e) {
            throw new DetectionFailedException(Messages.SystemTools_failureOnProperties(), e);
        }
    }

    /**
     * Determines the CPU of the current JVM.
     *
     * @return the current CPU
     * @throws DetectionFailedException
     *             when the current platform node is not supported.
     */
    public static CPU current() throws DetectionFailedException {
        return detect(null, System.getProperties());
    }

    private static CPU detect(@Nullable Computer computer, Map<Object, Object> systemProperties) throws DetectionFailedException {
        String arch = ((String) systemProperties.get("os.arch")).toLowerCase(Locale.ENGLISH);
        if (arch.contains("amd64") || arch.contains("86_64")) {
            return amd64;
        }
        if (arch.contains("86")) {
            return i386;
        }
        if (arch.contains("arm")) {
            // try to get the specific architecture of arm CPU
            try {
                FilePath rootPath = new FilePath((computer != null ? computer.getChannel() : null), "/");
                arch = rootPath.act(new ArchitectureCallable());
            } catch (IOException | InterruptedException e) {
                throw new DetectionFailedException(Messages.CPU_unknown(arch), e);
            }
            switch (arch) {
            case "armv7l":
                return armv7l;
            case "armv6l":
                return armv6l;
            case "arm64":
            case "aarch64":
                return arm64;
            }
        }
        if (arch.contains("ppc")) {
            return ppc64;
        }
        throw new DetectionFailedException(Messages.CPU_unknown(arch));
    }

    /**
     * Returns the machine hardware name for the current Linux computer.
     *
     * @author Nikolas Falco
     */
    /* package */static class ArchitectureCallable extends MasterToSlaveFileCallable<String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            Charset charset = Charset.defaultCharset();

            FilePath basePath = new FilePath(f);
            Launcher launcher = basePath.createLauncher(new StreamTaskListener(new NullOutputStream(), charset));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Proc starter = launcher.launch().cmdAsSingleString("uname -m").stdout(baos).start();
            int exitCode = starter.join();
            if (exitCode != 0) {
                throw new IOException("Fail to execute 'uname -m' because: " + baos.toString(charset.name()));
            }

            return new String(baos.toByteArray(), charset).trim();
        }
    };

}