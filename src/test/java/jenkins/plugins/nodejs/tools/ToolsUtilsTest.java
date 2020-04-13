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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import hudson.model.Node;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CPU.class)
public class ToolsUtilsTest {

    @Before
    public void setup() {
        CPU[] cpuValues = CPU.values();
        CPU mock = PowerMockito.mock(CPU.class);
        for (CPU c : cpuValues) {
            Whitebox.setInternalState(mock, "name", c.name());
            Whitebox.setInternalState(mock, "ordinal", c.ordinal());
        }

        PowerMockito.mockStatic(CPU.class);
        PowerMockito.when(CPU.values()).thenReturn(cpuValues);
    }

    @Test
    public void nodejs_supports_32bit_64bit_on_windows_linux_mac() throws Exception {
        Node currentNode = mock(Node.class);
        
        when(CPU.of(currentNode)).thenReturn(CPU.amd64);
        CPU cpu = ToolsUtils.getCPU(currentNode, true);
        Assertions.assertThat(cpu).isEqualTo(CPU.i386);

        cpu = ToolsUtils.getCPU(currentNode);
        Assertions.assertThat(cpu).isEqualTo(CPU.amd64);
    }

    @Test(expected = DetectionFailedException.class)
    public void nodejs_doesn_t_supports_32bit_on_armv64() throws Exception {
        Node currentNode = mock(Node.class);

        when(CPU.of(currentNode)).thenReturn(CPU.arm64);
        ToolsUtils.getCPU(currentNode, true);
    }

    @Test
    public void nodejs_supports_32bit_on_armv6_armv7() throws Exception {
        Node currentNode = mock(Node.class);

        when(CPU.of(currentNode)).thenReturn(CPU.armv7l);
        CPU cpu = ToolsUtils.getCPU(currentNode, true);
        Assertions.assertThat(cpu).isEqualTo(CPU.armv7l);

        when(CPU.of(currentNode)).thenReturn(CPU.armv6l);
        cpu = ToolsUtils.getCPU(currentNode, true);
        Assertions.assertThat(cpu).isEqualTo(CPU.armv6l);
    }

}